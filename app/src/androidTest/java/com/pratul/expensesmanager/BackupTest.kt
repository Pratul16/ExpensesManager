package com.pratul.expensesmanager

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pratul.expensesmanager.data.Backup
import com.pratul.expensesmanager.data.BackupData
import com.pratul.expensesmanager.data.BillStore
import com.pratul.expensesmanager.data.BinEntry
import com.pratul.expensesmanager.data.BinKind
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.Loan
import com.pratul.expensesmanager.data.LoanType
import com.pratul.expensesmanager.data.Profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val expenses = listOf(
        Expense(1, 450.5, "Food", "Lunch", "with \"team\", office", 1_759_650_000_000),
        Expense(2, 60.0, "Groceries", "", "", 1_759_560_000_000)
    )
    private val data = BackupData(
        exportedAt = 1_759_660_000_000,
        expenses = expenses,
        catalog = listOf(Category("Food", listOf("Lunch"), "🍔"), Category("Pets", emptyList(), "🐶")),
        bin = listOf(BinEntry(9, 5, BinKind.Item, "Food", "Snacks", emptyList(), listOf(expenses[0]), "🍔")),
        profile = Profile("Pratul", 800_000_000_000, 0),
        photo = byteArrayOf(1, 2, 3, 127, -1),
        loans = listOf(
            Loan(7, LoanType.Lent, "Rahul", 2000.0, 1_759_600_000_000, "Movie tickets", dueMillis = 1_760_000_000_000),
            Loan(8, LoanType.Borrowed, "Asha", 450.5, 1_759_500_000_000, settled = true, settledAt = 1_759_550_000_000)
        )
    )

    @Test
    fun backupRoundTripKeepsEverything() {
        val back = Backup.fromJson(Backup.toJson(data))
        assertEquals(data.exportedAt, back.exportedAt)
        assertEquals(data.expenses, back.expenses)
        assertEquals(data.catalog, back.catalog)
        assertEquals(data.bin, back.bin)
        assertEquals("Pratul", back.profile?.name)
        assertEquals(800_000_000_000, back.profile?.dobUtcMillis)
        assertArrayEquals(data.photo, back.photo)
        assertEquals(data.loans, back.loans)
    }

    @Test
    fun zipBackupCarriesBills() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // A uniquely named bill so the app's real bills are never touched
        val billName = "backup-test-${System.nanoTime()}.jpg"
        val billBytes = ByteArray(5000) { (it % 251).toByte() }
        val billFile = BillStore.file(context, billName)
        billFile.writeBytes(billBytes)
        val zip = File(context.cacheDir, "backup-test.zip")
        try {
            val withBill = data.copy(expenses = listOf(expenses[0].copy(bill = billName), expenses[1]))
            Backup.writeZip(context, Uri.fromFile(zip), withBill)

            val back = Backup.read(context, Uri.fromFile(zip))
            assertEquals(withBill.expenses, back.expenses)
            assertEquals(withBill.catalog, back.catalog)
            assertArrayEquals(billBytes, File(back.billsDir, billName).readBytes())
        } finally {
            billFile.delete()
            zip.delete()
        }
    }

    @Test
    fun oldJsonBackupsStillImport() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = File(context.cacheDir, "backup-test.json")
        try {
            json.writeText(Backup.toJson(data))
            val back = Backup.read(context, Uri.fromFile(json))
            assertEquals(data.expenses, back.expenses)
            assertEquals(null, back.billsDir)
        } finally {
            json.delete()
        }
    }

    @Test
    fun rejectsFilesThatAreNotBackups() {
        for (text in listOf("hello", "{}", "[1,2,3]", """{"format":"something-else"}""")) {
            try {
                Backup.fromJson(text)
                fail("Accepted: $text")
            } catch (e: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun csvQuotesCommasAndQuotes() {
        val csv = Backup.toCsv(expenses)
        assertTrue(csv.startsWith("﻿Date,Category,Item,Amount,Note,Bill\r\n"))
        assertTrue(csv.contains(",Food,Lunch,450.50,\"with \"\"team\"\", office\",\r\n"))
        // Oldest first
        assertTrue(csv.indexOf("Groceries") < csv.indexOf("Food,Lunch"))
    }
}
