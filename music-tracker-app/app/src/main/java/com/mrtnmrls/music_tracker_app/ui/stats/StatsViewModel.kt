package com.mrtnmrls.music_tracker_app.ui.stats

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrtnmrls.music_tracker_app.domain.model.SelectedMonth
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import com.mrtnmrls.music_tracker_app.domain.usecase.GetMonthlyStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PlayRepository,
    private val getMonthlyStatsUseCase: GetMonthlyStatsUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
    }

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var topListLimit = 20
    private var selectedMonth = SelectedMonth.current()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private var statsJob: Job? = null

    init {
        loadStats()
        viewModelScope.launch {
            runCatching { repository.downloadAndMerge() }
                .onFailure {
                    Log.e(TAG, "downloadAndMerge failed", it)
                    _events.emit("Cannot synchronize from server")
                }
        }
    }

    fun handleIntent(intent: StatsIntent) {
        when (intent) {
            StatsIntent.NextMonth -> updateMonth { it.next() }
            StatsIntent.PreviousMonth -> updateMonth { it.previous() }
        }
    }

    private fun updateMonth(transform: (SelectedMonth) -> SelectedMonth) {
        selectedMonth = transform(selectedMonth)
        _uiState.value = StatsUiState.Loading
        loadStats()
    }

    private fun loadStats() {
        statsJob?.cancel()
        statsJob = viewModelScope.launch {
            getMonthlyStatsUseCase(
                month = selectedMonth,
                limit = topListLimit
            )
                .catch {
                    Log.e(TAG, "loadStats from DB", it)
                    _events.emit("Cannot load stats")
                }
                .collect { stats ->
                    _uiState.value = StatsUiState.Success(
                        selectedMonth = selectedMonth,
                        topArtists = stats.topArtists,
                        topSongs = stats.topSongs
                    )
                }
        }
    }
}