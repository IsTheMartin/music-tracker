package com.mrtnmrls.music_tracker_app.ui.wrapped

import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import com.mrtnmrls.music_tracker_app.ui.stats.SelectedMonth

sealed interface WrappedUiState {
    data object Loading : WrappedUiState
    data class Success(
        val month: SelectedMonth,
        val totalPlays: Int,
        val topArtists: List<ArtistStat>,
        val topSongs: List<SongStat>
    ) : WrappedUiState
}
