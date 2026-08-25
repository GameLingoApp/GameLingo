package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val genre: String = "GAME",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
