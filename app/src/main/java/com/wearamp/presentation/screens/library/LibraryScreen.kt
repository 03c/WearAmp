package com.wearamp.presentation.screens.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearamp.data.api.model.PlexMetadata

@Composable
fun LibraryScreen(
    onArtistsClick: (sectionId: String) -> Unit,
    onAlbumsClick: (sectionId: String) -> Unit,
    onRecentAlbumClick: (albumId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is LibraryUiState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        }

        is LibraryUiState.Ready -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ListHeader { Text(text = "WearAmp") }
                }
                item {
                    Chip(
                        label = { Text(text = "Artists") },
                        onClick = { onArtistsClick(state.sectionId) },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
                item {
                    Chip(
                        label = { Text(text = "Albums") },
                        onClick = { onAlbumsClick(state.sectionId) },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
                if (state.recentlyPlayed.isNotEmpty()) {
                    item {
                        ListHeader { Text(text = "Recently Played") }
                    }
                    items(state.recentlyPlayed, key = { it.ratingKey }) { item ->
                        RecentlyPlayedChip(
                            item = item,
                            onRecentAlbumClick = onRecentAlbumClick
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
                item {
                    Chip(
                        label = { Text(text = "Settings") },
                        onClick = onSettingsClick,
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }

        is LibraryUiState.Error -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ListHeader { Text(text = "Error") }
                }
                item {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.body2
                    )
                }
                item {
                    Chip(
                        label = { Text(text = "Retry") },
                        onClick = { viewModel.loadLibrary() },
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
                item {
                    Chip(
                        label = { Text(text = "Settings") },
                        onClick = onSettingsClick,
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedChip(
    item: PlexMetadata,
    onRecentAlbumClick: (albumId: String) -> Unit
) {
    val albumId = item.parentRatingKey ?: return
    val label = item.title
    val secondaryLabel = item.grandparentTitle ?: item.parentTitle

    Chip(
        label = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = secondaryLabel?.let {
            {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        onClick = { onRecentAlbumClick(albumId) },
        colors = ChipDefaults.primaryChipColors()
    )
}
