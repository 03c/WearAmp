package com.wearamp.presentation.screens.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearamp.data.api.model.PlexMetadata
import com.wearamp.data.repository.MediaRepository
import com.wearamp.service.PlaybackManager
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
    private val playbackManager: PlaybackManager,
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

    /** Fetch all tracks for an artist and start playback on repeat. */
    suspend fun playArtist(artistId: String) {
        val tracks = mediaRepository.getArtistTracks(artistId).getOrThrow()
        playbackManager.playTracks(tracks)
    }
}

@HiltViewModel
class BrowseAlbumsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
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

    /** Fetch all tracks for an album and start playback on repeat. */
    suspend fun playAlbum(albumId: String) {
        val tracks = mediaRepository.getTracks(albumId).getOrThrow()
        playbackManager.playTracks(tracks)
    }
}

@HiltViewModel
class BrowseAllAlbumsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sectionId: String = checkNotNull(savedStateHandle["sectionId"])

    private val _uiState = MutableStateFlow<BrowseUiState>(BrowseUiState.Loading)
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = BrowseUiState.Loading
            mediaRepository.getAllAlbumsInSection(sectionId).fold(
                onSuccess = { _uiState.value = BrowseUiState.Success(it) },
                onFailure = { _uiState.value = BrowseUiState.Error(it.message ?: "Error") }
            )
        }
    }

    /** Fetch all tracks for an album and start playback on repeat. */
    suspend fun playAlbum(albumId: String) {
        val tracks = mediaRepository.getTracks(albumId).getOrThrow()
        playbackManager.playTracks(tracks)
    }
}

@HiltViewModel
class BrowseTracksViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager,
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

    /** Play a single track (plus the rest of the album from that point) on repeat. */
    suspend fun playTrack(track: PlexMetadata) {
        val allTracks = (uiState.value as? BrowseUiState.Success)?.items ?: listOf(track)
        val startIndex = allTracks.indexOfFirst { it.ratingKey == track.ratingKey }.coerceAtLeast(0)
        // Reorder: selected track first, then the rest wrapping around
        val reordered = allTracks.subList(startIndex, allTracks.size) +
                allTracks.subList(0, startIndex)
        playbackManager.playTracks(reordered)
    }

    fun rateTrack(ratingKey: String, starred: Boolean) {
        viewModelScope.launch {
            mediaRepository.rateTrack(ratingKey, starred)
        }
    }
}
