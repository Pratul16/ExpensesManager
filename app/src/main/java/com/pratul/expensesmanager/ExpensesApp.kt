package com.pratul.expensesmanager

import android.app.Application
import android.content.Context
import com.pratul.expensesmanager.security.AppLock

class ExpensesApp : Application() {

    /** One lock for the whole app; it watches every screen to know when the app goes to the background. */
    lateinit var appLock: AppLock
        private set

    override fun onCreate() {
        super.onCreate()
        appLock = AppLock(this)
        appLock.attach(this)
    }

    companion object {
        fun lock(context: Context): AppLock = (context.applicationContext as ExpensesApp).appLock
    }
}

/**
 * Call right before opening another app's screen (camera, file picker, share sheet, email, Google Play
 * payment), so coming back from it doesn't ask to unlock again.
 */
fun Context.expectExternalReturn() = ExpensesApp.lock(this).expectExternalReturn()
