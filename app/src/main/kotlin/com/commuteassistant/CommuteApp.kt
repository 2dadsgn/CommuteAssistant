package com.commuteassistant

import android.app.Application
import com.commuteassistant.notifications.NotificationChannels
import com.commuteassistant.notifications.TrafficCheckWorker
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.launch

@HiltAndroidApp
class CommuteApp : Application(), Configuration.Provider {
    @javax.inject.Inject lateinit var workerFactory: HiltWorkerFactory
    @javax.inject.Inject lateinit var repository: com.commuteassistant.data.repository.CommuteRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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
