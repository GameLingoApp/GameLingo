package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TranslationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTranslations(limit: Int = 5): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteTranslations(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_history WHERE sourceText = :sourceText AND sourceLangCode = :sourceLang AND targetLangCode = :targetLang ORDER BY timestamp DESC LIMIT 1")
    suspend fun findCachedTranslation(sourceText: String, sourceLang: String, targetLang: String): TranslationRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(record: TranslationRecord): Long

    @Query("UPDATE translation_history SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("DELETE FROM translation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}
