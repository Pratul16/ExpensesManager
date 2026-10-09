package com.pratul.expensesmanager.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AutoBackupSettings {
    private const val PREFS = "auto_backup_settings"
    private const val K_ENABLED = "enabled"
    private const val K_HOUR = "hour"
    private const val K_MINUTE = "minute"
    private const val K_LAST_BACKUP = "last_backup"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(K_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(K_ENABLED, enabled).apply()
        if (enabled) AutoBackupScheduler.schedule(context)
        else AutoBackupScheduler.cancel(context)
    }

    fun getBackupTime(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Pair(prefs.getInt(K_HOUR, 2), prefs.getInt(K_MINUTE, 0)) // Default 2:00 AM
    }

    fun setBackupTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(K_HOUR, hour)
            .putInt(K_MINUTE, minute)
            .apply()
        AutoBackupScheduler.schedule(context)
    }

    fun getLastBackupTime(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(K_LAST_BACKUP, 0)

    fun markBackupCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(K_LAST_BACKUP, System.currentTimeMillis())
            .apply()
    }
}

class AutoBackupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!AutoBackupSettings.isEnabled(context)) return Result.success()

            val expenses = ExpenseStore.load(context)
            val catalog = ExpenseStore.loadCatalog(context)
            val bin = ExpenseStore.loadBin(context)
            val profile = ProfileStore.load(context)
            val loans = ExpenseStore.loadLoans(context)

            val data = BackupData(
                exportedAt = System.currentTimeMillis(),
                expenses = expenses,
                catalog = catalog,
                bin = bin,
                profile = profile,
                photo = ProfileStore.photoBytes(context),
                loans = loans
            )

            // Save automated daily backup to local internal cache
            val json = Backup.toJson(data)
            val backupFile = File(context.filesDir, "auto_daily_backup.json")
            backupFile.writeText(json, Charsets.UTF_8)

            AutoBackupSettings.markBackupCompleted(context)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

object AutoBackupScheduler {
    private const val WORK_NAME = "DailyAutoBackup"

    fun schedule(context: Context) {
        if (!AutoBackupSettings.isEnabled(context)) return

        val (hour, minute) = AutoBackupSettings.getBackupTime(context)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
