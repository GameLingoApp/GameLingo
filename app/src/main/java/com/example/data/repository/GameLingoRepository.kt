package com.example.data.repository

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationRecord
import kotlinx.coroutines.flow.Flow

class GameLingoRepository(
    private val translationDao: TranslationDao
) {
    val allTranslations: Flow<List<TranslationRecord>> = translationDao.getAllTranslations()
    val recentTranslations: Flow<List<TranslationRecord>> = translationDao.getRecentTranslations(5)
    val favoriteTranslations: Flow<List<TranslationRecord>> = translationDao.getFavoriteTranslations()

    suspend fun saveTranslation(record: TranslationRecord): Long =
        translationDao.insertTranslation(record)

    suspend fun toggleTranslationFavorite(id: Long) =
        translationDao.toggleFavorite(id)

    suspend fun deleteTranslation(id: Long) =
        translationDao.deleteById(id)

    suspend fun clearHistory() =
        translationDao.clearAll()
}
