package com.wearamp.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.local.UserPreferences
import com.wearamp.data.repository.AuthRepository
import com.wearamp.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

sealed interface LibraryRefreshState {
    data object Idle : LibraryRefreshState
    data object Loading : LibraryRefreshState
    data object Success : LibraryRefreshState
    data class Error(val message: String) : LibraryRefreshState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val mediaRepository: MediaRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val username: StateFlow<String?> = userPreferences.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val serverUrl: StateFlow<String?> = userPreferences.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _serverUrlError = MutableStateFlow<String?>(null)
    val serverUrlError: StateFlow<String?> = _serverUrlError.asStateFlow()

    private val _libraryRefreshState = MutableStateFlow<LibraryRefreshState>(LibraryRefreshState.Idle)
    val libraryRefreshState: StateFlow<LibraryRefreshState> = _libraryRefreshState.asStateFlow()

    /**
     * Validates and persists the Plex server URL.
     * Returns `true` if the URL was valid and saved, `false` otherwise
     * (with [serverUrlError] updated).
     */
    fun saveServerUrl(url: String): Boolean {
        val trimmed = url.trim()
        val normalised = trimmed.trimEnd('/') + "/"
        when {
            trimmed.isBlank() -> {
                _serverUrlError.value = "URL cannot be empty"
                return false
            }
            !trimmed.startsWith("http://") && !trimmed.startsWith("https://") -> {
                _serverUrlError.value = "URL must start with http:// or https://"
                return false
            }
            normalised.toHttpUrlOrNull() == null -> {
                _serverUrlError.value = "Invalid URL format"
                return false
            }
        }
        _serverUrlError.value = null
        viewModelScope.launch {
            userPreferences.saveServerUrl(normalised)
        }
        return true
    }

    /**
     * Clears the in-memory library cache and re-fetches artists and albums from the Plex server.
     * [libraryRefreshState] reflects the progress of the operation.
     */
    fun refreshLibraryCache() {
        if (_libraryRefreshState.value == LibraryRefreshState.Loading) return
        viewModelScope.launch {
            _libraryRefreshState.value = LibraryRefreshState.Loading
            mediaRepository.getMusicLibrarySections().fold(
                onSuccess = { sections ->
                    val sectionId = sections.firstOrNull()?.key
                    if (sectionId == null) {
                        _libraryRefreshState.value =
                            LibraryRefreshState.Error(
                                "No music library found. Please verify your Plex server has a music library configured."
                            )
                        return@fold
                    }
                    mediaRepository.refreshLibraryCache(sectionId).fold(
                        onSuccess = { _libraryRefreshState.value = LibraryRefreshState.Success },
                        onFailure = { e ->
                            _libraryRefreshState.value =
                                LibraryRefreshState.Error(e.message ?: "Refresh failed")
                        }
                    )
                },
                onFailure = { e ->
                    _libraryRefreshState.value =
                        LibraryRefreshState.Error(e.message ?: "Failed to connect to server")
                }
            )
        }
    }

    /** Resets [libraryRefreshState] back to [LibraryRefreshState.Idle]. */
    fun clearLibraryRefreshState() {
        _libraryRefreshState.value = LibraryRefreshState.Idle
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
