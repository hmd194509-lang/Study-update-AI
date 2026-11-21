package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AcademicProfile
import com.example.data.model.AppSettings
import com.example.data.model.DEFAULT_MODEL_ID
import com.example.data.model.LanguagePreference
import com.example.data.model.ThemeMode
import com.example.security.KeystoreCrypto
import com.example.security.SecretSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
        val MODEL_ID = stringPreferencesKey("selected_model_id")
        val USE_CUSTOM_KEY = booleanPreferencesKey("use_custom_api_key")
        val ENCRYPTED_API_KEY = stringPreferencesKey("encrypted_custom_api_key")
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
            selectedModelId = prefs[Keys.MODEL_ID] ?: DEFAULT_MODEL_ID,
            useCustomApiKey = prefs[Keys.USE_CUSTOM_KEY] ?: false,
            maskedCustomApiKey = decrypted?.let { SecretSanitizer.maskApiKey(it) }
        )
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

    suspend fun updateModelId(modelId: String) {
        context.dataStore.edit { it[Keys.MODEL_ID] = modelId }
    }

    suspend fun updateUseCustomApiKey(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_CUSTOM_KEY] = enabled }
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
}
