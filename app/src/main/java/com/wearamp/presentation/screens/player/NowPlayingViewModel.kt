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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
    val thumbUrl: String? = null,
    val connectionError: String? = null
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionJob: Job? = null

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
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(playerListener)
                    updateState()
                    startPositionUpdates()
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        connectionError = e.message ?: "Failed to connect to media service"
                    )
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState()
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
        override fun onPlaybackStateChanged(playbackState: Int) = updateState()
    }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                        durationMs = controller.duration.coerceAtLeast(0L)
                    )
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
        // One final position snapshot
        val controller = mediaController ?: return
        _uiState.value = _uiState.value.copy(
            currentPositionMs = controller.currentPosition.coerceAtLeast(0L)
        )
    }

    private fun updateState() {
        val controller = mediaController ?: return
        val mediaItem = controller.currentMediaItem
        _uiState.value = PlayerUiState(
            isPlaying = controller.isPlaying,
            trackTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing",
            artistName = mediaItem?.mediaMetadata?.artist?.toString() ?: "",
            albumTitle = mediaItem?.mediaMetadata?.albumTitle?.toString() ?: "",
            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
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

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun onCleared() {
        positionJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
