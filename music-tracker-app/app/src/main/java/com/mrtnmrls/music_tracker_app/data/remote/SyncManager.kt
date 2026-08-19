package com.mrtnmrls.music_tracker_app.data.remote

import com.mrtnmrls.music_tracker_app.data.local.dao.PlayDao
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

class SyncManager(
    private val playDao: PlayDao,
    private val deviceId: String
) {
    suspend fun syncPending() {
        val unsyncedPlays = playDao.getAllUnsyncedPlays()
        if (unsyncedPlays.isEmpty()) return

        runCatching {
            SupabaseClientProvider.client
                .from("plays")
                .insert(unsyncedPlays.map { it.toDto() })
        }.onSuccess {
            playDao.markSynced(unsyncedPlays.map { it.id })
        }
    }

    suspend fun downloadAndMerge() {
        val remotePlays = SupabaseClientProvider.client
            .from("plays")
            .select(Columns.list(
                "title", "artist", "album", "art_uri",
                "duration_ms", "listened_ms", "started_at", "ended_at",
                "source_package"
            ))
            .decodeList<RemotePlayDto>()

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

    private fun PlayEntity.toDto(): PlayDto = PlayDto(
        deviceId = deviceId,
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
