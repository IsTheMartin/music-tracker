package com.mrtnmrls.music_tracker_app.domain.model

data class Play(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val artUri: String,
    val durationMs: Long,
    val listenedMs: Long,
    val startedAt: Long,
    val endedAt: Long,
    val skipped: Boolean,
    val sourcePackage: String,
)
