package com.pratul.expensesmanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.categoryEmojiChoices
import com.pratul.expensesmanager.data.defaultEmojiFor
import com.pratul.expensesmanager.ui.theme.HeroGradientDark
import com.pratul.expensesmanager.ui.theme.HeroGradientLight
import com.pratul.expensesmanager.ui.theme.categoryColor
import com.pratul.expensesmanager.ui.theme.isAppDark
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---------- Category emoji ----------

/** The current category list, so any screen can look up a category's emoji. */
val LocalCatalog = compositionLocalOf<List<Category>> { emptyList() }

@Composable
@ReadOnlyComposable
fun categoryEmoji(category: String): String =
    LocalCatalog.current.firstOrNull { it.name == category }?.emoji ?: defaultEmojiFor(category)

/** The category's emoji on a soft circle of its colour. */
@Composable
fun CategoryBadge(category: String, size: Dp = 40.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(categoryColor(category).copy(alpha = if (isAppDark()) 0.28f else 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(categoryEmoji(category), style = TextStyle(fontSize = (size.value * 0.5f).sp))
    }
}

// ---------- Gradient hero card ----------

@Composable
@ReadOnlyComposable
fun heroBrush(): Brush = Brush.linearGradient(if (isAppDark()) HeroGradientDark else HeroGradientLight)

/** A big rounded card on the brand gradient. Everything inside is drawn in white. */
@Composable
fun GradientCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(heroBrush())
            .padding(24.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { content() }
    }
}

// ---------- Count-up number ----------

/** Shows an amount that counts up from 0 whenever [target] changes. */
@Composable
fun CountUpText(target: Double, format: (Double) -> String, style: TextStyle, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(target) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Text(format(target * progress.value), style = style, modifier = modifier)
}

// ---------- Category dialog ----------

/** Name plus emoji for a new or edited category. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    title: String,
    confirmLabel: String,
    existing: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
    initialName: String = "",
    initialEmoji: String = categoryEmojiChoices.first()
) {
    var name by remember { mutableStateOf(initialName) }
    var emoji by remember { mutableStateOf(initialEmoji) }
    val trimmed = name.trim()
    val duplicate = existing.any { it.equals(trimmed, ignoreCase = true) && !it.equals(initialName, ignoreCase = true) }
    val changed = trimmed != initialName || emoji != initialEmoji

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    leadingIcon = { Text(emoji, style = TextStyle(fontSize = 22.sp)) },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text("Already in the list") }) else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pick an emoji", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (choice in (listOf(initialEmoji) + categoryEmojiChoices).distinct()) {
                        val selected = choice == emoji
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                .clickable { emoji = choice },
                            contentAlignment = Alignment.Center
                        ) { Text(choice, style = TextStyle(fontSize = 22.sp)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(trimmed.replaceFirstChar { it.uppercase() }, emoji) },
                enabled = trimmed.isNotEmpty() && !duplicate && changed
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------- Confetti ----------

private class Piece(val angle: Float, val speed: Float, val spin: Float, val color: Color, val w: Float, val h: Float)

/** A one-off confetti burst from the bottom centre. Changing [trigger] fires it again. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    val colors = listOf(
        MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary, Color(0xFFFFC53D), Color(0xFF38BDF8)
    )
    val pieces = remember(trigger) {
        List(70) {
            Piece(
                // Upward fan, roughly -150° to -30°
                angle = Math.toRadians(-150.0 + Random.nextDouble() * 120.0).toFloat(),
                speed = 900f + Random.nextFloat() * 1300f,
                spin = Random.nextFloat() * 720f - 360f,
                color = colors.random(),
                w = 10f + Random.nextFloat() * 10f,
                h = 6f + Random.nextFloat() * 8f
            )
        }
    }
    val time = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { time.animateTo(1f, tween(1600, easing = LinearEasing)) }
    if (time.value >= 1f) return

    Canvas(modifier.fillMaxSize()) {
        val t = time.value * 1.6f // seconds
        val gravity = 1800f
        val origin = Offset(size.width / 2, size.height * 0.85f)
        val alpha = (1f - time.value).coerceIn(0f, 1f)
        for (p in pieces) {
            val x = origin.x + cos(p.angle) * p.speed * t
            val y = origin.y + sin(p.angle) * p.speed * t + 0.5f * gravity * t * t
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - p.w / 2, y - p.h / 2), Size(p.w, p.h))
            }
        }
    }
}
