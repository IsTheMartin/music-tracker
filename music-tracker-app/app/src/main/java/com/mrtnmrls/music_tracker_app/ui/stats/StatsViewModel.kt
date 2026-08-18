package com.mrtnmrls.music_tracker_app.ui.stats

import android.app.Application
import android.net.Uri
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class StatsViewModel(app: Application): AndroidViewModel(app) {
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
    }

    fun handleIntent(intent: StatsIntent) {
        when(intent) {
            is StatsIntent.ExportToUri -> exportToUri(intent.uri)
            is StatsIntent.ImportFromUri -> importFromUri(intent.uri)
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

    private fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val plays = repository.getAllPlays()
                val json = serializePlays(plays)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("Could not open output stream")
                plays.size
            }.fold(
                onSuccess = { count -> _events.emit("Exported $count plays successfully") },
                onFailure = { _events.emit("Export failed: ${it.message}") }
            )
        }
    }

    private fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: error("Could not open input stream")
                val plays = deserializePlays(json)
                repository.importPlays(plays)
                plays.size
            }.fold(
                onSuccess = { count -> _events.emit("Imported $count plays successfully") },
                onFailure = { _events.emit("Import failed: ${it.message}") }
            )
        }
    }

    private fun serializePlays(plays: List<Play>): String {
        val array = JSONArray()
        plays.forEach { play ->
            array.put(JSONObject().apply {
                put("id", play.id)
                put("title", play.title)
                put("artist", play.artist)
                put("album", play.album)
                put("artUri", play.artUri)
                put("durationMs", play.durationMs)
                put("listenedMs", play.listenedMs)
                put("startedAt", play.startedAt)
                put("endedAt", play.endedAt)
                put("skipped", play.skipped)
                put("sourcePackage", play.sourcePackage)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("plays", array)
        }.toString(2)
    }

    private fun deserializePlays(json: String): List<Play> {
        val root = JSONObject(json)
        val array = root.getJSONArray("plays")
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            Play(
                id = obj.getLong("id"),
                title = obj.getString("title"),
                artist = obj.getString("artist"),
                album = obj.getString("album"),
                artUri = obj.optString("artUri", ""),
                durationMs = obj.getLong("durationMs"),
                listenedMs = obj.getLong("listenedMs"),
                startedAt = obj.getLong("startedAt"),
                endedAt = obj.getLong("endedAt"),
                skipped = obj.getBoolean("skipped"),
                sourcePackage = obj.getString("sourcePackage")
            )
        }
    }
}