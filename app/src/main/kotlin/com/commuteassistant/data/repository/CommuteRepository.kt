package com.commuteassistant.data.repository

import com.commuteassistant.data.db.*
import com.commuteassistant.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommuteRepository @Inject constructor(
    private val routineDao: CommuteRoutineDao,
    private val snapshotDao: TrafficSnapshotDao
) {

    // ── Routines ──────────────────────────────────────────────────────────────

    fun observeRoutines(): Flow<List<CommuteRoutine>> =
        routineDao.observeActiveRoutines().map { list -> list.map { it.toDomain() } }

    suspend fun getRoutineById(id: Long): CommuteRoutine? =
        routineDao.getById(id)?.toDomain()

    suspend fun getTodayRoutines(): List<CommuteRoutine> {
        val today = java.time.LocalDate.now().dayOfWeek.value
        return routineDao.getByDay(today).map { it.toDomain() }
    }

    suspend fun saveRoutine(routine: CommuteRoutine): Long =
        routineDao.upsert(routine.toEntity())

    suspend fun deleteRoutine(routine: CommuteRoutine) =
        routineDao.delete(routine.toEntity())

    // ── Traffic Snapshots ─────────────────────────────────────────────────────

    suspend fun recordTrafficSnapshot(snapshot: TrafficSnapshot) {
        snapshotDao.insert(snapshot.toEntity())
        // prune entries older than 90 days
        val cutoff = Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli()
        snapshotDao.pruneOlderThan(cutoff)
    }

    suspend fun getRecentSnapshots(routineId: Long, days: Int = 30): List<TrafficSnapshot> {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS).toEpochMilli()
        return snapshotDao.getSnapshots(routineId, since).map { it.toDomain() }
    }

    suspend fun getAverageDuration(routineId: Long): Double? =
        snapshotDao.averageDuration(routineId)

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun CommuteRoutineEntity.toDomain() = CommuteRoutine(
        id = id,
        dayOfWeek = DayOfWeek.of(dayOfWeek),
        usualDepartureTime = LocalTime.of(usualDepartureHour, usualDepartureMinute),
        originLat = originLat,
        originLng = originLng,
        originName = originName,
        destinationLat = destinationLat,
        destinationLng = destinationLng,
        destinationName = destinationName,
        isActive = isActive,
        isNotificationEnabled = isNotificationEnabled,
        isPriorityAlert = isPriorityAlert,
        notificationOffsetMins = notificationOffsetMins,
        notificationCount = notificationCount
    )

    private fun CommuteRoutine.toEntity() = CommuteRoutineEntity(
        id = id,
        dayOfWeek = dayOfWeek.value,
        usualDepartureHour = usualDepartureTime.hour,
        usualDepartureMinute = usualDepartureTime.minute,
        originLat = originLat,
        originLng = originLng,
        originName = originName,
        destinationLat = destinationLat,
        destinationLng = destinationLng,
        destinationName = destinationName,
        isActive = isActive,
        isNotificationEnabled = isNotificationEnabled,
        isPriorityAlert = isPriorityAlert,
        notificationOffsetMins = notificationOffsetMins,
        notificationCount = notificationCount
    )

    private fun TrafficSnapshotEntity.toDomain() = TrafficSnapshot(
        id = id,
        routineId = routineId,
        capturedAt = capturedAt,
        durationMinutes = durationMinutes,
        normalDurationMinutes = normalDurationMinutes,
        congestionLevel = CongestionLevel.valueOf(congestionLevel)
    )

    private fun TrafficSnapshot.toEntity() = TrafficSnapshotEntity(
        id = id,
        routineId = routineId,
        capturedAt = capturedAt,
        durationMinutes = durationMinutes,
        normalDurationMinutes = normalDurationMinutes,
        congestionLevel = congestionLevel.name
    )
}
