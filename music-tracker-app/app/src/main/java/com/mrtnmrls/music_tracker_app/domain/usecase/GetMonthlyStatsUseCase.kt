package com.mrtnmrls.music_tracker_app.domain.usecase

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
        return combine(topArtists, topSongs, totalPlays) { artists, songs, plays ->
            MonthlyStats(
                topArtists = artists,
                topSongs = songs,
                totalPlays = plays
            )
        }
    }
}