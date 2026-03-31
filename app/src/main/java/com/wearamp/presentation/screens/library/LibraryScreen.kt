package com.wearamp.presentation.screens.library

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
fun LibraryScreen(
    onSectionSelected: (sectionId: String) -> Unit,
    onNowPlayingClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is LibraryUiState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        }

        is LibraryUiState.Success -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    ListHeader { Text(text = "Music Libraries") }
                }
                items(state.sections) { section ->
                    Chip(
                        label = { Text(text = section.title) },
                        onClick = { onSectionSelected(section.key) },
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
                    Text(
                        text = state.message,
                        color = MaterialTheme.colors.error
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
