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
    data class PinReady(val pin: String, val pinId: Long) : LoginUiState
    data object WaitingForAuth : LoginUiState
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

    fun startLogin() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.GeneratingPin
            authRepository.createPin(clientId).fold(
                onSuccess = { pin ->
                    _uiState.value = LoginUiState.PinReady(pin.code, pin.id)
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
            _uiState.value = LoginUiState.WaitingForAuth
            authRepository.pollForAuthToken(pinId, clientId).fold(
                onSuccess = { token ->
                    authRepository.fetchAndSaveUser(token)
                    _uiState.value = LoginUiState.Success
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
        _uiState.value = LoginUiState.Idle
    }
}
