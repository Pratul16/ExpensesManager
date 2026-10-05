package com.pratul.expensesmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletContainerLight,
    onPrimaryContainer = OnVioletContainerLight,
    secondary = Pink,
    onSecondary = Color.White,
    secondaryContainer = PinkContainerLight,
    onSecondaryContainer = OnPinkContainerLight,
    tertiary = Mint,
    onTertiary = Color.White,
    tertiaryContainer = MintContainerLight,
    onTertiaryContainer = OnMintContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFCFAFF),
    surfaceContainer = Color(0xFFF6F1FF),
    surfaceContainerHigh = SurfaceVariantLight,
    surfaceContainerHighest = Color(0xFFE9E1FB),
    outline = OutlineLight,
    outlineVariant = Color(0xFFDCD3F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = VioletDark,
    onPrimary = Color(0xFF2A0A6E),
    primaryContainer = VioletContainerDark,
    onPrimaryContainer = OnVioletContainerDark,
    secondary = PinkDark,
    onSecondary = Color(0xFF4F0629),
    secondaryContainer = PinkContainerDark,
    onSecondaryContainer = OnPinkContainerDark,
    tertiary = MintDark,
    onTertiary = Color(0xFF003828),
    tertiaryContainer = MintContainerDark,
    onTertiaryContainer = OnMintContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = BackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = Color(0xFF0B0814),
    surfaceContainerLow = Color(0xFF151024),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF211A37),
    surfaceContainerHighest = SurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF3A3256)
)

// Chunkier, rounder corners everywhere
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/** The app always uses its own brand colours (not the phone's wallpaper colours). */
@Composable
fun ExpensesManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
