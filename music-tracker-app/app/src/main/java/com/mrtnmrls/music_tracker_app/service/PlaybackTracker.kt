package com.mrtnmrls.music_tracker_app.service

class PlaybackTracker {

    private var active: ActiveTrack? = null
    private var totalPausedMs: Long = 0L
    private var pauseStartedAt: Long? = null

    fun isSameTrack(title: String, artist: String): Boolean =
        active?.title == title && active?.artist == artist

    fun startTrack(track: ActiveTrack) {
        active = track
        totalPausedMs = 0L
        pauseStartedAt = null
    }

    fun updateDuration(durationMs: Long) {
        active?.let {
            if (durationMs > 0 && it.durationMs != durationMs)
                active = it.copy(durationMs = durationMs)
        }
    }

    fun onPlaying(now: Long) {
        pauseStartedAt?.let {
            totalPausedMs += now - it
            pauseStartedAt = null

        }
    }

    fun onPaused(now: Long) {
        if (pauseStartedAt == null)
            pauseStartedAt = now
    }

    fun closeTrack(now: Long, skipThreshold: Double): ClosedPlay? {
        val track = active ?: return null
        active = null

        val ongoingPauseMs = pauseStartedAt?.let { now - it } ?: 0L
        val listenedMs = (now - track.startedAt - totalPausedMs - ongoingPauseMs).coerceAtLeast(0L)
        totalPausedMs = 0
        pauseStartedAt = null

        val skipped = track.durationMs > 0 && listenedMs < skipThreshold * track.durationMs

        return ClosedPlay(
            title = track.title,
            artist = track.artist,
            album = track.album,
            artUri = track.artUri,
            remoteArtUri = track.remoteArtUri,
            durationMs = track.durationMs,
            startedAt = track.startedAt,
            listenedMs = listenedMs,
            endedAt = now,
            skipped = skipped
        )
    }
}

data class ActiveTrack(
    val title: String,
    val artist: String,
    val album: String,
    val artUri: String,
    val remoteArtUri: String,
    val durationMs: Long,
    val startedAt: Long,
)

data class ClosedPlay(
    val title: String,
    val artist: String,
    val album: String,
    val artUri: String,
    val remoteArtUri: String,
    val durationMs: Long,
    val startedAt: Long,
    val listenedMs: Long,
    val endedAt: Long,
    val skipped: Boolean,
)