package com.mrtnmrls.music_tracker_app.data.remote

import com.mrtnmrls.music_tracker_app.data.local.dao.PlayDao
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity
import io.github.jan.supabase.postgrest.from

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

    private fun PlayEntity.toDto(): PlayDto = PlayDto(
        deviceId = deviceId,
        title = title,
        artist = artist,
        album = album,
        artUri = artUri,
        durationMs = durationMs,
        listenedMs = listenedMs,
        startedAt = startedAt,
        endedAt = endedAt,
        skipped = skipped,
        sourcePackage = sourcePackage
    )
}