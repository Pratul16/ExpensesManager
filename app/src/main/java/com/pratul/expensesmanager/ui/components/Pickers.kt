package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.GroupBy
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.shortMonthName

/** The "Category wise | Item wise" switch used on every tab. */
@Composable
fun GroupByToggle(selected: GroupBy, onChange: (GroupBy) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        GroupBy.entries.forEachIndexed { i, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onChange(option) },
                shape = SegmentedButtonDefaults.itemShape(i, GroupBy.entries.size)
            ) { Text(option.label) }
        }
    }
}

/**
 * A scrolling row of chips plus an "All ▾" dropdown listing every option.
 * Both let you add a new option. `noneLabel` adds a "nothing selected" choice, [display] decorates
 * how an option is shown (e.g. with an emoji), and [addDialog] replaces the plain name dialog.
 */
@Composable
fun ChipPicker(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    addTitle: String,
    onAdd: (String) -> Unit,
    noneLabel: String? = null,
    display: @Composable (String) -> String = { it },
    addDialog: (@Composable (onDismiss: () -> Unit, onAdded: (String) -> Unit) -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (selected != null) "$title: ${display(selected)}" else title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("All ▾") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (noneLabel != null) {
                        DropdownMenuItem(
                            text = { Text(noneLabel) },
                            onClick = { onSelect(null); menuOpen = false },
                            trailingIcon = if (selected == null) ({ Text("✓") }) else null
                        )
                    }
                    for (option in options) {
                        DropdownMenuItem(
                            text = { Text(display(option)) },
                            onClick = { onSelect(option); menuOpen = false },
                            trailingIcon = if (option == selected) ({ Text("✓") }) else null
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ $addTitle", color = MaterialTheme.colorScheme.primary) },
                        onClick = { menuOpen = false; adding = true }
                    )
                }
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (noneLabel != null) {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text(noneLabel) })
            }
            for (option in options) {
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(display(option)) })
            }
            AssistChip(onClick = { adding = true }, label = { Text("+ New") })
        }
    }

    if (adding && addDialog != null) {
        addDialog({ adding = false }) { name ->
            onSelect(name)
            adding = false
        }
    } else if (adding) {
        NameDialog(
            title = addTitle,
            confirmLabel = "Add",
            existing = options,
            onDismiss = { adding = false },
            onConfirm = { name ->
                onAdd(name)
                onSelect(name)
                adding = false
            }
        )
    }
}

/** A button showing e.g. "Jul 2025 ▾" that opens a month grid. Future months can't be picked. */
@Composable
fun MonthPickerButton(month: MonthKey, onChange: (MonthKey) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Text(month.shortLabel() + " ▾", maxLines = 1)
    }
    if (open) {
        MonthPickerDialog(
            initial = month,
            onDismiss = { open = false },
            onPick = { onChange(it); open = false }
        )
    }
}

@Composable
private fun MonthPickerDialog(initial: MonthKey, onDismiss: () -> Unit, onPick: (MonthKey) -> Unit) {
    val current = MonthKey.current()
    var year by remember { mutableIntStateOf(initial.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }) {
                    Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "Previous year")
                }
                Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { year++ }, enabled = year < current.year) {
                    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "Next year")
                }
            }
        },
        text = {
            Column {
                for (row in 0 until 4) {
                    Row {
                        for (col in 0 until 3) {
                            val key = MonthKey(year, row * 3 + col)
                            Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (key == initial) {
                                    FilledTonalButton(onClick = { onPick(key) }) { Text(shortMonthName(key.month)) }
                                } else {
                                    TextButton(onClick = { onPick(key) }, enabled = key <= current) {
                                        Text(shortMonthName(key.month))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** A button showing a year that opens a list of `years` to choose from. */
@Composable
fun YearPickerButton(year: Int, years: List<Int>, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text("$year ▾") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (y in years) {
                DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = { onChange(y); open = false },
                    trailingIcon = if (y == year) ({ Text("✓") }) else null
                )
            }
        }
    }
}
