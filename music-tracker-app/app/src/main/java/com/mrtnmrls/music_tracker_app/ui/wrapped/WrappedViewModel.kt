package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import com.mrtnmrls.music_tracker_app.ui.stats.SelectedMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val repository: PlayRepository
) : ViewModel() {

    companion object {
        private const val TOP_LIMIT = 5
    }

    private val _uiState = MutableStateFlow<WrappedUiState>(WrappedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        val month = SelectedMonth.current()
        val (from, to) = month.toEpochRange()
        viewModelScope.launch {
            combine(
                repository.topArtists(from, to, limit = TOP_LIMIT),
                repository.topSongs(from, to, limit = TOP_LIMIT),
                repository.totalPlays(from, to)
            ) { artists, songs, total ->
                WrappedUiState.Success(
                    month = month,
                    totalPlays = total,
                    topArtists = artists,
                    topSongs = songs
                )
            }.collect { _uiState.value = it }
        }
    }
}
