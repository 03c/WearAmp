package com.wearamp.presentation.screens.browse

import androidx.lifecycle.SavedStateHandle
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

sealed interface BrowseUiState {
    data object Loading : BrowseUiState
    data class Success(val items: List<PlexMetadata>) : BrowseUiState
    data class Error(val message: String) : BrowseUiState
}

@HiltViewModel
class BrowseArtistsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadArtists()
    }

    fun loadArtists() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            mediaRepository.getArtists(sectionId).fold(
                onSuccess = { _uiState.value = BrowseUiState.Success(it) },
                onFailure = { _uiState.value = BrowseUiState.Error(it.message ?: "Error") }
            )
        }
    }
}

@HiltViewModel
class BrowseAlbumsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            mediaRepository.getAlbums(artistId).fold(
                onSuccess = { _uiState.value = BrowseUiState.Success(it) },
                onFailure = { _uiState.value = BrowseUiState.Error(it.message ?: "Error") }
            )
        }
    }
}

@HiltViewModel
class BrowseTracksViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadTracks()
    }

    fun loadTracks() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            mediaRepository.getTracks(albumId).fold(
                onSuccess = { _uiState.value = BrowseUiState.Success(it) },
                onFailure = { _uiState.value = BrowseUiState.Error(it.message ?: "Error") }
            )
        }
    }

    fun rateTrack(ratingKey: String, starred: Boolean) {
        viewModelScope.launch {
            mediaRepository.rateTrack(ratingKey, starred)
        }
    }
}
