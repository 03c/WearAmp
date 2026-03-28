package com.wearamp.presentation.screens.browse

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun BrowseArtistsScreen(
    onArtistSelected: (artistId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    viewModel: BrowseArtistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BrowseListScreen(
        title = "Artists",
        uiState = uiState,
        onItemClick = { item -> onArtistSelected(item.ratingKey) },
        itemLabel = { it.title },
        onNowPlayingClick = onNowPlayingClick,
        onRetry = { viewModel.loadArtists() }
    )
}

@Composable
fun BrowseAlbumsScreen(
    onAlbumSelected: (albumId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    viewModel: BrowseAlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BrowseListScreen(
        title = "Albums",
        uiState = uiState,
        onItemClick = { item -> onAlbumSelected(item.ratingKey) },
        itemLabel = { "${it.title}${if (it.year != null) " (${it.year})" else ""}" },
        onNowPlayingClick = onNowPlayingClick,
        onRetry = { viewModel.loadAlbums() }
    )
}

@Composable
fun BrowseTracksScreen(
    onNowPlayingClick: () -> Unit,
    viewModel: BrowseTracksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    BrowseListScreen(
        title = "Tracks",
        uiState = uiState,
        onItemClick = { /* TODO: play track via media service */ },
        itemLabel = { it.title },
        onNowPlayingClick = onNowPlayingClick,
        onRetry = { viewModel.loadTracks() }
    )
}

@Composable
private fun BrowseListScreen(
    title: String,
    uiState: BrowseUiState,
    onItemClick: (com.wearamp.data.api.model.PlexMetadata) -> Unit,
    itemLabel: (com.wearamp.data.api.model.PlexMetadata) -> String,
    onNowPlayingClick: () -> Unit,
    onRetry: () -> Unit
) {
    when (val state = uiState) {
        is BrowseUiState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        }

        is BrowseUiState.Success -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { ListHeader { Text(text = title) } }
                items(state.items) { item ->
                    Chip(
                        label = { Text(text = itemLabel(item)) },
                        onClick = { onItemClick(item) },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
                item {
                    Chip(
                        label = { Text(text = "Now Playing") },
                        onClick = onNowPlayingClick,
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }

        is BrowseUiState.Error -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(text = state.message, color = MaterialTheme.colors.error)
                }
                item {
                    Chip(
                        label = { Text(text = "Retry") },
                        onClick = onRetry,
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
            }
        }
    }
}
