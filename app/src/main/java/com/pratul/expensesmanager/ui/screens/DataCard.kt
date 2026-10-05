package com.pratul.expensesmanager.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pratul.expensesmanager.R
import com.pratul.expensesmanager.data.Backup
import com.pratul.expensesmanager.data.BackupData
import com.pratul.expensesmanager.data.Profile
import com.pratul.expensesmanager.data.ProfileStore
import com.pratul.expensesmanager.ui.AppState
import com.pratul.expensesmanager.ui.components.ConfirmDialog
import com.pratul.expensesmanager.util.formatDate
import com.pratul.expensesmanager.util.formatMoneyWhole
import kotlinx.coroutines.launch

/**
 * "Your data" on the Profile page: everything lives on this phone, with backup export/import and a
 * spreadsheet export, so nothing depends on signing in.
 * [onProfileReplaced] is called after an import changed the saved profile, so the page can reload it.
 */
@Composable
fun DataCard(state: AppState, onProfileReplaced: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lastBackup by remember { mutableStateOf(Backup.lastBackupAt(context)) }
    var pendingImport by remember { mutableStateOf<BackupData?>(null) }
    var confirmReplace by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val data = BackupData(
                    exportedAt = System.currentTimeMillis(),
                    expenses = state.expenses,
                    catalog = state.catalog,
                    bin = state.bin,
                    profile = ProfileStore.load(context),
                    photo = ProfileStore.photoBytes(context)
                )
                Backup.writeZip(context, uri, data)
                Backup.markBackedUp(context)
                lastBackup = Backup.lastBackupAt(context)
                toast("Backup saved ✅ ${state.expenses.size} expenses")
            } catch (e: Exception) {
                error = "Couldn't save the backup: ${e.message}"
            }
        }
    }

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                Backup.writeText(context, uri, Backup.toCsv(state.expenses))
                toast("Spreadsheet saved ✅ ${state.expenses.size} expenses")
            } catch (e: Exception) {
                error = "Couldn't save the spreadsheet: ${e.message}"
            }
        }
    }

    // Some file apps label .json files oddly, so accept any file and check its contents instead
    val importBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                pendingImport = Backup.read(context, uri)
            } catch (e: IllegalArgumentException) {
                error = e.message
            } catch (e: Exception) {
                error = "Couldn't read that file: ${e.message}"
            }
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Text("Your data 🔒", fontWeight = FontWeight.SemiBold)
            Text(
                "Everything is saved on this phone. No sign-in needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                lastBackup?.let { "Last backup: ${formatDate(it)}" }
                    ?: "⚠️ No backup yet. Export one so you don't lose your data if you change or reset your phone.",
                style = MaterialTheme.typography.bodySmall,
                color = if (lastBackup == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        MenuRow(
            icon = R.drawable.ic_upload,
            title = "Export backup",
            subtitle = "Save everything, bills included, to a file you can import later",
            onClick = { exportBackup.launch(Backup.suggestedFileName()) }
        )
        HorizontalDivider(Modifier.padding(start = 56.dp))
        MenuRow(
            icon = R.drawable.ic_download,
            title = "Import backup",
            subtitle = "Bring back data from a backup file",
            onClick = { importBackup.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "*/*")) }
        )
        HorizontalDivider(Modifier.padding(start = 56.dp))
        MenuRow(
            icon = R.drawable.ic_table,
            title = "Export to Excel (CSV)",
            subtitle = "For spreadsheets. Can't be imported back",
            onClick = { exportCsv.launch(Backup.suggestedCsvName()) }
        )
    }

    // ---------- Import: choose merge or replace ----------
    val backup = pendingImport
    if (backup != null && !confirmReplace) {
        val total = backup.expenses.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import this backup?") },
            text = {
                Column {
                    Text(
                        (if (backup.exportedAt > 0) "Made on ${formatDate(backup.exportedAt)}. " else "") +
                                "It has ${backup.expenses.size} expenses (${formatMoneyWhole(total)}) " +
                                "and ${backup.catalog.size} categories."
                    )
                    Text(
                        "\nYour expenses data will change after this.\n\n" +
                                "• Merge adds anything new and skips expenses you already have.\n" +
                                "• Replace removes your current data and uses the backup instead.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val added = state.merge(backup)
                    if (backup.profile != null && ProfileStore.load(context).name.isBlank()) {
                        restoreProfile(context, backup)
                        onProfileReplaced()
                    }
                    pendingImport = null
                    toast(if (added == 0) "Nothing new to add. You already had all of it 👍" else "Merged ✅ $added new expenses added")
                }) { Text("Merge") }
            },
            dismissButton = {
                Column {
                    TextButton(
                        onClick = { confirmReplace = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Replace") }
                    TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
                }
            }
        )
    }

    // Replacing overwrites everything, so it gets a second, explicit warning
    if (backup != null && confirmReplace) {
        ConfirmDialog(
            title = "Replace all your data?",
            message = "Your current ${state.expenses.size} expenses, categories, recycle bin and profile will be " +
                    "replaced by the backup. This can't be undone. Export a backup first if you might want them back.",
            confirmLabel = "Replace",
            destructive = true,
            onDismiss = { confirmReplace = false; pendingImport = null },
            onConfirm = {
                state.replaceAll(backup)
                restoreProfile(context, backup)
                onProfileReplaced()
                confirmReplace = false
                pendingImport = null
                toast("Restored ✅ ${backup.expenses.size} expenses")
            }
        )
    }

    error?.let {
        ConfirmDialog(
            title = "Something went wrong",
            message = it,
            confirmLabel = "OK",
            onDismiss = { error = null },
            onConfirm = { error = null }
        )
    }
}

/** Puts the backup's name, birthday and photo back. */
private fun restoreProfile(context: Context, backup: BackupData) {
    val p = backup.profile ?: return
    backup.photo?.let { ProfileStore.restorePhotoBytes(context, it) }
    ProfileStore.save(context, Profile(p.name, p.dobUtcMillis, System.currentTimeMillis()))
}
