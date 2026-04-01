package com.wearamp.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearamp.data.api.model.PlexConnection
import com.wearamp.data.api.model.PlexResource

@Composable
fun ServerPickerScreen(
    onServerSelected: () -> Unit,
    viewModel: ServerPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is ServerPickerUiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Finding servers…",
                    style = MaterialTheme.typography.body2
                )
            }
        }

        is ServerPickerUiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
                Button(onClick = { viewModel.retry() }) {
                    Text("Retry")
                }
            }
        }

        is ServerPickerUiState.Success -> {
            ServerList(
                servers = state.servers,
                onConnectionSelected = { resource, connection ->
                    viewModel.selectConnection(resource, connection)
                    onServerSelected()
                }
            )
        }
    }
}

@Composable
private fun ServerList(
    servers: List<PlexResource>,
    onConnectionSelected: (PlexResource, PlexConnection) -> Unit
) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ListHeader { Text(text = "Select Server") }
        }

        for (server in servers) {
            val connections = server.connections ?: continue

            item {
                ListHeader {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.caption1
                    )
                }
            }

            items(connections) { connection ->
                val label = if (connection.local) "Local" else "Remote"
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "${connection.address}:${connection.port}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "$label · ${connection.protocol}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = { onConnectionSelected(server, connection) },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}
