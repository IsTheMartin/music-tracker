package com.mrtnmrls.music_tracker_app.data.remote

import android.content.Context
import java.util.UUID

object DeviceIdProvider {

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences("music-tracker-prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null)
        if (!deviceId.isNullOrEmpty()) {
            return deviceId
        }

        val newDeviceId = UUID.randomUUID().toString()
        prefs.edit().putString("device_id", newDeviceId).apply()
        return newDeviceId
    }
}
