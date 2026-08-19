package com.mrtnmrls.music_tracker_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plays")
data class PlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String,
    val artUri: String,
    val remoteArtUri: String = "",
    val durationMs: Long,
    val listenedMs: Long,
    val startedAt: Long,
    val endedAt: Long,
    val sourcePackage: String,
    val synced: Boolean = false
)
