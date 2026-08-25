package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.UserAccount
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthManager(private val context: Context) {

    companion object {
        private const val TAG = "AuthManager"
        private const val PREFS_NAME = "gamelingo_auth_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHOTO = "user_photo"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        // Default OAuth Web Client ID
        private const val DEFAULT_WEB_CLIENT_ID = "692874714541-nqqvl7r9avemsq3apj8v2oqpf7ksebu4.apps.googleusercontent.com"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow(loadUserFromPrefs())
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false) || _currentUser.value != null
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private fun loadUserFromPrefs(): UserAccount? {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) return null

        val id = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "Игрок") ?: "Игрок"
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val photo = prefs.getString(KEY_USER_PHOTO, null)
        val isGuest = prefs.getBoolean(KEY_IS_GUEST, false)

        return UserAccount(
            id = id,
            displayName = name,
            email = email,
            photoUrl = photo,
            isGuest = isGuest,
            isAuthenticated = true
        )
    }

    suspend fun signInWithGoogle(): Result<UserAccount> {
        return try {
            val clientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                DEFAULT_WEB_CLIENT_ID
            }

            Log.d(TAG, "Using client ID: $clientId")

            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "Requesting credential from CredentialManager...")
            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            Log.d(TAG, "Credential type: ${credential.type}")
            Log.d(TAG, "Credential class: ${credential::class.java.name}")

            when {
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {

                    Log.d(TAG, "Parsing Google ID token credential...")
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    Log.d(TAG, "Got ID token, signing in with Firebase...")

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()

                    val firebaseUser = authResult.user
                    Log.d(TAG, "Firebase sign-in success: ${firebaseUser?.displayName}")

                    val user = UserAccount(
                        id = firebaseUser?.uid ?: googleIdTokenCredential.id,
                        displayName = firebaseUser?.displayName ?: googleIdTokenCredential.displayName ?: "Google User",
                        email = firebaseUser?.email ?: googleIdTokenCredential.id,
                        photoUrl = firebaseUser?.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isGuest = false,
                        isAuthenticated = true
                    )
                    saveUser(user)
                    Result.success(user)
                }
                else -> {
                    Log.e(TAG, "Unexpected credential type: ${credential.type}")
                    Result.failure(Exception("Unexpected credential type: ${credential.type}"))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available: ${e.message}")
            Result.failure(Exception("No Google accounts found on this device. Please add a Google account in Settings."))
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential error: ${e.message}", e)
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Result.failure(Exception("Sign-in failed: ${e.message}"))
        }
    }

    fun continueAsGuest(): UserAccount {
        val guest = UserAccount.Guest
        saveUser(guest)
        return guest
    }

    private fun saveUser(user: UserAccount) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .putBoolean(KEY_IS_GUEST, user.isGuest)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()

        _currentUser.value = user
        _isOnboardingCompleted.value = true
    }

    suspend fun signOut() {
        try {
            Firebase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error signing out from Firebase: ${e.message}")
        }
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing credential state: ${e.message}")
        }
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHOTO)
            .remove(KEY_IS_GUEST)
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .apply()

        _currentUser.value = null
        _isOnboardingCompleted.value = false
    }
}
