package com.wearamp.presentation.screens.browse

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.launch

@Composable
fun BrowseArtistsScreen(
    onArtistSelected: (artistId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    viewModel: BrowseArtistsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    BrowseListScreen(
        title = "Artists",
        uiState = uiState,
        onItemClick = { item -> onArtistSelected(item.ratingKey) },
        onPlayClick = { item ->
            scope.launch {
                viewModel.playArtist(item.ratingKey)
                onNowPlayingClick()
            }
        },
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
    val scope = rememberCoroutineScope()
    BrowseListScreen(
        title = "Albums",
        uiState = uiState,
        onItemClick = { item -> onAlbumSelected(item.ratingKey) },
        onPlayClick = { item ->
            scope.launch {
                viewModel.playAlbum(item.ratingKey)
                onNowPlayingClick()
            }
        },
        itemLabel = { "${it.title}${if (it.year != null) " (${it.year})" else ""}" },
        onNowPlayingClick = onNowPlayingClick,
        onRetry = { viewModel.loadAlbums() }
    )
}

@Composable
fun BrowseAllAlbumsScreen(
    onAlbumSelected: (albumId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    viewModel: BrowseAllAlbumsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    BrowseListScreen(
        title = "Albums",
        uiState = uiState,
        onItemClick = { item -> onAlbumSelected(item.ratingKey) },
        onPlayClick = { item ->
            scope.launch {
                viewModel.playAlbum(item.ratingKey)
                onNowPlayingClick()
            }
        },
        itemLabel = { "${it.title}${if (it.parentTitle != null) " – ${it.parentTitle}" else ""}${if (it.year != null) " (${it.year})" else ""}" },
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
    val scope = rememberCoroutineScope()
    BrowseListScreen(
        title = "Tracks",
        uiState = uiState,
        onItemClick = { item ->
            scope.launch {
                viewModel.playTrack(item)
                onNowPlayingClick()
            }
        },
        onPlayClick = null,
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
    onPlayClick: ((com.wearamp.data.api.model.PlexMetadata) -> Unit)?,
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
                    if (onPlayClick != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Chip(
                                modifier = Modifier.weight(1f),
                                label = { Text(text = itemLabel(item)) },
                                onClick = { onItemClick(item) },
                                colors = ChipDefaults.primaryChipColors()
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { onPlayClick(item) },
                                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play"
                                )
                            }
                        }
                    } else {
                        Chip(
                            label = { Text(text = itemLabel(item)) },
                            onClick = { onItemClick(item) },
                            colors = ChipDefaults.primaryChipColors()
                        )
                    }
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
