package com.pratul.expensesmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.AppStyle

/** The look the user picked; components that draw their own backgrounds or gradients read it. */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.Vibrant }

// Chunkier, rounder corners everywhere
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Builds a full colour scheme from a handful of colours. [card] is what cards sit on; the other
 * surface containers are derived from it so menus, dialogs and the navigation bar match.
 */
private fun scheme(
    dark: Boolean,
    primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color, secondaryContainer: Color, onSecondaryContainer: Color,
    tertiary: Color, background: Color, card: Color, onSurface: Color, onSurfaceVariant: Color,
    outline: Color, outlineVariant: Color,
    navBar: Color = card, dialog: Color = if (dark) card else Color.White, surface: Color = background,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary,
        primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
        secondary = secondary, onSecondary = onSecondary,
        secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary, onTertiary = if (dark) Color(0xFF00281E) else Color.White,
        tertiaryContainer = if (dark) MintContainerDark else MintContainerLight,
        onTertiaryContainer = if (dark) OnMintContainerDark else OnMintContainerLight,
        background = background, onBackground = onSurface,
        surface = surface, onSurface = onSurface,
        surfaceVariant = card, onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = surface, surfaceContainerLow = navBar,
        // Menus (dropdowns) use surfaceContainer and must stay solid to be readable, even in Glass;
        // the bottom bar uses surfaceContainerLow, which may be see-through
        surfaceContainer = dialog, surfaceContainerHigh = dialog, surfaceContainerHighest = card,
        outline = outline, outlineVariant = outlineVariant,
    )
}

private fun vibrant(dark: Boolean) = if (dark) scheme(
    true, VioletDark, Color(0xFF2A0A6E), VioletContainerDark, OnVioletContainerDark,
    PinkDark, Color(0xFF4F0629), PinkContainerDark, OnPinkContainerDark,
    MintDark, BackgroundDark, SurfaceVariantDark, OnSurfaceDark, OnSurfaceVariantDark,
    OutlineDark, Color(0xFF3A3256), navBar = SurfaceDark, dialog = Color(0xFF211A37)
) else scheme(
    false, Violet, Color.White, VioletContainerLight, OnVioletContainerLight,
    Pink, Color.White, PinkContainerLight, OnPinkContainerLight,
    Mint, BackgroundLight, Color(0xFFE9E1FB), OnSurfaceLight, OnSurfaceVariantLight,
    OutlineLight, Color(0xFFDCD3F0), navBar = Color(0xFFF6F1FF), surface = SurfaceLight
)

/** Plain and high-contrast: black on white (or white on black), one clear blue accent. */
private fun simple(dark: Boolean) = if (dark) scheme(
    true, Color(0xFF93C5FD), Color(0xFF00213F), Color(0xFF1E3A5F), Color(0xFFDBEAFE),
    Color(0xFF93C5FD), Color(0xFF00213F), Color(0xFF1E3A5F), Color(0xFFDBEAFE),
    Color(0xFF86EFAC), Color.Black, Color(0xFF1C1C1E), Color.White, Color(0xFFD4D4D8),
    Color(0xFFA1A1AA), Color(0xFF3F3F46), navBar = Color(0xFF111113), dialog = Color(0xFF1C1C1E)
) else scheme(
    false, Color(0xFF1D4ED8), Color.White, Color(0xFFDBEAFE), Color(0xFF0B1F4D),
    Color(0xFF1D4ED8), Color.White, Color(0xFFDBEAFE), Color(0xFF0B1F4D),
    Color(0xFF047857), Color.White, Color(0xFFF2F2F7), Color(0xFF0A0A0A), Color(0xFF3F3F46),
    Color(0xFF71717A), Color(0xFFD4D4D8), navBar = Color(0xFFF8F8FA)
)

/**
 * Frosted glass: see-through cards, menus and bars over a soft colour wash (drawn by AppBackground).
 * Dialogs stay nearly solid so text in them is always easy to read.
 */
