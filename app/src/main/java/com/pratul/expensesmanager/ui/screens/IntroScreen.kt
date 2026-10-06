package com.pratul.expensesmanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.ui.lock.LockBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Opening animation built from the app logo: the tile pops in, the white ring draws itself, the
 * amber slice bounces into place, then the name rises in. "Made with ❤️ by Pratul" sits at the
 * bottom. Calls [onFinished] after about 2.5 s; tapping anywhere skips ahead.
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val tile = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val slice = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { tile.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
        delay(250)
        ring.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        launch { slice.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)) }
        delay(150)
        text.animateTo(1f, tween(500))
        delay(900)
        onFinished()
    }

    // A gentle heartbeat for the ❤️
    val beat by rememberInfiniteTransition(label = "heart").animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "beat"
    )

    LockBackground(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFinished)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(
                Modifier
                    .size(150.dp)
                    .graphicsLayer(scaleX = tile.value, scaleY = tile.value, alpha = tile.value.coerceIn(0f, 1f))
            ) {
                // The launcher icon's tile and ring, drawn at the icon's own 108-unit proportions
                val unit = size.minDimension / 108f
                drawRoundRect(
                    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFFC026D3), Color(0xFFF43F5E))),
                    cornerRadius = CornerRadius(30 * unit)
                )
                drawRoundRect(Color.White.copy(alpha = 0.18f), cornerRadius = CornerRadius(30 * unit), style = Stroke(1.5f * unit))
                val r = 22 * unit
                val topLeft = Offset(center.x - r, center.y - r)
                val box = Size(2 * r, 2 * r)
                drawArc(Color.White, -90f, 250f * ring.value, false, topLeft, box, style = Stroke(10 * unit))
                if (slice.value > 0f) {
                    // The amber slice flies out from the centre and lands in its gap
                    val pop = slice.value
                    val gr = r * (0.4f + 0.6f * pop)
                    drawArc(
                        Color(0xFFFFD54F), -90f + 265f, 80f, false,
                        Offset(center.x - gr, center.y - gr), Size(2 * gr, 2 * gr),
                        style = Stroke(10 * unit * pop.coerceIn(0.2f, 1.2f)),
                        alpha = pop.coerceIn(0f, 1f)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Column(
                Modifier.graphicsLayer(alpha = text.value, translationY = (1f - text.value) * 40f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Expenses Manager", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Track · Compare · Wrapped",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
                .alpha(text.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Made with ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Box(Modifier.scale(beat)) { Text("❤️", style = MaterialTheme.typography.bodyLarge) }
            Text(" by Pratul", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
