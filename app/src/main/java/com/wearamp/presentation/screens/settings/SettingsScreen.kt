package com.wearamp.presentation.screens.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val username by viewModel.username.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()

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
                secondaryLabel = { Text(text = serverUrl ?: "Not configured") },
                onClick = {},
                colors = ChipDefaults.secondaryChipColors()
            )
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Sign Out") },
                onClick = { viewModel.logout(onLogout) },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}
