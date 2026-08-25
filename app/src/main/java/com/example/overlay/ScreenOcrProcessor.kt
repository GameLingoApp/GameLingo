package com.example.overlay

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ScreenOcrProcessor {

    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val japaneseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    private val chineseRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    private val koreanRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeText(bitmap: Bitmap?, preferredLanguage: String = "auto"): String = withContext(Dispatchers.Default) {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return@withContext ""
        }
        val inputImage = try {
            InputImage.fromBitmap(bitmap, 0)
        } catch (e: Exception) {
            Log.e("ScreenOcrProcessor", "Cannot create InputImage: ${e.message}")
            return@withContext ""
        }
        
        try {
            val recognizer = when (preferredLanguage.lowercase()) {
                "ja" -> japaneseRecognizer
                "zh" -> chineseRecognizer
                "ko" -> koreanRecognizer
                else -> latinRecognizer
            }

            val visionText = recognizer.process(inputImage).await()
            val text = visionText.text.trim()

            // If latin didn't find anything or user is on auto, try other Asian script recognizers if text is empty
            if (text.isEmpty() && preferredLanguage == "auto") {
                val jaText = try { japaneseRecognizer.process(inputImage).await().text.trim() } catch (e: Exception) { "" }
                if (jaText.isNotEmpty()) return@withContext jaText

                val zhText = try { chineseRecognizer.process(inputImage).await().text.trim() } catch (e: Exception) { "" }
                if (zhText.isNotEmpty()) return@withContext zhText

                val koText = try { koreanRecognizer.process(inputImage).await().text.trim() } catch (e: Exception) { "" }
                if (koText.isNotEmpty()) return@withContext koText
            }

            text
        } catch (e: Exception) {
            Log.e("ScreenOcrProcessor", "OCR failed: ${e.message}", e)
            // Fallback to latin if specific recognizer failed
            try {
                latinRecognizer.process(inputImage).await().text.trim()
            } catch (fallbackEx: Exception) {
                ""
            }
        }
    }
}
