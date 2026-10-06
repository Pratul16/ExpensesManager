package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.ui.components.AppBackground
import com.pratul.expensesmanager.ui.components.CountUpText
import com.pratul.expensesmanager.ui.components.DonutWithLegend
import com.pratul.expensesmanager.ui.components.ExpenseRow
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.ui.components.GroupByToggle
import com.pratul.expensesmanager.ui.components.breakdownOf
import com.pratul.expensesmanager.ui.components.categoryEmoji
import com.pratul.expensesmanager.ui.theme.isAppDark
import com.pratul.expensesmanager.util.DateRange
import com.pratul.expensesmanager.util.PeriodPreset
import com.pratul.expensesmanager.util.formatMoneyWhole
import com.pratul.expensesmanager.util.localMillisToPickerDate
import com.pratul.expensesmanager.util.rangeFor
import com.pratul.expensesmanager.util.rangeFromPicker

/**
 * Full-screen view of spending in any period (presets or a custom date range), optionally narrowed
 * to one category and item.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpendFinderDialog(
    expenses: List<Expense>,
    catalog: List<Category>,
    onDelete: (Expense) -> Unit,
    onClose: () -> Unit
) {
    var preset by rememberSaveable { mutableStateOf(PeriodPreset.ThisMonth) }
    var customStart by rememberSaveable { mutableStateOf<Long?>(null) }
    var customEnd by rememberSaveable { mutableStateOf<Long?>(null) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var item by rememberSaveable { mutableStateOf<String?>(null) }
    var groupBy by rememberSaveable { mutableStateOf(GroupBy.Category) }
    var pickingRange by remember { mutableStateOf(false) }

    val start = customStart
    val end = customEnd
    val range = if (preset == PeriodPreset.Custom && start != null && end != null) DateRange(start, end)
    else rangeFor(preset, expenses.minOfOrNull { it.dateMillis })

    val matching = expenses
        .filter { it.dateMillis in range }
        .filter { category == null || it.category == category }
        .filter { item == null || it.item == item }
        .sortedByDescending { it.dateMillis }
    val total = matching.sumOf { it.amount }
    val breakdown = breakdownOf(matching, groupBy)

    // Items offered in the filter: the category's list plus any used in past expenses
    val itemChoices = category?.let { c ->
        (catalog.firstOrNull { it.name == c }?.items.orEmpty() +
                expenses.filter { it.category == c && it.item.isNotBlank() }.map { it.item }).distinct()
    }.orEmpty()

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // This full-screen window draws behind the status bar, so match its icons to light/dark mode
        val dark = isAppDark()
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        AppBackground(Modifier.fillMaxSize()) {
            Column(Modifier.systemBarsPadding()) {
                SubScreenHeader("Spend finder 🔎", onClose)
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "presets") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (p in PeriodPreset.entries) {
                                FilterChip(
                                    selected = preset == p,
                                    onClick = { if (p == PeriodPreset.Custom) pickingRange = true else preset = p },
                                    label = { Text(p.label) }
                                )
                            }
                        }
                    }
                    item(key = "range") {
                        Text("📅 " + range.label(), style = MaterialTheme.typography.titleMedium)
                    }
                    item(key = "filters") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterDropdown(
                                label = category?.let { "${categoryEmoji(it)} $it" } ?: "All categories",
                                options = listOf<String?>(null) + catalog.map { it.name },
                                optionLabel = { it?.let { c -> "${categoryEmoji(c)} $c" } ?: "All categories" },
                                onPick = {
                                    category = it
                                    item = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterDropdown(
                                label = item ?: if (category == null) "Pick a category first" else "All items",
                                options = listOf<String?>(null) + itemChoices,
                                optionLabel = { it ?: "All items" },
                                onPick = { item = it },
                                enabled = category != null,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item(key = "toggle") { GroupByToggle(groupBy, { groupBy = it }) }
                    item(key = "total") {
                        GradientCard {
                            Text(
                                listOfNotNull(category?.let { "${categoryEmoji(it)} $it" }, item).joinToString(" · ")
                                    .ifEmpty { "Everything" },
                                style = MaterialTheme.typography.titleSmall
                            )
                            CountUpText(total, ::formatMoneyWhole, MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${matching.size} spend${if (matching.size == 1) "" else "s"} · " +
                                        "avg ${formatMoneyWhole(total / range.days())} a day over ${range.days()} " +
                                        "day${if (range.days() == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    item(key = "chart") {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                            DonutWithLegend(
                                ring = breakdown.ring,
                                legend = breakdown.legend,
                                centerTitle = preset.label.takeIf { preset != PeriodPreset.Custom } ?: "Selected",
                                centerValue = formatMoneyWhole(total),
                                emptyText = "Nothing spent in this period 🌱",
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    if (matching.isNotEmpty()) {
                        item(key = "listTitle") {
                            Text(
                                "${matching.size} spend${if (matching.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(matching, key = { it.id }) { expense ->
                            ExpenseRow(expense, onDelete = { onDelete(expense) })
                        }
                    }
                }
            }
        }
    }

    if (pickingRange) {
        val today = localMillisToPickerDate(System.currentTimeMillis())
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = localMillisToPickerDate(range.startMillis),
            initialSelectedEndDateMillis = localMillisToPickerDate(range.endMillis - 1),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pickerState.selectedStartDateMillis
                        if (s != null) {
                            // A single tapped day counts as a one-day range
                            val picked = rangeFromPicker(s, pickerState.selectedEndDateMillis ?: s)
                            customStart = picked.startMillis
                            customEnd = picked.endMillis
                            preset = PeriodPreset.Custom
                        }
                        pickingRange = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null
                ) { Text("Show") }
            },
            dismissButton = { TextButton(onClick = { pickingRange = false }) { Text("Cancel") } }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    options: List<String?>,
    optionLabel: @Composable (String?) -> String,
    onPick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label ▾", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (option in options) {
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onPick(option)
                        open = false
                    }
                )
            }
        }
    }
}
