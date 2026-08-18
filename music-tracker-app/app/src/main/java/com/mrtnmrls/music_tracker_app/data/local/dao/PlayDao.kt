package com.mrtnmrls.music_tracker_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mrtnmrls.music_tracker_app.data.local.entity.PlayEntity
import com.mrtnmrls.music_tracker_app.domain.model.ArtistStat
import com.mrtnmrls.music_tracker_app.domain.model.SongStat
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(play: PlayEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(plays: List<PlayEntity>)

    @Query("SELECT * FROM plays ORDER BY startedAt ASC")
    suspend fun getAllPlays(): List<PlayEntity>

    @Query("""
       SELECT artist, COUNT(*) AS playCount
        FROM plays
        WHERE (:includedSkipped = 1 OR skipped = 0)
        AND startedAt BETWEEN :from AND :to
        GROUP BY artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun topArtists(
        from: Long,
        to: Long,
        includedSkipped: Boolean,
        limit: Int
    ): Flow<List<ArtistStat>>

    @Query("""
       SELECT title, artist, COUNT(*) AS playCount, MAX(artUri) AS artUri
        FROM plays
        WHERE (:includedSkipped = 1 OR skipped = 0)
        AND startedAt BETWEEN :from AND :to
        GROUP BY title, artist
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    fun topSongs(
        from: Long,
        to: Long,
        includedSkipped: Boolean,
        limit: Int
    ): Flow<List<SongStat>>

    @Query("""
        SELECT * FROM plays
        WHERE synced = 0
        ORDER BY startedAt
    """)
    suspend fun getAllUnsyncedPlays(): List<PlayEntity>

    @Query("UPDATE plays SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)
}
