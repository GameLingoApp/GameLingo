package com.example.data.model

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

val SupportedLanguages = listOf(
    Language(code = "en", name = "English", nativeName = "English", flagEmoji = "🇺🇸"),
    Language(code = "ru", name = "Russian", nativeName = "Русский", flagEmoji = "🇷🇺"),
    Language(code = "ja", name = "Japanese", nativeName = "日本語", flagEmoji = "🇯🇵"),
    Language(code = "ko", name = "Korean", nativeName = "한국어", flagEmoji = "🇰🇷"),
    Language(code = "zh", name = "Chinese", nativeName = "简体中文", flagEmoji = "🇨🇳"),
    Language(code = "de", name = "German", nativeName = "Deutsch", flagEmoji = "🇩🇪"),
    Language(code = "fr", name = "French", nativeName = "Français", flagEmoji = "🇫🇷"),
    Language(code = "es", name = "Spanish", nativeName = "Español", flagEmoji = "🇪🇸"),
    Language(code = "pt", name = "Portuguese", nativeName = "Português", flagEmoji = "🇧🇷"),
    Language(code = "tr", name = "Turkish", nativeName = "Türkçe", flagEmoji = "🇹🇷"),
    Language(code = "vi", name = "Vietnamese", nativeName = "Tiếng Việt", flagEmoji = "🇻🇳"),
    Language(code = "id", name = "Indonesian", nativeName = "Bahasa Indonesia", flagEmoji = "🇮🇩")
)

fun getLanguageByCode(code: String): Language {
    return SupportedLanguages.find { it.code.equals(code, ignoreCase = true) }
        ?: Language(code = code, name = code.uppercase(), nativeName = code.uppercase(), flagEmoji = "🌐")
}
