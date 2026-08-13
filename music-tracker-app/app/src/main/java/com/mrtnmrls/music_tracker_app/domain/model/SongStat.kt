package com.mrtnmrls.music_tracker_app.domain.model

data class SongStat(
    val title: String,
    val artist: String,
    val playCount: Int,
    val artUri: String = ""
)
