package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.ai.StudyFlashcardBank
import com.example.data.model.AcademicProfile
import com.example.data.model.AppLimitRule
import com.example.data.model.AppSettings
import com.example.data.model.DEFAULT_MODEL_ID
import com.example.data.model.FlashcardSubject
import com.example.data.model.GeminiNotebook
import com.example.data.model.LanguagePreference
import com.example.data.model.MessageRole
import com.example.data.model.NotebookMessage
import com.example.data.model.NotebookSource
import com.example.data.model.NotebookSourceType
import com.example.data.model.PodcastTurn
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyProgressStats
import com.example.data.model.StudyTimerSession
import com.example.data.model.ThemeMode
import com.example.data.model.UnlockCodeRecord
import com.example.security.KeystoreCrypto
import com.example.security.SecretSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "study_update_ai_prefs"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANG_PREF = stringPreferencesKey("language_preference")
        val ACADEMIC_PROFILE = stringPreferencesKey("academic_profile")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val VOICE_LANG = stringPreferencesKey("voice_input_lang")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val AI_VOICE_NAME = stringPreferencesKey("ai_voice_name")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val MODEL_ID = stringPreferencesKey("selected_model_id")
        val USE_CUSTOM_KEY = booleanPreferencesKey("use_custom_api_key")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_custom_api_key")
        val OFFLINE_MODE_ENABLED = booleanPreferencesKey("offline_mode_enabled")
        val APP_LIMITS_JSON = stringPreferencesKey("app_limits_json")
        val UNLOCK_CODES_JSON = stringPreferencesKey("unlock_codes_json")
        val FLASHCARDS_JSON = stringPreferencesKey("flashcards_json")
        val STUDY_PROGRESS_JSON = stringPreferencesKey("study_progress_json")
        val STUDY_TIMER_SESSIONS_JSON = stringPreferencesKey("study_timer_sessions_json")
        val NOTEBOOKS_JSON = stringPreferencesKey("gemini_notebooks_json")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val encrypted = prefs[Keys.ENCRYPTED_API_KEY]
        val decrypted = encrypted?.let { KeystoreCrypto.decrypt(it) }
        AppSettings(
            themeMode = ThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            languagePreference = LanguagePreference.fromRaw(prefs[Keys.LANG_PREF]),
            academicProfile = AcademicProfile.fromRaw(prefs[Keys.ACADEMIC_PROFILE]),
            temperature = prefs[Keys.TEMPERATURE] ?: 0.7f,
            autoSpeak = prefs[Keys.AUTO_SPEAK] ?: false,
            voiceInputLang = prefs[Keys.VOICE_LANG] ?: "bn-BD",
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            aiVoiceName = prefs[Keys.AI_VOICE_NAME] ?: "AOEDE",
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            selectedModelId = prefs[Keys.MODEL_ID] ?: DEFAULT_MODEL_ID,
            useCustomApiKey = prefs[Keys.USE_CUSTOM_KEY] ?: false,
            maskedCustomApiKey = decrypted?.let { SecretSanitizer.maskApiKey(it) },
            offlineModeEnabled = prefs[Keys.OFFLINE_MODE_ENABLED] ?: false
        )
    }

    val appLimitsFlow: Flow<List<AppLimitRule>> = context.dataStore.data.map { prefs ->
        decodeAppLimits(prefs[Keys.APP_LIMITS_JSON])
    }

    val unlockRecordsFlow: Flow<List<UnlockCodeRecord>> = context.dataStore.data.map { prefs ->
        decodeUnlockRecords(prefs[Keys.UNLOCK_CODES_JSON])
    }

    val flashcardsFlow: Flow<List<StudyFlashcard>> = context.dataStore.data.map { prefs ->
        val saved = decodeFlashcards(prefs[Keys.FLASHCARDS_JSON])
        if (saved.isEmpty()) StudyFlashcardBank.defaultCards else saved
    }

    val progressStatsFlow: Flow<StudyProgressStats> = context.dataStore.data.map { prefs ->
        decodeProgressStats(prefs[Keys.STUDY_PROGRESS_JSON])
    }

    val studyTimerSessionsFlow: Flow<List<StudyTimerSession>> = context.dataStore.data.map { prefs ->
        decodeStudyTimerSessions(prefs[Keys.STUDY_TIMER_SESSIONS_JSON])
    }

    val notebooksFlow: Flow<List<GeminiNotebook>> = context.dataStore.data.map { prefs ->
        decodeNotebooks(prefs[Keys.NOTEBOOKS_JSON])
    }

    suspend fun getDecryptedCustomApiKey(): String? {
        val prefs = context.dataStore.data.first()
        val encrypted = prefs[Keys.ENCRYPTED_API_KEY] ?: return null
        return KeystoreCrypto.decrypt(encrypted)?.trim()?.takeIf { it.isNotEmpty() }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateLanguagePreference(pref: LanguagePreference) {
        context.dataStore.edit { it[Keys.LANG_PREF] = pref.name }
    }

    suspend fun updateAcademicProfile(profile: AcademicProfile) {
        context.dataStore.edit { it[Keys.ACADEMIC_PROFILE] = profile.name }
    }

    suspend fun updateTemperature(value: Float) {
        context.dataStore.edit { it[Keys.TEMPERATURE] = value.coerceIn(0f, 1.4f) }
    }

    suspend fun updateAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun updateVoiceInputLang(lang: String) {
        context.dataStore.edit { it[Keys.VOICE_LANG] = lang }
    }

    suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun updateAiVoice(voiceId: String, pitch: Float) {
        context.dataStore.edit {
            it[Keys.AI_VOICE_NAME] = voiceId
            it[Keys.TTS_PITCH] = pitch.coerceIn(0.7f, 1.4f)
        }
    }

    suspend fun updateTtsPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch.coerceIn(0.7f, 1.4f) }
    }

    suspend fun updateModelId(modelId: String) {
        context.dataStore.edit { it[Keys.MODEL_ID] = modelId }
    }

    suspend fun updateUseCustomApiKey(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_CUSTOM_KEY] = enabled }
    }

    suspend fun updateOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE_ENABLED] = enabled }
    }

    suspend fun saveCustomApiKey(rawKey: String) {
        val encrypted = KeystoreCrypto.encrypt(rawKey.trim())
        context.dataStore.edit {
            it[Keys.ENCRYPTED_API_KEY] = encrypted
            it[Keys.USE_CUSTOM_KEY] = true
        }
    }

    suspend fun clearCustomApiKey() {
        context.dataStore.edit {
            it.remove(Keys.ENCRYPTED_API_KEY)
            it[Keys.USE_CUSTOM_KEY] = false
        }
    }

    suspend fun saveAppLimits(rules: List<AppLimitRule>) {
        context.dataStore.edit { it[Keys.APP_LIMITS_JSON] = encodeAppLimits(rules) }
    }

    suspend fun saveUnlockRecords(records: List<UnlockCodeRecord>) {
        context.dataStore.edit { it[Keys.UNLOCK_CODES_JSON] = encodeUnlockRecords(records) }
    }

    suspend fun saveFlashcards(cards: List<StudyFlashcard>) {
        context.dataStore.edit { it[Keys.FLASHCARDS_JSON] = encodeFlashcards(cards) }
    }

    suspend fun saveProgressStats(stats: StudyProgressStats) {
        context.dataStore.edit { it[Keys.STUDY_PROGRESS_JSON] = encodeProgressStats(stats) }
    }

    suspend fun saveStudyTimerSessions(sessions: List<StudyTimerSession>) {
        context.dataStore.edit { it[Keys.STUDY_TIMER_SESSIONS_JSON] = encodeStudyTimerSessions(sessions) }
    }

    suspend fun saveNotebooks(notebooks: List<GeminiNotebook>) {
        context.dataStore.edit { it[Keys.NOTEBOOKS_JSON] = encodeNotebooks(notebooks) }
    }

    private fun encodeStudyTimerSessions(sessions: List<StudyTimerSession>): String {
        val arr = JSONArray()
        sessions.take(50).forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("subject", s.subject.name)
                    put("durationMinutes", s.durationMinutes)
                    put("completedAtMs", s.completedAtMs)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeStudyTimerSessions(raw: String?): List<StudyTimerSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                StudyTimerSession(
                    id = obj.optString("id"),
                    subject = FlashcardSubject.fromRaw(obj.optString("subject")),
                    durationMinutes = obj.optInt("durationMinutes", 25),
                    completedAtMs = obj.optLong("completedAtMs", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeAppLimits(rules: List<AppLimitRule>): String {
        val arr = JSONArray()
        rules.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("appName", r.appName)
                    put("packageName", r.packageName)
                    put("dailyLimitMinutes", r.dailyLimitMinutes)
                    put("remainingSeconds", r.remainingSeconds)
                    put("isTrackingActive", r.isTrackingActive)
                    put("isLocked", r.isLocked)
                    put("lastUpdatedEpochMs", r.lastUpdatedEpochMs)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeAppLimits(raw: String?): List<AppLimitRule> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                AppLimitRule(
                    id = obj.optString("id"),
                    appName = obj.optString("appName"),
                    packageName = obj.optString("packageName"),
                    dailyLimitMinutes = obj.optInt("dailyLimitMinutes", 30),
                    remainingSeconds = obj.optInt("remainingSeconds", 1800),
                    isTrackingActive = obj.optBoolean("isTrackingActive", false),
                    isLocked = obj.optBoolean("isLocked", false),
                    lastUpdatedEpochMs = obj.optLong("lastUpdatedEpochMs", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeUnlockRecords(records: List<UnlockCodeRecord>): String {
        val arr = JSONArray()
        records.take(30).forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("id", rec.id)
                    put("code", rec.code)
                    put("targetAppId", rec.targetAppId)
                    put("targetAppName", rec.targetAppName)
                    put("extraMinutes", rec.extraMinutes)
                    put("reason", rec.reason)
                    put("recipientContact", rec.recipientContact)
                    put("createdAtMs", rec.createdAtMs)
                    put("isUsed", rec.isUsed)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeUnlockRecords(raw: String?): List<UnlockCodeRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                UnlockCodeRecord(
                    id = obj.optString("id"),
                    code = obj.optString("code"),
                    targetAppId = obj.optString("targetAppId"),
                    targetAppName = obj.optString("targetAppName"),
                    extraMinutes = obj.optInt("extraMinutes", 15),
                    reason = obj.optString("reason"),
                    recipientContact = obj.optString("recipientContact"),
                    createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                    isUsed = obj.optBoolean("isUsed", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeFlashcards(cards: List<StudyFlashcard>): String {
        val arr = JSONArray()
        cards.take(150).forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("subject", c.subject.name)
                    put("front", c.front)
                    put("back", c.back)
                    put("admissionNote", c.admissionNote)
                    put("isMastered", c.isMastered)
                    put("reviewCount", c.reviewCount)
                    put("isCustom", c.isCustom)
                    put("lastReviewedMs", c.lastReviewedMs)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeFlashcards(raw: String?): List<StudyFlashcard> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                StudyFlashcard(
                    id = obj.optString("id"),
                    subject = FlashcardSubject.fromRaw(obj.optString("subject")),
                    front = obj.optString("front"),
                    back = obj.optString("back"),
                    admissionNote = obj.optString("admissionNote"),
                    isMastered = obj.optBoolean("isMastered", false),
                    reviewCount = obj.optInt("reviewCount", 0),
                    isCustom = obj.optBoolean("isCustom", false),
                    lastReviewedMs = obj.optLong("lastReviewedMs", 0L)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeProgressStats(stats: StudyProgressStats): String {
        return JSONObject().apply {
            put("totalQuestionsAsked", stats.totalQuestionsAsked)
            put("offlineQuestionsAnswered", stats.offlineQuestionsAnswered)
            put("flashcardsReviewed", stats.flashcardsReviewed)
            put("hscTopicsStudied", stats.hscTopicsStudied)
            put("studyStreakDays", stats.studyStreakDays)
            put("lastStudyDayKey", stats.lastStudyDayKey)
            put("unlockRequestsApproved", stats.unlockRequestsApproved)
            put("unlockRequestsRejected", stats.unlockRequestsRejected)
            put(
                "subjectQuestionCounts",
                JSONObject().apply {
                    stats.subjectQuestionCounts.forEach { (k, v) -> put(k, v) }
                }
            )
            put(
                "dailyActivityCounts",
                JSONObject().apply {
                    stats.dailyActivityCounts.entries
                        .sortedByDescending { it.key }
                        .take(14)
                        .forEach { (k, v) -> put(k, v) }
                }
            )
        }.toString()
    }

    private fun decodeProgressStats(raw: String?): StudyProgressStats {
        if (raw.isNullOrBlank()) return StudyProgressStats()
        return try {
            val obj = JSONObject(raw)
            val subjObj = obj.optJSONObject("subjectQuestionCounts")
            val subjMap = buildMap<String, Int> {
                subjObj?.keys()?.forEach { key ->
                    put(key, subjObj.optInt(key, 0))
                }
            }
            val dailyObj = obj.optJSONObject("dailyActivityCounts")
            val dailyMap = buildMap<String, Int> {
                dailyObj?.keys()?.forEach { key ->
                    put(key, dailyObj.optInt(key, 0))
                }
            }
            StudyProgressStats(
                totalQuestionsAsked = obj.optInt("totalQuestionsAsked", 0),
                offlineQuestionsAnswered = obj.optInt("offlineQuestionsAnswered", 0),
                flashcardsReviewed = obj.optInt("flashcardsReviewed", 0),
                hscTopicsStudied = obj.optInt("hscTopicsStudied", 0),
                studyStreakDays = obj.optInt("studyStreakDays", 0),
                lastStudyDayKey = obj.optString("lastStudyDayKey", ""),
                subjectQuestionCounts = subjMap,
                dailyActivityCounts = dailyMap,
                unlockRequestsApproved = obj.optInt("unlockRequestsApproved", 0),
                unlockRequestsRejected = obj.optInt("unlockRequestsRejected", 0)
            )
        } catch (_: Exception) {
            StudyProgressStats()
        }
    }

    private fun encodeNotebooks(notebooks: List<GeminiNotebook>): String {
        val arr = JSONArray()
        notebooks.take(20).forEach { nb ->
            val srcArr = JSONArray()
            nb.sources.forEach { s ->
                srcArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("type", s.type.name)
                        put("sizeBytes", s.sizeBytes)
                        put("localFilePath", s.localFilePath)
                        put("textContent", s.textContent.take(25_000))
                        put("addedAtMs", s.addedAtMs)
                    }
                )
            }
            val turnArr = JSONArray()
            nb.podcastTurns.forEach { t ->
                turnArr.put(
                    JSONObject().apply {
                        put("speaker", t.speaker)
                        put("speakerName", t.speakerName)
                        put("text", t.text)
                    }
                )
            }
            val msgArr = JSONArray()
            nb.messages.takeLast(40).forEach { m ->
                msgArr.put(
                    JSONObject().apply {
                        put("id", m.id)
                        put("role", m.role.name)
                        put("content", m.content)
                        put("createdAtMs", m.createdAtMs)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", nb.id)
                    put("title", nb.title)
                    put("createdAtMs", nb.createdAtMs)
                    put("updatedAtMs", nb.updatedAtMs)
                    put("sources", srcArr)
                    put("podcastTitle", nb.podcastTitle)
                    put("podcastTurns", turnArr)
                    put("studyGuideMarkdown", nb.studyGuideMarkdown)
                    put("faqMarkdown", nb.faqMarkdown)
                    put("quizMarkdown", nb.quizMarkdown)
                    put("messages", msgArr)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeNotebooks(raw: String?): List<GeminiNotebook> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val srcArr = obj.optJSONArray("sources")
                val sources = if (srcArr != null) {
                    (0 until srcArr.length()).mapNotNull { j ->
                        val so = srcArr.optJSONObject(j) ?: return@mapNotNull null
                        NotebookSource(
                            id = so.optString("id"),
                            title = so.optString("title"),
                            type = NotebookSourceType.fromRaw(so.optString("type")),
                            sizeBytes = so.optLong("sizeBytes", 0L),
                            localFilePath = so.optString("localFilePath", ""),
                            textContent = so.optString("textContent", ""),
                            addedAtMs = so.optLong("addedAtMs", System.currentTimeMillis())
                        )
                    }
                } else emptyList()

                val turnArr = obj.optJSONArray("podcastTurns")
                val turns = if (turnArr != null) {
                    (0 until turnArr.length()).mapNotNull { j ->
                        val to = turnArr.optJSONObject(j) ?: return@mapNotNull null
                        PodcastTurn(
                            speaker = to.optString("speaker", "HOST_A"),
                            speakerName = to.optString("speakerName", "Host"),
                            text = to.optString("text", "")
                        )
                    }
                } else emptyList()

                val msgArr = obj.optJSONArray("messages")
                val messages = if (msgArr != null) {
                    (0 until msgArr.length()).mapNotNull { j ->
                        val mo = msgArr.optJSONObject(j) ?: return@mapNotNull null
                        NotebookMessage(
                            id = mo.optString("id"),
                            role = MessageRole.fromRaw(mo.optString("role")),
                            content = mo.optString("content"),
                            createdAtMs = mo.optLong("createdAtMs", System.currentTimeMillis())
                        )
                    }
                } else emptyList()

                GeminiNotebook(
                    id = obj.optString("id"),
                    title = obj.optString("title", "Untitled Notebook"),
                    createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                    updatedAtMs = obj.optLong("updatedAtMs", System.currentTimeMillis()),
                    sources = sources,
                    podcastTitle = obj.optString("podcastTitle", ""),
                    podcastTurns = turns,
                    studyGuideMarkdown = obj.optString("studyGuideMarkdown", ""),
                    faqMarkdown = obj.optString("faqMarkdown", ""),
                    quizMarkdown = obj.optString("quizMarkdown", ""),
                    messages = messages
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
