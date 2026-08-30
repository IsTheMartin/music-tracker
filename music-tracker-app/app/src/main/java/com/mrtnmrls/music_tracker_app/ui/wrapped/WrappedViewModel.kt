package com.mrtnmrls.music_tracker_app.ui.wrapped

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrtnmrls.music_tracker_app.domain.model.SelectedMonth
import com.mrtnmrls.music_tracker_app.domain.usecase.GetMonthlyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val getMonthlyStatsUseCase: GetMonthlyStatsUseCase
) : ViewModel() {

    companion object {
        private const val TOP_LIMIT = 5
    }

    private val _uiState = MutableStateFlow<WrappedUiState>(WrappedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        val month = SelectedMonth.current()
        viewModelScope.launch {
            getMonthlyStatsUseCase(month = month, limit = TOP_LIMIT)
                .collect { stats ->
                    _uiState.value = WrappedUiState.Success(
                        month = month,
                        totalPlays = stats.totalPlays,
                        topArtists = stats.topArtists,
                        topSongs = stats.topSongs
                    )
                }
        }
    }
}