private fun glass(dark: Boolean) = if (dark) scheme(
    true, Color(0xFFD8CCFF), Color(0xFF2A0A6E), Color(0x664A2A9E), Color(0xFFF1EBFF),
    Color(0xFFFFB4DA), Color(0xFF4F0629), Color(0x667A1A4A), Color(0xFFFFE0EE),
    MintDark, Color.Transparent, Color(0x24FFFFFF), Color(0xFFF5F2FF), Color(0xFFD6CFEA),
    Color(0x99FFFFFF), Color(0x33FFFFFF), navBar = Color(0x59160F2A), dialog = Color(0xF21E1832),
    surface = Color(0x000F0B1A)
) else scheme(
    false, Color(0xFF6D28D9), Color.White, Color(0x99EDE4FF), Color(0xFF2E0D6B),
    Color(0xFFDB2777), Color.White, Color(0x99FFE0EE), Color(0xFF5C0A30),
    Mint, Color.Transparent, Color(0x8CFFFFFF), Color(0xFF1B1530), Color(0xFF4A4363),
    Color(0x99FFFFFF), Color(0xB3FFFFFF), navBar = Color(0xA6FFFFFF), dialog = Color(0xF5FFFFFF),
    surface = Color(0x00FFFFFF)
)

private fun ocean(dark: Boolean) = if (dark) scheme(
    true, Color(0xFF7DD3FC), Color(0xFF00344F), Color(0xFF0C4A6E), Color(0xFFE0F2FE),
    Color(0xFF5EEAD4), Color(0xFF00382F), Color(0xFF115E59), Color(0xFFCCFBF1),
    Color(0xFF86EFAC), Color(0xFF04131C), Color(0xFF10324A), Color(0xFFE6F4FB), Color(0xFFB4CFDD),
    Color(0xFF6B8A9A), Color(0xFF1E4A63), navBar = Color(0xFF0B2230), dialog = Color(0xFF123247)
) else scheme(
    false, Color(0xFF0369A1), Color.White, Color(0xFFE0F2FE), Color(0xFF082F49),
    Color(0xFF0D9488), Color.White, Color(0xFFCCFBF1), Color(0xFF042F2E),
    Color(0xFF15803D), Color(0xFFF0F9FF), Color(0xFFDCEFFA), Color(0xFF0B1F2A), Color(0xFF3F5A68),
    Color(0xFF7C99A8), Color(0xFFC9E3F1), navBar = Color(0xFFE8F5FC), surface = Color.White
)

private fun sunset(dark: Boolean) = if (dark) scheme(
    true, Color(0xFFFDBA74), Color(0xFF4A1D00), Color(0xFF7C2D12), Color(0xFFFFEDD5),
    Color(0xFFFDA4AF), Color(0xFF4C0519), Color(0xFF881337), Color(0xFFFFE4E6),
    Color(0xFF86EFAC), Color(0xFF1A0E08), Color(0xFF3A1F14), Color(0xFFFFF1E8), Color(0xFFE5C7B5),
    Color(0xFF9A7563), Color(0xFF4A2A1C), navBar = Color(0xFF26150D), dialog = Color(0xFF331B11)
) else scheme(
    false, Color(0xFFC2410C), Color.White, Color(0xFFFFEDD5), Color(0xFF431407),
    Color(0xFFE11D48), Color.White, Color(0xFFFFE4E6), Color(0xFF4C0519),
    Color(0xFF15803D), Color(0xFFFFF7ED), Color(0xFFFFE7D1), Color(0xFF26120A), Color(0xFF6B4A3A),
    Color(0xFFA8806C), Color(0xFFF5D6C0), navBar = Color(0xFFFFF1E3), surface = Color.White
)

fun colorSchemeFor(style: AppStyle, dark: Boolean): ColorScheme = when (style) {
    AppStyle.Vibrant -> vibrant(dark)
    AppStyle.Simple -> simple(dark)
    AppStyle.Glass -> glass(dark)
    AppStyle.Ocean -> ocean(dark)
    AppStyle.Sunset -> sunset(dark)
}

/** The app always uses its own colours (not the phone's wallpaper colours), in the chosen style. */
@Composable
fun ExpensesManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppStyle = AppStyle.Vibrant,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAppStyle provides style) {
        MaterialTheme(
            colorScheme = colorSchemeFor(style, darkTheme),
            // Same text sizes in every style, so layouts line up identically whichever theme is picked
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
