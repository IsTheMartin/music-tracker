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
import com.mrtnmrls.music_tracker_app.domain.model.Play
import com.mrtnmrls.music_tracker_app.domain.repository.PlayRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class MusicTrackerService : NotificationListenerService() {

    @Inject
    lateinit var repository: PlayRepository

    private var mediaSessionManager: MediaSessionManager? = null
    private var mediaController: MediaController? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val tracker = PlaybackTracker()

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

        if (tracker.isSameTrack(title, artist)) {
            val newDuration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            tracker.updateDuration(newDuration)
            return
        }

        logAllMetadata(metadata)
        closeCurrentPlay()

        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val remoteArtUri = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI).orEmpty()
        val artUri = resolveArtUri(metadata)
        val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        Log.d(TAG, "Now playing: $title - $artist ($album) [${durationMs}ms]")

        val track = ActiveTrack(
            title = title,
            artist = artist,
            album = album,
            artUri = artUri,
            remoteArtUri = remoteArtUri,
            durationMs = durationMs,
            startedAt = System.currentTimeMillis()
        )
        tracker.startTrack(track)
    }

    private fun resolveArtUri(metadata: MediaMetadata): String {
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
                tracker.onPlaying(System.currentTimeMillis())
            }

            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED -> {
                tracker.onPaused(System.currentTimeMillis())
            }

            else -> Unit
        }
    }

    private fun closeCurrentPlay() {
        val closedPlay = tracker.closeTrack(System.currentTimeMillis(), SKIP_THRESHOLD) ?: return

        if (closedPlay.skipped) {
            Log.d(
                TAG,
                "Skipped: ${closedPlay.title} — listened ${closedPlay.listenedMs}ms / ${closedPlay.durationMs}ms, discarding"
            )
            return
        }

        Log.d(
            TAG,
            "Saving: ${closedPlay.title} — listened ${closedPlay.listenedMs}ms / ${closedPlay.durationMs}ms"
        )

        scope.launch {
            repository.save(
                Play(
                    title = closedPlay.title,
                    artist = closedPlay.artist,
                    album = closedPlay.album,
                    artUri = closedPlay.artUri,
                    remoteArtUri = closedPlay.remoteArtUri,
                    durationMs = closedPlay.durationMs,
                    listenedMs = closedPlay.listenedMs,
                    startedAt = closedPlay.startedAt,
                    endedAt = closedPlay.endedAt,
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