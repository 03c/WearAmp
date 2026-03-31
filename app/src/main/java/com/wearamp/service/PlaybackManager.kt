package com.wearamp.service

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wearamp.data.api.model.PlexMetadata
import com.wearamp.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "PlaybackManager"

/**
 * Shared singleton for queueing tracks and controlling playback.
 * Connects lazily to [WearAmpMediaService] via [MediaController].
 */
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) {

    private val mutex = Mutex()
    private var controller: MediaController? = null

    /** Get (or create) a connected [MediaController]. */
    suspend fun getController(): MediaController = mutex.withLock {
        controller?.takeIf { it.isConnected }?.let { return it }

        return suspendCancellableCoroutine { cont ->
            val token = SessionToken(
                context,
                ComponentName(context, WearAmpMediaService::class.java)
            )
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener(
                {
                    try {
                        val mc = future.get()
                        controller = mc
                        cont.resume(mc)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                },
                MoreExecutors.directExecutor()
            )
            cont.invokeOnCancellation { MediaController.releaseFuture(future) }
        }
    }

    /**
     * Replace the queue with [tracks] and start playback, applying the user's
     * persisted shuffle and repeat mode preferences.
     */
    suspend fun playTracks(tracks: List<PlexMetadata>) {
        val mc = getController()
        // Prefer server-specific token, fall back to plex.tv auth token
        val token = userPreferences.serverToken.firstOrNull()
            ?: userPreferences.authToken.firstOrNull()
            ?: throw IllegalStateException("Not signed in")
        val serverUrl = (userPreferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("No Plex server configured"))
            .trimEnd('/')  // avoid double-slash

        val mediaItems = tracks.mapNotNull { track ->
            val partKey = track.media?.firstOrNull()?.parts?.firstOrNull()?.key
                ?: return@mapNotNull null
            // partKey starts with '/' e.g. /library/parts/12345/file.mp3
            val uri = android.net.Uri.parse("$serverUrl$partKey")
                .buildUpon()
                .appendQueryParameter("X-Plex-Token", token)
                .build()
            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.grandparentTitle ?: track.parentTitle ?: "")
                        .setAlbumTitle(track.parentTitle ?: "")
                        .build()
                )
                .build()
        }

        if (mediaItems.isEmpty()) throw IllegalStateException("No playable tracks found")

        Log.d(TAG, "Queueing ${mediaItems.size} tracks, first: ${mediaItems.first().mediaMetadata.title}")
        Log.d(TAG, "First URL: ${mediaItems.first().localConfiguration?.uri}")

        // Map stored Int to a valid @Player.RepeatMode constant to satisfy lint.
        val savedRepeatMode = when (userPreferences.repeatMode.first()) {
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        val savedShuffle = userPreferences.shuffleModeEnabled.first()

        mc.setMediaItems(mediaItems)
        mc.repeatMode = savedRepeatMode
        mc.shuffleModeEnabled = savedShuffle
        mc.prepare()
        mc.play()
    }
}
