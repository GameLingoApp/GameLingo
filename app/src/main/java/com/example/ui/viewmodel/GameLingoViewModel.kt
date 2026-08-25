package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthManager
import com.example.data.DataStoreManager
import com.example.data.local.GameLingoDatabase
import com.example.data.model.Language
import com.example.data.model.SupportedLanguages
import com.example.data.model.TranslationRecord
import com.example.data.model.UserAccount
import com.example.data.remote.PaymentManager
import com.example.data.repository.GameLingoRepository
import com.example.engine.GameTranslationEngine
import com.example.engine.TranslationResult
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.functions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    TRANSLATE("Перевод"),
    OVERLAY("Оверлей"),
    HISTORY("История"),
    PRO("GameLingo Pro")
}

data class UiTranslationState(
    val isLoading: Boolean = false,
    val result: TranslationResult? = null,
    val errorMessage: String? = null
)

data class OverlaySettings(
    val isOverlayEnabled: Boolean = false,
    val opacity: Float = 0.85f,
    val autoTranslateFrequencySec: Int = 3,
    val selectedGameZone: String = "Диалоговое окно (Низ)",
    val autoCopy: Boolean = false
)

class GameLingoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameLingoRepository
    private val engine = GameTranslationEngine()
    val authManager: AuthManager = AuthManager(application)
    val paymentManager: PaymentManager = PaymentManager(Firebase.functions, Firebase.firestore, application, authManager)
    private val dataStoreManager = DataStoreManager(application)

    // User & Authentication State
    val currentUser: StateFlow<UserAccount?> = authManager.currentUser
    val isOnboardingCompleted: StateFlow<Boolean> = authManager.isOnboardingCompleted
    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    // Premium Subscription State from PaymentManager
    private val _isProUnlocked = MutableStateFlow(paymentManager.getCachedPremiumStatus())
    val isProUnlocked: StateFlow<Boolean> = _isProUnlocked.asStateFlow()

    init {
        val db = GameLingoDatabase.getDatabase(application)
        repository = GameLingoRepository(db.translationDao())

        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                val userId = user?.id ?: ""
                if (userId.isNotEmpty()) {
                    _isProUnlocked.value = paymentManager.checkPremiumStatus(userId)
                    paymentManager.observePremiumStatus(userId).collect { isPrem ->
                        _isProUnlocked.value = isPrem
                    }
                } else {
                    _isProUnlocked.value = paymentManager.getCachedPremiumStatus()
                }
            }
        }
    }

    // Active Navigation Tab
    private val _selectedTab = MutableStateFlow(AppTab.TRANSLATE)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // Languages
    private val _sourceLanguage = MutableStateFlow(SupportedLanguages[0]) // English
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(SupportedLanguages[1]) // Russian
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    // Input Text
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // Translation State
    private val _translationState = MutableStateFlow(UiTranslationState())
    val translationState: StateFlow<UiTranslationState> = _translationState.asStateFlow()

    // Settings
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // Limits
    val dailyUsageCount: StateFlow<Int> = dataStoreManager.dailyUsageCount

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Overlay HUD
    private val _overlaySettings = MutableStateFlow(OverlaySettings())
    val overlaySettings: StateFlow<OverlaySettings> = _overlaySettings.asStateFlow()

    // History search
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    // Database streams
    val recentTranslations: StateFlow<List<TranslationRecord>> = repository.recentTranslations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTranslations: StateFlow<List<TranslationRecord>> = combine(
        repository.allTranslations,
        _historySearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.sourceText.contains(query, ignoreCase = true) ||
                    it.translatedText.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setSourceLanguage(language: Language) {
        _sourceLanguage.value = language
    }

    fun setTargetLanguage(language: Language) {
        _targetLanguage.value = language
    }

    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun clearInput() {
        _inputText.value = ""
        _translationState.value = UiTranslationState()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun checkPaymentStatus() {
        val uid = currentUser.value?.id ?: ""
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                val status = paymentManager.checkPremiumStatus(uid)
                _isProUnlocked.value = status
            }
        }
    }

    fun translateCurrent() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        // Check daily limit (10 translations per day for free tier, unlimited for Pro)
        val canTranslate = _isProUnlocked.value || (dailyUsageCount.value < 10)
        if (!canTranslate) {
            val isGuest = currentUser.value?.isGuest != false
            val limitMsg = if (isGuest) {
                "Daily limit reached. Sign in to continue."
            } else {
                "Daily limit reached. Upgrade to Pro."
            }
            _translationState.value = UiTranslationState(
                isLoading = false,
                errorMessage = limitMsg
            )
            _snackbarMessage.value = limitMsg
            return
        }

        _translationState.value = UiTranslationState(isLoading = true)

        viewModelScope.launch {
            try {
                val result = engine.translate(
                    text = text,
                    sourceLang = _sourceLanguage.value.code,
                    targetLang = _targetLanguage.value.code,
                    customApiKey = _customApiKey.value
                )

                _translationState.value = UiTranslationState(
                    isLoading = false,
                    result = result
                )

                // Increment daily usage counter on success
                dataStoreManager.incrementUsage()

                // Save to Room history
                repository.saveTranslation(
                    TranslationRecord(
                        sourceText = text,
                        translatedText = result.translatedText,
                        sourceLangCode = _sourceLanguage.value.code,
                        targetLangCode = _targetLanguage.value.code,
                        genre = "GAME",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                _translationState.value = UiTranslationState(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Ошибка перевода"
                )
            }
        }
    }

    fun translateSample(sampleText: String) {
        _inputText.value = sampleText
        translateCurrent()
    }

    fun toggleTranslationFavorite(id: Long) {
        viewModelScope.launch {
            repository.toggleTranslationFavorite(id)
        }
    }

    fun deleteTranslation(id: Long) {
        viewModelScope.launch {
            repository.deleteTranslation(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun updateOverlaySettings(transform: (OverlaySettings) -> OverlaySettings) {
        val newSettings = transform(_overlaySettings.value)
        if (newSettings.isOverlayEnabled && !_isProUnlocked.value) {
            _snackbarMessage.value = "Оверлей доступен только для подписчиков GameLingo Pro"
            _overlaySettings.value = newSettings.copy(isOverlayEnabled = false)
        } else {
            _overlaySettings.value = newSettings
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    fun signInWithGoogle(onSuccess: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (_isSigningIn.value) return
        _isSigningIn.value = true
        viewModelScope.launch {
            try {
                val result = authManager.signInWithGoogle()
                _isSigningIn.value = false
                result.fold(
                    onSuccess = { user ->
                        _snackbarMessage.value = "Добро пожаловать, ${user.displayName}!"
                        onSuccess?.invoke()
                    },
                    onFailure = { error ->
                        val msg = error.localizedMessage ?: "Ошибка входа через Google"
                        _snackbarMessage.value = msg
                        onError?.invoke(msg)
                    }
                )
            } catch (e: Exception) {
                _isSigningIn.value = false
                val msg = e.localizedMessage ?: "Не удалось войти через Google"
                _snackbarMessage.value = msg
                onError?.invoke(msg)
            }
        }
    }

    fun continueAsGuest(onSuccess: (() -> Unit)? = null) {
        authManager.continueAsGuest()
        _snackbarMessage.value = "Вход выполнен в гостевом режиме"
        onSuccess?.invoke()
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _isProUnlocked.value = false
            _snackbarMessage.value = "Вы вышли из аккаунта"
        }
    }
}
