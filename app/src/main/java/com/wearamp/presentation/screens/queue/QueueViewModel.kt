package com.wearamp.presentation.screens.queue

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.wearamp.service.WearAmpMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "QueueVM"

data class QueueItem(
    val index: Int,
    val title: String,
    val artist: String,
    val isCurrent: Boolean
)

data class QueueUiState(
    val items: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class QueueViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueueUiState())
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

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
                    refreshQueue()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to media service", e)
                    _uiState.value = QueueUiState(
                        isLoading = false,
                        error = "Cannot connect to media service"
                    )
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private val playerListener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = refreshQueue()
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refreshQueue()
    }

    private fun refreshQueue() {
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        val currentIdx = controller.currentMediaItemIndex

        val items = (0 until count).map { i ->
            val item = controller.getMediaItemAt(i)
            val metadata = item.mediaMetadata
            QueueItem(
                index = i,
                title = metadata.title?.toString() ?: "Unknown",
                artist = metadata.artist?.toString() ?: "",
                isCurrent = i == currentIdx
            )
        }

        _uiState.value = QueueUiState(
            items = items,
            currentIndex = currentIdx,
            isLoading = false
        )
    }

    fun removeItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(index)
            // Queue will update via onTimelineChanged listener
        }
    }

    fun moveItemUp(index: Int) {
        if (index <= 0) return
        val controller = mediaController ?: return
        if (index < controller.mediaItemCount) {
            controller.moveMediaItem(index, index - 1)
        }
    }

    fun moveItemDown(index: Int) {
        val controller = mediaController ?: return
        if (index >= controller.mediaItemCount - 1) return
        controller.moveMediaItem(index, index + 1)
    }

    fun skipToItem(index: Int) {
        val controller = mediaController ?: return
        if (index in 0 until controller.mediaItemCount) {
            controller.seekTo(index, 0L)
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
