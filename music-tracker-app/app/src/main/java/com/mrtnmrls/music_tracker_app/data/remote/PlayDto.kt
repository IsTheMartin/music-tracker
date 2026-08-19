package com.mrtnmrls.music_tracker_app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayDto(
    @SerialName("device_id") val deviceId: String,
    val title: String,
    val artist: String,
    val album: String,
    @SerialName("art_uri") val artUri: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("listened_ms") val listenedMs: Long,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("ended_at") val endedAt: Long,
    @SerialName("source_package") val sourcePackage: String,
)
