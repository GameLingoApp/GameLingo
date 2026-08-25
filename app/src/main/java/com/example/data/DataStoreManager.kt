package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DataStoreManager(context: Context) {

    companion object {
        const val FREE_DAILY_LIMIT = 10
        private const val PREFS_NAME = "gamelingo_usage_prefs"
        private const val KEY_DAILY_COUNT = "daily_translation_count"
        private const val KEY_LAST_DATE = "last_translation_date"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _dailyUsageCount = MutableStateFlow(getOrResetCount())
    val dailyUsageCount: StateFlow<Int> = _dailyUsageCount.asStateFlow()

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    @Synchronized
    private fun getOrResetCount(): Int {
        val today = getTodayDateString()
        val lastDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        return if (lastDate != today) {
            prefs.edit()
                .putString(KEY_LAST_DATE, today)
                .putInt(KEY_DAILY_COUNT, 0)
                .apply()
            0
        } else {
            prefs.getInt(KEY_DAILY_COUNT, 0)
        }
    }

    fun canTranslate(isPremium: Boolean): Boolean {
        if (isPremium) return true
        val currentCount = getOrResetCount()
        _dailyUsageCount.value = currentCount
        return currentCount < FREE_DAILY_LIMIT
    }

    @Synchronized
    fun incrementUsage() {
        val current = getOrResetCount()
        val newCount = current + 1
        val today = getTodayDateString()
        prefs.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_DAILY_COUNT, newCount)
            .apply()
        _dailyUsageCount.value = newCount
    }

    fun getRemainingTranslations(isPremium: Boolean): Int {
        if (isPremium) return Int.MAX_VALUE
        val count = getOrResetCount()
        _dailyUsageCount.value = count
        return (FREE_DAILY_LIMIT - count).coerceAtLeast(0)
    }
}
