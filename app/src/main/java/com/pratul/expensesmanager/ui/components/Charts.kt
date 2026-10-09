package com.pratul.expensesmanager.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.util.formatMoney
import com.pratul.expensesmanager.util.formatMoneyCompact
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * One slice of a ring chart. [category] / [item] say what it stands for, so a legend row can be
 * swiped right to add a spend there (both null for groups like "Others").
 */
data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color,
    val category: String? = null,
    val item: String? = null
)

/**
 * Ring chart with a title and value in the middle. Tapping a slice reports it through [onSliceTap]
 * (null when tapping outside the ring); the [highlighted] slice is drawn thicker and the rest faded,
 * and the [selected] slice's name, amount and share of [total] replace the centre text.
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    centerTitle: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    selected: ChartSlice? = null,
    highlighted: ChartSlice? = selected,
    total: Double = slices.sumOf { it.value },
    onSliceTap: (ChartSlice?) -> Unit = {}
) {
    val ringTotal = slices.sumOf { it.value }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    // The ring sweeps in clockwise each time the data changes
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(slices) {
                    detectTapGestures { pos ->
                        val side = minOf(size.width, size.height).toFloat()
                        val stroke = side * 0.14f
                        val outer = side / 2f
                        val dx = pos.x - size.width / 2f
                        val dy = pos.y - size.height / 2f
                        val r = hypot(dx, dy)
                        // Generous touch area around the ring
                        if (ringTotal <= 0 || r < outer - stroke * 1.8f || r > outer + stroke * 0.4f) {
                            onSliceTap(null)
                            return@detectTapGestures
                        }
                        // Angle measured clockwise from 12 o'clock, like the drawing
                        val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 90 + 360) % 360).toFloat()
                        var start = 0f
                        for (slice in slices) {
                            val sweep = (slice.value / ringTotal * 360).toFloat()
                            if (angle < start + sweep) {
                                onSliceTap(slice)
                                return@detectTapGestures
                            }
                            start += sweep
                        }
                        onSliceTap(slices.lastOrNull())
                    }
                }
        ) {
            val stroke = size.minDimension * 0.14f
            // Leave room for the thicker highlighted slice
            val diameter = size.minDimension - stroke * 1.3f
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (ringTotal <= 0) return@Canvas
            val gap = if (slices.size > 1) 3f else 0f
            val limit = -90f + 360f * reveal.value
            var start = -90f
            for (slice in slices) {
                val sweep = (slice.value / ringTotal * 360).toFloat()
                val visible = minOf(sweep - gap, limit - start - gap / 2)
                if (visible <= 0f) break
                val isHighlighted = highlighted == slice
                drawArc(
                    color = if (highlighted == null || isHighlighted) slice.color else slice.color.copy(alpha = 0.3f),
                    startAngle = start + gap / 2,
                    sweepAngle = visible.coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(if (isHighlighted) stroke * 1.3f else stroke, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        Column(Modifier.fillMaxWidth(0.58f), horizontalAlignment = Alignment.CenterHorizontally) {
            if (selected == null) {
                Text(centerTitle, style = MaterialTheme.typography.bodySmall)
                Text(centerValue, style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    selected.label,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(formatMoneyWhole(selected.value), style = MaterialTheme.typography.titleLarge, maxLines = 1)
                val percent = if (total > 0) (selected.value / total * 100).toInt() else 0
                Text(
                    "$percent% of total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Colour dot, name, amount and share of total for each slice. Tapping a row selects it; swiping a row
 * right adds a spend in that category/item.
 */
@Composable
fun ChartLegend(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    selected: ChartSlice? = null,
    onSelect: (ChartSlice) -> Unit = {}
) {
    val total = slices.sumOf { it.value }
    val quickAdd = LocalQuickAdd.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (slice in slices) {
            val category = slice.category
            SwipeActions(
                onSwipeRight = if (category != null) ({ quickAdd(category, slice.item) }) else null,
                corner = 12.dp
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (slice == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .clickable { onSelect(slice) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorDot(slice.color)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        slice.label,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(formatMoney(slice.value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    val percent = if (total > 0) (slice.value / total * 100).toInt() else 0
                    Text(
                        "$percent%",
                        modifier = Modifier.width(44.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/**
 * Ring chart plus its legend, sharing one selection. [ring] may group small slices (e.g. "Others")
 * while [legend] lists everything; picking a legend row that's grouped highlights the group.
 */
@Composable
fun DonutWithLegend(
    ring: List<ChartSlice>,
    legend: List<ChartSlice>,
    centerTitle: String,
    centerValue: String,
    emptyText: String,
    modifier: Modifier = Modifier
) {
    var selectedLabel by remember(legend) { mutableStateOf<String?>(null) }
    val selected = legend.firstOrNull { it.label == selectedLabel } ?: ring.firstOrNull { it.label == selectedLabel }
    val highlighted = selected?.let { s -> ring.firstOrNull { it.label == s.label } ?: ring.lastOrNull { it.category == null } }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DonutChart(
            slices = ring,
            centerTitle = centerTitle,
            centerValue = centerValue,
            modifier = Modifier.size(170.dp),
            selected = selected,
            highlighted = highlighted,
            total = legend.sumOf { it.value },
            onSliceTap = { tapped -> selectedLabel = if (tapped == null || tapped.label == selectedLabel) null else tapped.label }
        )
        Spacer(Modifier.height(12.dp))
        if (legend.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        } else {
            Text(
                "Tap a slice or row for details · swipe a row right to add",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ChartLegend(
                legend,
                Modifier.fillMaxWidth(),
                selected = selected,
                onSelect = { selectedLabel = if (it.label == selectedLabel) null else it.label }
            )
        }
    }
}


@Composable
fun ColorDot(color: Color, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

data class Bar(val label: String, val value: Double)

/** Simple column chart. The highlighted bar gets the brand gradient, the rest a muted colour. */
@Composable
fun VerticalBarChart(
    bars: List<Bar>,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    barAreaHeight: Dp = 112.dp,
    showValues: Boolean = true
) {
    val max = bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    val strong = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))
    val muted = SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        bars.forEachIndexed { i, bar ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (showValues) {
                    Text(
                        if (bar.value > 0) formatMoneyCompact(bar.value) else "",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Box(Modifier.height(barAreaHeight).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val fraction = animatedFraction((bar.value / max).toFloat())
                    if (bar.value > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (highlightIndex == null || highlightIndex == i) strong else muted)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(bar.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
    }
}

/** One horizontal bar scaled against `max`, with its amount at the end. */
@Composable
fun HorizontalBar(value: Double, max: Double, color: Color, modifier: Modifier = Modifier) {
    val fraction = animatedFraction(if (max > 0) (value / max).toFloat() else 0f)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(10.dp)) {
            if (value > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(color)
                )
            }
        }
        Text(
            formatMoneyWhole(value),
            modifier = Modifier.width(88.dp).padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            textAlign = TextAlign.End
        )
    }
}

/** Grows smoothly from 0 to [target] the first time, then glides to each new value. */
@Composable
private fun animatedFraction(target: Float): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(800, easing = FastOutSlowInEasing)) }
    return anim.value
}
