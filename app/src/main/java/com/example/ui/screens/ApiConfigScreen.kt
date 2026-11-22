package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.ConnectionTestState
import com.example.data.model.GEMINI_API_KEY_PORTAL_URL
import com.example.data.model.SUPPORTED_GEMINI_MODELS
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    settings: AppSettings,
    connectionTestState: ConnectionTestState,
    onBack: () -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleUseCustomKey: (Boolean) -> Unit,
    onSaveCustomKey: (String, (Boolean, String) -> Unit) -> Unit,
    onClearCustomKey: () -> Unit,
    onTestConnection: (String?) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var apiKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var saveFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var linkCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = { Text("API Configuration") },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("api_config_back_button")
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
            // Direct Gemini API Key Link Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Get Gemini API Key Link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = GEMINI_API_KEY_PORTAL_URL,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GEMINI_API_KEY_PORTAL_URL))
                                runCatching { context.startActivity(intent) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("open_gemini_api_link_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Gemini API Key")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(GEMINI_API_KEY_PORTAL_URL))
                                linkCopied = true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (linkCopied) "Copied!" else "Copy Link")
                        }
                    }
                }
            }

            // Connection Status Card
            val isBuiltInActive = !settings.useCustomApiKey
            val isOwnerDefault = settings.useCustomApiKey && settings.usingDefaultOwnerKey
            val isCustomReady = settings.useCustomApiKey && !settings.maskedCustomApiKey.isNullOrBlank()
            val statusHealthy = isBuiltInActive || isCustomReady

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (statusHealthy) {
                        EmeraldSuccess.copy(alpha = 0.12f)
                    } else {
                        AmberWarning.copy(alpha = 0.14f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (statusHealthy) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (statusHealthy) EmeraldSuccess else AmberWarning
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isBuiltInActive -> "Connected via Firebase AI Logic"
                                    isOwnerDefault -> "Default Study AI Key Active"
                                    isCustomReady -> "Personal API Key Active"
                                    else -> "Custom API Key Required"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = when {
                                    isBuiltInActive -> "Zero-key App Check attestation enabled • Model: ${settings.selectedModelId}"
                                    isOwnerDefault -> "Pre-configured Owner Key (${settings.maskedCustomApiKey}) • Anyone can add their own key below • Model: ${settings.selectedModelId}"
                                    isCustomReady -> "Encrypted Personal Key: ${settings.maskedCustomApiKey} • Model: ${settings.selectedModelId}"
                                    else -> "Enter your API key below or switch back to built-in Firebase AI Logic."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { onTestConnection(apiKeyInput.takeIf { it.isNotBlank() }) },
                        enabled = connectionTestState !is ConnectionTestState.Testing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("test_connection_button")
                    ) {
                        if (connectionTestState is ConnectionTestState.Testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Connection")
                        }
                    }

                    when (connectionTestState) {
                        is ConnectionTestState.Connected -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Connected to ${connectionTestState.model} (${connectionTestState.engineLabel}) in ${connectionTestState.latencyMs} ms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is ConnectionTestState.Failed -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = connectionTestState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }
                }
            }

            // Engine Mode & Custom Owner Key Entry
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use Own Gemini API Key",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Optional: supply your personal Google AI Studio key (also enables Veo Video Generation).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.useCustomApiKey,
                            onCheckedChange = onToggleUseCustomKey,
                            modifier = Modifier.testTag("use_custom_key_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            saveFeedback = null
                        },
                        label = { Text("Personal API Key (Optional Override)") },
                        placeholder = {
                            Text(
                                when {
                                    settings.usingDefaultOwnerKey -> "Owner Default Active (${settings.maskedCustomApiKey}) — paste yours to override"
                                    settings.maskedCustomApiKey != null -> "Personal Key: ${settings.maskedCustomApiKey} (enter new to replace)"
                                    else -> "Paste AQ... or AIza... key here"
                                }
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showKey) "Hide key" else "Show key"
                                )
                            }
                        },
                        visualTransformation = if (showKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input")
                    )

                    if (saveFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = saveFeedback!!.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (saveFeedback!!.first) EmeraldSuccess else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                onSaveCustomKey(apiKeyInput) { ok, msg ->
                                    saveFeedback = ok to msg
                                    if (ok) apiKeyInput = ""
                                }
                            },
                            enabled = apiKeyInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_api_key_button")
                        ) {
                            Text("Save Encrypted Key")
                        }

                        if (!settings.usingDefaultOwnerKey && settings.maskedCustomApiKey != null) {
                            OutlinedButton(
                                onClick = {
                                    onClearCustomKey()
                                    saveFeedback = null
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("remove_api_key_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset to Default")
                            }
                        }
                    }
                }
            }

            // Gemini Model Selector
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Study AI Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SUPPORTED_GEMINI_MODELS.forEach { modelOption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.selectedModelId == modelOption.id,
                                onClick = { onSelectModel(modelOption.id) },
                                modifier = Modifier.testTag("model_radio_${modelOption.id}")
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = modelOption.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = modelOption.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Security Guarantee Notice
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Credential Protection: Any custom Gemini API key is encrypted with AES-256-GCM inside Android Keystore before storage, never hardcoded, never uploaded to Firestore, and automatically redacted from chat messages and error logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
