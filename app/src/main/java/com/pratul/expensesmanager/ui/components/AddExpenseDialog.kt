package com.pratul.expensesmanager.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.BillStore
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.localMillisToPickerDate
import com.pratul.expensesmanager.util.pickerDateToLocalMillis

/**
 * Asks for amount, category, item, date and note. New categories and items are reported through
 * [onAddCategory] / [onAddItem] straight away so they're kept even if the dialog is cancelled.
 * The returned expense has id 0; the caller assigns a real one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    catalog: List<Category>,
    expenses: List<Expense>,
    onAddCategory: (name: String, emoji: String) -> Unit,
    onAddItem: (category: String, item: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Expense) -> Unit,
    initialCategory: String? = null,
    initialItem: String? = null
) {
    // Most-used first, so the chips you need are the ones already on screen.
    // Ties keep the catalog order (sortedByDescending is stable).
    // Anything added in this dialog, or pre-chosen by a swipe, goes in front too.
    val categoryUse = remember(expenses) { expenses.groupingBy { it.category }.eachCount() }
    val itemUse = remember(expenses) { expenses.groupingBy { it.category to it.item }.eachCount() }
    var addedHere by remember {
        mutableStateOf(setOfNotNull(initialCategory, initialItem?.let { "$initialCategory/$it" }))
    }
    val categoryNames = catalog.map { it.name }
        .sortedByDescending { if (it in addedHere) Int.MAX_VALUE else categoryUse[it] ?: 0 }

    var amountText by remember { mutableStateOf("") }
    // Start from the swiped category/item when there is one, otherwise the most-used category
    var category by remember { mutableStateOf(initialCategory?.takeIf { it in categoryNames } ?: categoryNames.first()) }
    var item by remember { mutableStateOf(initialItem?.takeIf { category == initialCategory }) }
    var note by remember { mutableStateOf("") }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var addingCategory by remember { mutableStateOf(false) }
    var addingItem by remember { mutableStateOf(false) }
    var bill by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Closing without saving throws away a bill attached here
    fun cancel() {
        bill?.let { BillStore.delete(context, it) }
        onDismiss()
    }

    val items = catalog.firstOrNull { it.name == category }?.items.orEmpty()
        .sortedByDescending { if ("$category/$it" in addedHere) Int.MAX_VALUE else itemUse[category to it] ?: 0 }

    AlertDialog(
        onDismissRequest = ::cancel,
        title = { Text("New spend 💸") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        showError = false
                    },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = "Enter a valid amount",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Category and item as compact dropdowns (most-used first), each with "add new" at the bottom
                DropdownField(
                    label = "Category",
                    value = "${categoryEmoji(category)}  $category",
                    options = categoryNames,
                    display = { "${categoryEmoji(it)}  $it" },
                    selected = category,
                    onSelect = { picked ->
                        if (picked != null && picked != category) {
                            category = picked
                            item = null
                        }
                    },
                    addLabel = "Add new category",
                    onAddClick = { addingCategory = true }
                )
                Spacer(Modifier.height(10.dp))
                DropdownField(
                    label = "Item (optional)",
                    value = item ?: "None",
                    options = items,
                    display = { it },
                    selected = item,
                    onSelect = { item = it },
                    addLabel = "Add new item to $category",
                    onAddClick = { addingItem = true },
                    noneLabel = "None"
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: " + formatDate(dateMillis))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                BillPicker(
                    bill = bill,
                    onChange = { new ->
                        // A bill picked here isn't saved yet, so replacing or removing it deletes the file
                        bill?.let { if (it != new) BillStore.delete(context, it) }
                        bill = new
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.trim().replace(",", "").toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    showError = true
                } else {
                    onSave(Expense(0, amount, category, item.orEmpty(), note.trim(), dateMillis, bill.orEmpty()))
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = ::cancel) { Text("Cancel") }
        }
    )

    if (addingCategory) {
        CategoryDialog(
            title = "Add new category",
            confirmLabel = "Add",
            existing = categoryNames,
            onDismiss = { addingCategory = false },
            onConfirm = { name, emoji ->
                addedHere = addedHere + name
                onAddCategory(name, emoji)
                category = name
                item = null
                addingCategory = false
            }
        )
    }
    if (addingItem) {
        NameDialog(
            title = "Add new item to $category",
            confirmLabel = "Add",
            existing = items,
            onDismiss = { addingItem = false },
            onConfirm = { name ->
                addedHere = addedHere + "$category/$name"
                onAddItem(category, name)
                item = name
                addingItem = false
            }
        )
    }

    if (showDatePicker) {
        val todayPicker = localMillisToPickerDate(System.currentTimeMillis())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToPickerDate(dateMillis),
            selectableDates = object : SelectableDates {
                // No future dates
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayPicker
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { dateMillis = pickerDateToLocalMillis(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
