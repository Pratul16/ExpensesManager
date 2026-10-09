package com.pratul.expensesmanager.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Everything a backup file holds. */
data class BackupData(
    val exportedAt: Long,
    val expenses: List<Expense>,
    val catalog: List<Category>,
    val bin: List<BinEntry>,
    val profile: Profile?,
    val photo: ByteArray?,
    /** Where an imported .zip's bill files were unpacked (null for exports and old .json backups). */
    val billsDir: File? = null,
    /** Borrow & lend records (older backups have none). */
    val loans: List<Loan> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupData

        if (exportedAt != other.exportedAt) return false
        if (expenses != other.expenses) return false
        if (catalog != other.catalog) return false
        if (bin != other.bin) return false
        if (profile != other.profile) return false
        if (photo != null) {
            if (other.photo == null) return false
            if (!photo.contentEquals(other.photo)) return false
        } else if (other.photo != null) return false
        if (billsDir != other.billsDir) return false
        if (loans != other.loans) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exportedAt.hashCode()
        result = 31 * result + expenses.hashCode()
        result = 31 * result + catalog.hashCode()
        result = 31 * result + bin.hashCode()
        result = 31 * result + profile.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + (billsDir?.hashCode() ?: 0)
        result = 31 * result + loans.hashCode()
        return result
    }
}

/**
 * Backup files (JSON, can be imported again) and spreadsheet exports (CSV, export only).
 * Files are read and written through Android's file picker, so the user chooses where they live.
 */
object Backup {
    private const val FORMAT = "expenses-manager-backup"
    private const val VERSION = 1
    private const val MAX_FILE_BYTES = 30 * 1024 * 1024
    private const val JSON_ENTRY = "backup.json"
    private const val BILLS_PREFIX = "bills/"

    fun suggestedFileName(): String =
        "expenses-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".zip"

    fun suggestedCsvName(): String =
        "expenses-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".csv"

    // ---------- Writing ----------

