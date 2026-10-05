package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.ui.components.CountUpText
import com.pratul.expensesmanager.ui.components.ExpenseRow
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.ui.components.GroupByToggle
import com.pratul.expensesmanager.ui.components.DonutWithLegend
import com.pratul.expensesmanager.ui.components.breakdownOf
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatMoneyWhole
import com.pratul.expensesmanager.util.shortMonthName
import java.util.Calendar
import kotlin.math.abs


@Composable
fun MonthScreen(expenses: List<Expense>, onDelete: (Expense) -> Unit) {
    val current = MonthKey.current()
    var selectedIndex by rememberSaveable { mutableIntStateOf(current.toIndex()) }
    var groupBy by rememberSaveable { mutableStateOf(GroupBy.Category) }
    var showWrapped by rememberSaveable { mutableStateOf(false) }
    val month = MonthKey.fromIndex(selectedIndex)

    val monthExpenses = expenses
        .filter { MonthKey.of(it.dateMillis) == month }
        .sortedByDescending { it.dateMillis }
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
                    "Where'd your money go? 👀",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        }

        item {
            GradientCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedIndex-- }) {
                        Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous month")
                    }
                    Text(
                        text = if (month == current) "This month" else month.label(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { selectedIndex++ }, enabled = month < current) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_right),
                            contentDescription = "Next month",
                            tint = if (month < current) Color.White else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("You've spent", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                CountUpText(
                    target = total,
                    format = ::formatMoneyWhole,
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // e.g. "Sep 1–5" while this month is still going, "Sep" for a finished month
                    val prevShort = shortMonthName(month.previous().month)
                    Pill(vsLastMonthText(total, previousTotal, if (month == current) "$prevShort 1–$dayLimit" else prevShort))
                    Spacer(Modifier.weight(1f))
                    if (monthExpenses.isNotEmpty()) {
                        Pill("✨ Wrapped", Modifier.clickable { showWrapped = true })
                    }
                }
            }
        }

        item { GroupByToggle(groupBy, { groupBy = it }, Modifier.padding(top = 4.dp)) }

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

        if (monthExpenses.isNotEmpty()) {
            item {
                Row(Modifier.padding(start = 4.dp, top = 12.dp), verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${monthExpenses.size} spend${if (monthExpenses.size == 1) "" else "s"}",
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
            items(monthExpenses, key = { it.id }) { expense ->
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
}

/** Small frosted label on the gradient card. */
@Composable
private fun Pill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
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
