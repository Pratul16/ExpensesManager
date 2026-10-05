package com.pratul.expensesmanager.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.ui.components.categoryEmoji
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Story(val emoji: String, val kicker: String, val headline: String, val detail: String)

// Each story card gets its own bold gradient; they're full-screen, so they look the same in light and dark mode
private val storyGradients = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFFDB2777)),
    listOf(Color(0xFFF97316), Color(0xFFDB2777)),
    listOf(Color(0xFF059669), Color(0xFF0EA5E9)),
    listOf(Color(0xFFDB2777), Color(0xFF9333EA)),
    listOf(Color(0xFF2563EB), Color(0xFF7C3AED)),
    listOf(Color(0xFFEAB308), Color(0xFFF97316)),
    listOf(Color(0xFF111827), Color(0xFF7C3AED))
)

/**
 * Instagram-story style summary of a month. Swipe or tap the sides to move between cards.
 * For the month in progress, [previousTotal] covers the same days of last month.
 */
@Composable
fun WrappedDialog(month: MonthKey, expenses: List<Expense>, previousTotal: Double, onClose: () -> Unit) {
    val partial = month == MonthKey.current()
    val stories = buildStories(month, expenses, previousTotal, partial)
    val pager = rememberPagerState { stories.size }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onClose,
        // Full-screen, drawing behind the status and navigation bars like a real story
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                StoryCard(
                    story = stories[page],
                    colors = storyGradients[page % storyGradients.size],
                    visible = pager.currentPage == page,
                    onTapLeft = { scope.launch { pager.animateScrollToPage((page - 1).coerceAtLeast(0)) } },
                    onTapRight = {
                        if (page == stories.lastIndex) onClose()
                        else scope.launch { pager.animateScrollToPage(page + 1) }
                    }
                )
            }

            // Story progress bars and close button
            Column(Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in stories.indices) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = if (i <= pager.currentPage) 0.95f else 0.35f))
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClose) {
                        Text("✕ Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryCard(
    story: Story,
    colors: List<Color>,
    visible: Boolean,
    onTapLeft: () -> Unit,
    onTapRight: () -> Unit
) {
    // The big emoji pops in each time its card comes into view
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(visible) {
        if (visible) {
            pop.snapTo(0.4f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors))
            .pointerInput(Unit) {
                detectTapGestures { offset -> if (offset.x < size.width / 3) onTapLeft() else onTapRight() }
            }
            .systemBarsPadding()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(story.emoji, style = TextStyle(fontSize = 88.sp), modifier = Modifier.scale(pop.value))
            Spacer(Modifier.height(24.dp))
            Text(story.kicker.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(8.dp))
            Text(story.headline, style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(story.detail, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
        }
        Text(
            "tap to continue →",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun buildStories(month: MonthKey, expenses: List<Expense>, previousTotal: Double, partial: Boolean): List<Story> {
    val total = expenses.sumOf { it.amount }
    val stories = mutableListOf<Story>()

    stories += Story(
        "✨",
        month.label() + if (partial) " (so far)" else "",
        "Your month, wrapped",
        "${expenses.size} spend${if (expenses.size == 1) "" else "s"} · ${formatMoneyWhole(total)} in total"
    )

    expenses.groupBy { it.category }
        .maxByOrNull { (_, list) -> list.sumOf { it.amount } }
        ?.let { (category, list) ->
            val amount = list.sumOf { it.amount }
            stories += Story(
                categoryEmoji(category),
                "Top category",
                "$category ran the show",
                "${formatMoneyWhole(amount)} · ${(amount / total * 100).toInt()}% of everything you spent"
            )
        }

    expenses.filter { it.item.isNotBlank() }
        .groupBy { it.item }
        .maxByOrNull { (_, list) -> list.size }
        ?.takeIf { (_, list) -> list.size >= 2 }
        ?.let { (item, list) ->
            stories += Story(
                categoryEmoji(list.first().category),
                "Your ride-or-die",
                item,
                "Bought ${list.size} times · ${formatMoneyWhole(list.sumOf { it.amount })}"
            )
        }

    expenses.maxByOrNull { it.amount }?.let { e ->
        stories += Story(
            "💸",
            "Biggest splurge",
            formatMoneyWhole(e.amount),
            "on ${e.item.ifBlank { e.category }} · ${formatDate(e.dateMillis)}"
        )
    }

    expenses.groupBy { dayKey(it.dateMillis) }
        .maxByOrNull { (_, list) -> list.size }
        ?.takeIf { (_, list) -> list.size >= 2 }
        ?.let { (_, list) ->
            stories += Story(
                "📅",
                "Busiest day",
                SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(list.first().dateMillis)),
                "${list.size} spends · ${formatMoneyWhole(list.sumOf { it.amount })}"
            )
        }

    val prevName = month.previous().monthName()
    val versus = if (partial) "the same days of $prevName" else prevName
    stories += when {
        previousTotal <= 0 -> Story("📒", "Compared to $prevName", "Fresh start", "Nothing logged in $prevName, so this is your baseline.")
        total < previousTotal -> Story(
            "💅", "Compared to $versus", "Saving era",
            "${formatMoneyWhole(previousTotal - total)} less (${((previousTotal - total) / previousTotal * 100).toInt()}% down). Love that for you."
        )
        total > previousTotal -> Story(
            "🛍️", "Compared to $versus", "Treat-yourself era?",
            "${formatMoneyWhole(total - previousTotal)} more (${((total - previousTotal) / previousTotal * 100).toInt()}% up). No judgement… mostly."
        )
        else -> Story("⚖️", "Compared to $versus", "Perfectly balanced", "Exactly the same as $prevName. Spooky.")
    }

    stories += Story("🎬", "That's a wrap", "See you next month", "Screenshot your favourite card and flex it 📸")
    return stories
}

private fun dayKey(millis: Long): Int {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}