    fun toJson(data: BackupData): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", data.exportedAt)
            .put("expenses", ExpenseStore.expensesToJson(data.expenses))
            .put("catalog", ExpenseStore.catalogToJson(data.catalog))
            .put("bin", ExpenseStore.binToJson(data.bin))
            .put("loans", LoanJson.toJson(data.loans))
        data.profile?.let {
            root.put(
                "profile",
                JSONObject().put("name", it.name).apply { if (it.dobUtcMillis != null) put("dob", it.dobUtcMillis) }
            )
        }
        data.photo?.let { root.put("photo", Base64.encodeToString(it, Base64.NO_WRAP)) }
        return root.toString(2)
    }

    /** Spreadsheet-friendly list of expenses, oldest first. */
    fun toCsv(expenses: List<Expense>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        // Byte-order mark so Excel reads ₹, emoji and other non-English text correctly
        sb.append('﻿')
        sb.append("Date,Category,Item,Amount,Note,Bill\r\n")
        for (e in expenses.sortedBy { it.dateMillis }) {
            sb.append(dateFormat.format(Date(e.dateMillis))).append(',')
                .append(csvField(e.category)).append(',')
                .append(csvField(e.item)).append(',')
                .append(String.format(Locale.US, "%.2f", e.amount)).append(',')
                .append(csvField(e.note)).append(',')
                .append(if (e.bill.isNotBlank()) "Yes" else "").append("\r\n")
        }
        return sb.toString()
    }

    private fun csvField(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value

    // ---------- Reading ----------

    /** Reads a backup file's text. Throws [IllegalArgumentException] with a readable message if it isn't one. */
    fun fromJson(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("This file isn't an Expenses Manager backup.")
        }
        if (root.optString("format") != FORMAT) {
            throw IllegalArgumentException("This file isn't an Expenses Manager backup.")
        }
        if (root.optInt("version") > VERSION) {
            throw IllegalArgumentException("This backup was made by a newer version of the app. Update the app and try again.")
        }
        return try {
            val profile = root.optJSONObject("profile")?.let {
                Profile(
                    name = it.optString("name", ""),
                    dobUtcMillis = if (it.has("dob")) it.getLong("dob") else null,
                    photoVersion = 0
                )
            }
            BackupData(
                exportedAt = root.optLong("exportedAt", 0),
                expenses = ExpenseStore.expensesFromJson(root.getJSONArray("expenses")),
                catalog = ExpenseStore.catalogFromJson(root.getJSONArray("catalog")),
                bin = root.optJSONArray("bin")?.let { ExpenseStore.binFromJson(it) } ?: emptyList(),
                loans = root.optJSONArray("loans")?.let { LoanJson.fromJson(it) } ?: emptyList(),
                profile = profile,
                photo = root.optString("photo", "").takeIf { it.isNotEmpty() }?.let { Base64.decode(it, Base64.DEFAULT) }
            )
        } catch (e: Exception) {
            throw IllegalArgumentException("This backup file is damaged and can't be read.")
        }
    }

    // ---------- Zip backups (data + bill files) ----------

    /** Writes a .zip with backup.json and every bill the backup's expenses point to. */
    suspend fun writeZip(context: Context, uri: Uri, data: BackupData) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Couldn't open the file for writing.")
        val bills = (data.expenses + data.bin.flatMap { it.expenses }).map { it.bill }.filter { it.isNotBlank() }.distinct()
        ZipOutputStream(stream.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY))
            zip.write(toJson(data).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            for (name in bills) {
                val file = BillStore.file(context, name)
                if (!file.exists()) continue
                zip.putNextEntry(ZipEntry(BILLS_PREFIX + name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Reads a backup: a .zip from this version (bills are unpacked to a temporary folder, see
     * [BackupData.billsDir]) or a .json from older versions.
     */
    suspend fun read(context: Context, uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val isZip = context.contentResolver.openInputStream(uri)?.use { input ->
            val head = ByteArray(2)
            input.read(head) == 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
        } ?: throw IllegalArgumentException("Couldn't open that file.")
        if (!isZip) return@withContext fromJson(readText(context, uri))

        val billsDir = File(context.cacheDir, "import_bills").apply { deleteRecursively(); mkdirs() }
        var json: String? = null
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Couldn't open that file.")
        try {
            ZipInputStream(stream.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    when {
                        name == JSON_ENTRY -> {
                            val bytes = zip.readBytes()
                            if (bytes.size > MAX_FILE_BYTES) throw IllegalArgumentException("That backup is too big to read.")
                            json = bytes.toString(Charsets.UTF_8)
                        }
                        name.startsWith(BILLS_PREFIX) && !entry.isDirectory -> {
                            // Only plain file names: never let a backup write outside the folder
                            val fileName = name.removePrefix(BILLS_PREFIX)
                            if (fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName && ".." !in fileName) {
                                File(billsDir, fileName).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("This backup file is damaged and can't be read.")
        }
        fromJson(json ?: throw IllegalArgumentException("This file isn't an Expenses Manager backup."))
            .copy(billsDir = billsDir)
    }

    // ---------- Files ----------

    suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IllegalStateException("Couldn't open the file for writing.")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Couldn't open that file.")
        val bytes = stream.use { it.readBytes() }
        if (bytes.size > MAX_FILE_BYTES) throw IllegalArgumentException("That file is too big to be a backup.")
        bytes.toString(Charsets.UTF_8)
    }

    // ---------- Last backup date ----------

    private const val PREFS = "settings"
    private const val LAST_BACKUP_KEY = "lastBackupAt"

    fun lastBackupAt(context: Context): Long? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_BACKUP_KEY, 0).takeIf { it > 0 }

    fun markBackedUp(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAST_BACKUP_KEY, System.currentTimeMillis()).apply()
    }
}
