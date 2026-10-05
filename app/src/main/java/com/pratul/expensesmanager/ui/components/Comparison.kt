package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.ui.theme.decreaseColor
import com.pratul.expensesmanager.data.totalsBy
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlin.math.abs


private data class Change(val label: String, val category: String, val before: Double, val after: Double) {
    val diff get() = after - before
}

/**
 * Adds a "before vs after" comparison to a LazyColumn: a totals card, then one card per
 * category or item with two bars, biggest change first.
 */
fun LazyListScope.comparisonItems(
    keyPrefix: String,
    before: List<Expense>,
    after: List<Expense>,
    beforeLabel: String,
    afterLabel: String,
    groupBy: GroupBy
) {
    val beforeTotals = before.totalsBy(groupBy).associateBy { it.label }
    val afterTotals = after.totalsBy(groupBy).associateBy { it.label }
    val changes = (afterTotals.keys + beforeTotals.keys).map { label ->
        val b = beforeTotals[label]
        val a = afterTotals[label]
        Change(label, (a ?: b)!!.category, b?.amount ?: 0.0, a?.amount ?: 0.0)
    }.sortedByDescending { abs(it.diff) }
    val beforeSum = before.sumOf { it.amount }
    val afterSum = after.sumOf { it.amount }
    val barMax = changes.maxOfOrNull { maxOf(it.before, it.after) } ?: 0.0

    item(key = "$keyPrefix-totals") {
        GradientCard {
            Row {
                TotalColumn(beforeLabel, beforeSum, Modifier.weight(1f))
                TotalColumn(afterLabel, afterSum, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                totalChangeText(beforeSum, afterSum, beforeLabel),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }

    if (changes.isEmpty()) {
        item(key = "$keyPrefix-empty") {
            Text(
                "Nothing logged in $beforeLabel or $afterLabel yet 🤷",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        return
    }

    item(key = "$keyPrefix-legend") {
        Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            ColorDot(MaterialTheme.colorScheme.outline)
            Text(" $beforeLabel", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(16.dp))
            ColorDot(MaterialTheme.colorScheme.primary)
            Text(" $afterLabel", style = MaterialTheme.typography.bodySmall)
        }
    }

    items(changes, key = { "$keyPrefix-row-${it.label}" }) { change ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(change.category, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(change.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (groupBy == GroupBy.Item && !change.label.startsWith(change.category)) {
                            Text(
                                change.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    DiffLabel(change.before, change.after)
                }
                Spacer(Modifier.height(10.dp))
                HorizontalBar(change.before, barMax, MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(6.dp))
                HorizontalBar(change.after, barMax, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TotalColumn(label: String, amount: Double, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.85f))
        CountUpText(amount, ::formatMoneyWhole, MaterialTheme.typography.headlineMedium)
    }
}

/** "▲ ₹500 (12%)" in red for more spending, "▼ ₹200 (5%)" in green for less. */
@Composable
private fun DiffLabel(before: Double, after: Double) {
    val diff = after - before
    val (text, color) = when {
        diff > 0 -> "▲ " + formatMoneyWhole(diff) + percentText(diff, before) to MaterialTheme.colorScheme.error
        diff < 0 -> "▼ " + formatMoneyWhole(-diff) + percentText(diff, before) to decreaseColor()
        else -> "No change" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
}

private fun percentText(diff: Double, before: Double): String =
    if (before > 0) " (${(abs(diff) / before * 100).toInt()}%)" else " (new)"

private fun totalChangeText(before: Double, after: Double, beforeLabel: String): String {
    val diff = after - before
    return when {
        before == 0.0 && after == 0.0 -> "Nothing spent in either yet 🌱"
        diff > 0 -> "▲ ${formatMoneyWhole(diff)} more than $beforeLabel" + percentText(diff, before) + " 😬"
        diff < 0 -> "▼ ${formatMoneyWhole(-diff)} less than $beforeLabel" + percentText(diff, before) + " 🎉"
        else -> "Exactly the same as $beforeLabel ⚖️"
    }
}
