package com.mrtnmrls.music_tracker_app.ui.stats

import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.SongStat

sealed interface StatsUiState {
    data object Loading: StatsUiState
    data class Success(
        val selectedMonth: SelectedMonth,
        val topArtists: List<ArtistStat>,
        val topSongs: List<SongStat>
    ): StatsUiState
}