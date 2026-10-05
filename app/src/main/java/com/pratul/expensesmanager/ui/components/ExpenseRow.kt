package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoney

/**
 * One expense. Swipe right to add another spend in the same category and item; swipe left to
 * delete (it asks first, and the expense goes to the Recycle bin). Tap to see details and its bill.
 */
@Composable
fun ExpenseRow(expense: Expense, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val quickAdd = LocalQuickAdd.current

    SwipeActions(
        onSwipeRight = { quickAdd(expense.category, expense.item.ifBlank { null }) },
        onSwipeLeft = { confirmDelete = true }
    ) {
        Card(onClick = { showDetails = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryBadge(expense.category, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    // Show the item as the title when there is one, with its category underneath
                    Text(
                        expense.item.ifBlank { expense.category },
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOfNotNull(
                        expense.category.takeIf { expense.item.isNotBlank() },
                        expense.note.ifBlank { null },
                        formatDate(expense.dateMillis)
                    ).joinToString(" • ")
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (expense.bill.isNotBlank()) Text("📎 ", style = MaterialTheme.typography.bodyMedium)
                Text(formatMoney(expense.amount), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDetails) ExpenseDetailsDialog(expense, onDismiss = { showDetails = false })

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete expense?") },
            text = {
                Text(
                    "${expense.item.ifBlank { expense.category }}, ${formatMoney(expense.amount)} on " +
                            "${formatDate(expense.dateMillis)}. It goes to the Recycle bin (Profile tab), where you can restore it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
