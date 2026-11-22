package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoCameraBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.AttachmentPayload
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.MessageRole
import com.example.data.model.StudyMode
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.displayName
import com.example.data.model.idValue
import com.example.ui.components.LiquidGlassIconBadge
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MarkdownMessageView
import com.example.ui.components.StudyAiLiquidLogo
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import com.example.util.AttachmentReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

private data class QuickStarter(
    val title: String,
    val subtitle: String,
    val mode: StudyMode,
    val prompt: String
)

private val QUICK_STARTERS = listOf(
    QuickStarter(
        title = "HSC Physics • BUET",
        subtitle = "ভেক্টর, প্রাস ও আধুনিক পদার্থবিজ্ঞান CQ/MCQ",
        mode = StudyMode.HSC_PHYSICS,
        prompt = "HSC পদার্থবিজ্ঞান ১ম পত্রের ভেক্টর ও প্রাসের গতি অধ্যায় থেকে একটি বোর্ড-মানের সৃজনশীল (CQ) প্রশ্ন তৈরি করে ক, খ, গ ও ঘ ধাপে ধাপে সমাধান এবং BUET/DU MCQ শর্টকাট বুঝিয়ে দাও।"
    ),
    QuickStarter(
        title = "HSC Chemistry • জৈব",
        subtitle = "SN1/SN2, বেনজিন, pH ও তড়িৎ রসায়ন",
        mode = StudyMode.HSC_CHEMISTRY,
        prompt = "HSC রসায়ন জৈব যৌগ (SN1 বনাম SN2, অ্যালডল ঘনীভবন, লুকাস পরীক্ষা) ও রাসায়নিক সাম্যাবস্থা (Kp/Kc/pH) অ্যাডমিশন লেভেলের উদাহরণসহ বুঝিয়ে দাও।"
    ),
    QuickStarter(
        title = "HSC Biology • Medical",
        subtitle = "হৃদপিণ্ড, নেফ্রন, DNA ও কোষ বিভাজন",
        mode = StudyMode.HSC_BIOLOGY,
        prompt = "মেডিকেল অ্যাডমিশন ও HSC পরীক্ষার জন্য মানব হৃদপিণ্ডের কার্ডিয়াক চক্র, নেফ্রন এবং DNA অনুলিপনের গুরুত্বপূর্ণ তথ্য ও পার্থক্যগুলো বাংলায় গুছিয়ে দাও।"
    ),
    QuickStarter(
        title = "HSC Math Solver",
        subtitle = "Calculus, vectors & step-by-step derivations",
        mode = StudyMode.HSC_MATH,
        prompt = "Explain the geometric meaning of definite integration and solve ∫₀^(π/2) sin²(x) dx step by step."
    ),
    QuickStarter(
        title = "Admission Quiz",
        subtitle = "মেডিকেল, বুয়েট ও ঢাবি 'ক' মক MCQ",
        mode = StudyMode.QUIZ,
        prompt = "HSC Physics, Chemistry ও Biology মিলিয়ে মেডিকেল/ঢাবি 'ক' মানের ৫টি ট্রিকি MCQ দাও এবং শেষে ব্যাখ্যাসহ উত্তরপত্র দাও।"
    ),
    QuickStarter(
        title = "Study Routine",
        subtitle = "দৈনিক পড়ার রুটিন ও টাইম লিমিট গাইড",
        mode = StudyMode.ROUTINE,
        prompt = "আমার আগামী ৩ মাস পর HSC ও এডমিশন পরীক্ষা। প্রতিদিন ৬ ঘণ্টা পড়াশোনার জন্য পদার্থ, রসায়ন, গণিত ও জীববিজ্ঞান মিলিয়ে একটি বাস্তবসম্মত দৈনিক রুটিন তৈরি করে দাও।"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    activeSession: ChatSession?,
    isTemporaryChat: Boolean,
    isOfflineEffective: Boolean,
    isDeviceOnline: Boolean,
    messagesState: UiState<List<ChatMessage>>,
    activeMode: StudyMode,
    settings: AppSettings,
    userDisplayName: String?,
    isStreaming: Boolean,
    streamingText: String?,
    pendingAttachments: List<AttachmentPayload>,
    bannerMessage: String?,
    autoSpeakEvents: SharedFlow<String>,
    onOpenDrawer: () -> Unit,
    onOpenApiConfig: () -> Unit,
    onOpenNotebook: () -> Unit,
    onOpenStudio: (Int) -> Unit,
    onOpenHscLab: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenStudyTimer: () -> Unit,
    onOpenAdmissionDetails: () -> Unit,
    onOpenFocusLock: () -> Unit,
    onOpenLiveVoice: () -> Unit,
    onToggleTemporaryChat: () -> Unit,
    onToggleOfflineMode: () -> Unit,
    onToggleTheme: () -> Unit,
    onSelectMode: (StudyMode) -> Unit,
    onSendMessage: (String, StudyMode?) -> Unit,
    onSendImageInChat: (String) -> Unit,
    onStopStreaming: () -> Unit,
    onRegenerateLast: () -> Unit,
    onAddAttachment: (AttachmentPayload) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onShowBanner: (String) -> Unit,
    onDismissBanner: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var inputText by remember { mutableStateOf("") }
    var isImageGenMode by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var partialVoiceText by remember { mutableStateOf("") }
    var showPlusMenu by remember { mutableStateOf(false) }
    var showThreeDotMenu by remember { mutableStateOf(false) }
    var speakingMessageId by remember { mutableStateOf<String?>(null) }
    var copiedMessageId by remember { mutableStateOf<String?>(null) }
    var copiedCodeOnlyId by remember { mutableStateOf<String?>(null) }
    var fullscreenImage by remember { mutableStateOf<Pair<Bitmap, String>?>(null) }

    val listState = rememberLazyListState()

    // Text-to-Speech engine setup
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                speakingMessageId = null
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                speakingMessageId = null
            }
        })
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    fun speakText(messageId: String, rawText: String) {
        if (!ttsReady) {
            onShowBanner("Text-to-Speech engine is not ready yet.")
            return
        }
        if (speakingMessageId == messageId) {
            tts.stop()
            speakingMessageId = null
            return
        }
        val clean = rawText
            .replace(Regex("```[\\s\\S]*?```"), " Code block. ")
            .replace(Regex("[#*_`>]"), " ")
            .trim()
        if (clean.isEmpty()) return

        val hasBengali = clean.any { it in '\u0980'..'\u09FF' }
        val locale = if (hasBengali) Locale.forLanguageTag("bn-BD") else Locale.US
        tts.language = locale
        tts.setSpeechRate(settings.speechRate)
        tts.setPitch(settings.ttsPitch)
        speakingMessageId = messageId
        tts.speak(clean.take(3500), TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    LaunchedEffect(autoSpeakEvents) {
        autoSpeakEvents.collect { text ->
            speakText("auto_${System.currentTimeMillis()}", text)
        }
    }

    // SpeechRecognizer (Voice-to-Text with live partial results) + RecognizerIntent fallback
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    DisposableEffect(speechRecognizer) {
        onDispose { speechRecognizer?.destroy() }
    }

    val recognizerIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        partialVoiceText = ""
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
            }
        }
    }

    fun startVoiceRecognition() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            partialVoiceText = ""
            return
        }
        val lang = settings.voiceInputLang.ifBlank { "bn-BD" }
        if (speechRecognizer != null) {
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    partialVoiceText = ""
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }
                override fun onError(error: Int) {
                    isListening = false
                    partialVoiceText = ""
                    if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH) {
                        onShowBanner("Voice-to-text unavailable on this device (code $error).")
                    }
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    partialVoiceText = ""
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) {
                        inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        partialVoiceText = partial
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
        } else {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                }
                isListening = true
                recognizerIntentLauncher.launch(intent)
            } catch (_: Exception) {
                isListening = false
                onShowBanner("Speech recognition service is not installed on this device.")
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecognition()
        } else {
            onShowBanner("Microphone permission is needed for Voice-to-Text.")
        }
    }

    // Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                AttachmentReader.readImage(context, uri)
                    .onSuccess { onAddAttachment(it) }
                    .onFailure { onShowBanner(it.localizedMessage ?: "Failed to load image.") }
            }
        }
    }

    // Zero-permission SAF Document Picker (PDF & Text/Code)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                AttachmentReader.readDocument(context, uri)
                    .onSuccess { onAddAttachment(it) }
                    .onFailure { onShowBanner(it.localizedMessage ?: "Failed to read document.") }
            }
        }
    }

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size, isStreaming, streamingText?.length) {
        val targetIndex = (messages.size + if (isStreaming) 1 else 0) - 1
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    val customKeyMissing = settings.useCustomApiKey && settings.maskedCustomApiKey.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Study AI Top Bar with Liquid Glass Logo, Polished Action Badges & Liquid Glass 3-Dot Menu
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_drawer_button")
                ) {
                    LiquidGlassIconBadge(
                        icon = Icons.Default.Menu,
                        contentDescription = "Open sidebar",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Centered Study AI Liquid Glass Pill
                LiquidGlassSurface(
                    shape = RoundedCornerShape(999.dp),
                    accentGlow = true,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { onOpenApiConfig() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        StudyAiLiquidLogo(size = 26.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTemporaryChat) {
                                "Temporary • Study AI"
                            } else {
                                activeSession?.title?.take(22) ?: "Study AI • ${activeMode.label}"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Quick In-Chat Image Skill Toggle
                IconButton(
                    onClick = { isImageGenMode = !isImageGenMode },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("in_chat_image_toggle")
                ) {
                    LiquidGlassIconBadge(
                        icon = Icons.Default.Palette,
                        contentDescription = "Generate picture in chat",
                        tint = if (isImageGenMode) CyanTertiary else MaterialTheme.colorScheme.primary,
                        active = isImageGenMode
                    )
                }

                // Quick Live Voice Logo Button
                IconButton(
                    onClick = onOpenLiveVoice,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("topbar_live_voice_button")
                ) {
                    LiquidGlassIconBadge(
                        icon = Icons.Default.GraphicEq,
                        contentDescription = "Live Voice Chat",
                        tint = VioletSecondary,
                        active = true
                    )
                }

                if (customKeyMissing) {
                    IconButton(
                        onClick = onOpenApiConfig,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        LiquidGlassIconBadge(
                            icon = Icons.Default.Warning,
                            contentDescription = "API Key Needed",
                            tint = AmberWarning,
                            active = true
                        )
                    }
                }

                // Liquid Glass 3-Dot Overflow Menu
                Box {
                    IconButton(
                        onClick = { showThreeDotMenu = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("three_dot_menu_button")
                    ) {
                        LiquidGlassIconBadge(
                            icon = Icons.Default.MoreVert,
                            contentDescription = "Study AI Tools Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                            active = showThreeDotMenu
                        )
                    }

                    DropdownMenu(
                        expanded = showThreeDotMenu,
                        onDismissRequest = { showThreeDotMenu = false },
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(24.dp),
                            accentGlow = true,
                            tintAlpha = if (isDark) 0.86f else 0.94f,
                            modifier = Modifier.widthIn(min = 248.dp, max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    StudyAiLiquidLogo(size = 24.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Study AI Tools",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isTemporaryChat) "Exit Temporary Chat" else "Temporary Chat") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(
                                            icon = if (isTemporaryChat) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (isTemporaryChat) AmberWarning else IndigoPrimary,
                                            size = 30.dp
                                        )
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onToggleTemporaryChat()
                                    },
                                    modifier = Modifier.testTag("temporary_chat_toggle_button")
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isOfflineEffective) "Switch to Online Mode" else "Offline Study Mode") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(
                                            icon = if (isOfflineEffective) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (isOfflineEffective) AmberWarning else EmeraldSuccess,
                                            size = 30.dp
                                        )
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onToggleOfflineMode()
                                    },
                                    modifier = Modifier.testTag("offline_mode_topbar_button")
                                )
                                DropdownMenuItem(
                                    text = { Text("Progress Dashboard") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.Analytics, null, IndigoPrimary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenProgress()
                                    },
                                    modifier = Modifier.testTag("progress_topbar_button")
                                )
                                DropdownMenuItem(
                                    text = { Text("App Time Limit & Unlock") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.Timer, null, AmberWarning, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenFocusLock()
                                    },
                                    modifier = Modifier.testTag("focus_lock_topbar_button")
                                )
                                DropdownMenuItem(
                                    text = { Text("Study Timer (Pomodoro)") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.HourglassTop, null, IndigoPrimary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenStudyTimer()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("All BD Admission Circulars") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.EventAvailable, null, CyanTertiary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenAdmissionDetails()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Unlimited Flashcards") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.Style, null, EmeraldSuccess, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenFlashcards()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("HSC Physics / Chem / Bio Lab") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.School, null, CyanTertiary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenHscLab()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Picture & Video Studio") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.VideoCameraBack, null, VioletSecondary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenStudio(0)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Study AI Models & API Key") },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(Icons.Default.Key, null, IndigoPrimary, size = 30.dp)
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onOpenApiConfig()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (settings.themeMode == ThemeMode.LIGHT) "Dark Mode" else "Light Mode"
                                        )
                                    },
                                    leadingIcon = {
                                        LiquidGlassIconBadge(
                                            icon = if (settings.themeMode == ThemeMode.LIGHT) Icons.Default.DarkMode else Icons.Default.LightMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            size = 30.dp
                                        )
                                    },
                                    onClick = {
                                        showThreeDotMenu = false
                                        onToggleTheme()
                                    },
                                    modifier = Modifier.testTag("theme_toggle_button")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Offline Mode Status Strip
        if (isOfflineEffective) {
            Surface(
                color = IndigoPrimary.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!isDeviceOnline) {
                            "No Internet — Offline Mode active (Local HSC Knowledge, Flashcards & Progress)."
                        } else {
                            "Offline Mode active — answers come from your local HSC & Flashcard decks."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (settings.offlineModeEnabled && isDeviceOnline) {
                        TextButton(onClick = onToggleOfflineMode) {
                            Text("Go Online", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Temporary Chat Status Strip
        if (isTemporaryChat) {
            Surface(
                color = AmberWarning.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Temporary Chat active — messages stay in memory only and won't save to history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Study Mode Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(StudyMode.entries) { mode ->
                val selected = mode == activeMode
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("mode_chip_${mode.name}")
                )
            }
        }

        // Optional Banner Alert
        if (!bannerMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = bannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismissBanner) {
                        Text("OK")
                    }
                }
            }
        }

        // Main Chat Content or Gemini Empty Greeting
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty() && !isStreaming) {
                GeminiWelcomeContent(
                    userDisplayName = userDisplayName,
                    isTemporaryChat = isTemporaryChat,
                    isOfflineEffective = isOfflineEffective,
                    onOpenNotebook = onOpenNotebook,
                    onOpenPictureStudio = { onOpenStudio(0) },
                    onOpenVideoStudio = { onOpenStudio(1) },
                    onOpenLiveVoice = onOpenLiveVoice,
                    onOpenHscLab = onOpenHscLab,
                    onOpenFlashcards = onOpenFlashcards,
                    onOpenProgress = onOpenProgress,
                    onOpenStudyTimer = onOpenStudyTimer,
                    onOpenAdmissionDetails = onOpenAdmissionDetails,
                    onOpenFocusLock = onOpenFocusLock,
                    onStarterClick = { starter ->
                        onSendMessage(starter.prompt, starter.mode)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 1040.dp)
                        .align(Alignment.TopCenter)
                ) {
                    itemsIndexed(messages, key = { _, item -> item.id }) { index, msg ->
                        val isLastModel =
                            MessageRole.fromRaw(msg.role) == MessageRole.MODEL &&
                                index == messages.lastIndex &&
                                !isStreaming

                        val extractedCode = remember(msg.content) {
                            extractOnlyCodeBlocks(msg.content)
                        }

                        GeminiBubbleRow(
                            message = msg,
                            hasCodeBlocks = !extractedCode.isNullOrBlank(),
                            isSpeaking = speakingMessageId == msg.id,
                            isCopied = copiedMessageId == msg.id,
                            isCodeCopied = copiedCodeOnlyId == msg.id,
                            showRegenerate = isLastModel,
                            onSpeak = { speakText(msg.id, msg.content) },
                            onCopyCodeOnly = {
                                val pureCode = extractedCode ?: return@GeminiBubbleRow
                                clipboard.setText(AnnotatedString(pureCode))
                                copiedCodeOnlyId = msg.id
                                scope.launch {
                                    delay(1600)
                                    if (copiedCodeOnlyId == msg.id) copiedCodeOnlyId = null
                                }
                            },
                            onCopy = {
                                clipboard.setText(AnnotatedString(msg.content))
                                copiedMessageId = msg.id
                                scope.launch {
                                    delay(1600)
                                    if (copiedMessageId == msg.id) copiedMessageId = null
                                }
                            },
                            onShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Study AI:\n\n${msg.content}")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Study AI Response"))
                            },
                            onRegenerate = onRegenerateLast,
                            onExpandImage = { bmp ->
                                fullscreenImage = bmp to msg.content
                            }
                        )
                    }

                    if (isStreaming) {
                        item(key = "streaming_bubble") {
                            StreamingBubbleRow(text = streamingText.orEmpty())
                        }
                    }
                }
            }
        }

        // Live Voice-to-Text Partial Preview Chip
        if (isListening) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = partialVoiceText.ifBlank { "Listening (${settings.voiceInputLang})... Speak now" },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Gemini-Style Floating Pill Composer Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            if (isImageGenMode) {
                LiquidGlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    accentGlow = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = CyanTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "In-Chat Picture Mode — describe any photo or HSC diagram",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { isImageGenMode = false },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Image Mode", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (pendingAttachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(pendingAttachments, key = { it.idValue }) { att ->
                        AttachmentChip(
                            attachment = att,
                            onRemove = { onRemoveAttachment(att.idValue) }
                        )
                    }
                }
            }

            fun dispatchComposerSubmit() {
                val prompt = inputText
                if (prompt.isBlank() && pendingAttachments.isEmpty()) return
                val trimmed = prompt.trim()
                inputText = ""
                if (isImageGenMode) {
                    isImageGenMode = false
                    onSendImageInChat(trimmed)
                } else if (trimmed.startsWith("/image ", ignoreCase = true) || trimmed.startsWith("/pic ", ignoreCase = true)) {
                    val clean = trimmed.substringAfter(" ").trim()
                    onSendImageInChat(clean)
                } else {
                    onSendMessage(prompt, null)
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isImageGenMode) CyanTertiary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    // Plus (+) Tools Menu: Photo, PDF (up to 1 GB), Create Picture in Chat, Video, Podcast & Tools
                    Box {
                        IconButton(
                            onClick = { showPlusMenu = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("composer_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Photo, PDF & Study AI Tools",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Create Picture in Chat") },
                                leadingIcon = { Icon(Icons.Default.Palette, null, tint = CyanTertiary) },
                                onClick = {
                                    showPlusMenu = false
                                    isImageGenMode = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attach Photo / Diagram") },
                                leadingIcon = { Icon(Icons.Default.Image, null, tint = IndigoPrimary) },
                                onClick = {
                                    showPlusMenu = false
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Upload PDF / File (up to 1 GB)") },
                                leadingIcon = { Icon(Icons.Default.AttachFile, null, tint = VioletSecondary) },
                                onClick = {
                                    showPlusMenu = false
                                    docPickerLauncher.launch(arrayOf("application/pdf", "text/*", "application/json"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Study AI Notebook & 1GB PDF Podcast") },
                                leadingIcon = { Icon(Icons.Default.AutoStories, null, tint = IndigoPrimary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenNotebook()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Picture Studio (Full Editor)") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = IndigoPrimary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenStudio(0)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Video Studio (Veo)") },
                                leadingIcon = { Icon(Icons.Default.VideoCameraBack, null, tint = VioletSecondary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenStudio(1)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("HSC Physics / Chem / Bio Lab") },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = CyanTertiary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenHscLab()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Unlimited Study Flashcards") },
                                leadingIcon = { Icon(Icons.Default.Style, null, tint = EmeraldSuccess) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenFlashcards()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress Dashboard") },
                                leadingIcon = { Icon(Icons.Default.Analytics, null, tint = AmberWarning) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenProgress()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Study Timer (Pomodoro)") },
                                leadingIcon = { Icon(Icons.Default.HourglassTop, null, tint = IndigoPrimary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenStudyTimer()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("BD Admission Circulars & Links") },
                                leadingIcon = { Icon(Icons.Default.EventAvailable, null, tint = CyanTertiary) },
                                onClick = {
                                    showPlusMenu = false
                                    onOpenAdmissionDetails()
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = when {
                                    isImageGenMode -> "Describe picture to generate in chat..."
                                    isTemporaryChat -> "Ask Study AI (Temporary Chat)..."
                                    else -> "Ask Study AI..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (!isStreaming) {
                                    dispatchComposerSubmit()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    // Voice-to-Text Mic Button
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                startVoiceRecognition()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop voice-to-text" else "Voice-to-text",
                            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Voice Chat Button (when idle and text empty)
                    if (!isStreaming && inputText.isBlank() && pendingAttachments.isEmpty()) {
                        IconButton(
                            onClick = onOpenLiveVoice,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary, CyanTertiary))
                                )
                                .testTag("live_voice_launch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Live Voice Chat",
                                tint = Color.White
                            )
                        }
                    } else if (isStreaming) {
                        IconButton(
                            onClick = onStopStreaming,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .testTag("stop_streaming_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop generating",
                                tint = Color.White
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { dispatchComposerSubmit() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(IndigoPrimary, CyanTertiary)))
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = if (isImageGenMode) Icons.Default.AutoAwesome else Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Viewer Dialog for In-Chat Generated Pictures
    fullscreenImage?.let { (bmp, caption) ->
        Dialog(
            onDismissRequest = { fullscreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = caption,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Button(
                        onClick = { shareChatBitmap(context, bmp) },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / Save")
                    }

                    IconButton(
                        onClick = { fullscreenImage = null },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close fullscreen", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GeminiWelcomeContent(
    userDisplayName: String?,
    isTemporaryChat: Boolean,
    isOfflineEffective: Boolean,
    onOpenNotebook: () -> Unit,
    onOpenPictureStudio: () -> Unit,
    onOpenVideoStudio: () -> Unit,
    onOpenLiveVoice: () -> Unit,
    onOpenHscLab: () -> Unit,
    onOpenFlashcards: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenStudyTimer: () -> Unit,
    onOpenAdmissionDetails: () -> Unit,
    onOpenFocusLock: () -> Unit,
    onStarterClick: (QuickStarter) -> Unit
) {
    val firstName = userDisplayName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() } ?: "Learner"

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1040.dp)
    ) {
        // Study AI Gradient Greeting Header
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                Text(
                    text = when {
                        isOfflineEffective -> "Offline Study Mode"
                        isTemporaryChat -> "Temporary Chat"
                        else -> "Hello, $firstName"
                    },
                    style = MaterialTheme.typography.displaySmall.merge(
                        TextStyle(
                            brush = Brush.linearGradient(
                                listOf(IndigoPrimary, VioletSecondary, CyanTertiary)
                            )
                        )
                    ),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isOfflineEffective -> "Search local HSC Physics, Chemistry, Biology notes & Flashcards without internet."
                        isTemporaryChat -> "Ask Study AI privately — nothing is saved to history."
                        else -> "How can Study AI help you learn, code, or create today?"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Gemini-Style Skill Pills
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenNotebook,
                    label = { Text("PDF Podcast & Notebook") },
                    leadingIcon = { Icon(Icons.Default.Headphones, null, Modifier.size(17.dp), tint = VioletSecondary) }
                )
                AssistChip(
                    onClick = onOpenFlashcards,
                    label = { Text("Study Flashcards") },
                    leadingIcon = { Icon(Icons.Default.Style, null, Modifier.size(17.dp), tint = EmeraldSuccess) }
                )
                AssistChip(
                    onClick = onOpenProgress,
                    label = { Text("Progress Dashboard") },
                    leadingIcon = { Icon(Icons.Default.Analytics, null, Modifier.size(17.dp), tint = IndigoPrimary) }
                )
                AssistChip(
                    onClick = onOpenHscLab,
                    label = { Text("HSC & Admission Lab") },
                    leadingIcon = { Icon(Icons.Default.School, null, Modifier.size(17.dp), tint = CyanTertiary) }
                )
                AssistChip(
                    onClick = onOpenPictureStudio,
                    label = { Text("Generate Picture") },
                    leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(17.dp), tint = IndigoPrimary) }
                )
                AssistChip(
                    onClick = onOpenVideoStudio,
                    label = { Text("Generate Video") },
                    leadingIcon = { Icon(Icons.Default.VideoCameraBack, null, Modifier.size(17.dp), tint = VioletSecondary) }
                )
                AssistChip(
                    onClick = onOpenLiveVoice,
                    label = { Text("Live Voice Chat") },
                    leadingIcon = { Icon(Icons.Default.GraphicEq, null, Modifier.size(17.dp), tint = CyanTertiary) }
                )
                AssistChip(
                    onClick = onOpenStudyTimer,
                    label = { Text("Study Timer") },
                    leadingIcon = { Icon(Icons.Default.HourglassTop, null, Modifier.size(17.dp), tint = IndigoPrimary) }
                )
                AssistChip(
                    onClick = onOpenAdmissionDetails,
                    label = { Text("Admission Circulars") },
                    leadingIcon = { Icon(Icons.Default.EventAvailable, null, Modifier.size(17.dp), tint = CyanTertiary) }
                )
                AssistChip(
                    onClick = onOpenFocusLock,
                    label = { Text("App Time Limit") },
                    leadingIcon = { Icon(Icons.Default.Timer, null, Modifier.size(17.dp), tint = AmberWarning) }
                )
            }
        }

        item {
            Text(
                text = "HSC & ADMISSION QUICK STARTERS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        items(QUICK_STARTERS.chunked(2)) { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { starter ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onStarterClick(starter) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = starter.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = starter.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GeminiBubbleRow(
    message: ChatMessage,
    hasCodeBlocks: Boolean,
    isSpeaking: Boolean,
    isCopied: Boolean,
    isCodeCopied: Boolean,
    showRegenerate: Boolean,
    onSpeak: () -> Unit,
    onCopyCodeOnly: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onExpandImage: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val isUser = MessageRole.fromRaw(message.role) == MessageRole.USER

    if (isUser) {
        val visibleAttachments = remember(message.attachmentNames) {
            message.attachmentNames.filterNot { it.startsWith("IMG_GEN:") }
        }
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .clip(RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp))
                    .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary)))
                    .padding(horizontal = 15.dp, vertical = 11.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (visibleAttachments.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            visibleAttachments.forEach { fileName ->
                                Surface(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = fileName,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 140.dp)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }
    } else {
        val imgFileName = remember(message.attachmentNames) {
            message.attachmentNames.firstOrNull { it.startsWith("IMG_GEN:") }?.removePrefix("IMG_GEN:")
        }
        val generatedBitmap by produceState<Bitmap?>(initialValue = null, imgFileName, message.generatedImagePath) {
            value = withContext(Dispatchers.IO) {
                try {
                    val path = when {
                        message.generatedImagePath.isNotBlank() -> message.generatedImagePath
                        !imgFileName.isNullOrBlank() -> File(context.filesDir, "chat_images/$imgFileName").absolutePath
                        else -> ""
                    }
                    if (path.isNotBlank() && File(path).exists()) {
                        BitmapFactory.decodeFile(path)
                    } else null
                } catch (_: Exception) {
                    null
                }
            }
        }

        // Full-width Study AI response bubble
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            ) {
                if (message.errorFlag) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    StudyAiLiquidLogo(size = 24.dp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (message.errorFlag) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (message.errorFlag) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val contentColor = if (message.errorFlag) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    generatedBitmap?.let { bmp ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onExpandImage(bmp) }
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = message.content,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 240.dp, max = 460.dp)
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "View Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Full View",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    MarkdownMessageView(
                        content = message.content,
                        textColor = contentColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop speaking" else "Listen to response",
                                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (hasCodeBlocks) {
                            AssistChip(
                                onClick = onCopyCodeOnly,
                                label = {
                                    Text(
                                        text = if (isCodeCopied) "Code Copied" else "Copy Code",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.Code,
                                        contentDescription = "Copy code only",
                                        tint = if (isCodeCopied) EmeraldSuccess else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy response",
                                tint = if (isCopied) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val bmp = generatedBitmap
                                if (bmp != null) shareChatBitmap(context, bmp) else onShare()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share response",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (showRegenerate) {
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate response",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun shareChatBitmap(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, "study_ai_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Study AI Picture"))
}

private fun extractOnlyCodeBlocks(markdown: String): String? {
    val regex = Regex("```[^\\n]*\\n([\\s\\S]*?)```")
    val blocks = regex.findAll(markdown)
        .map { it.groupValues[1].trimEnd() }
        .filter { it.isNotBlank() }
        .toList()
    return if (blocks.isEmpty()) null else blocks.joinToString("\n\n")
}

@Composable
private fun StreamingBubbleRow(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        ) {
            StudyAiLiquidLogo(size = 24.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Study AI",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                if (text.isBlank()) {
                    TypingIndicatorDots()
                } else {
                    MarkdownMessageView(
                        content = text,
                        textColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    val dot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "d1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), RepeatMode.Reverse),
        label = "d2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), RepeatMode.Reverse),
        label = "d3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        listOf(dot1 to IndigoPrimary, dot2 to VioletSecondary, dot3 to CyanTertiary).forEach { (yOffset, color) ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = yOffset.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Study AI is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttachmentChip(
    attachment: AttachmentPayload,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (attachment is AttachmentPayload.ImageAttachment) {
                Image(
                    bitmap = attachment.bitmap.asImageBitmap(),
                    contentDescription = attachment.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = attachment.displayName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 110.dp)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
