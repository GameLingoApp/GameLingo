package com.example.data.repository

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationRecord
import com.example.engine.GameTranslationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface TranslationRepository {
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String
}

class TranslationRepositoryImpl(
    private val translationEngine: GameTranslationEngine = GameTranslationEngine(),
    private val translationDao: TranslationDao
) : TranslationRepository {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        // 1. Check Room Cache
        val cached = translationDao.findCachedTranslation(trimmed, sourceLang, targetLang)
        if (cached != null && cached.translatedText.isNotBlank()) {
            return@withContext cached.translatedText
        }

        // 2. Perform translation with engine
        val result = translationEngine.translate(
            text = trimmed,
            sourceLang = sourceLang,
            targetLang = targetLang
        )
        val translatedText = result.translatedText

        // 3. Cache result in Room
        if (translatedText.isNotBlank()) {
            translationDao.insertTranslation(
                TranslationRecord(
                    sourceText = trimmed,
                    translatedText = translatedText,
                    sourceLangCode = sourceLang,
                    targetLangCode = targetLang,
                    genre = "TRANSLATE",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        translatedText
    }
}
