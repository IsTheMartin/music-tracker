package com.mrtnmrls.music_tracker_app.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class DeviceIdProvider @Inject constructor(@ApplicationContext context: Context) {

    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(DEVICE_ID, null)
        if (!stored.isNullOrEmpty()) {
            stored
        } else {
            val newDeviceId = UUID.randomUUID().toString()
            prefs.edit { putString(DEVICE_ID, newDeviceId) }
            newDeviceId
        }
    }

    companion object {
        private const val APP_PREFS = "music-tracker-prefs"
        private const val DEVICE_ID = "device_id"
    }
}
