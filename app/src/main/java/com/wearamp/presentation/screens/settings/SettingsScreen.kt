package com.wearamp.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverUrlError by viewModel.serverUrlError.collectAsState()

    var showServerUrlDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember(serverUrl) { mutableStateOf(serverUrl ?: "") }

    Dialog(
        showDialog = showServerUrlDialog,
        onDismissRequest = { showServerUrlDialog = false }
    ) {
        Alert(
            title = { Text(text = "Server URL") },
            message = {
                if (serverUrlError != null) {
                    Text(
                        text = serverUrlError.orEmpty(),
                        color = MaterialTheme.colors.error
                    )
                }
            },
            onCancel = { showServerUrlDialog = false },
            onConfirm = {
                if (viewModel.saveServerUrl(serverUrlInput)) {
                    showServerUrlDialog = false
                }
            }
        ) {
            item {
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
        }
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
                label = { Text(text = "Sign Out") },
                onClick = { viewModel.logout(onLogout) },
                colors = ChipDefaults.secondaryChipColors()
            )
        }
    }
}
