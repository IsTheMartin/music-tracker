package com.mrtnmrls.music_tracker_app.domain.usecase

import com.mrtnmrls.music_tracker_app.domain.model.HeatMapActivity
import com.mrtnmrls.music_tracker_app.domain.model.MonthlyStats
import com.mrtnmrls.music_tracker_app.domain.model.SelectedMonth
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetMonthlyStatsUseCase @Inject constructor(
    private val repository: PlayRepository
) {

    operator fun invoke(month: SelectedMonth, limit: Int): Flow<MonthlyStats> {
        val (from, to) = month.toEpochRange()
        val topArtists = repository.topArtists(from, to, limit)
        val topSongs = repository.topSongs(from, to, limit)
        val totalPlays = repository.totalPlays(from, to)
        val playsByHour = repository.playsByHour(from, to)
        val playsByDay = repository.playsByDay(from, to)
        return combine(
            topArtists, topSongs, totalPlays, playsByHour, playsByDay
        ) { artists, songs, plays, hourlyPlays, dailyPlays ->
            val playsPerDay = (1..month.daysInMonth()).map { day ->
                dailyPlays.find { it.day == day }?.playCount ?: 0
            }

            val playsPerHour = (0..23).map { hour ->
                hourlyPlays.find { it.hour == hour }?.playCount ?: 0
            }
            val startPeakHour = (0 until playsPerHour.lastIndex)
                .maxByOrNull { hour -> playsPerHour[hour] + playsPerHour[hour + 1] } ?: 0
            val averagePlaysPerDay = plays / month.elapsedDays().toFloat()

            MonthlyStats(
                topArtists = artists,
                topSongs = songs,
                totalPlays = plays,
                heatMapActivity = HeatMapActivity(
                    playsPerDay = playsPerDay,
                    averagePlaysPerDay = averagePlaysPerDay,
                    startPeakHour = startPeakHour,
                    endPeakHour = startPeakHour + 1
                )
            )
        }
    }
}