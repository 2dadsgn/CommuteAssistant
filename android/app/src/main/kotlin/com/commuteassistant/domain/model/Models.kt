package com.commuteassistant.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Represents the user's saved commute routine for a specific day.
 */
data class CommuteRoutine(
    val id: Long = 0,
    val dayOfWeek: DayOfWeek,
    val usualDepartureTime: LocalTime,
    val originLat: Double,
    val originLng: Double,
    val originName: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val destinationName: String,
    val isActive: Boolean = true,
    val isNotificationEnabled: Boolean = true,
    val isPriorityAlert: Boolean = false,
    val notificationOffsetMins: Int = 15,
    val notificationCount: Int = 1
)

/**
 * A traffic snapshot for a specific route at a point in time.
 */
data class TrafficSnapshot(
    val id: Long = 0,
    val routineId: Long,
    val capturedAt: Long,               // epoch millis
    val durationMinutes: Int,           // actual travel time
    val normalDurationMinutes: Int,     // free-flow reference
    val congestionLevel: CongestionLevel
)

enum class CongestionLevel(val label: String, val color: Long) {
    FREE("Free flow", 0xFF4CAF50),
    LIGHT("Light traffic", 0xFF8BC34A),
    MODERATE("Moderate", 0xFFFFC107),
    HEAVY("Heavy traffic", 0xFFFF5722),
    STANDSTILL("Standstill", 0xFFF44336)
}

/**
 * A departure recommendation built from historical + live traffic data.
 */
data class DepartureRecommendation(
    val routineId: Long,
    val recommendedDepartureTime: LocalTime,
    val estimatedTravelMinutes: Int,
    val confidencePercent: Int,         // 0-100
    val reason: String,
    val alternativeTimes: List<LocalTime> = emptyList()
)
