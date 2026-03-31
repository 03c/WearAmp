package com.wearamp.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object GeneratingPin : LoginUiState
    data class WaitingForAuth(val pin: String, val linkUrl: String) : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val clientId: String = UUID.randomUUID().toString()

    init {
        startLogin()
    }

    fun startLogin() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.GeneratingPin
            authRepository.createPin(clientId).fold(
                onSuccess = { pin ->
                    val linkUrl = "https://plex.tv/link?pin=${pin.code}"
                    _uiState.value = LoginUiState.WaitingForAuth(pin.code, linkUrl)
                    waitForAuth(pin.id)
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Failed to generate PIN"
                    )
                }
            )
        }
    }

    private fun waitForAuth(pinId: Long) {
        viewModelScope.launch {
            authRepository.pollForAuthToken(pinId, clientId).fold(
                onSuccess = { token ->
                    // Fetch user info
                    authRepository.fetchAndSaveUser(token, clientId).fold(
                        onSuccess = {
                            // Auto-discover the user's Plex server
                            authRepository.discoverAndSaveServer(token, clientId).fold(
                                onSuccess = {
                                    // Everything ready — now persist the auth token.
                                    // This triggers NavGraph to redirect to Library.
                                    authRepository.finaliseLogin(token, clientId)
                                    _uiState.value = LoginUiState.Success
                                },
                                onFailure = { error ->
                                    _uiState.value = LoginUiState.Error(
                                        error.message ?: "Could not find a Plex server"
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = LoginUiState.Error(
                                error.message ?: "Failed to fetch user information"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.message ?: "Authentication failed"
                    )
                }
            )
        }
    }

    fun retry() {
        startLogin()
    }
}
