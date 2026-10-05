package com.pratul.expensesmanager.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity behind a Compose context (needed by system sheets like screen-lock checks and payments). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
