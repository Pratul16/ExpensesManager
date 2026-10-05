package com.pratul.expensesmanager.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.data.totalsBy
import com.pratul.expensesmanager.ui.theme.categoryColor
import com.pratul.expensesmanager.ui.theme.rankColor

// Item-wise rings show this many items in colour; the rest share one grey "Others" slice
private const val MAX_ITEM_SLICES = 9

/** Slices for the ring chart, and the legend that lists every category or item. */
data class Breakdown(val ring: List<ChartSlice>, val legend: List<ChartSlice>)

@Composable
fun breakdownOf(expenses: List<Expense>, groupBy: GroupBy): Breakdown {
    val totals = expenses.totalsBy(groupBy)
    if (groupBy == GroupBy.Category) {
        val slices = totals.map {
            ChartSlice("${categoryEmoji(it.label)}  ${it.label}", it.amount, categoryColor(it.label), category = it.label)
        }
        return Breakdown(slices, slices)
    }
    val otherColor = MaterialTheme.colorScheme.outline
    val legend = totals.mapIndexed { i, t ->
        // "Groceries (general)" stands for expenses with no item
        val item = if (expenses.any { it.category == t.category && it.item == t.label }) t.label else null
        ChartSlice(t.label, t.amount, if (i < MAX_ITEM_SLICES) rankColor(i) else otherColor, category = t.category, item = item)
    }
    val rest = legend.drop(MAX_ITEM_SLICES)
    val ring = legend.take(MAX_ITEM_SLICES) +
            if (rest.isEmpty()) emptyList() else listOf(ChartSlice("Others", rest.sumOf { it.value }, otherColor))
    return Breakdown(ring, legend)
}
