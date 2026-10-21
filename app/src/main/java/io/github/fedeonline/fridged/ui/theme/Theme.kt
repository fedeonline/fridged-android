package io.github.fedeonline.fridged.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Always dark, with the fixed pastel [Palette]: the app has its own identity, not the wallpaper's. */
private val colors = darkColorScheme(
    primary = Palette.Mint,
    onPrimary = Palette.OnMint,
    primaryContainer = Palette.Mint,
    onPrimaryContainer = Palette.OnMint,
    secondary = Palette.Blue,
    onSecondary = Palette.OnBlue,
    secondaryContainer = Palette.SurfaceHigh,
    onSecondaryContainer = Palette.OnSurface,
    tertiary = Palette.Lilac,
    onTertiary = Palette.OnLilac,
    background = Palette.Background,
    onBackground = Palette.OnSurface,
    surface = Palette.Background,
    onSurface = Palette.OnSurface,
    surfaceVariant = Palette.Surface,
    onSurfaceVariant = Palette.Muted,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.Surface,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Outline,
    outlineVariant = Palette.Track,
    error = Palette.Red,
    onError = Palette.OnRed,
    errorContainer = Palette.Red,
    onErrorContainer = Palette.OnRed,
)

@Composable
fun FridgedTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = FridgedTypography, content = content)
}
