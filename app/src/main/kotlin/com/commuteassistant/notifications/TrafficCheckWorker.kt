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
import com.commuteassistant.data.repository.CommuteRepository
import com.commuteassistant.domain.usecase.GetDepartureRecommendationUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

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
        val routines = repository.getTodayRoutines()
        val now = LocalTime.now()

        routines.forEach { routine ->
            // Only check within 2 hours before usual departure
            val minutesUntilDeparture = Duration.between(now, routine.usualDepartureTime).toMinutes()
            if (minutesUntilDeparture in 0..120) {
                val recommendation = recommendationUseCase(routine)
                val earlyBy = Duration.between(
                    recommendation.recommendedDepartureTime,
                    routine.usualDepartureTime
                ).toMinutes()

                // Notify if recommendation differs by more than 5 minutes
                if (earlyBy > 5) {
                    sendTrafficNotification(
                        routineName = "${routine.originName} → ${routine.destinationName}",
                        message = "Leave ${earlyBy} min earlier than usual. ${recommendation.reason}",
                        recommendedTime = recommendation.recommendedDepartureTime.toString()
                    )
                }
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

        /** Schedule periodic checks every 30 minutes */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TrafficCheckWorker>(30, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
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

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            TrafficCheckWorker.schedule(context)
        }
    }
}

// ─── Generic notification receiver (for future alarm-based pings) ─────────────

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Reserved for exact-alarm departure reminders
    }
}
