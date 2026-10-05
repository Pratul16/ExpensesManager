package com.pratul.expensesmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pratul.expensesmanager.data.defaultCatalog

/** True when the app is currently drawn dark, whether chosen by the user or by the phone. */
@Composable
@ReadOnlyComposable
fun isAppDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

// Same hues in both modes: deeper shades stand out on light backgrounds, softer ones on dark.
// The first 8 belong to the built-in categories; the rest are for categories people add.
private val lightPalette = listOf(
    Color(0xFFE53935), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF546E7A),
    Color(0xFF6D4C41), Color(0xFF9E9D24), Color(0xFF039BE5), Color(0xFF5E35B1),
    Color(0xFFF4511E), Color(0xFF7CB342), Color(0xFF3949AB), Color(0xFFF9A825)
)
private val darkPalette = listOf(
    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFF06292), Color(0xFF90A4AE),
    Color(0xFFA1887F), Color(0xFFDCE775), Color(0xFF4FC3F7), Color(0xFF9575CD),
    Color(0xFFFF8A65), Color(0xFFAED581), Color(0xFF7986CB), Color(0xFFFFD54F)
)
private val builtInNames = defaultCatalog.map { it.name }

@Composable
@ReadOnlyComposable
private fun palette() = if (isAppDark()) darkPalette else lightPalette

@Composable
@ReadOnlyComposable
fun categoryColor(category: String): Color {
    val palette = palette()
    val index = builtInNames.indexOf(category)
    if (index >= 0) return palette[index]
    return palette[builtInNames.size + Math.floorMod(category.lowercase().hashCode(), palette.size - builtInNames.size)]
}

/** Colours for a ranked list of slices (largest first), e.g. items in a chart. */
@Composable
@ReadOnlyComposable
fun rankColor(rank: Int): Color {
    val palette = palette()
    return palette[rank % palette.size]
}

/** Green for "you spent less", readable on both light and dark cards. */
@Composable
@ReadOnlyComposable
fun decreaseColor(): Color = if (isAppDark()) Color(0xFF81C784) else Color(0xFF2E7D32)
