package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.AuthManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PaymentManager(
    private val functions: FirebaseFunctions = Firebase.functions,
    private val firestore: FirebaseFirestore = Firebase.firestore,
    context: Context? = null,
    private val authManager: AuthManager? = null
) {

    companion object {
        private const val TAG = "PaymentManager"
        private const val PREFS_NAME = "gamelingo_payment_prefs"
        private const val KEY_CACHED_PREMIUM = "cached_is_premium"
        private const val KEY_CACHED_EXPIRY = "cached_premium_expiry"
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun createPayment(userId: String, planType: String): Result<String> {
        val user = authManager?.currentUser?.value
        if (user == null || user.isGuest || userId.isEmpty()) {
            return Result.failure(Exception("Sign in required"))
        }

        return try {
            Log.d(TAG, "Calling createPayment function for userId: $userId, plan: $planType")
            val data = hashMapOf(
                "userId" to userId,
                "planType" to planType
            )
            val result = functions.getHttpsCallable("createPayment")
                .call(data)
                .await()

            val resultMap = result.data as? Map<*, *>
            val url = resultMap?.get("confirmation_url") as? String
                ?: throw IllegalStateException("No confirmation_url returned in function response")

            Log.d(TAG, "Received payment URL: $url")
            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating YooKassa payment: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun checkPremiumStatus(userId: String): Boolean {
        if (userId.isEmpty()) return getCachedPremiumStatus()
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            val isPremium = doc.getBoolean("isPremium") ?: false
            val expiry = doc.getLong("premiumExpiry") ?: 0L
            val active = isPremium && (expiry == 0L || expiry > System.currentTimeMillis())
            cachePremiumStatus(active, expiry)
            active
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch premium status from Firestore: ${e.message}, using cache")
            getCachedPremiumStatus()
        }
    }

    fun observePremiumStatus(userId: String): Flow<Boolean> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(getCachedPremiumStatus())
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Snapshot error observing premium status: ${error.message}")
                    trySend(getCachedPremiumStatus())
                    return@addSnapshotListener
                }
                val isPremium = snapshot?.getBoolean("isPremium") ?: false
                val expiry = snapshot?.getLong("premiumExpiry") ?: 0L
                val active = isPremium && (expiry == 0L || expiry > System.currentTimeMillis())
                cachePremiumStatus(active, expiry)
                trySend(active)
            }
        awaitClose { listener.remove() }
    }

    private fun cachePremiumStatus(isPremium: Boolean, expiry: Long) {
        prefs?.edit()
            ?.putBoolean(KEY_CACHED_PREMIUM, isPremium)
            ?.putLong(KEY_CACHED_EXPIRY, expiry)
            ?.apply()
    }

    fun getCachedPremiumStatus(): Boolean {
        val isPremium = prefs?.getBoolean(KEY_CACHED_PREMIUM, false) ?: false
        val expiry = prefs?.getLong(KEY_CACHED_EXPIRY, 0L) ?: 0L
        return isPremium && (expiry == 0L || expiry > System.currentTimeMillis())
    }

    fun setLocalPremiumOverride(isPremium: Boolean) {
        val expiry = if (isPremium) System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 else 0L
        cachePremiumStatus(isPremium, expiry)
    }
}
