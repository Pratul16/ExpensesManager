package com.pratul.expensesmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.BinEntry
import com.pratul.expensesmanager.data.BinKind
import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.CategoryBadge
import com.pratul.expensesmanager.ui.components.ConfirmDialog
import com.pratul.expensesmanager.ui.components.DoubleConfirmDialog
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoney

private sealed class BinAction {
    data class Restore(val entry: BinEntry) : BinAction()
    data class DeleteForever(val entry: BinEntry) : BinAction()
    data object EmptyBin : BinAction()
}

@Composable
fun RecycleBinScreen(state: AppState, onBack: () -> Unit) {
    var action by remember { mutableStateOf<BinAction?>(null) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("Recycle bin", onBack) {
            if (state.bin.isNotEmpty()) {
                TextButton(
                    onClick = { action = BinAction.EmptyBin },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Empty") }
            }
        }
        if (state.bin.isEmpty()) {
            Text(
                "The recycle bin is empty. Anything you delete will appear here, and you can restore it.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        "Deleted things stay here until you restore them or delete them forever.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(state.bin, key = { it.id }) { entry ->
                    BinCard(
                        entry,
                        onRestore = { action = BinAction.Restore(entry) },
                        onDelete = { action = BinAction.DeleteForever(entry) }
                    )
                }
            }
        }
    }

    when (val a = action) {
        null -> Unit
        is BinAction.Restore -> ConfirmDialog(
            title = "Restore ${titleOf(a.entry)}?",
            message = "Your expenses data will change after this. " + restoreDetails(a.entry),
            confirmLabel = "Restore",
            onDismiss = { action = null },
            onConfirm = { state.restore(a.entry); action = null }
        )
        is BinAction.DeleteForever -> DoubleConfirmDialog(
            firstTitle = "Delete forever?",
            firstMessage = "${titleOf(a.entry).replaceFirstChar { it.uppercase() }} and " +
                    "${expenseSummary(a.entry.expenses)} will be permanently deleted.",
            secondTitle = "This can't be undone",
            secondMessage = "Once deleted forever, this data can't be restored. Delete it?",
            confirmLabel = "Delete forever",
            onDismiss = { action = null },
            onConfirm = { state.deleteForever(a.entry); action = null }
        )
        BinAction.EmptyBin -> DoubleConfirmDialog(
            firstTitle = "Empty the recycle bin?",
            firstMessage = "All ${state.bin.size} deleted entries and " +
                    "${expenseSummary(state.bin.flatMap { it.expenses })} will be permanently deleted.",
            secondTitle = "This can't be undone",
            secondMessage = "Once the bin is emptied, none of it can be restored. Empty it?",
            confirmLabel = "Empty bin",
            onDismiss = { action = null },
            onConfirm = { state.emptyBin(); action = null }
        )
    }
}

@Composable
private fun BinCard(entry: BinEntry, onRestore: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(entry.category, 44.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (entry.kind) {
                            BinKind.Expense -> "Expense"
                            BinKind.Category -> "Category"
                            BinKind.Item -> "Item"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(nameOf(entry), fontWeight = FontWeight.SemiBold)
                    Text(detailsOf(entry), style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Deleted " + formatDate(entry.deletedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete forever") }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onRestore) { Text("Restore") }
            }
        }
    }
}

private fun nameOf(entry: BinEntry): String = when (entry.kind) {
    BinKind.Expense -> {
        val e = entry.expenses.first()
        "${e.item.ifBlank { e.category }} · ${formatMoney(e.amount)}"
    }
    BinKind.Category -> entry.category
    BinKind.Item -> "${entry.item} (${entry.category})"
}

private fun detailsOf(entry: BinEntry): String = when (entry.kind) {
    BinKind.Expense -> {
        val e = entry.expenses.first()
        listOfNotNull(
            e.category.takeIf { e.item.isNotBlank() },
            e.note.ifBlank { null },
            formatDate(e.dateMillis)
        ).joinToString(" • ")
    }
    BinKind.Category -> "${entry.categoryItems.size} items · ${expenseSummary(entry.expenses)}"
    BinKind.Item -> expenseSummary(entry.expenses)
}

private fun titleOf(entry: BinEntry): String = when (entry.kind) {
    BinKind.Expense -> "this expense"
    BinKind.Category -> "\"${entry.category}\""
    BinKind.Item -> "\"${entry.item}\""
}

private fun restoreDetails(entry: BinEntry): String {
    val back = if (entry.expenses.isEmpty()) "No expenses will be added back."
    else "${expenseSummary(entry.expenses).replaceFirstChar { it.uppercase() }} will be added back " +
            "to your totals and charts."
    return when (entry.kind) {
        BinKind.Expense -> back
        BinKind.Category -> "The category and its items will come back. $back"
        BinKind.Item -> "The item will come back under ${entry.category}. $back"
    }
}
