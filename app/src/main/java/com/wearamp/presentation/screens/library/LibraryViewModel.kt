package com.wearamp.presentation.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.api.model.PlexMetadata
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
    data class Ready(
        val sectionId: String,
        val recentlyPlayed: List<PlexMetadata> = emptyList()
    ) : LibraryUiState
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
                        loadRecentlyPlayed()
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

    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            val current = _uiState.value as? LibraryUiState.Ready ?: return@launch
            mediaRepository.getRecentlyPlayed().onSuccess { items ->
                // Deduplicate by ratingKey, keeping the most recent occurrence (first in list).
                val unique = items.distinctBy { it.ratingKey }
                _uiState.value = current.copy(recentlyPlayed = unique)
            }
            // Silently ignore failures – the section simply stays empty.
        }
    }
}
