package com.commuteassistant.domain.usecase

import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.model.*
import com.commuteassistant.data.GoogleMapsApiService
import com.commuteassistant.data.ApiKeyProvider
import android.util.Log
import java.time.LocalTime
import javax.inject.Inject

/**
 * Builds a departure recommendation by combining:
 *  1. Historical average travel time for the routine
 *  2. A desired arrival buffer (5 min by default)
 *  3. A congestion penalty derived from recent snapshots
 *
 *  The algorithm is intentionally simple so you can plug in a real
 *  traffic API (Google Maps Routes API, TomTom, HERE) and replace
 *  [fetchLiveTrafficMinutes] without touching the rest.
 */
class GetDepartureRecommendationUseCase @Inject constructor(
    private val repository: CommuteRepository,
    private val apiService: GoogleMapsApiService,
    private val apiKeyProvider: ApiKeyProvider
) {
    companion object {
        private const val ARRIVAL_BUFFER_MINUTES = 5
        private const val HEAVY_TRAFFIC_THRESHOLD = 1.4   // 40 % above normal = heavy
        private const val MODERATE_TRAFFIC_THRESHOLD = 1.2
    }

    suspend operator fun invoke(routine: CommuteRoutine): DepartureRecommendation {
        val recentSnapshots = repository.getRecentSnapshots(routine.id, days = 14)
        val avgHistorical = repository.getAverageDuration(routine.id)

        // Attempt to fetch live traffic
        var liveTrafficMinutes: Int? = null
        var normalDurationMinutes: Int? = null
        try {
            val key = apiKeyProvider.getMapsApiKey()
            if (key.isNotEmpty()) {
                val response = apiService.getDirections(
                    origin = "${routine.originLat},${routine.originLng}",
                    destination = "${routine.destinationLat},${routine.destinationLng}",
                    apiKey = key
                )
                val leg = response.routes?.firstOrNull()?.legs?.firstOrNull()
                liveTrafficMinutes = leg?.durationInTraffic?.value?.let { (it / 60).toInt() }
                normalDurationMinutes = leg?.duration?.value?.let { (it / 60).toInt() }
            }
        } catch (e: Exception) {
            Log.e("TrafficCheck", "Failed to fetch directions", e)
        }

        val baseMinutes = normalDurationMinutes ?: avgHistorical?.toInt() ?: 30
        var totalEstimatedMinutes: Int
        var penalty: Int
        var congestion: CongestionLevel

        if (liveTrafficMinutes != null) {
            totalEstimatedMinutes = liveTrafficMinutes + ARRIVAL_BUFFER_MINUTES
            penalty = liveTrafficMinutes - baseMinutes
            if (penalty < 0) penalty = 0
            
            congestion = when {
                penalty >= 20 -> CongestionLevel.STANDSTILL
                penalty >= 10 -> CongestionLevel.HEAVY
                penalty >= 5  -> CongestionLevel.MODERATE
                penalty > 0   -> CongestionLevel.LIGHT
                else          -> CongestionLevel.FREE
            }
        } else {
            val latestSnapshot = recentSnapshots.firstOrNull()
            congestion = latestSnapshot?.congestionLevel ?: CongestionLevel.FREE

            penalty = when (congestion) {
                CongestionLevel.STANDSTILL -> 25
                CongestionLevel.HEAVY      -> 15
                CongestionLevel.MODERATE   -> 8
                CongestionLevel.LIGHT      -> 3
                CongestionLevel.FREE       -> 0
            }
            totalEstimatedMinutes = baseMinutes + penalty + ARRIVAL_BUFFER_MINUTES
        }

        val recommended = routine.usualDepartureTime
            .minusMinutes(totalEstimatedMinutes.toLong() - ARRIVAL_BUFFER_MINUTES.toLong())
            .minusMinutes(ARRIVAL_BUFFER_MINUTES.toLong())

        val confidence = if (liveTrafficMinutes != null) 99 else when {
            recentSnapshots.size >= 10 -> 90
            recentSnapshots.size >= 5  -> 75
            recentSnapshots.size >= 1  -> 55
            else                       -> 35   // no data yet
        }

        val dataPointsText = if (liveTrafficMinutes != null) {
            "Live traffic data from Google Maps."
        } else if (recentSnapshots.size > 0) {
            "Based on ${recentSnapshots.size} recent trips."
        } else {
            "No historical data yet."
        }

        val reason = buildReason(congestion, penalty, dataPointsText)

        // Offer two alternatives: 10 min earlier and 15 min later
        val alternatives = listOf(
            recommended.minusMinutes(10),
            recommended.plusMinutes(15)
        )

        return DepartureRecommendation(
            routineId = routine.id,
            recommendedDepartureTime = recommended,
            estimatedTravelMinutes = totalEstimatedMinutes,
            confidencePercent = confidence,
            reason = reason,
            alternativeTimes = alternatives
        )
    }

    private fun buildReason(
        congestion: CongestionLevel,
        penalty: Int,
        dataPointsText: String
    ): String = buildString {
        when (congestion) {
            CongestionLevel.FREE       -> append("Traffic looks clear.")
            CongestionLevel.LIGHT      -> append("Light traffic expected.")
            CongestionLevel.MODERATE   -> append("Moderate congestion — adding ${penalty} min buffer.")
            CongestionLevel.HEAVY      -> append("Heavy traffic detected — leaving earlier is strongly recommended.")
            CongestionLevel.STANDSTILL -> append("Severe congestion! Consider delaying or using alternate routes.")
        }
        append(" $dataPointsText")
    }
}
