package com.commuteassistant.data

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getTomTomApiKey(): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, 
                PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.tomtom.api.KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
