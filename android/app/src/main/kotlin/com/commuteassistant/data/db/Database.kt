package com.commuteassistant.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalTime

// ─── Entities ────────────────────────────────────────────────────────────────

@Entity(tableName = "commute_routines")
data class CommuteRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,                 // DayOfWeek.value (1=Mon … 7=Sun)
    val usualDepartureHour: Int,
    val usualDepartureMinute: Int,
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

@Entity(tableName = "traffic_snapshots")
data class TrafficSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val capturedAt: Long,
    val durationMinutes: Int,
    val normalDurationMinutes: Int,
    val congestionLevel: String          // CongestionLevel.name
)

// ─── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface CommuteRoutineDao {

    @Query("SELECT * FROM commute_routines WHERE isActive = 1 ORDER BY dayOfWeek, usualDepartureHour")
    fun observeActiveRoutines(): Flow<List<CommuteRoutineEntity>>

    @Query("SELECT * FROM commute_routines WHERE id = :id")
    suspend fun getById(id: Long): CommuteRoutineEntity?

    @Query("SELECT * FROM commute_routines WHERE dayOfWeek = :dayValue AND isActive = 1")
    suspend fun getByDay(dayValue: Int): List<CommuteRoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CommuteRoutineEntity): Long

    @Delete
    suspend fun delete(entity: CommuteRoutineEntity)

    @Query("UPDATE commute_routines SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)
}

@Dao
interface TrafficSnapshotDao {

    /** Last 30 days of snapshots for a routine, ordered by time desc */
    @Query("""
        SELECT * FROM traffic_snapshots
        WHERE routineId = :routineId AND capturedAt > :since
        ORDER BY capturedAt DESC
    """)
    suspend fun getSnapshots(routineId: Long, since: Long): List<TrafficSnapshotEntity>

    /** Average duration per hour-of-day slot (for historical modelling) */
    @Query("""
        SELECT AVG(durationMinutes) FROM traffic_snapshots
        WHERE routineId = :routineId
    """)
    suspend fun averageDuration(routineId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrafficSnapshotEntity): Long

    /** Keep only the last 90 days to avoid unbounded growth */
    @Query("DELETE FROM traffic_snapshots WHERE capturedAt < :before")
    suspend fun pruneOlderThan(before: Long)
}

// ─── Database ────────────────────────────────────────────────────────────────

@Database(
    entities = [CommuteRoutineEntity::class, TrafficSnapshotEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CommuteDatabase : RoomDatabase() {
    abstract fun routineDao(): CommuteRoutineDao
    abstract fun snapshotDao(): TrafficSnapshotDao
}
