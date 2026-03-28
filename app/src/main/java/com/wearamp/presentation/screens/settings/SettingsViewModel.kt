package com.wearamp.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.local.UserPreferences
import com.wearamp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val username: StateFlow<String?> = userPreferences.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val serverUrl: StateFlow<String?> = userPreferences.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _serverUrlError = MutableStateFlow<String?>(null)
    val serverUrlError: StateFlow<String?> = _serverUrlError.asStateFlow()

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

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
