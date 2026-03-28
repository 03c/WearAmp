package com.wearamp.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.local.UserPreferences
import com.wearamp.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            userPreferences.saveServerUrl(url.trimEnd('/') + "/")
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
