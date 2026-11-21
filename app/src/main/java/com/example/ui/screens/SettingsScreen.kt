package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicProfile
import com.example.data.model.AppSettings
import com.example.data.model.LanguagePreference
import com.example.data.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    userEmail: String?,
    onBack: () -> Unit,
    onUpdateTheme: (ThemeMode) -> Unit,
    onUpdateLanguage: (LanguagePreference) -> Unit,
    onUpdateAcademicProfile: (AcademicProfile) -> Unit,
    onUpdateTemperature: (Float) -> Unit,
    onUpdateAutoSpeak: (Boolean) -> Unit,
    onUpdateVoiceLang: (String) -> Unit,
    onUpdateSpeechRate: (Float) -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 640.dp)
        ) {
            // Theme Mode
            SettingsSectionCard(title = "Appearance") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }
            }

            // Language & Study Profile
            SettingsSectionCard(title = "Language & Academic Context") {
                Text(
                    text = "AI Response Language",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguagePreference.entries.forEach { pref ->
                        FilterChip(
                            selected = settings.languagePreference == pref,
                            onClick = { onUpdateLanguage(pref) },
                            label = { Text(pref.label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Study Focus Profile",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AcademicProfile.entries.forEach { profile ->
                        FilterChip(
                            selected = settings.academicProfile == profile,
                            onClick = { onUpdateAcademicProfile(profile) },
                            label = { Text(profile.label) }
                        )
                    }
                }
            }

            // Voice & Speech
            SettingsSectionCard(title = "Voice Input & Text-to-Speech") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-speak AI responses",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Read replies aloud automatically after streaming finishes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoSpeak,
                        onCheckedChange = onUpdateAutoSpeak,
                        modifier = Modifier.testTag("auto_speak_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Voice Microphone Language",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.voiceInputLang == "bn-BD",
                        onClick = { onUpdateVoiceLang("bn-BD") },
                        label = { Text("বাংলা (bn-BD)") }
                    )
                    FilterChip(
                        selected = settings.voiceInputLang == "en-US",
                        onClick = { onUpdateVoiceLang("en-US") },
                        label = { Text("English (en-US)") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                val rateDisplay = ((settings.speechRate * 10).roundToInt() / 10f)
                Text(
                    text = "Speech Speed: ${rateDisplay}x",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = onUpdateSpeechRate,
                    valueRange = 0.6f..1.6f
                )
            }

            // Creativity / Temperature
            SettingsSectionCard(title = "AI Creativity (Temperature)") {
                val tempDisplay = ((settings.temperature * 10).roundToInt() / 10f)
                Text(
                    text = "Temperature: $tempDisplay (lower = precise math/facts, higher = creative writing)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = settings.temperature,
                    onValueChange = onUpdateTemperature,
                    valueRange = 0.0f..1.4f
                )
            }

            // Account
            SettingsSectionCard(title = "Google Account") {
                Text(
                    text = userEmail ?: "Signed in with Google",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                    Text("  Sign Out")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
