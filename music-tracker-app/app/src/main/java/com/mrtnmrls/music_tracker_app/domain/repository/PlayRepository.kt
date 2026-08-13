package com.mrtnmrls.music_tracker_app.domain.repository

import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.Play
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import kotlinx.coroutines.flow.Flow

interface PlayRepository {
    suspend fun save(play: Play)
    fun topArtists(from: Long, to: Long, includedSkipped: Boolean = false, limit: Int = 20): Flow<List<ArtistStat>>
    fun topSongs(from: Long, to: Long, includedSkipped: Boolean = false, limit: Int = 20): Flow<List<SongStat>>
    suspend fun getAllPlays(): List<Play>
    suspend fun importPlays(plays: List<Play>)
}