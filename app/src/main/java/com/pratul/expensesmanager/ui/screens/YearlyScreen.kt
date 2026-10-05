package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.data.totalsBy
import com.pratul.expensesmanager.ui.components.Bar
import com.pratul.expensesmanager.ui.components.CategoryBadge
import com.pratul.expensesmanager.ui.components.GroupByToggle
import com.pratul.expensesmanager.ui.components.VerticalBarChart
import com.pratul.expensesmanager.ui.components.YearPickerButton
import com.pratul.expensesmanager.ui.components.comparisonItems
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatMoney
import com.pratul.expensesmanager.util.formatMoneyWhole
import com.pratul.expensesmanager.util.shortMonthName
import com.pratul.expensesmanager.util.yearOf

@Composable
fun YearlyScreen(expenses: List<Expense>) {
    val currentYear = MonthKey.current().year
    val byYear = expenses.groupBy { yearOf(it.dateMillis) }
    val years = byYear.keys.sortedDescending()
    // Years offered in the compare pickers: every year with data, plus this year and last year
    val pickerYears = (years + currentYear + (currentYear - 1)).distinct().sortedDescending()

    var groupBy by rememberSaveable { mutableStateOf(GroupBy.Category) }
    var expandedYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var beforeYear by rememberSaveable { mutableIntStateOf(currentYear - 1) }
    var afterYear by rememberSaveable { mutableIntStateOf(currentYear) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your years in money 📅",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                )
            }
        }

        if (years.isEmpty()) {
            item(key = "empty") {
                Text("No spends yet. Tap + to log your first one 🚀", style = MaterialTheme.typography.bodyLarge)
            }
            return@LazyColumn
        }

        item(key = "toggle") { GroupByToggle(groupBy, { groupBy = it }) }

        item(key = "chart") {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total per year", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    // Oldest on the left; at most the last 6 years so the bars stay readable
                    val shown = years.take(6).reversed()
                    VerticalBarChart(
                        bars = shown.map { Bar(it.toString(), byYear.getValue(it).sumOf { e -> e.amount }) },
                        highlightIndex = shown.indexOf(currentYear).takeIf { it >= 0 }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "All time: " + formatMoney(expenses.sumOf { it.amount }),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item(key = "compare-title") {
            Text(
                "Year vs year 🥊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        item(key = "compare-pickers") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                YearPickerButton(beforeYear, pickerYears, { beforeYear = it }, Modifier.weight(1f))
                Text("  vs  ", style = MaterialTheme.typography.bodyMedium)
                YearPickerButton(afterYear, pickerYears, { afterYear = it }, Modifier.weight(1f))
            }
        }
        comparisonItems(
            keyPrefix = "years",
            before = byYear[beforeYear].orEmpty(),
            after = byYear[afterYear].orEmpty(),
            beforeLabel = beforeYear.toString(),
            afterLabel = afterYear.toString(),
            groupBy = groupBy
        )

        item(key = "years-title") {
            Text(
                "Year by year 🗓️",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        items(years, key = { "year-$it" }) { year ->
            YearCard(
                year = year,
                expenses = byYear.getValue(year),
                groupBy = groupBy,
                isCurrentYear = year == currentYear,
                expanded = expandedYear == year,
                onClick = { expandedYear = if (expandedYear == year) -1 else year }
            )
        }
    }
}

@Composable
private fun YearCard(
    year: Int,
    expenses: List<Expense>,
    groupBy: GroupBy,
    isCurrentYear: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val total = expenses.sumOf { it.amount }
    // Average over the months that have passed: all 12 for past years
    val monthsElapsed = if (isCurrentYear) MonthKey.current().month + 1 else 12

    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(year.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Avg ${formatMoneyWhole(total / monthsElapsed)} / month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(formatMoney(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (expanded) "  ▲" else "  ▼", style = MaterialTheme.typography.bodySmall)
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                Text("By month", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val monthTotals = DoubleArray(12)
                for (e in expenses) monthTotals[MonthKey.of(e.dateMillis).month] += e.amount
                VerticalBarChart(
                    bars = (0 until 12).map { Bar(shortMonthName(it).take(3), monthTotals[it]) },
                    barAreaHeight = 90.dp,
                    showValues = false,
                    highlightIndex = if (isCurrentYear) MonthKey.current().month else null
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    if (groupBy == GroupBy.Category) "By category" else "By item",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(8.dp))
                for (t in expenses.totalsBy(groupBy)) {
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(t.category, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            t.label,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(formatMoney(t.amount), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
