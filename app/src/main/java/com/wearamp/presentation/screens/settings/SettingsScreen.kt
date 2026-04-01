package com.wearamp.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onFindServersClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverUrlError by viewModel.serverUrlError.collectAsState()
    val libraryRefreshState by viewModel.libraryRefreshState.collectAsState()

    var showServerUrlDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember(serverUrl) { mutableStateOf(serverUrl ?: "") }

    // Show an error dialog whenever a refresh error occurs, then reset state on dismiss.
    var refreshErrorMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(libraryRefreshState) {
        if (libraryRefreshState is LibraryRefreshState.Error) {
            refreshErrorMessage = (libraryRefreshState as LibraryRefreshState.Error).message
        }
    }

    Dialog(
        showDialog = showServerUrlDialog,
        onDismissRequest = { showServerUrlDialog = false }
    ) {
        Alert(
            title = { Text(text = "Server URL") },
            negativeButton = {
                Button(
                    onClick = { showServerUrlDialog = false },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) { Text("Cancel") }
            },
            positiveButton = {
                Button(
                    onClick = {
                        if (viewModel.saveServerUrl(serverUrlInput)) {
                            showServerUrlDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            content = {
                if (serverUrlError != null) {
                    Text(
                        text = serverUrlError.orEmpty(),
                        color = MaterialTheme.colors.error
                    )
                }
                BasicTextField(
                    value = serverUrlInput,
                    onValueChange = { serverUrlInput = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colors.primary)
                )
            }
        )
    }

    Dialog(
        showDialog = refreshErrorMessage != null,
        onDismissRequest = {
            refreshErrorMessage = null
            viewModel.clearLibraryRefreshState()
        }
    ) {
        Alert(
            title = { Text(text = "Refresh Failed") },
            negativeButton = {},
            positiveButton = {
                Button(
                    onClick = {
                        refreshErrorMessage = null
                        viewModel.clearLibraryRefreshState()
                    }
                ) { Text("OK") }
            },
            content = {
                Text(
                    text = refreshErrorMessage.orEmpty(),
                    color = MaterialTheme.colors.error
                )
            }
        )
    }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListHeader { Text(text = "Settings") }
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Account") },
                secondaryLabel = { Text(text = username ?: "Not signed in") },
                onClick = {},
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Server") },
                secondaryLabel = { Text(text = serverUrl ?: "Tap to configure") },
                onClick = {
                    serverUrlInput = serverUrl ?: ""
                    showServerUrlDialog = true
                },
                colors = ChipDefaults.primaryChipColors()
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Find Servers") },
                onClick = onFindServersClick,
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            val isRefreshing = libraryRefreshState == LibraryRefreshState.Loading
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = if (isRefreshing) "Refreshing…" else "Refresh Plex Library")
                },
                icon = if (isRefreshing) {
                    { CircularProgressIndicator(modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                onClick = { viewModel.refreshLibraryCache() },
                enabled = !isRefreshing,
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Sign Out") },
                onClick = { viewModel.logout(onLogout) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
