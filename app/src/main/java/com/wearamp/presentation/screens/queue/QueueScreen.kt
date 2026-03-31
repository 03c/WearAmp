package com.wearamp.presentation.screens.queue

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow

@Composable
fun QueueScreen(
    viewModel: QueueViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when {
        state.isLoading -> {
            CircularProgressIndicator(modifier = Modifier.fillMaxSize())
        }

        state.error != null -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = state.error ?: "Unknown error",
                        color = MaterialTheme.colors.error
                    )
                }
            }
        }

        state.items.isEmpty() -> {
            ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
                item { ListHeader { Text(text = "Queue") } }
                item {
                    Text(
                        text = "Queue is empty",
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        else -> {
            val listState = rememberScalingLazyListState(
                initialCenterItemIndex = (state.currentIndex + 1).coerceAtLeast(0)
            )

            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item { ListHeader { Text(text = "Queue") } }

                itemsIndexed(state.items) { _, queueItem ->
                    QueueItemRow(
                        item = queueItem,
                        isFirst = queueItem.index == 0,
                        isLast = queueItem.index == state.items.size - 1,
                        onTap = { viewModel.skipToItem(queueItem.index) },
                        onRemove = { viewModel.removeItem(queueItem.index) },
                        onMoveUp = { viewModel.moveItemUp(queueItem.index) },
                        onMoveDown = { viewModel.moveItemDown(queueItem.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    item: QueueItem,
    isFirst: Boolean,
    isLast: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track chip — highlighted if currently playing
        Chip(
            modifier = Modifier.weight(1f),
            label = {
                Text(
                    text = item.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            secondaryLabel = if (item.artist.isNotEmpty()) {
                {
                    Text(
                        text = item.artist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            icon = if (item.isCurrent) {
                {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Now playing",
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            onClick = onTap,
            colors = if (item.isCurrent) {
                ChipDefaults.primaryChipColors()
            } else {
                ChipDefaults.secondaryChipColors()
            }
        )

        Spacer(modifier = Modifier.width(2.dp))

        // Move up
        Button(
            onClick = onMoveUp,
            enabled = !isFirst,
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            colors = ButtonDefaults.secondaryButtonColors()
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropUp,
                contentDescription = "Move up"
            )
        }

        // Move down
        Button(
            onClick = onMoveDown,
            enabled = !isLast,
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            colors = ButtonDefaults.secondaryButtonColors()
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Move down"
            )
        }

        // Remove
        Button(
            onClick = onRemove,
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            colors = ButtonDefaults.secondaryButtonColors()
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove"
            )
        }
    }
}
