package com.commuteassistant

import android.app.Application
import com.commuteassistant.notifications.NotificationChannels
import com.commuteassistant.notifications.TrafficCheckWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CommuteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        TrafficCheckWorker.schedule(this)
    }
}
