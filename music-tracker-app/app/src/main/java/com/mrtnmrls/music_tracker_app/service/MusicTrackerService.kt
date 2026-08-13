package com.mrtnmrls.music_tracker_app.service

import android.content.ComponentName
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.util.Log
import com.mrtnmrls.music_tracker_app.data.local.db.AppDatabase
import com.mrtnmrls.music_tracker_app.data.repository.PlayRepositoryImpl
import com.mrtnmrls.music_tracker_app.domain.model.Play
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.getValue

class MusicTrackerService : NotificationListenerService() {

    private val repository: PlayRepository by lazy {
        PlayRepositoryImpl(AppDatabase.getInstance(this).playDao())
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaController: MediaController? = null
    private var activePlay: ActivePlay? = null

    private var totalPausedMs: Long = 0L
    private var pauseStartedAt: Long? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class ActivePlay(
        val title: String,
        val artist: String,
        val album: String,
        val artUri: String,
        val durationMs: Long,
        val startedAt: Long
    )

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        updateMediaController(sessions)
    }

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = handleMetadataChanged(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) =
            handlePlaybackStateChanged(state)

        override fun onSessionDestroyed() = closeCurrentPlay()
    }

    override fun onListenerConnected() {
        Log.d(TAG, "Listener connected")
        val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        mediaSessionManager = manager
        val componentName = ComponentName(this, MusicTrackerService::class.java)
        manager.addOnActiveSessionsChangedListener(sessionListener, componentName)
        updateMediaController(manager.getActiveSessions(componentName))
    }

    override fun onListenerDisconnected() {
        Log.d(TAG, "Listener connected")
        cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    private fun cleanup() {
        mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        mediaController?.unregisterCallback(mediaCallback)
        mediaController = null
        closeCurrentPlay()
        scope.cancel()
    }

    private fun updateMediaController(sessions: List<MediaController>?) {
        val activeSession = sessions?.firstOrNull { it.packageName == YT_MUSIC_PACKAGE }
        Log.d(TAG, "Session updated - Music active: ${activeSession != null}")
        if (activeSession?.sessionToken == mediaController?.sessionToken) return

        mediaController?.unregisterCallback(mediaCallback)
        mediaController = activeSession?.also { it.registerCallback(mediaCallback) }
        if (activeSession == null) closeCurrentPlay()
    }

    private fun handleMetadataChanged(metadata: MediaMetadata?) {
        metadata ?: run { closeCurrentPlay(); return }

        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: run { closeCurrentPlay(); return }
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: run { closeCurrentPlay(); return }

        if (activePlay?.title == title && activePlay?.artist == artist) {
            val newDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            if (newDuration > 0 && activePlay?.durationMs != newDuration) {
                activePlay = activePlay?.copy(durationMs = newDuration)
                Log.d(TAG, "Updated duration for '$title': ${newDuration}ms")
            }
            Log.d(TAG, "Duplicated metadata event for '$title', ignoring")
            return
        }

        logAllMetadata(metadata)
        closeCurrentPlay()

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val artUri = resolveArtUri(metadata)
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        Log.d(TAG, "Now playing: $title - $artist ($album) [${durationMs}ms]")
        activePlay = ActivePlay(
            title = title,
            artist = artist,
            album = album,
            artUri = artUri,
            durationMs = durationMs,
            startedAt = System.currentTimeMillis()
        )
        totalPausedMs = 0L
        pauseStartedAt = null
    }

    private fun resolveArtUri(metadata: MediaMetadata): String {
        val remoteUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI).orEmpty()
        if (remoteUri.isNotEmpty()) return remoteUri

        val bitmap = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: return ""

        return saveBitmapLocally(bitmap, metadata)
    }

    private fun saveBitmapLocally(bitmap: Bitmap, metadata: MediaMetadata): String {
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "unknown"
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "unknown"

        val filename = "${(title + artist).hashCode()}.jpg"
        val file = File(File(filesDir, "art").also { it.mkdirs() }, filename)
        return try {
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Uri.fromFile(file).toString()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save album art: ${e.message}")
            ""
        }
    }

    private fun logAllMetadata(metadata: MediaMetadata) {
        val strings = listOf(
            "TITLE" to MediaMetadata.METADATA_KEY_TITLE,
            "ARTIST" to MediaMetadata.METADATA_KEY_ARTIST,
            "ALBUM" to MediaMetadata.METADATA_KEY_ALBUM,
            "ALBUM_ARTIST" to MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            "AUTHOR" to MediaMetadata.METADATA_KEY_AUTHOR,
            "COMPOSER" to MediaMetadata.METADATA_KEY_COMPOSER,
            "WRITER" to MediaMetadata.METADATA_KEY_WRITER,
            "GENRE" to MediaMetadata.METADATA_KEY_GENRE,
            "DATE" to MediaMetadata.METADATA_KEY_DATE,
            "COMPILATION" to MediaMetadata.METADATA_KEY_COMPILATION,
            "MEDIA_ID" to MediaMetadata.METADATA_KEY_MEDIA_ID,
            "ART_URI" to MediaMetadata.METADATA_KEY_ART_URI,
            "ALBUM_ART_URI" to MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            "DISPLAY_TITLE" to MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
            "DISPLAY_SUBTITLE" to MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
            "DISPLAY_DESC" to MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
            "DISPLAY_ICON_URI" to MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
        val longs = listOf(
            "DURATION" to MediaMetadata.METADATA_KEY_DURATION,
            "YEAR" to MediaMetadata.METADATA_KEY_YEAR,
            "TRACK_NUMBER" to MediaMetadata.METADATA_KEY_TRACK_NUMBER,
            "NUM_TRACKS" to MediaMetadata.METADATA_KEY_NUM_TRACKS,
            "DISC_NUMBER" to MediaMetadata.METADATA_KEY_DISC_NUMBER,
        )
        val bitmaps = listOf(
            "ART" to MediaMetadata.METADATA_KEY_ART,
            "ALBUM_ART" to MediaMetadata.METADATA_KEY_ALBUM_ART,
            "DISPLAY_ICON" to MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )

        Log.d(TAG, "── MediaMetadata dump ──────────────────────")
        strings.forEach { (label, key) ->
            val value = metadata.getString(key)
            if (value != null) Log.d(TAG, "  $label: $value")
        }
        longs.forEach { (label, key) ->
            val value = metadata.getLong(key)
            if (value != 0L) Log.d(TAG, "  $label: $value")
        }
        bitmaps.forEach { (label, key) ->
            val bmp = metadata.getBitmap(key)
            if (bmp != null) Log.d(TAG, "  $label: ${bmp.width}x${bmp.height}px")
        }
        Log.d(TAG, "────────────────────────────────────────────")
    }

    private fun handlePlaybackStateChanged(state: PlaybackState?) {
        state ?: return
        when (state.state) {
            PlaybackState.STATE_PLAYING -> {
                pauseStartedAt?.let { paused ->
                    totalPausedMs += System.currentTimeMillis() - paused
                    pauseStartedAt = null
                }
                Log.d(TAG, "Playback resumed (total paused so far: ${totalPausedMs}ms)")
            }

            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED -> {
                if (pauseStartedAt == null) pauseStartedAt = System.currentTimeMillis()
                Log.d(TAG, "Playback paused/stopped")
            }

            else -> Unit
        }
    }

    private fun closeCurrentPlay() {
        val play = activePlay ?: return
        activePlay = null

        val ongoingPauseMs = pauseStartedAt?.let { System.currentTimeMillis() - it } ?: 0L
        val listenedMs =
            (System.currentTimeMillis() - play.startedAt - totalPausedMs - ongoingPauseMs)
                .coerceAtLeast(0L)
        totalPausedMs = 0L
        pauseStartedAt = null

        val skipped = play.durationMs > 0 && listenedMs < SKIP_THRESHOLD * play.durationMs

        Log.d(
            TAG,
            "Saving: ${play.title} — listened ${listenedMs}ms / ${play.durationMs}ms, skipped=$skipped"
        )

        scope.launch {
            repository.save(
                Play(
                    title = play.title,
                    artist = play.artist,
                    album = play.album,
                    artUri = play.artUri,
                    durationMs = play.durationMs,
                    listenedMs = listenedMs,
                    startedAt = play.startedAt,
                    endedAt = System.currentTimeMillis(),
                    skipped = skipped,
                    sourcePackage = YT_MUSIC_PACKAGE
                )
            )
        }
    }

    companion object {
        const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
        private const val SKIP_THRESHOLD = 0.2
        private const val TAG = "MusicTrackerService"
    }
}