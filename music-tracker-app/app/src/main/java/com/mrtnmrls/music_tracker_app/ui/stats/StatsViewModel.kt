package com.mrtnmrls.music_tracker_app.ui.stats

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrtnmrls.music_tracker_app.data.local.db.AppDatabase
import com.mrtnmrls.music_tracker_app.data.remote.DeviceIdProvider
import com.mrtnmrls.music_tracker_app.data.remote.SyncManager
import com.mrtnmrls.music_tracker_app.data.repository.PlayRepositoryImpl
import com.mrtnmrls.music_tracker_app.domain.model.Play
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class StatsViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "StatsViewModel"
    }
    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val repository: PlayRepository by lazy {
        val playDao = AppDatabase.getInstance(app).playDao()
        val syncManager = SyncManager(playDao, DeviceIdProvider.getOrCreate(app))
        PlayRepositoryImpl(playDao, syncManager)
    }

    private var topListLimit = 20
    private var selectedMonth = SelectedMonth.current()

    private val _events = MutableSharedFlow<String>()
    val events = _events.asSharedFlow()

    private var statsJob: Job? = null

    init {
        loadStats()
        viewModelScope.launch {
            runCatching { repository.downloadAndMerge() }
                .onFailure { Log.e(TAG, "downloadAndMerge failed", it) }
        }
    }

    fun handleIntent(intent: StatsIntent) {
        when(intent) {
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
        val(from, to) = selectedMonth.toEpochRange()
        statsJob = viewModelScope.launch {
            combine(
                repository.topArtists(from, to, limit = topListLimit),
                repository.topSongs(from, to, limit = topListLimit)
            ) { artists, songs -> artists to songs }
                .collect { (artists, songs) ->
                    _uiState.value = StatsUiState.Success(
                        selectedMonth = selectedMonth,
                        topArtists = artists,
                        topSongs = songs
                    )
                }
        }
    }
}