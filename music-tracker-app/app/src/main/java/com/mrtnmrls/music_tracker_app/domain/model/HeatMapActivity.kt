package com.mrtnmrls.music_tracker_app.domain.model

data class HeatMapActivity(
    val playsPerDay: List<Int>,
    val averagePlaysPerDay: Float,
    val startPeakHour: Int,
    val endPeakHour: Int
)
