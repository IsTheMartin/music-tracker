package com.mrtnmrls.music_tracker_app.ui.wrapped

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrtnmrls.music_tracker_app.data.local.db.AppDatabase
import com.mrtnmrls.music_tracker_app.data.remote.DeviceIdProvider
import com.mrtnmrls.music_tracker_app.data.remote.SyncManager
import com.mrtnmrls.music_tracker_app.data.repository.PlayRepositoryImpl
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import com.mrtnmrls.music_tracker_app.ui.stats.SelectedMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class WrappedViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TOP_LIMIT = 5
    }

    private val _uiState = MutableStateFlow<WrappedUiState>(WrappedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val repository: PlayRepository by lazy {
        val playDao = AppDatabase.getInstance(app).playDao()
        val syncManager = SyncManager(playDao, DeviceIdProvider.getOrCreate(app))
        PlayRepositoryImpl(playDao, syncManager)
    }

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
