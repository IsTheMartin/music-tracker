package com.mrtnmrls.music_tracker_app.data.repository

import com.mrtnmrls.music_tracker_app.data.local.dao.PlayDao
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity
import com.mrtnmrls.music_tracker_app.data.remote.SyncManager
import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.Play
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import kotlinx.coroutines.flow.Flow

class PlayRepositoryImpl(
    private val playDao: PlayDao,
    private val syncManager: SyncManager
) : PlayRepository {
    override suspend fun save(play: Play) {
        playDao.insert(play.toEntity())
        syncManager.syncPending()
    }

    override fun topArtists(from: Long, to: Long, limit: Int): Flow<List<ArtistStat>> =
        playDao.topArtists(from = from, to = to, limit = limit)

    override fun topSongs(from: Long, to: Long, limit: Int): Flow<List<SongStat>> =
        playDao.topSongs(from = from, to = to, limit = limit)

    override suspend fun getAllPlays(): List<Play> = playDao.getAllPlays().map { it.toDomain() }

    override suspend fun importPlays(plays: List<Play>) {
        playDao.insertAll(plays.map { play -> play.toEntity() })
    }

    override suspend fun downloadAndMerge() = syncManager.downloadAndMerge()

    private fun Play.toEntity() = PlayEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artUri = artUri,
        remoteArtUri = remoteArtUri,
        durationMs = durationMs,
        listenedMs = listenedMs,
        startedAt = startedAt,
        endedAt = endedAt,
        sourcePackage = sourcePackage
    )

    private fun PlayEntity.toDomain() = Play(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artUri = artUri,
        remoteArtUri = remoteArtUri,
        durationMs = durationMs,
        listenedMs = listenedMs,
        startedAt = startedAt,
        endedAt = endedAt,
        sourcePackage = sourcePackage
    )
}
