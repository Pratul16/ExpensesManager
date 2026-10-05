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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.DEFAULT_EMOJI
import com.pratul.expensesmanager.data.Expense

import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.CategoryBadge
import com.pratul.expensesmanager.ui.components.CategoryDialog
import com.pratul.expensesmanager.ui.components.LocalQuickAdd
import com.pratul.expensesmanager.ui.components.SwipeActions
import com.pratul.expensesmanager.ui.components.ConfirmDialog
import com.pratul.expensesmanager.ui.components.DoubleConfirmDialog
import com.pratul.expensesmanager.ui.components.NameDialog
import com.pratul.expensesmanager.util.formatMoneyWhole

/** Something on this screen the user is acting on; drives which dialog is open. */
private sealed class Action {
    data object AddCategory : Action()
    data class RenameCategory(val name: String) : Action()
    data class DeleteCategory(val name: String) : Action()
    data class AddItem(val category: String) : Action()
    data class RenameItem(val category: String, val item: String) : Action()
    data class DeleteItem(val category: String, val item: String) : Action()
}

@Composable
fun ManageCategoriesScreen(state: AppState, onBack: () -> Unit) {
    var action by remember { mutableStateOf<Action?>(null) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        SubScreenHeader("Categories & items", onBack)
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Tap a category to see its items. Swipe a category or item right to add a spend ➕",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                OutlinedButton(onClick = { action = Action.AddCategory }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Add category")
                }
            }
            items(state.catalog, key = { it.name }) { category ->
                CategoryCard(
                    category = category,
                    expenses = state.expensesIn(category.name),
                    expanded = expanded == category.name,
                    onToggle = { expanded = if (expanded == category.name) null else category.name },
                    onAction = { action = it }
                )
            }
        }
    }

    when (val a = action) {
        null -> Unit
        Action.AddCategory -> CategoryDialog(
            title = "Add category",
            confirmLabel = "Add",
            existing = state.catalog.map { it.name },
            onDismiss = { action = null },
            onConfirm = { name, emoji -> state.addCategory(name, emoji); action = null }
        )
        is Action.RenameCategory -> CategoryDialog(
            title = "Edit category",
            confirmLabel = "Save",
            existing = state.catalog.map { it.name },
            initialName = a.name,
            initialEmoji = state.catalog.firstOrNull { it.name == a.name }?.emoji ?: DEFAULT_EMOJI,
            onDismiss = { action = null },
            onConfirm = { name, emoji ->
                state.editCategory(a.name, name, emoji)
                if (expanded == a.name) expanded = name
                action = null
            }
        )
        is Action.DeleteCategory -> {
            if (state.catalog.size <= 1) {
                ConfirmDialog(
                    title = "Can't delete",
                    message = "You need at least one category. Add another one first.",
                    confirmLabel = "OK",
                    onDismiss = { action = null },
                    onConfirm = { action = null }
                )
            } else {
                val lost = state.expensesIn(a.name)
                val itemCount = state.catalog.first { it.name == a.name }.items.size
                DoubleConfirmDialog(
                    firstTitle = "Delete \"${a.name}\"?",
                    firstMessage = "This deletes the category, its $itemCount item${plural(itemCount)} " +
                            "and ${expenseSummary(lost)} saved in it.",
                    secondTitle = "Your expenses data will be lost",
                    secondMessage = lossWarning(lost),
                    confirmLabel = "Delete",
                    onDismiss = { action = null },
                    onConfirm = { state.deleteCategory(a.name); action = null }
                )
            }
        }
        is Action.AddItem -> NameDialog(
            title = "Add item to ${a.category}",
            confirmLabel = "Add",
            existing = state.catalog.first { it.name == a.category }.items,
            onDismiss = { action = null },
            onConfirm = { state.addItem(a.category, it); action = null }
        )
        is Action.RenameItem -> NameDialog(
            title = "Rename item",
            confirmLabel = "Rename",
            existing = state.catalog.first { it.name == a.category }.items,
            initial = a.item,
            onDismiss = { action = null },
            onConfirm = { state.renameItem(a.category, a.item, it); action = null }
        )
        is Action.DeleteItem -> {
            val lost = state.expensesIn(a.category, a.item)
            DoubleConfirmDialog(
                firstTitle = "Delete \"${a.item}\"?",
                firstMessage = "This deletes the item from ${a.category} and ${expenseSummary(lost)} saved with it.",
                secondTitle = "Your expenses data will be lost",
                secondMessage = lossWarning(lost),
                confirmLabel = "Delete",
                onDismiss = { action = null },
                onConfirm = { state.deleteItem(a.category, a.item); action = null }
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    expenses: List<Expense>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAction: (Action) -> Unit
) {
    val quickAdd = LocalQuickAdd.current
    // Rows need a solid background so the swipe colour only shows beside them
    val rowColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(Modifier.fillMaxWidth()) {
        Column {
            // Swipe a category right to add a spend in it
            SwipeActions(onSwipeRight = { quickAdd(category.name, null) }, corner = 12.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(rowColor)
                        .clickable(onClick = onToggle)
                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryBadge(category.name, 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${category.items.size} item${plural(category.items.size)} · ${expenseSummary(expenses)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                    RenameDeleteMenu(
                        renameLabel = "Edit name & emoji",
                        onRename = { onAction(Action.RenameCategory(category.name)) },
                        onDelete = { onAction(Action.DeleteCategory(category.name)) }
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                for (item in category.items) {
                    val count = expenses.count { it.item == item }
                    // Swipe an item right to add a spend with both the category and item chosen
                    SwipeActions(onSwipeRight = { quickAdd(category.name, item) }, corner = 12.dp) {
                        Row(
                            Modifier.fillMaxWidth().background(rowColor).padding(start = 40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item, Modifier.weight(1f))
                            Text(
                                "$count expense${plural(count)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            RenameDeleteMenu(
                                onRename = { onAction(Action.RenameItem(category.name, item)) },
                                onDelete = { onAction(Action.DeleteItem(category.name, item)) }
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onAction(Action.AddItem(category.name)) },
                    modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
                ) { Text("+ Add item") }
            }
        }
    }
}

@Composable
private fun RenameDeleteMenu(onRename: () -> Unit, onDelete: () -> Unit, renameLabel: String = "Rename") {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painterResource(R.drawable.ic_more_vert), contentDescription = "More options")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(renameLabel) }, onClick = { open = false; onRename() })
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

/** Back arrow and title for screens opened from the Account tab. */
@Composable
fun SubScreenHeader(title: String, onBack: () -> Unit, action: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
        }
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        action()
    }
}

private fun plural(count: Int) = if (count == 1) "" else "s"

/** e.g. "no expenses", "1 expense (₹450)", "12 expenses (₹4,500)" */
fun expenseSummary(expenses: List<Expense>): String =
    if (expenses.isEmpty()) "no expenses"
    else "${expenses.size} expense${plural(expenses.size)} (${formatMoneyWhole(expenses.sumOf { it.amount })})"

private fun lossWarning(lost: List<Expense>): String =
    if (lost.isEmpty()) {
        "No expenses are saved in it, so your totals won't change. It goes to the Recycle bin, where you can restore it."
    } else {
        "${expenseSummary(lost).replaceFirstChar { it.uppercase() }} will be removed from all your totals " +
                "and charts. They go to the Recycle bin, where you can restore them."
    }
