package com.wearamp.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme

/**
 * Animated equalizer bars that bounce at different speeds to give a
 * "music is playing" visual effect.  When [isPlaying] is false the bars
 * sit at a minimal height.
 */
@Composable
fun EqualizerAnimation(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    barWidth: Dp = 4.dp,
    spacing: Dp = 3.dp,
    height: Dp = 24.dp,
    color: Color = MaterialTheme.colors.primary
) {
    val transition = rememberInfiniteTransition(label = "equalizer")

    // Each bar gets a slightly different duration so they look organic.
    val durations = listOf(420, 340, 500, 300, 460)
    val fractions = List(barCount) { index ->
        val fraction by transition.animateFloat(
            initialValue = 0.15f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = durations[index % durations.size],
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
        fraction
    }

    val totalWidth = barWidth * barCount + spacing * (barCount - 1)

    Canvas(
        modifier = modifier
            .width(totalWidth)
            .height(height)
    ) {
        val barW = barWidth.toPx()
        val gap = spacing.toPx()
        val maxH = size.height
        val minFraction = 0.15f

        fractions.forEachIndexed { index, animatedFraction ->
            val fraction = if (isPlaying) animatedFraction else minFraction
            val barH = maxH * fraction
            val x = index * (barW + gap)
            val y = maxH - barH // grow upward from the bottom

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}
