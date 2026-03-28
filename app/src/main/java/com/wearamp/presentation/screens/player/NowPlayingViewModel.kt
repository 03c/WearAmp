package com.wearamp.presentation.screens.player

import android.content.ComponentName
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.wearamp.service.WearAmpMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val trackTitle: String = "Nothing playing",
    val artistName: String = "",
    val albumTitle: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isStarred: Boolean = false,
    val thumbUrl: String? = null
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    init {
        connectToMediaService()
    }

    private fun connectToMediaService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearAmpMediaService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                mediaController = controllerFuture?.get()
                mediaController?.addListener(playerListener)
                updateState()
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
    }

    private fun updateState() {
        val controller = mediaController ?: return
        val mediaItem = controller.currentMediaItem
        _uiState.value = PlayerUiState(
            isPlaying = controller.isPlaying,
            trackTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing",
            artistName = mediaItem?.mediaMetadata?.artist?.toString() ?: "",
            albumTitle = mediaItem?.mediaMetadata?.albumTitle?.toString() ?: "",
            durationMs = controller.duration.coerceAtLeast(0L),
            thumbUrl = mediaItem?.mediaMetadata?.artworkUri?.toString()
        )
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
