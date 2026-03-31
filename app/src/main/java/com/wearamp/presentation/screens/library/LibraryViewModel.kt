package com.wearamp.presentation.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    /** We resolved the first music library section – ready to browse. */
    data class Ready(val sectionId: String) : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            mediaRepository.getMusicLibrarySections().fold(
                onSuccess = { sections ->
                    val first = sections.firstOrNull()
                    if (first != null) {
                        _uiState.value = LibraryUiState.Ready(first.key)
                    } else {
                        _uiState.value = LibraryUiState.Error("No music libraries found")
                    }
                },
                onFailure = { error ->
                    _uiState.value = LibraryUiState.Error(
                        error.message ?: "Failed to load library"
                    )
                }
            )
        }
    }
}
