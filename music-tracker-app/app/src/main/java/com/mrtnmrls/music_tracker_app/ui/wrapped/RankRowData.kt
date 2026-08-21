package com.mrtnmrls.music_tracker_app.ui.wrapped

data class RankRowData(
    val rank: Int,
    val primary: String,
    val secondary: String? = null,
    val artUri: String? = null,
    val playCount: Int,
)
