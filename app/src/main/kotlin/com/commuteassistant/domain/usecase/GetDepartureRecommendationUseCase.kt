package com.commuteassistant.domain.usecase

import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.model.*
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
    private val repository: CommuteRepository
) {
    companion object {
        private const val ARRIVAL_BUFFER_MINUTES = 5
        private const val HEAVY_TRAFFIC_THRESHOLD = 1.4   // 40 % above normal = heavy
        private const val MODERATE_TRAFFIC_THRESHOLD = 1.2
    }

    suspend operator fun invoke(routine: CommuteRoutine): DepartureRecommendation {
        val recentSnapshots = repository.getRecentSnapshots(routine.id, days = 14)
        val avgHistorical = repository.getAverageDuration(routine.id)

        // Estimate travel time: prefer historical average, fall back to 30 min
        val baseMinutes = avgHistorical?.toInt() ?: 30

        // Determine congestion from most recent snapshot (if available)
        val latestSnapshot = recentSnapshots.firstOrNull()
        val congestion = latestSnapshot?.congestionLevel ?: CongestionLevel.FREE

        val penalty = when (congestion) {
            CongestionLevel.STANDSTILL -> 25
            CongestionLevel.HEAVY      -> 15
            CongestionLevel.MODERATE   -> 8
            CongestionLevel.LIGHT      -> 3
            CongestionLevel.FREE       -> 0
        }

        val estimatedMinutes = baseMinutes + penalty + ARRIVAL_BUFFER_MINUTES

        val recommended = routine.usualDepartureTime
            .minusMinutes(penalty.toLong())
            .minusMinutes(ARRIVAL_BUFFER_MINUTES.toLong())

        val confidence = when {
            recentSnapshots.size >= 10 -> 90
            recentSnapshots.size >= 5  -> 75
            recentSnapshots.size >= 1  -> 55
            else                       -> 35   // no data yet
        }

        val reason = buildReason(congestion, penalty, recentSnapshots.size)

        // Offer two alternatives: 10 min earlier and 15 min later
        val alternatives = listOf(
            recommended.minusMinutes(10),
            recommended.plusMinutes(15)
        )

        return DepartureRecommendation(
            routineId = routine.id,
            recommendedDepartureTime = recommended,
            estimatedTravelMinutes = estimatedMinutes,
            confidencePercent = confidence,
            reason = reason,
            alternativeTimes = alternatives
        )
    }

    private fun buildReason(
        congestion: CongestionLevel,
        penalty: Int,
        dataPoints: Int
    ): String = buildString {
        when (congestion) {
            CongestionLevel.FREE       -> append("Traffic looks clear.")
            CongestionLevel.LIGHT      -> append("Light traffic expected.")
            CongestionLevel.MODERATE   -> append("Moderate congestion — adding ${penalty} min buffer.")
            CongestionLevel.HEAVY      -> append("Heavy traffic detected — leaving earlier is strongly recommended.")
            CongestionLevel.STANDSTILL -> append("Severe congestion! Consider delaying or using alternate routes.")
        }
        if (dataPoints > 0) {
            append(" Based on $dataPoints recent trips.")
        } else {
            append(" No historical data yet — confidence will improve over time.")
        }
    }
}
