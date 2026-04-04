package com.commuteassistant.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.commuteassistant.MainActivity
import com.commuteassistant.R
import com.commuteassistant.data.GoogleMapsApiService
import com.commuteassistant.data.ApiKeyProvider
import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.launch

// ─── Channel IDs ─────────────────────────────────────────────────────────────

object NotificationChannels {
    const val TRAFFIC_ALERTS  = "traffic_alerts"
    const val DEPARTURE_REMINDERS = "departure_reminders"

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(TRAFFIC_ALERTS, "Traffic Alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Live traffic alerts for your route" },
            NotificationChannel(DEPARTURE_REMINDERS, "Departure Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Smart reminders based on your routine" }
        ).forEach { nm.createNotificationChannel(it) }
    }
}

// ─── WorkManager Worker ───────────────────────────────────────────────────────

/**
 * Runs periodically (every 30 min during commute hours) to check traffic
 * and fire a notification if leaving now vs usual time makes a difference.
 */
@HiltWorker
class TrafficCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CommuteRepository,
    private val recommendationUseCase: GetDepartureRecommendationUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val routineId = inputData.getLong("ROUTINE_ID", -1L)
        
        if (routineId != -1L) {
            // Process single routine if ID is provided
            val routine = repository.getRoutineById(routineId) ?: return Result.success()
            if (!routine.isNotificationEnabled || !routine.isActive) return Result.success()
            
            val recommendation = recommendationUseCase(routine)
            sendTrafficNotification(
                routineName = "${routine.originName} → ${routine.destinationName}",
                message = "Traffic Update: Expected travel time is ${recommendation.estimatedTravelMinutes} mins. ${recommendation.reason}",
                recommendedTime = recommendation.recommendedDepartureTime.toString()
            )
        } else {
            // Fallback for legacy global triggers
            val routines = repository.getTodayRoutines()
            routines.forEach { routine ->
                if (!routine.isNotificationEnabled || !routine.isActive) return@forEach
                val recommendation = recommendationUseCase(routine)
                sendTrafficNotification(
                    routineName = "${routine.originName} → ${routine.destinationName}",
                    message = "Traffic Update: Expected travel time is ${recommendation.estimatedTravelMinutes} mins. ${recommendation.reason}",
                    recommendedTime = recommendation.recommendedDepartureTime.toString()
                )
            }
        }
        
        return Result.success()
    }

    private fun sendTrafficNotification(routineName: String, message: String, recommendedTime: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.TRAFFIC_ALERTS)
            .setSmallIcon(R.drawable.ic_traffic)
            .setContentTitle("🚦 Traffic Alert: $routineName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\n\n⏰ Recommended departure: $recommendedTime"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val WORK_NAME = "traffic_check"

        /** Schedule periodic checks on user-defined interval */
        fun scheduleForRoutine(context: Context, routineId: Long, intervalMins: Int) {
            val request = PeriodicWorkRequestBuilder<TrafficCheckWorker>(intervalMins.toLong(), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInputData(androidx.work.workDataOf("ROUTINE_ID" to routineId))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_$routineId",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

// ─── Boot Receiver ────────────────────────────────────────────────────────────

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject lateinit var repository: CommuteRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val receiverResult = goAsync()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val routines = repository.observeRoutines().first()
                    routines.forEach { routine ->
                        if (routine.isNotificationEnabled) {
                            TrafficCheckWorker.scheduleForRoutine(context, routine.id, routine.notificationOffsetMins)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    receiverResult.finish()
                }
            }
        }
    }
}

// ─── Generic notification receiver ─────────────

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: CommuteRepository
    @Inject lateinit var recommendationUseCase: GetDepartureRecommendationUseCase
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val routineId = intent.getLongExtra("ROUTINE_ID", -1L)
        if (routineId == -1L) return

        val receiverResult = goAsync()

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val routine = repository.getRoutineById(routineId)
                if (routine != null && routine.isActive && routine.isNotificationEnabled) {
                    val recommendation = recommendationUseCase(routine)
                    val earlyBy = Duration.between(
                        recommendation.recommendedDepartureTime,
                        routine.usualDepartureTime
                    ).toMinutes()

                    sendTrafficNotification(
                        context,
                        routineName = "${routine.originName} → ${routine.destinationName}",
                        message = if (earlyBy > 0) "Leave ${earlyBy} min earlier. ${recommendation.reason}" else "Traffic looks normal. ${recommendation.reason}",
                        recommendedTime = recommendation.recommendedDepartureTime.toString()
                    )
                    
                    // Reschedule for next week when all instances have fired?
                    // The scheduler handles generating alarms for the *next* time it matches dayOfWeek. 
                    // This creates an infinite loop where the alarm handles itself for next week automatically!
                    notificationScheduler.scheduleAlertsFor(routine)
                }
            } finally {
                receiverResult.finish()
            }
        }
    }

    private fun sendTrafficNotification(context: Context, routineName: String, message: String, recommendedTime: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.TRAFFIC_ALERTS)
            .setSmallIcon(R.drawable.ic_traffic)
            .setContentTitle("🚦 Traffic Alert: $routineName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\n\n⏰ Recommended departure: $recommendedTime"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
