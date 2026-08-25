package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthManager
import com.example.data.remote.PaymentManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PremiumViewModel(
    private val paymentManager: PaymentManager,
    private val authManager: AuthManager
) : ViewModel() {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isSignedIn = MutableStateFlow(authManager.currentUser.value?.isGuest == false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _paymentUrl = MutableStateFlow<String?>(null)
    val paymentUrl: StateFlow<String?> = _paymentUrl.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    val userId: String
        get() = authManager.currentUser.value?.id ?: ""

    init {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _isSignedIn.value = (user != null && !user.isGuest)
                val currentId = user?.id ?: ""
                if (currentId.isNotEmpty() && user?.isGuest == false) {
                    _isPremium.value = paymentManager.checkPremiumStatus(currentId)
                    paymentManager.observePremiumStatus(currentId).collect { status ->
                        val wasNotPremium = !_isPremium.value
                        _isPremium.value = status
                        if (status && wasNotPremium) {
                            _showSuccess.value = true
                        }
                    }
                } else {
                    _isPremium.value = paymentManager.getCachedPremiumStatus()
                }
            }
        }
    }

    fun subscribe(planType: String) {
        if (authManager.currentUser.value?.isGuest != false) {
            _error.value = "Please sign in with Google to subscribe"
            return
        }
        if (userId.isEmpty()) {
            _error.value = "Please sign in first"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = paymentManager.createPayment(userId, planType)
            _isLoading.value = false
            result.fold(
                onSuccess = { url -> _paymentUrl.value = url },
                onFailure = { e -> _error.value = "Payment error: ${e.message}" }
            )
        }
    }

    fun signInAndThen(context: Context? = null, planType: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = authManager.signInWithGoogle()
            _isLoading.value = false
            result.fold(
                onSuccess = {
                    subscribe(planType)
                },
                onFailure = { e ->
                    _error.value = e.localizedMessage ?: "Sign in failed"
                }
            )
        }
    }

    fun openPayment(context: Context) {
        _paymentUrl.value?.let { url ->
            try {
                val intent = CustomTabsIntent.Builder()
                    .setToolbarColor(0xFF3B82F6.toInt())
                    .setShowTitle(true)
                    .build()
                intent.launchUrl(context, Uri.parse(url))
            } catch (e: Exception) {
                _error.value = "Cannot open payment page: ${e.message}"
            }
            _paymentUrl.value = null
        }
    }

    fun onReturnToApp() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(3000)
            if (userId.isNotEmpty()) {
                val status = paymentManager.checkPremiumStatus(userId)
                _isPremium.value = status
                if (status) {
                    _showSuccess.value = true
                }
            }
            _isLoading.value = false
        }
    }

    fun dismissSuccess() {
        _showSuccess.value = false
    }

    fun dismissError() {
        _error.value = null
    }
}
