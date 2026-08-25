package com.example.engine

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitTranslator {

    companion object {
        @Volatile
        private var instance: MlKitTranslator? = null

        fun getInstance(): MlKitTranslator {
            return instance ?: synchronized(this) {
                instance ?: MlKitTranslator().also { instance = it }
            }
        }

        private val translators = ConcurrentHashMap<String, Translator>()
        private val modelManager = RemoteModelManager.getInstance()
        private val downloadMutex = Mutex()
        private val downloadedPairs = ConcurrentHashMap<String, Boolean>()
    }

    fun detectLanguage(text: String): String {
        for (char in text) {
            when (char) {
                in '\u3040'..'\u30ff' -> return "ja" // Hiragana / Katakana
                in '\uac00'..'\ud7af' -> return "ko" // Korean Hangul
                in '\u0400'..'\u04ff' -> return "ru" // Cyrillic
                in '\u4e00'..'\u9faf' -> return "zh" // Chinese CJK
            }
        }
        return "en"
    }

    private fun normalizeLanguageCode(code: String, textSample: String = ""): String {
        val lower = code.lowercase().trim()
        if (lower == "auto" || lower.isEmpty()) {
            val detected = detectLanguage(textSample)
            return normalizeLanguageCode(detected)
        }

        return when (lower) {
            "en", "eng" -> TranslateLanguage.ENGLISH
            "ru", "rus" -> TranslateLanguage.RUSSIAN
            "ja", "jpn", "jp" -> TranslateLanguage.JAPANESE
            "ko", "kor", "kr" -> TranslateLanguage.KOREAN
            "zh", "zho", "chi", "cn" -> TranslateLanguage.CHINESE
            "de", "deu", "ger" -> TranslateLanguage.GERMAN
            "fr", "fra", "fre" -> TranslateLanguage.FRENCH
            "es", "spa" -> TranslateLanguage.SPANISH
            "it", "ita" -> TranslateLanguage.ITALIAN
            "pt", "por" -> TranslateLanguage.PORTUGUESE
            "tr", "tur" -> TranslateLanguage.TURKISH
            "vi", "vie" -> TranslateLanguage.VIETNAMESE
            "id", "ind" -> TranslateLanguage.INDONESIAN
            "th", "tha" -> TranslateLanguage.THAI
            "ar", "ara" -> TranslateLanguage.ARABIC
            "pl", "pol" -> TranslateLanguage.POLISH
            "uk", "ukr" -> TranslateLanguage.UKRAINIAN
            else -> TranslateLanguage.fromLanguageTag(lower) ?: TranslateLanguage.ENGLISH
        }
    }

    private fun getTranslator(src: String, tgt: String): Translator {
        val key = "$src-$tgt"
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(src)
                .setTargetLanguage(tgt)
                .build()
            Translation.getClient(options)
        }
    }

    /**
     * Translates text from sourceLang to targetLang.
     * Reuses shared model instances to prevent duplicate native allocations.
     */
    suspend fun translate(
        text: String,
        sourceLang: String = "en",
        targetLang: String = "ru"
    ): String = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext ""

        val srcNorm = normalizeLanguageCode(sourceLang, trimmed)
        val tgtNorm = normalizeLanguageCode(targetLang)
        if (srcNorm == tgtNorm) {
            return@withContext trimmed
        }

        val key = "$srcNorm-$tgtNorm"
        val translator = getTranslator(srcNorm, tgtNorm)

        // Ensure model is downloaded only once per pair to prevent redundant native initialization
        if (downloadedPairs[key] != true) {
            downloadMutex.withLock {
                if (downloadedPairs[key] != true) {
                    try {
                        val conditions = DownloadConditions.Builder().build()
                        withTimeoutOrNull(15000L) {
                            awaitTask(translator.downloadModelIfNeeded(conditions))
                        }
                        downloadedPairs[key] = true
                    } catch (e: Exception) {
                        Log.w("MlKitTranslator", "Model check for $key: ${e.message}")
                    }
                }
            }
        }

        // Execute translation with timeout
        try {
            val translated = withTimeoutOrNull(8000L) {
                awaitTask(translator.translate(trimmed))
            } ?: throw IllegalStateException("Translation timeout")

            translated
        } catch (e: Exception) {
            Log.w("MlKitTranslator", "Translate failed for $key: ${e.message}")
            throw e
        }
    }

    suspend fun downloadModel(language: String): Boolean = withContext(Dispatchers.IO) {
        val langCode = normalizeLanguageCode(language)
        val model = TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder().build()
        try {
            withTimeoutOrNull(20000L) {
                awaitTask(modelManager.download(model, conditions))
            } != null
        } catch (e: Exception) {
            Log.e("MlKitTranslator", "Failed to download model $language: ${e.message}")
            false
        }
    }

    suspend fun deleteModel(language: String): Boolean = withContext(Dispatchers.IO) {
        val langCode = normalizeLanguageCode(language)
        val model = TranslateRemoteModel.Builder(langCode).build()
        try {
            awaitTask(modelManager.deleteDownloadedModel(model))
            true
        } catch (e: Exception) {
            Log.e("MlKitTranslator", "Failed to delete model $language: ${e.message}")
            false
        }
    }

    suspend fun isModelDownloaded(language: String): Boolean = withContext(Dispatchers.IO) {
        val langCode = normalizeLanguageCode(language)
        val model = TranslateRemoteModel.Builder(langCode).build()
        try {
            val downloadedModels = awaitTask(modelManager.getDownloadedModels(TranslateRemoteModel::class.java))
            downloadedModels.any { it.language == model.language }
        } catch (e: Exception) {
            false
        }
    }

    fun close() {
        translators.values.forEach { 
            try {
                it.close()
            } catch (_: Exception) {}
        }
        translators.clear()
        downloadedPairs.clear()
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { result ->
            if (cont.isActive) {
                cont.resume(result)
            }
        }.addOnFailureListener { exception ->
            if (cont.isActive) {
                cont.resumeWithException(exception)
            }
        }.addOnCanceledListener {
            if (cont.isActive) {
                cont.cancel()
            }
        }
    }
}
