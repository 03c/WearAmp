package com.wearamp.presentation.screens.player

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wearamp.data.local.UserPreferences
import com.wearamp.service.WearAmpMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "NowPlayingVM"

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val trackTitle: String = "Nothing playing",
    val artistName: String = "",
    val albumTitle: String = "",
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackError: String? = null,
    val shuffleModeEnabled: Boolean = false,
    /** Matches Player.REPEAT_MODE_* constants: 0 = off, 1 = one, 2 = all. */
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
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
                try {
                    mediaController = controllerFuture?.get()
                    mediaController?.addListener(playerListener)
                    // Restore persisted playback modes
                    viewModelScope.launch {
                        val shuffle = userPreferences.shuffleModeEnabled.first()
                        val repeat = userPreferences.repeatMode.first()
                        mediaController?.shuffleModeEnabled = shuffle
                        mediaController?.repeatMode = repeat
                    }
                    updateState()
                    // Continuously poll position regardless of play state
                    startPositionTicker()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to media service", e)
                    _uiState.value = _uiState.value.copy(
                        playbackError = "Cannot connect to media service"
                    )
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Clear any previous error when a new track starts
            _uiState.value = _uiState.value.copy(playbackError = null)
            updateState()
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            updateState()
            if (playbackState == Player.STATE_IDLE) {
                Log.w(TAG, "Player returned to IDLE")
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.errorCodeName} – ${error.message}", error)
            _uiState.value = _uiState.value.copy(
                playbackError = "Playback error: ${error.errorCodeName}\n${error.message}"
            )
        }
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = updateState()
        override fun onRepeatModeChanged(repeatMode: Int) = updateState()
    }

    /**
     * Polls [MediaController.getCurrentPosition] every 500 ms so the
     * progress bar and time labels stay in sync.  Runs for the entire
     * lifetime of the ViewModel – no stop/restart dance needed.
     */
    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.coerceAtLeast(0L)
                    _uiState.value = _uiState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur
                    )
                }
                delay(500L)
            }
        }
    }

    private fun updateState() {
        val controller = mediaController ?: return
        val mediaItem = controller.currentMediaItem
        _uiState.value = _uiState.value.copy(
            isPlaying = controller.isPlaying,
            trackTitle = mediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing",
            artistName = mediaItem?.mediaMetadata?.artist?.toString() ?: "",
            albumTitle = mediaItem?.mediaMetadata?.albumTitle?.toString() ?: "",
            currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = controller.duration.coerceAtLeast(0L),
            shuffleModeEnabled = controller.shuffleModeEnabled,
            repeatMode = controller.repeatMode
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

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newValue = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newValue
        viewModelScope.launch { userPreferences.saveShuffleMode(newValue) }
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
        viewModelScope.launch { userPreferences.saveRepeatMode(newMode) }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
