package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomDarkColorScheme = darkColorScheme(
    primary = YoutubeRed,
    secondary = TextSecondary,
    background = DeepBlack,
    surface = StudioCard,
    onPrimary = PureWhite,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = TextSecondary,
    outline = AccentGrey
)

@Composable
fun MyApplicationTheme(
    // Force dark mode as default as requested by the user
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
