package com.mrtnmrls.music_tracker_app.data.remote

import com.mrtnmrls.music_tracker_app.data.local.dao.PlayDao
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.collections.plusAssign

@Serializable
internal data class RemotePlayDto(
    val title: String,
    val artist: String,
    val album: String,
    @SerialName("art_uri") val artUri: String,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("listened_ms") val listenedMs: Long,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("ended_at") val endedAt: Long,
    @SerialName("source_package") val sourcePackage: String,
)

@Serializable
internal data class StartedAtDto(
    @SerialName("started_at") val startedAt: Long
)

class SyncManager @Inject constructor(
    private val playDao: PlayDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val supabaseClient: SupabaseClient?
) {

    private val syncMutex = Mutex()

    suspend fun syncPending() {
        syncMutex.withLock {
            val client = supabaseClient ?: return
            val unsyncedPlays = playDao.getAllUnsyncedPlays()
            if (unsyncedPlays.isEmpty()) return

            runCatching {
                client
                    .from("plays")
                    .insert(unsyncedPlays.map { it.toDto() })
            }.onSuccess {
                withContext(NonCancellable) {
                    playDao.markSynced(unsyncedPlays.map { it.id })
                }
            }
        }
    }

    suspend fun downloadAndMerge() {
        val client = supabaseClient ?: return
        val pageSize = 1000L
        val remotePlays = mutableListOf<RemotePlayDto>()
        var offset = 0L

        while(true) {
            val page = client
                .from("plays")
                .select(
                    Columns.list(
                        "title", "artist", "album", "art_uri",
                        "duration_ms", "listened_ms", "started_at", "ended_at",
                        "source_package"
                    )
                ) {
                    range(offset, offset + pageSize - 1)
                }
                .decodeList<RemotePlayDto>()
            remotePlays += page
            if (page.size < pageSize) break
            offset += pageSize
        }

        val localStartedAts = playDao.getAllStartedAts().toHashSet()

        val newEntities = remotePlays
            .filter { it.startedAt !in localStartedAts }
            .map { it.toEntity() }
        if (newEntities.isNotEmpty()) {
            playDao.insertAll(newEntities)
        }

        remotePlays
            .filter { it.startedAt in localStartedAts && !it.artUri.startsWith("file://") }
            .forEach { playDao.updateArtUri(it.startedAt, it.artUri) }
    }

    suspend fun reconcileSyncState() {
        val client = supabaseClient ?: return
        val pageSize = 1000L
        val remoteStartedAts = mutableListOf<StartedAtDto>()
        var offset = 0L

        while(true) {
            val page = client
                .from("plays")
                .select(
                    Columns.list("started_at")
                ) {
                    filter { eq("device_id", deviceIdProvider.deviceId) }
                    range(offset, offset + pageSize - 1)
                }.decodeList<StartedAtDto>()

            remoteStartedAts += page
            if (page.size < pageSize) break
            offset += pageSize
        }

        playDao.markSyncedByStartedAt(remoteStartedAts.map { it.startedAt })
    }

    private fun PlayEntity.toDto(): PlayDto = PlayDto(
        deviceId = deviceIdProvider.deviceId,
        title = title,
        artist = artist,
        album = album,
        artUri = remoteArtUri,
        durationMs = durationMs,
        listenedMs = listenedMs,
        startedAt = startedAt,
        endedAt = endedAt,
        sourcePackage = sourcePackage
    )

    private fun RemotePlayDto.toEntity() = PlayEntity(
        id = 0,
        title = title,
        artist = artist,
        album = album,
        artUri = artUri,
        remoteArtUri = artUri,
        durationMs = durationMs,
        listenedMs = listenedMs,
        startedAt = startedAt,
        endedAt = endedAt,
        sourcePackage = sourcePackage,
        synced = true
    )
}
