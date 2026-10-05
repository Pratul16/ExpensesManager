package com.pratul.expensesmanager.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.pratul.expensesmanager.R

data class GoogleAccount(val name: String, val email: String)

sealed class SignInResult {
    data class Success(val account: GoogleAccount) : SignInResult()
    data object Cancelled : SignInResult()
    data class Failed(val message: String) : SignInResult()
}

/** Google sign-in through Android's Credential Manager. The signed-in account is remembered on the phone. */
object GoogleAuth {
    private const val PREFS = "account"

    fun isConfigured(context: Context): Boolean =
        !context.getString(R.string.google_web_client_id).startsWith("PASTE_")

    fun savedAccount(context: Context): GoogleAccount? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val email = prefs.getString("email", null) ?: return null
        return GoogleAccount(prefs.getString("name", email) ?: email, email)
    }

    /** `activityContext` must be an Activity so the account picker can be shown. */
    suspend fun signIn(activityContext: Context): SignInResult {
        if (!isConfigured(activityContext)) {
            return SignInResult.Failed("Google sign-in isn't set up yet: the web client ID is missing.")
        }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activityContext.getString(R.string.google_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(activityContext)
                .getCredential(activityContext, request)
                .credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                val account = GoogleAccount(name = google.displayName ?: google.id, email = google.id)
                activityContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("name", account.name)
                    .putString("email", account.email)
                    .apply()
                SignInResult.Success(account)
            } else {
                SignInResult.Failed("Google returned an unexpected kind of sign-in.")
            }
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failed("No Google account found on this phone. Add one in Settings and try again.")
        } catch (e: GetCredentialException) {
            SignInResult.Failed("Sign-in failed: ${e.message ?: e.type}")
        }
    }

    suspend fun signOut(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Signing out locally still works even if clearing Google's state fails
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
