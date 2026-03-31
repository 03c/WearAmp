package com.wearamp.presentation.navigation

import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.wearamp.data.local.UserPreferences
import com.wearamp.service.WearAmpMediaService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    val userPreferences: UserPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isMediaActive = MutableStateFlow(false)
    val isMediaActive: StateFlow<Boolean> = _isMediaActive.asStateFlow()

    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    init {
        checkMediaState()
    }

    private fun checkMediaState() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearAmpMediaService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                try {
                    val controller = controllerFuture?.get()
                    _isMediaActive.value = controller?.isPlaying == true ||
                            (controller?.currentMediaItem != null &&
                                    controller.playbackState != androidx.media3.common.Player.STATE_IDLE)
                } catch (_: Exception) {
                    _isMediaActive.value = false
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
