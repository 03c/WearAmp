package com.wearamp.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.api.model.PlexConnection
import com.wearamp.data.api.model.PlexResource
import com.wearamp.data.local.UserPreferences
import com.wearamp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerPickerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServerPickerUiState>(ServerPickerUiState.Loading)
    val uiState: StateFlow<ServerPickerUiState> = _uiState.asStateFlow()

    init {
        discoverServers()
    }

    private fun discoverServers() {
        viewModelScope.launch {
            _uiState.value = ServerPickerUiState.Loading
            val authToken = userPreferences.authToken.first()
            val clientId = userPreferences.clientId.first()
            if (authToken == null || clientId == null) {
                _uiState.value = ServerPickerUiState.Error("Not signed in")
                return@launch
            }
            authRepository.getServers(authToken, clientId)
                .onSuccess { servers ->
                    if (servers.isEmpty()) {
                        _uiState.value = ServerPickerUiState.Error("No servers found")
                    } else {
                        _uiState.value = ServerPickerUiState.Success(servers)
                    }
                }
                .onFailure { e ->
                    _uiState.value =
                        ServerPickerUiState.Error(e.message ?: "Discovery failed")
                }
        }
    }

    fun selectConnection(resource: PlexResource, connection: PlexConnection) {
        viewModelScope.launch {
            authRepository.saveServerConnection(resource, connection)
        }
    }

    fun retry() {
        discoverServers()
    }
}

sealed interface ServerPickerUiState {
    data object Loading : ServerPickerUiState
    data class Success(val servers: List<PlexResource>) : ServerPickerUiState
    data class Error(val message: String) : ServerPickerUiState
}
