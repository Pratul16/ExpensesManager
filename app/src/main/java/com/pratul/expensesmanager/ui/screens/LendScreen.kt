package com.pratul.expensesmanager.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.data.Loan
import com.pratul.expensesmanager.data.LoanShare
import com.pratul.expensesmanager.data.LoanType
import com.pratul.expensesmanager.data.totals
import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.ConfirmDialog
import com.pratul.expensesmanager.ui.components.CountUpText
import com.pratul.expensesmanager.ui.components.GradientCard
import com.pratul.expensesmanager.ui.theme.decreaseColor
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoney
import com.pratul.expensesmanager.util.formatMoneyWhole
import com.pratul.expensesmanager.util.localMillisToPickerDate
import com.pratul.expensesmanager.util.pickerDateToLocalMillis
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class LoanFilter(val label: String) { Open("Pending"), Settled("Settled"), All("All") }

/** Colour for money coming back to you (lent) vs money you owe (borrowed). */
@Composable
private fun lentColor() = decreaseColor()

@Composable
private fun borrowedColor() = MaterialTheme.colorScheme.error

/**
 * Borrow & lend: what others owe you and what you owe them, with the surplus or deficit between
 * the two, records grouped by person, and a shareable picture for each record.
 */
@Composable
fun LendScreen(state: AppState, onAdd: () -> Unit) {
    var filter by rememberSaveable { mutableStateOf(LoanFilter.Open) }
    var opened by remember { mutableStateOf<Loan?>(null) }
    var editing by remember { mutableStateOf<Loan?>(null) }
    val totals = state.loans.totals()
    val shown = state.loans.filter {
        when (filter) {
            LoanFilter.Open -> !it.settled
            LoanFilter.Settled -> it.settled
            LoanFilter.All -> true
        }
    }
    // People with the most money still open first, then alphabetically
    val byPerson = shown.groupBy { it.person.trim() }
        .toList()
        .sortedWith(compareByDescending<Pair<String, List<Loan>>> { (_, l) -> abs(l.totals().net) }.thenBy { it.first.lowercase() })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "title") {
            Text("Borrow & Lend 🤝", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        }
        item(key = "summary") {
            GradientCard {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("You'll get back", style = MaterialTheme.typography.bodyMedium)
                        CountUpText(totals.lentOpen, ::formatMoneyWhole, MaterialTheme.typography.headlineMedium)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("You owe", style = MaterialTheme.typography.bodyMedium)
                        CountUpText(totals.borrowedOpen, ::formatMoneyWhole, MaterialTheme.typography.headlineMedium)
                    }
                }
                Spacer(Modifier.height(14.dp))
                val net = totals.net
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        when {
                            net > 0 -> "▲ Surplus ${formatMoneyWhole(net)} 🎉"
                            net < 0 -> "▼ Deficit ${formatMoneyWhole(-net)}"
                            else -> "⚖️ All square"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        item(key = "filter") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                LoanFilter.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = filter == f,
                        onClick = { filter = f },
                        shape = SegmentedButtonDefaults.itemShape(i, LoanFilter.entries.size)
                    ) { Text(f.label) }
                }
            }
        }

        if (shown.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🤝", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.loans.isEmpty()) "Lent money to a friend, or borrowed some?\nTap + to keep track of it."
                        else "Nothing here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (state.loans.isEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = onAdd) { Text("+ Add a record") }
                    }
                }
            }
        }

        for ((person, loans) in byPerson) {
            item(key = "person-$person") {
                val net = loans.filter { !it.settled }.totals().net
                Row(Modifier.padding(start = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(person.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(person, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        when {
                            net > 0 -> "owes you ${formatMoneyWhole(net)}"
                            net < 0 -> "you owe ${formatMoneyWhole(-net)}"
                            else -> "settled up"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            net > 0 -> lentColor()
                            net < 0 -> borrowedColor()
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            items(loans.sortedByDescending { it.dateMillis }, key = { "loan-${it.id}" }) { loan ->
                LoanRow(loan, onClick = { opened = loan })
            }
        }
    }

    opened?.let { loan ->
        // Re-read so the dialog reflects changes (settled, edited)
        val current = state.loans.firstOrNull { it.id == loan.id }
        if (current == null) opened = null
        else LoanDetailsDialog(
            loan = current,
            onDismiss = { opened = null },
            onToggleSettled = { state.setLoanSettled(current.id, !current.settled) },
            onEdit = {
                opened = null
                editing = current
            },
            onDelete = {
                state.deleteLoan(current.id)
                opened = null
            }
        )
    }
    editing?.let { loan ->
        LoanDialog(
            initial = loan,
            people = state.loans.map { it.person.trim() }.distinct(),
            onDismiss = { editing = null },
            onSave = {
                state.saveLoan(it)
                editing = null
            }
        )
    }
}

@Composable
private fun LoanRow(loan: Loan, onClick: () -> Unit) {
    val lent = loan.type == LoanType.Lent
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background((if (lent) lentColor() else borrowedColor()).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (lent) "↗" else "↙", style = MaterialTheme.typography.titleLarge, color = if (lent) lentColor() else borrowedColor())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (lent) "Lent" else "Borrowed", fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        formatDate(loan.dateMillis),
                        loan.dueMillis?.takeIf { !loan.settled }?.let { "due ${formatDate(it)}" },
                        loan.note.ifBlank { null }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(loan.amount), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (loan.settled) "Settled ✅" else "Pending ⏳",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoanDetailsDialog(
    loan: Loan,
    onDismiss: () -> Unit,
    onToggleSettled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val lent = loan.type == LoanType.Lent

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(if (lent) "🤝" else "🙏", style = MaterialTheme.typography.displaySmall) },
        title = { Text(if (lent) "Lent to ${loan.person}" else "Borrowed from ${loan.person}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(formatMoney(loan.amount), style = MaterialTheme.typography.headlineMedium)
                Text("📅 ${formatDate(loan.dateMillis)}", style = MaterialTheme.typography.bodyMedium)
                loan.dueMillis?.let { Text("⏰ Pay back by ${formatDate(it)}", style = MaterialTheme.typography.bodyMedium) }
                if (loan.note.isNotBlank()) Text("📝 ${loan.note}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (loan.settled) "Settled ✅${loan.settledAt?.let { " on ${formatDate(it)}" } ?: ""}" else "Pending ⏳",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            if (!LoanShare.share(context, loan)) {
                                Toast.makeText(context, "No app found to share with", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("📤 Share as image") }
                OutlinedButton(onClick = onToggleSettled, modifier = Modifier.fillMaxWidth()) {
                    Text(if (loan.settled) "Mark as pending again" else "✅ Mark as settled")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this record?",
            message = "${if (lent) "Lent to" else "Borrowed from"} ${loan.person}, ${formatMoney(loan.amount)}. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            }
        )
    }
}

/** Add (when [initial] is null) or edit a borrow/lend record. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDialog(initial: Loan?, people: List<String>, onDismiss: () -> Unit, onSave: (Loan) -> Unit) {
    var type by remember { mutableStateOf(initial?.type ?: LoanType.Lent) }
    var person by remember { mutableStateOf(initial?.person ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableLongStateOf(initial?.dateMillis ?: System.currentTimeMillis()) }
    var due by remember { mutableStateOf(initial?.dueMillis) }
    var picking by remember { mutableStateOf<String?>(null) } // "date" or "due"
    var showErrors by remember { mutableStateOf(false) }
    var suggestOpen by remember { mutableStateOf(false) }

    val amount = amountText.trim().replace(",", "").toDoubleOrNull()?.takeIf { it > 0 }
    val suggestions = people.filter { it.contains(person.trim(), ignoreCase = true) && !it.equals(person.trim(), ignoreCase = true) }.take(5)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New borrow / lend 🤝" else "Edit record") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    LoanType.entries.forEachIndexed { i, t ->
                        SegmentedButton(
                            selected = type == t,
                            onClick = { type = t },
                            shape = SegmentedButtonDefaults.itemShape(i, LoanType.entries.size)
                        ) { Text(if (t == LoanType.Lent) "I lent 🤝" else "I borrowed 🙏") }
                    }
                }
                Box {
                    OutlinedTextField(
                        value = person,
                        onValueChange = {
                            person = it
                            suggestOpen = true
                        },
                        label = { Text(if (type == LoanType.Lent) "Lent to (name)" else "Borrowed from (name)") },
                        singleLine = true,
                        isError = showErrors && person.isBlank(),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // People already in the list, to avoid typing the same name twice differently
                    DropdownMenu(
                        expanded = suggestOpen && suggestions.isNotEmpty() && person.isNotBlank(),
                        onDismissRequest = { suggestOpen = false },
                        // Keep the keyboard open while suggestions show
                        properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                    ) {
                        for (p in suggestions) {
                            DropdownMenuItem(text = { Text(p) }, onClick = {
                                person = p
                                suggestOpen = false
                            })
                        }
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    isError = showErrors && amount == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { picking = "date" }, modifier = Modifier.fillMaxWidth()) {
                    Text("Date: ${formatDate(date)}")
                }
                OutlinedButton(onClick = { picking = "due" }, modifier = Modifier.fillMaxWidth()) {
                    Text(due?.let { "Pay back by: ${formatDate(it)}" } ?: "Pay back by (optional)")
                }
                if (due != null) TextButton(onClick = { due = null }) { Text("Remove pay-back date") }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (person.isBlank() || amount == null) {
                    showErrors = true
                } else {
                    onSave(
                        (initial ?: Loan(0, type, "", 0.0, date)).copy(
                            type = type,
                            person = person.trim(),
                            amount = amount,
                            dateMillis = date,
                            note = note.trim(),
                            dueMillis = due
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    picking?.let { which ->
        val today = localMillisToPickerDate(System.currentTimeMillis())
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localMillisToPickerDate(if (which == "due") (due ?: System.currentTimeMillis()) else date),
            selectableDates = object : SelectableDates {
                // The loan date can't be in the future; the pay-back date can
                override fun isSelectableDate(utcTimeMillis: Long) = which == "due" || utcTimeMillis <= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        val local = pickerDateToLocalMillis(it)
                        if (which == "due") due = local else date = local
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
}
