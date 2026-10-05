package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.ui.components.GroupByToggle
import com.pratul.expensesmanager.ui.components.MonthPickerButton
import com.pratul.expensesmanager.ui.components.comparisonItems
import com.pratul.expensesmanager.util.MonthKey

@Composable
fun CompareScreen(expenses: List<Expense>) {
    val current = MonthKey.current()
    var beforeIndex by rememberSaveable { mutableIntStateOf(current.previous().toIndex()) }
    var afterIndex by rememberSaveable { mutableIntStateOf(current.toIndex()) }
    var groupBy by rememberSaveable { mutableStateOf(GroupBy.Category) }
    val before = MonthKey.fromIndex(beforeIndex)
    val after = MonthKey.fromIndex(afterIndex)

    // Use the full "July 2025" form when the two months are in different years
    val sameYear = before.year == after.year
    val beforeLabel = if (sameYear) before.monthName() else before.label()
    val afterLabel = if (sameYear) after.monthName() else after.label()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "title") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Then vs now ⚖️",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp)
                )
            }
        }
        item(key = "pickers") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthPickerButton(before, { beforeIndex = it.toIndex() }, Modifier.weight(1f))
                Text("  vs  ", style = MaterialTheme.typography.bodyMedium)
                MonthPickerButton(after, { afterIndex = it.toIndex() }, Modifier.weight(1f))
            }
        }
        item(key = "toggle") {
            GroupByToggle(groupBy, { groupBy = it }, Modifier.padding(bottom = 4.dp))
        }

        comparisonItems(
            keyPrefix = "months",
            before = expenses.filter { MonthKey.of(it.dateMillis) == before },
            after = expenses.filter { MonthKey.of(it.dateMillis) == after },
            beforeLabel = beforeLabel,
            afterLabel = afterLabel,
            groupBy = groupBy
        )
    }
}
