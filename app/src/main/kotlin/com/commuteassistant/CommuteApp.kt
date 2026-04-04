package com.commuteassistant

import android.app.Application
import com.commuteassistant.notifications.NotificationChannels
import com.commuteassistant.notifications.TrafficCheckWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch

@HiltAndroidApp
class CommuteApp : Application() {
    @javax.inject.Inject lateinit var repository: com.commuteassistant.data.repository.CommuteRepository

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.getTodayRoutines().forEach { routine ->
                if (routine.isNotificationEnabled) {
                    TrafficCheckWorker.scheduleForRoutine(this@CommuteApp, routine.id, routine.notificationOffsetMins)
                }
            }
        }
    }
}
