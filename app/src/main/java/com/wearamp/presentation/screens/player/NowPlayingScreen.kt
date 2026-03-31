package com.wearamp.presentation.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import com.wearamp.presentation.components.EqualizerAnimation

@Composable
fun NowPlayingScreen(
    onQueueClick: () -> Unit = {},
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val progress = if (state.durationMs > 0) {
        (state.currentPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Equalizer animation
        EqualizerAnimation(
            isPlaying = state.isPlaying,
            barCount = 5,
            barWidth = 4.dp,
            spacing = 3.dp,
            height = 20.dp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Track title
        Text(
            text = state.trackTitle,
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Artist name
        if (state.artistName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = state.artistName,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        val trackColor = MaterialTheme.colors.onBackground.copy(alpha = 0.2f)
        val progressColor = MaterialTheme.colors.primary
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(4.dp)
        ) {
            val barHeight = size.height
            val radius = CornerRadius(barHeight / 2f, barHeight / 2f)
            // Track background
            drawRoundRect(
                color = trackColor,
                size = Size(size.width, barHeight),
                cornerRadius = radius
            )
            // Filled progress
            if (progress > 0f) {
                drawRoundRect(
                    color = progressColor,
                    size = Size(size.width * progress, barHeight),
                    cornerRadius = radius
                )
            }
        }

        // Time labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(state.currentPositionMs),
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = formatTime(state.durationMs),
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Playback error
        val error = state.playbackError
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.caption3,
                color = MaterialTheme.colors.error,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Transport controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.skipToPrevious() },
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous"
                )
            }

            Button(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play"
                )
            }

            Button(
                onClick = { viewModel.skipToNext() },
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next"
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Queue button
        Button(
            onClick = onQueueClick,
            modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            colors = ButtonDefaults.secondaryButtonColors()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = "Queue"
            )
        }
    }
}

/** Format milliseconds as mm:ss */
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
