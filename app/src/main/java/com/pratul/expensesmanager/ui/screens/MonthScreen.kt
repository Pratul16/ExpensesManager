package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.ui.components.AiAssistantDialog
import com.pratul.expensesmanager.ui.components.CountUpText
import com.pratul.expensesmanager.ui.components.DonutWithLegend
import com.pratul.expensesmanager.ui.components.ExpenseRow
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.ui.components.GroupByToggle
import com.pratul.expensesmanager.ui.components.breakdownOf
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatMoneyCompact
import com.pratul.expensesmanager.util.formatMoneyWhole
import com.pratul.expensesmanager.util.shortMonthName
import java.util.Calendar
import kotlin.math.abs

private enum class MonthViewTab { Breakdown, Calendar }

@Composable
fun MonthScreen(
    expenses: List<Expense>,
    onAddExpense: (Expense) -> Unit = {},
    onDelete: (Expense) -> Unit
) {
    val current = MonthKey.current()
    var selectedIndex by rememberSaveable { mutableIntStateOf(current.toIndex()) }
    var groupBy by rememberSaveable { mutableStateOf(GroupBy.Category) }
    var showWrapped by rememberSaveable { mutableStateOf(false) }
    var showAiAssistant by rememberSaveable { mutableStateOf(false) }
    var viewTab by rememberSaveable { mutableStateOf(MonthViewTab.Breakdown) }
    var selectedFilterDay by rememberSaveable { mutableStateOf<Int?>(null) }
    val month = MonthKey.fromIndex(selectedIndex)

    val monthExpenses = expenses
        .filter { MonthKey.of(it.dateMillis) == month }
        .sortedByDescending { it.dateMillis }

    // Filter list by selected calendar day if user tapped a date
    val displayedExpenses = if (selectedFilterDay != null) {
        monthExpenses.filter { dayOfMonth(it.dateMillis) == selectedFilterDay }
    } else {
        monthExpenses
    }

    // For the month in progress, compare with the same days of last month, not the whole month
    val dayLimit = if (month == current) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else 31
    val previousTotal = expenses
        .filter { MonthKey.of(it.dateMillis) == month.previous() && dayOfMonth(it.dateMillis) <= dayLimit }
        .sumOf { it.amount }
    val breakdown = breakdownOf(monthExpenses, groupBy)
    val total = monthExpenses.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Where'd your money go?",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4285F4), Color(0xFF9C27B0), Color(0xFFE91E63))
                            )
                        )
                        .clickable { showAiAssistant = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦ AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        item {
            GradientCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIndex--; selectedFilterDay = null }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month")
                    }
                    Text(
                        text = if (month == current) "This month" else month.label(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { selectedIndex++; selectedFilterDay = null }, enabled = month < current) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = "Next month",
                            tint = if (month < current) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("You've spent", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
                CountUpText(
                    target = total,
                    format = ::formatMoneyWhole,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val prevShort = shortMonthName(month.previous().month)
                    Pill(
                        vsLastMonthText(total, previousTotal, if (month == current) "$prevShort 1–$dayLimit" else prevShort),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (monthExpenses.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Pill("✨ Wrapped", Modifier.clickable { showWrapped = true })
                    }
                }
            }
        }

        // View Mode Switcher: Breakdown vs Calendar Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (viewTab == MonthViewTab.Breakdown) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { viewTab = MonthViewTab.Breakdown }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📊 Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (viewTab == MonthViewTab.Breakdown) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (viewTab == MonthViewTab.Calendar) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { viewTab = MonthViewTab.Calendar }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "📅 Calendar",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (viewTab == MonthViewTab.Calendar) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (viewTab == MonthViewTab.Breakdown) {
            item { GroupByToggle(groupBy, { groupBy = it }, Modifier.padding(top = 2.dp)) }

            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                    DonutWithLegend(
                        ring = breakdown.ring,
                        legend = breakdown.legend,
                        centerTitle = month.monthName(),
                        centerValue = formatMoneyWhole(total),
                        emptyText = "No spends yet. Your wallet is thriving 💅",
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            // Month Calendar View Card
            item {
                MonthCalendarCard(
                    month = month,
                    expenses = monthExpenses,
                    selectedDay = selectedFilterDay,
                    onSelectDay = { selectedFilterDay = it }
                )
            }
        }

        if (displayedExpenses.isNotEmpty()) {
            item {
                Row(Modifier.padding(start = 4.dp, top = 12.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (selectedFilterDay != null) "Spends on ${month.monthName()} $selectedFilterDay (${displayedExpenses.size})"
                        else "${displayedExpenses.size} spend${if (displayedExpenses.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "swipe → add · ← delete",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(displayedExpenses, key = { it.id }) { expense ->
                ExpenseRow(expense, onDelete = { onDelete(expense) })
            }
        }
    }

    if (showWrapped) {
        WrappedDialog(
            month = month,
            expenses = monthExpenses,
            previousTotal = previousTotal,
            onClose = { showWrapped = false }
        )
    }

    if (showAiAssistant) {
        AiAssistantDialog(
            expenses = expenses,
            onAddExpenses = { candidates ->
                val now = System.currentTimeMillis()
                var idOffset = 0L
                candidates.forEach { cand ->
                    onAddExpense(
                        Expense(
                            id = now + (idOffset++),
                            amount = cand.amount,
                            category = cand.category,
                            item = cand.item,
                            note = "Added via Google AI",
                            dateMillis = now
                        )
                    )
                }
            },
            onClose = { showAiAssistant = false }
        )
    }
}

@Composable
private fun MonthCalendarCard(
    month: MonthKey,
    expenses: List<Expense>,
    selectedDay: Int?,
    onSelectDay: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance().apply {
        clear()
        set(month.year, month.month, 1)
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    // 0 = Sunday, 1 = Monday, ..., 6 = Saturday
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

    val dailyTotals = remember(expenses) {
        expenses.groupBy { dayOfMonth(it.dateMillis) }
            .mapValues { (_, exps) -> exps.sumOf { it.amount } }
    }

    val todayCalendar = Calendar.getInstance()
    val isCurrentMonth = month == MonthKey.current()
    val today = if (isCurrentMonth) todayCalendar.get(Calendar.DAY_OF_MONTH) else -1

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${month.monthName()} ${month.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (selectedDay != null) {
                    Text(
                        "Show All Days",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onSelectDay(null) }
                    )
                }
            }

            // Day headers
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar Days Grid
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOfWeek + 1

                            if (dayNum in 1..daysInMonth) {
                                val dayAmount = dailyTotals[dayNum] ?: 0.0
                                val isToday = dayNum == today
                                val isSelected = dayNum == selectedDay

                                val bgColor = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    dayAmount > 0 -> MaterialTheme.colorScheme.surface
                                    else -> Color.Transparent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .clickable {
                                            if (isSelected) onSelectDay(null) else onSelectDay(dayNum)
                                        }
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else if (isToday) MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (dayAmount > 0) {
                                            Text(
                                                text = formatMoneyCompact(dayAmount),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 9.sp,
                                                maxLines = 1
                                            )
                                        } else {
                                            Spacer(Modifier.height(10.dp))
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small frosted label on the gradient card. */
@Composable
private fun Pill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun vsLastMonthText(now: Double, before: Double, beforeName: String): String {
    if (before <= 0) return if (now > 0) "First spends logged 🚀" else "Fresh start 🌱"
    val percent = (abs(now - before) / before * 100).toInt()
    return when {
        now < before -> "▼ $percent% vs $beforeName 🎉"
        now > before -> "▲ $percent% vs $beforeName 😬"
        else -> "Same as $beforeName"
    }
}

private fun dayOfMonth(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)
