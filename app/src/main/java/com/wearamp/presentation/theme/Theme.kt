package com.wearamp.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.Typography

private val WearAmpColors = Colors(
    primary = WearPrimary,
    primaryVariant = PlexOrangeDark,
    secondary = WearSecondary,
    background = WearBackground,
    surface = WearSurface,
    onPrimary = WearBackground,
    onSecondary = WearBackground,
    onBackground = WearOnSurface,
    onSurface = WearOnSurface,
    error = WearError,
    onError = WearBackground
)

@Composable
fun WearAmpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = WearAmpColors,
        content = content
    )
}
