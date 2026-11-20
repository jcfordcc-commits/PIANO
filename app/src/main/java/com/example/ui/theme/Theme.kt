package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AetherDarkColorScheme = darkColorScheme(
    primary = BrushedBrass,
    onPrimary = ObsidianVoid,
    primaryContainer = BrassDeep,
    onPrimaryContainer = BrassHighlight,
    secondary = BiolumCyan,
    onSecondary = ObsidianVoid,
    secondaryContainer = CyanDeep,
    onSecondaryContainer = BiolumCyan,
    tertiary = VioletAurora,
    onTertiary = ObsidianVoid,
    background = ObsidianVoid,
    onBackground = CrispIvoryText,
    surface = EbonyDeck,
    onSurface = CrispIvoryText,
    surfaceVariant = ElevatedConsole,
    onSurfaceVariant = MutedSlateText,
    outline = ConsoleBorder,
    error = CoralPulse,
    onError = ObsidianVoid
)

@Composable
fun AetherKeysTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherDarkColorScheme,
        typography = AetherTypography,
        content = content
    )
}
