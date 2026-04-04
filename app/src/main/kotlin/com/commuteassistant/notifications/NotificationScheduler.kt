package com.commuteassistant.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.commuteassistant.domain.model.CommuteRoutine
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlertsFor(routine: CommuteRoutine) {
        cancelAlertsFor(routine) // Clear any old ones first

        if (!routine.isNotificationEnabled || !routine.isActive) return

        if (routine.isPriorityAlert) {
            scheduleExactAlarms(routine)
        } else {
            // It's battery friendly; handled by TrafficCheckWorker
            // Note: TrafficCheckWorker runs for all applicable routines automatically
        }
    }

    private fun scheduleExactAlarms(routine: CommuteRoutine) {
        // Find the next occurrence of routine.dayOfWeek
        var nextDate = LocalDate.now()
        if (nextDate.dayOfWeek != routine.dayOfWeek || LocalTime.now().isAfter(routine.usualDepartureTime)) {
             nextDate = nextDate.with(TemporalAdjusters.next(routine.dayOfWeek))
        }

        // We want to schedule `notificationCount` alarms ending at `usualDepartureTime - notificationOffsetMins`
        // Let's spread them out evenly. If count=1, just fire at exactly offsetMins before.
        // If count=3, spread them over the window. Example: Offset is 30 mins. Count is 3.
        // Alert 1: 30 mins before, Alert 2: 20 mins before, Alert 3: 10 mins before.
        // For simplicity, we just fire them spaced by 10 minutes, bounded by the offset.
        
        val targetDepartureTime = LocalDateTime.of(nextDate, routine.usualDepartureTime)
        
        val interval = if (routine.notificationCount > 1) {
            routine.notificationOffsetMins / routine.notificationCount
        } else {
            0
        }

        for (i in 0 until routine.notificationCount) {
            val minutesBefore = routine.notificationOffsetMins - (i * interval)
            val alarmTime = targetDepartureTime.minusMinutes(minutesBefore.toLong())
            
            // Only schedule if it's in the future
            if (alarmTime.isAfter(LocalDateTime.now())) {
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("ROUTINE_ID", routine.id)
                }
                
                // Unique Request Code per routine + instance
                val requestCode = (routine.id * 100 + i).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val triggerAtMillis = alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    } else {
                        // Fallback
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            }
        }
    }

    fun cancelAlertsFor(routine: CommuteRoutine) {
        // Cancel up to Max (e.g. 5) possible pending intents just to be safe
        for (i in 0..5) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val requestCode = (routine.id * 100 + i).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
}
