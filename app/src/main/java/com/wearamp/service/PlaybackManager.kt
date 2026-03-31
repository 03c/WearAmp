package com.wearamp.service

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wearamp.data.api.model.PlexMetadata
import com.wearamp.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
     * Replace the queue with [tracks], enable repeat-all, and start playback.
     */
    suspend fun playTracks(tracks: List<PlexMetadata>) {
        val mc = getController()
        val authToken = userPreferences.authToken.firstOrNull()
            ?: throw IllegalStateException("Not signed in")
        val serverUrl = userPreferences.serverUrl.firstOrNull()
            ?: throw IllegalStateException("No Plex server configured")

        val mediaItems = tracks.mapNotNull { track ->
            val partKey = track.media?.firstOrNull()?.parts?.firstOrNull()?.key
                ?: return@mapNotNull null
            MediaItem.Builder()
                .setUri("$serverUrl$partKey?X-Plex-Token=$authToken")
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

        mc.setMediaItems(mediaItems)
        mc.repeatMode = Player.REPEAT_MODE_ALL
        mc.prepare()
        mc.play()
    }
}
