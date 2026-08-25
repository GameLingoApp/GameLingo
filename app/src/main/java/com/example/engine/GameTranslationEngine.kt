package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class TranslationResult(
    val translatedText: String,
    val isAiPowered: Boolean,
    val notes: String? = null
)

class GameTranslationEngine(
    private val mlKitTranslator: MlKitTranslator = MlKitTranslator.getInstance()
) {

    // Common game phrases across multiple languages for offline fallback
    private val gamePhrasebook = listOf(
        Pair(listOf("press start", "tap to start", "touch to begin"), mapOf("ru" to "Нажмите для начала", "en" to "Tap to start", "ja" to "タップしてスタート", "ko" to "터치하여 시작", "zh" to "点击开始")),
        Pair(listOf("inventory", "bag", "items"), mapOf("ru" to "Инвентарь / Предметы", "en" to "Inventory", "ja" to "所持品 / インベントリ", "ko" to "인벤토리 / 가방", "zh" to "背包 / 道具")),
        Pair(listOf("quest completed", "mission complete"), mapOf("ru" to "Задание выполнено!", "en" to "Quest Completed!", "ja" to "クエスト達成！", "ko" to "퀘스트 완료!", "zh" to "任务完成！")),
        Pair(listOf("level up", "rank up"), mapOf("ru" to "Повышение уровня!", "en" to "Level Up!", "ja" to "レベルアップ！", "ko" to "레벨 업!", "zh" to "等级提升！")),
        Pair(listOf("defeat the boss", "kill the boss"), mapOf("ru" to "Победите босса подземелья", "en" to "Defeat the dungeon boss", "ja" to "ボスを討伐せよ", "ko" to "보스를 처치하십시오", "zh" to "击败首领")),
        Pair(listOf("equip", "unequip"), mapOf("ru" to "Экипировать / Снять", "en" to "Equip / Unequip", "ja" to "装備 / 外す", "ko" to "장착 / 해제", "zh" to "装备 / 卸下")),
        Pair(listOf("stamina exhausted", "not enough resin", "no energy"), mapOf("ru" to "Недостаточно выносливости / смолы", "en" to "Not enough stamina / resin", "ja" to "スタミナが不足しています", "ko" to "스태ми나가 부족합니다", "zh" to "体力不足")),
        Pair(listOf("critical hit", "crit rate"), mapOf("ru" to "Критический удар!", "en" to "Critical Hit!", "ja" to "会心の一撃！", "ko" to "치명타 적중!", "zh" to "暴击！")),
        Pair(listOf("claim reward", "collect all"), mapOf("ru" to "Забрать награду / Получить всё", "en" to "Claim Reward / Collect All", "ja" to "報酬を受け取る / 一括受取", "ko" to "보상 수령 / 모두 받기", "zh" to "领取奖励 / 一键领取")),
        Pair(listOf("victory", "defeat"), mapOf("ru" to "Победа / Поражение", "en" to "Victory / Defeat", "ja" to "勝利 / 敗北", "ko" to "승리 / 패배", "zh" to "胜利 / 失败")),
        Pair(listOf("gacha pull", "wish 10x", "summon 10"), mapOf("ru" to "Призыв 10x / Совершить 10 круток", "en" to "Summon 10x / Make 10 Wishes", "ja" to "10連召喚 / 祈願", "ko" to "10회 소환 / 기원", "zh" to "十连抽 / 祈愿")),
        Pair(listOf("skill on cooldown", "cooldown active"), mapOf("ru" to "Навык перезаряжается (КД)", "en" to "Skill is on cooldown", "ja" to "スキル再使用待機中 (クールダウン)", "ko" to "스킬 쿨다운 중", "zh" to "技能冷却中")),
        Pair(listOf("healing potion", "mana potion"), mapOf("ru" to "Зелье здоровья / Зелье маны", "en" to "Health Potion / Mana Potion", "ja" to "HPポーション / MPポーション", "ko" to "체력 물약 / 마나 물약", "zh" to "生命药水 / 魔法药水")),
        Pair(listOf("party leader", "join party", "leave guild"), mapOf("ru" to "Лидер группы / Вступить в отряд / Покинуть гильдию", "en" to "Party Leader / Join Party / Leave Guild", "ja" to "パーティリーダー / 参加 / 脱退", "ko" to "파티장 / 파티 참가 / 탈퇴", "zh" to "队长 / 加入队伍 / 离开公会")),
        Pair(listOf("fast travel point unlocked", "teleport waypoint"), mapOf("ru" to "Точка телепортации открыта", "en" to "Teleport Waypoint Unlocked", "ja" to "ワープポイント解放", "ko" to "워프 포인트 활성화", "zh" to "传送锚点已解锁")),
        Pair(listOf("pity counter", "guaranteed 5 star"), mapOf("ru" to "Счётчик гаранта (Гарантированный 5★)", "en" to "Pity counter (Guaranteed 5-Star)", "ja" to "天井カウント (★5確定)", "ko" to "천장 카운터 (5성 확정)", "zh" to "保底计数器 (保底五星)")),
        Pair(listOf("matchmaking in progress", "finding match"), mapOf("ru" to "Поиск матча / Подбор игроков", "en" to "Matchmaking in progress", "ja" to "マッチング中...", "ko" to "매칭 중...", "zh" to "匹配中...")),
        Pair(listOf("defense reduced by 30%", "atk increased by 20%"), mapOf("ru" to "Защита снижена на 30% / Атака увеличена на 20%", "en" to "DEF -30% / ATK +20%", "ja" to "防御力30%低下 / 攻撃力20%上昇", "ko" to "방어력 30% 감소 / 공격력 20% 증가", "zh" to "防御力降低30% / 攻击力提升20%")),
        Pair(listOf("daily reset in", "server reset time"), mapOf("ru" to "Ежедневный сброс сервера через...", "en" to "Daily server reset in...", "ja" to "日課リセットまで...", "ko" to "일일 초기화까지...", "zh" to "每日重置倒计时...")),
        Pair(listOf("auto battle enabled", "skip dialogue"), mapOf("ru" to "Автобой включен / Пропуск диалога", "en" to "Auto-Battle On / Skip Dialogue", "ja" to "オートバトルON / 会話スキップ", "ko" to "자동 전투 활성화 / 대화 스킵", "zh" to "自动战斗开启 / 跳过对话"))
    )

    private val commonWordsRu = mapOf(
        "attack" to "атака",
        "defense" to "защита",
        "health" to "здоровье (HP)",
        "mana" to "мана (MP)",
        "stamina" to "выносливость",
        "agility" to "ловкость",
        "strength" to "сила",
        "intelligence" to "интеллект",
        "cooldown" to "перезарядка (КД)",
        "critical" to "критический",
        "crit rate" to "шанс крита",
        "crit damage" to "крит. урон",
        "damage" to "урон",
        "shield" to "щит",
        "invulnerable" to "неуязвим",
        "stunned" to "оглушён",
        "frozen" to "заморожен",
        "poisoned" to "отравлен",
        "bleeding" to "кровотечение",
        "burn" to "горение",
        "silence" to "молчание / безмолвие",
        "root" to "обездвиживание",
        "slow" to "замедление",
        "passive skill" to "пассивный навык",
        "active skill" to "активный навык",
        "ultimate skill" to "ультимативная способность (ульта)",
        "burst" to "взрыв стихии / бурст",
        "weapon" to "оружие",
        "armor" to "броня",
        "artifact" to "артефакт",
        "relic" to "реликвия",
        "equipment" to "снаряжение",
        "crafting" to "крафт / создание",
        "enhancement" to "улучшение / заточка",
        "refinement" to "пробуждение / улучшение",
        "ascension" to "возвышение",
        "dungeon" to "подземелье / данж",
        "raid" to "рейд",
        "boss" to "босс",
        "elite monster" to "элитный монстр",
        "minion" to "миньон / моб",
        "loot" to "лут / добыча",
        "drop" to "дроп",
        "chest" to "сундук",
        "reward" to "награда",
        "currency" to "валюта",
        "gold" to "золото",
        "gems" to "кристаллы / гемы",
        "banner" to "баннер призыва",
        "gacha" to "гача",
        "pity" to "гарант",
        "guild" to "гильдия / клан",
        "party" to "группа / отряд",
        "pvp" to "PvP (бой между игроками)",
        "pve" to "PvE (бой с монстрами)",
        "tank" to "танк",
        "healer" to "хилер / лекарь",
        "support" to "саппорт / поддержка",
        "debuff" to "дебафф / ослабление",
        "buff" to "бафф / усиление",
        "nerf" to "нерф / ослабление",
        "stagger" to "ошеломление / стан",
        "poise" to "баланс / стойкость"
    )

    suspend fun translate(
        text: String,
        sourceLang: String = "en",
        targetLang: String = "ru",
        customApiKey: String? = null
    ): TranslationResult = withContext(Dispatchers.IO) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return@withContext TranslationResult(
                translatedText = "",
                isAiPowered = false
            )
        }

        // Perform ML Kit Translation
        try {
            val rawTranslated = mlKitTranslator.translate(
                text = trimmed,
                sourceLang = sourceLang,
                targetLang = targetLang
            )

            TranslationResult(
                translatedText = rawTranslated,
                isAiPowered = true
            )
        } catch (e: Exception) {
            // Fallback to offline phrase translation if ML Kit encounters issue
            val offlineTranslation = translateOffline(trimmed, sourceLang, targetLang)

            TranslationResult(
                translatedText = offlineTranslation,
                isAiPowered = false,
                notes = "Офлайн перевод"
            )
        }
    }

    private fun translateOffline(text: String, sourceLang: String, targetLang: String): String {
        val lower = text.lowercase(Locale.ROOT)

        // Check phrasebook exact or contained matches
        for ((keys, translations) in gamePhrasebook) {
            for (key in keys) {
                if (lower.contains(key)) {
                    val targetTranslation = translations[targetLang.lowercase()]
                    if (targetTranslation != null) {
                        return targetTranslation
                    }
                }
            }
        }

        // Japanese game characters
        if (sourceLang.equals("ja", ignoreCase = true) || lower.any { it in '\u3040'..'\u30ff' || it in '\u4e00'..'\u9faf' }) {
            val jaTranslations = mapOf(
                "クエスト" to "Квест / Задание",
                "召喚" to "Призыв (Гача)",
                "ガチャ" to "Гача",
                "装備" to "Экипировка / Снаряжение",
                "強化" to "Улучшение / Заточка",
                "限界突破" to "Преодоление лимита (Limit Break)",
                "冒険" to "Приключение",
                "スキル" to "Навык / Способность",
                "奥義" to "Секретный навык / Ульта",
                "スタミナ" to "Выносливость / Энергия",
                "フレンド" to "Друзья",
                "ギルド" to "Гильдия",
                "ボス討伐" to "Битва с боссом",
                "報酬受取" to "Получить награду",
                "オート" to "Авторежим",
                "倍速" to "Ускорение x2",
                "設定" to "Настройки"
            )
            for ((jaKey, ruVal) in jaTranslations) {
                if (text.contains(jaKey)) {
                    return if (targetLang.equals("ru", ignoreCase = true)) ruVal else "$ruVal ($jaKey)"
                }
            }
        }

        // Korean game words
        if (sourceLang.equals("ko", ignoreCase = true) || lower.any { it in '\uac00'..'\ud7af' }) {
            val koTranslations = mapOf(
                "クエスト" to "Квест / Задание",
                "뽑기" to "Гача / Крутка",
                "소환" to "Призыв персонажа",
                "장비" to "Снаряжение / Оружие",
                "강화" to "Усиление / Заточка",
                "초월" to "Трансцендентность / Возвышение",
                "스킬" to "Способность / Навык",
                "궁극기" to "Ультимативный навык (Ульта)",
                "행동력" to "Очки действия / Энергия",
                "길드" to "Гильдия / Клан",
                "던전" to "Подземелье",
                "보상" to "Награда"
            )
            for ((koKey, ruVal) in koTranslations) {
                if (text.contains(koKey)) {
                    return if (targetLang.equals("ru", ignoreCase = true)) ruVal else "$ruVal ($koKey)"
                }
            }
        }

        // Word substitutions
        if (targetLang.equals("ru", ignoreCase = true)) {
            var localized = text
            for ((enWord, ruWord) in commonWordsRu) {
                val regex = Regex("(?i)\\b${Regex.escape(enWord)}\\b")
                if (regex.containsMatchIn(localized)) {
                    localized = regex.replace(localized, ruWord)
                }
            }
            if (localized != text) {
                return localized
            }
        }

        return "[Перевод GameLingo]: $text"
    }
}
