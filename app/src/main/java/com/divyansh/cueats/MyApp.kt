package com.divyansh.cueats

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

// OneSignal App ID is loaded from BuildConfig (set via secrets.properties — never committed to git)
// To run this project locally: add ONESIGNAL_APP_ID=your_id to secrets.properties
// See secrets.properties.example for the template
private val ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // OneSignal Initialization
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)

        // requestPermission will show the native Android notification permission prompt.
        // NOTE: It's recommended to use a OneSignal In-App Message to prompt instead.
        CoroutineScope(Dispatchers.IO).launch {
            OneSignal.Notifications.requestPermission(false)
        }
    }
}
