package com.example.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import com.example.data.model.AppLimitRule
import com.example.data.model.UnlockCodeRecord
import com.example.data.model.UnlockReasonEvaluation
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

private data class InstalledAppItem(val label: String, val packageName: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FocusLimitScreen(
    appLimits: List<AppLimitRule>,
    unlockRecords: List<UnlockCodeRecord>,
    isEvaluatingReason: Boolean,
    onBack: () -> Unit,
    onAddAppLimit: (String, String, Int) -> Unit,
    onToggleTracking: (String) -> Unit,
    onSimulateExpire: (String) -> Unit,
    onRemoveRule: (String) -> Unit,
    onGenerateUnlockCode: (String, Int, String, String, (Boolean, UnlockCodeRecord?, String, UnlockReasonEvaluation?) -> Unit) -> Unit,
    onApplyUnlockCode: (String, String, (Boolean, String) -> Unit) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var customAppName by remember { mutableStateOf("") }
    var customPackageName by remember { mutableStateOf("") }
    var limitMinutes by remember { mutableIntStateOf(30) }
    var showAppPicker by remember { mutableStateOf(false) }

    // Unlock dialog state
    var unlockDialogTarget by remember { mutableStateOf<AppLimitRule?>(null) }
    var enteredUnlockCode by remember { mutableStateOf("") }
    var unlockFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Unlock code generator state
    var selectedTargetAppId by remember(appLimits) {
        mutableStateOf(appLimits.firstOrNull()?.id.orEmpty())
    }
    var extraMinutes by remember { mutableIntStateOf(15) }
    var reasonInput by remember { mutableStateOf("") }
    var recipientContact by remember { mutableStateOf("") }
    var generatedFeedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var lastEvaluation by remember { mutableStateOf<UnlockReasonEvaluation?>(null) }
    var lastGeneratedRecord by remember { mutableStateOf<UnlockCodeRecord?>(null) }
    var copiedCodeBanner by remember { mutableStateOf<String?>(null) }

    val installedApps = remember {
        try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                .map { info ->
                    InstalledAppItem(
                        label = info.loadLabel(pm).toString(),
                        packageName = info.activityInfo.packageName
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label }
        } catch (_: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("App Time Limit & Unlock")
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("focus_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            }
        )

        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("App Time Limits") },
                icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                modifier = Modifier.testTag("focus_tab_limits")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Unlock Code & Reason") },
                icon = { Icon(Icons.Default.Key, contentDescription = null) },
                modifier = Modifier.testTag("focus_tab_unlock")
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .widthIn(max = 680.dp)
        ) {
            if (selectedTab == 0) {
                // Add Any App Time Limit Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Add Any App Time Limit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set a daily focus allowance for any app on your phone (or Study Update AI itself). When time ends, an Unlock Code is required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("YouTube", "Facebook", "Instagram", "TikTok", "Study Update AI").forEach { quickApp ->
                                AssistChip(
                                    onClick = {
                                        customAppName = quickApp
                                        customPackageName = ""
                                    },
                                    label = { Text(quickApp) }
                                )
                            }
                            if (installedApps.isNotEmpty()) {
                                AssistChip(
                                    onClick = { showAppPicker = true },
                                    label = { Text("Pick Installed App") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = customAppName,
                            onValueChange = { customAppName = it },
                            label = { Text("App Name") },
                            placeholder = { Text("e.g. YouTube, Facebook, Study Update AI") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("limit_app_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Daily Time Limit: $limitMinutes min",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = limitMinutes.toFloat(),
                            onValueChange = { limitMinutes = it.roundToInt() },
                            valueRange = 5f..180f
                        )

                        Button(
                            onClick = {
                                onAddAppLimit(customAppName, customPackageName, limitMinutes)
                                customAppName = ""
                                customPackageName = ""
                            },
                            enabled = customAppName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_app_limit_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set App Time Limit")
                        }
                    }
                }

                // Active Rules List
                if (appLimits.isEmpty()) {
                    Text(
                        text = "No app limits added yet. Add any app above to start tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    appLimits.forEach { rule ->
                        val mins = rule.remainingSeconds / 60
                        val secs = rule.remainingSeconds % 60
                        val timeText = "%02d:%02d".format(mins, secs)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (rule.isLocked) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (rule.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (rule.isLocked) MaterialTheme.colorScheme.error else EmeraldSuccess
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rule.appName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (rule.isLocked) {
                                                "LOCKED — Time limit (${rule.dailyLimitMinutes}m) reached"
                                            } else {
                                                "Remaining: $timeText of ${rule.dailyLimitMinutes}m"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = if (rule.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onRemoveRule(rule.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete limit")
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!rule.isLocked) {
                                        OutlinedButton(
                                            onClick = { onToggleTracking(rule.id) },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (rule.isTrackingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (rule.isTrackingActive) "Pause Timer" else "Start Timer")
                                        }
                                        TextButton(onClick = { onSimulateExpire(rule.id) }) {
                                            Text("Lock Now")
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                unlockDialogTarget = rule
                                                enteredUnlockCode = ""
                                                unlockFeedback = null
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Enter Unlock Code")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                selectedTargetAppId = rule.id
                                                selectedTab = 1
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Request / Generate Code")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Unlock Code Generator + Reason + Send User
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Generate Unlock Code (Reason Required)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Select the app, write the reason for extra screen time, generate a unique unlock code, and send it directly to the user via Share, SMS, or Email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Target App", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        if (appLimits.isEmpty()) {
                            Text(
                                text = "First add an app in the 'App Time Limits' tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberWarning
                            )
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                appLimits.forEach { rule ->
                                    FilterChip(
                                        selected = selectedTargetAppId == rule.id,
                                        onClick = { selectedTargetAppId = rule.id },
                                        label = { Text(rule.appName) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Extra Time to Grant", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(10, 15, 30, 60).forEach { mins ->
                                FilterChip(
                                    selected = extraMinutes == mins,
                                    onClick = { extraMinutes = mins },
                                    label = { Text("+$mins min") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = { reasonInput = it },
                            label = { Text("Reason for Unlock Code *") },
                            placeholder = { Text("e.g., Need 15 min for HSC Physics lecture / parent approved") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_reason_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = recipientContact,
                            onValueChange = { recipientContact = it },
                            label = { Text("User Phone or Email (Optional for SMS/Email)") },
                            placeholder = { Text("e.g., +8801... or user@gmail.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                copiedCodeBanner = null
                                onGenerateUnlockCode(
                                    selectedTargetAppId,
                                    extraMinutes,
                                    reasonInput,
                                    recipientContact
                                ) { ok, record, msg, eval ->
                                    generatedFeedback = ok to msg
                                    lastEvaluation = eval
                                    if (ok && record != null) {
                                        lastGeneratedRecord = record
                                        reasonInput = ""
                                    }
                                }
                            },
                            enabled = appLimits.isNotEmpty() && !isEvaluatingReason,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_unlock_code_button")
                        ) {
                            if (isEvaluatingReason) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking if Reason is Useful...")
                            } else {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify Reason & Generate Code")
                            }
                        }

                        if (generatedFeedback != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (generatedFeedback!!.first) {
                                    EmeraldSuccess.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.errorContainer
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    lastEvaluation?.let { ev ->
                                        Text(
                                            text = if (ev.approved) {
                                                "Request Accepted (Score ${ev.score}/100)"
                                            } else {
                                                "Code Not Issued — Reason Not Useful (Score ${ev.score}/100)"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ev.approved) EmeraldSuccess else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                    Text(
                                        text = generatedFeedback!!.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (generatedFeedback!!.first) {
                                            MaterialTheme.colorScheme.onSurface
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Latest Generated Code + Send to User Card
                lastGeneratedRecord?.let { rec ->
                    val shareText = buildUnlockShareMessage(rec)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldSuccess.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Generated Code Ready to Send",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSuccess
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = rec.code,
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "App: ${rec.targetAppName} (+${rec.extraMinutes} min)\nReason: ${rec.reason}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Send Unlock Code to User"))
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send to User")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val smsUri = Uri.parse("smsto:${rec.recipientContact}")
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                            putExtra("sms_body", shareText)
                                        }
                                        runCatching { context.startActivity(smsIntent) }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SMS")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${rec.recipientContact}")
                                            putExtra(Intent.EXTRA_SUBJECT, "Study Update AI Unlock Code: ${rec.targetAppName}")
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        runCatching { context.startActivity(mailIntent) }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Email")
                                }
                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(rec.code))
                                        copiedCodeBanner = "Copied code only: ${rec.code}"
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("copy_unlock_code_only_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Code Only")
                                }
                            }
                            if (copiedCodeBanner != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = copiedCodeBanner!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Reason & Unlock History Log
                if (unlockRecords.isNotEmpty()) {
                    Text(
                        text = "UNLOCK CODE & REASON HISTORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    unlockRecords.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rec.code} • ${rec.targetAppName} (+${rec.extraMinutes}m)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Reason: ${rec.reason}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (!rec.isUsed) {
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(rec.code))
                                            copiedCodeBanner = "Copied code only: ${rec.code}"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy unlock code only",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (rec.isUsed) "USED" else "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (rec.isUsed) MaterialTheme.colorScheme.onSurfaceVariant else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Installed Apps Picker Dialog
    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text("Select App to Limit") },
            text = {
                Column(
                    modifier = Modifier
                        .height(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    installedApps.forEach { item ->
                        TextButton(
                            onClick = {
                                customAppName = item.label
                                customPackageName = item.packageName
                                showAppPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.label,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppPicker = false }) { Text("Close") }
            }
        )
    }

    // Enter Unlock Code Dialog
    unlockDialogTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { unlockDialogTarget = null },
            title = { Text("Unlock ${target.appName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Time limit reached. Enter the unlock code sent to you to add extra minutes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = enteredUnlockCode,
                        onValueChange = { enteredUnlockCode = it },
                        label = { Text("Unlock Code (e.g. SU-123456)") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily)
                    )
                    if (unlockFeedback != null) {
                        Text(
                            text = unlockFeedback!!.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unlockFeedback!!.first) EmeraldSuccess else MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyUnlockCode(target.id, enteredUnlockCode) { ok, msg ->
                            unlockFeedback = ok to msg
                            if (ok) unlockDialogTarget = null
                        }
                    }
                ) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { unlockDialogTarget = null }) { Text("Cancel") }
            }
        )
    }
}

private fun buildUnlockShareMessage(rec: UnlockCodeRecord): String {
    return buildString {
        appendLine("Study Update AI — App Unlock Code")
        appendLine("App: ${rec.targetAppName}")
        appendLine("Unlock Code: ${rec.code}")
        appendLine("Extra Time: +${rec.extraMinutes} minutes")
        append("Reason: ${rec.reason}")
    }
}
