package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOutUser
import com.example.data.ai.GeminiRepository
import com.example.data.local.SettingsDataStore
import com.example.data.model.AppLimitRule
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.remote.ChatRepository
import com.example.security.configureAppCheck
import com.example.ui.components.AppRoute
import com.example.ui.components.ChatDrawerContent
import com.example.ui.screens.AdmissionDetailsScreen
import com.example.ui.screens.ApiConfigScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreativeStudioScreen
import com.example.ui.screens.FlashcardsScreen
import com.example.ui.screens.FocusLimitScreen
import com.example.ui.screens.GeminiNotebookScreen
import com.example.ui.screens.HscKnowledgeScreen
import com.example.ui.screens.LiveVoiceOverlay
import com.example.ui.screens.ProgressDashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyTimerScreen
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudyUpdateAiTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val settingsDataStore = remember { SettingsDataStore(context.applicationContext) }
    val appSettings by settingsDataStore.settingsFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.model.AppSettings()
    )

    val useDarkTheme = when (appSettings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    StudyUpdateAiTheme(darkTheme = useDarkTheme) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = {})
        } else {
            key(user.uid) {
                AuthenticatedWorkspace(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName,
                    userEmail = user.email,
                    settingsDataStore = settingsDataStore,
                    onSignOut = {
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedWorkspace(
    currentUserId: String,
    userDisplayName: String?,
    userEmail: String?,
    settingsDataStore: SettingsDataStore,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    appContext = app.applicationContext,
                    chatRepository = ChatRepository(db),
                    geminiRepository = GeminiRepository(),
                    settingsDataStore = settingsDataStore,
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val isTemporaryChat by viewModel.isTemporaryChat.collectAsStateWithLifecycle()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsStateWithLifecycle()
    val isEffectiveOffline by viewModel.isEffectiveOffline.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingMessageText.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val videoGenState by viewModel.videoGenState.collectAsStateWithLifecycle()
    val liveVoiceMode by viewModel.liveVoiceMode.collectAsStateWithLifecycle()
    val liveVoiceState by viewModel.liveVoiceState.collectAsStateWithLifecycle()
    val appLimits by viewModel.appLimits.collectAsStateWithLifecycle()
    val unlockRecords by viewModel.unlockRecords.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val progressStats by viewModel.progressStats.collectAsStateWithLifecycle()
    val studyTimerSessions by viewModel.studyTimerSessions.collectAsStateWithLifecycle()
    val studyTimerSubject by viewModel.studyTimerSubject.collectAsStateWithLifecycle()
    val studyTimerTotalSeconds by viewModel.studyTimerTotalSeconds.collectAsStateWithLifecycle()
    val studyTimerRemainingSeconds by viewModel.studyTimerRemainingSeconds.collectAsStateWithLifecycle()
    val isStudyTimerRunning by viewModel.isStudyTimerRunning.collectAsStateWithLifecycle()
    val isStudyTimerBreakMode by viewModel.isStudyTimerBreakMode.collectAsStateWithLifecycle()
    val notebooks by viewModel.notebooks.collectAsStateWithLifecycle()
    val activeNotebook by viewModel.activeNotebook.collectAsStateWithLifecycle()
    val isGeneratingPodcast by viewModel.isGeneratingPodcast.collectAsStateWithLifecycle()
    val isGeneratingNotebookArtifact by viewModel.isGeneratingNotebookArtifact.collectAsStateWithLifecycle()
    val isStreamingNotebookChat by viewModel.isStreamingNotebookChat.collectAsStateWithLifecycle()
    val streamingNotebookChatText by viewModel.streamingNotebookChatText.collectAsStateWithLifecycle()
    val isGeneratingFlashcards by viewModel.isGeneratingFlashcards.collectAsStateWithLifecycle()
    val isEvaluatingUnlockReason by viewModel.isEvaluatingUnlockReason.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf(AppRoute.CHAT) }
    var studioInitialTab by remember { mutableIntStateOf(0) }
    var studioInitialPrompt by remember { mutableStateOf("") }
    var showLiveVoiceOverlay by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val activeSession = sessions.firstOrNull { it.id == activeSessionId }

    // Self-lock check if Study Update AI itself has an expired time limit
    val selfLockedRule = appLimits.firstOrNull {
        it.isLocked && it.appName.contains("Study Update", ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = currentRoute == AppRoute.CHAT && !showLiveVoiceOverlay && selfLockedRule == null,
            drawerContent = {
                ChatDrawerContent(
                    sessionsState = sessionsState,
                    activeSessionId = activeSessionId,
                    isTemporaryChat = isTemporaryChat,
                    isOfflineEffective = isEffectiveOffline,
                    currentRoute = currentRoute,
                    settings = settings,
                    userEmail = userEmail,
                    onNewChat = { viewModel.startNewChat() },
                    onStartTemporaryChat = { viewModel.toggleTemporaryChat(true) },
                    onToggleOfflineMode = { viewModel.updateOfflineMode(it) },
                    onSelectSession = { viewModel.selectSession(it) },
                    onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                    onDeleteSession = { id -> viewModel.deleteSession(id) },
                    onNavigate = { route ->
                        if (route == AppRoute.STUDIO) {
                            studioInitialTab = 0
                            studioInitialPrompt = ""
                        }
                        currentRoute = route
                        scope.launch { drawerState.close() }
                    },
                    onSignOut = {
                        scope.launch { drawerState.close() }
                        onSignOut()
                    }
                )
            }
        ) {
            when (currentRoute) {
                AppRoute.CHAT -> {
                    ChatScreen(
                        activeSession = activeSession,
                        isTemporaryChat = isTemporaryChat,
                        isOfflineEffective = isEffectiveOffline,
                        isDeviceOnline = isDeviceOnline,
                        messagesState = messagesState,
                        activeMode = activeMode,
                        settings = settings,
                        userDisplayName = userDisplayName,
                        isStreaming = isStreaming,
                        streamingText = streamingText,
                        pendingAttachments = pendingAttachments,
                        bannerMessage = bannerMessage,
                        autoSpeakEvents = viewModel.autoSpeakEvents,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenApiConfig = { currentRoute = AppRoute.API_CONFIG },
                        onOpenNotebook = { currentRoute = AppRoute.NOTEBOOK },
                        onOpenStudio = { tabIndex ->
                            studioInitialTab = tabIndex
                            studioInitialPrompt = ""
                            currentRoute = AppRoute.STUDIO
                        },
                        onOpenHscLab = { currentRoute = AppRoute.HSC_HUB },
                        onOpenFlashcards = { currentRoute = AppRoute.FLASHCARDS },
                        onOpenProgress = { currentRoute = AppRoute.PROGRESS },
                        onOpenStudyTimer = { currentRoute = AppRoute.STUDY_TIMER },
                        onOpenAdmissionDetails = { currentRoute = AppRoute.ADMISSION },
                        onOpenFocusLock = { currentRoute = AppRoute.FOCUS_LOCK },
                        onOpenLiveVoice = { showLiveVoiceOverlay = true },
                        onToggleTemporaryChat = { viewModel.toggleTemporaryChat() },
                        onToggleOfflineMode = { viewModel.updateOfflineMode(!settings.offlineModeEnabled) },
                        onToggleTheme = {
                            val next = if (settings.themeMode == ThemeMode.LIGHT) {
                                ThemeMode.DARK
                            } else {
                                ThemeMode.LIGHT
                            }
                            viewModel.updateThemeMode(next)
                        },
                        onSelectMode = { viewModel.selectMode(it) },
                        onSendMessage = { text, mode -> viewModel.sendMessage(text, mode) },
                        onSendImageInChat = { prompt -> viewModel.sendImageInChat(prompt) },
                        onStopStreaming = { viewModel.stopStreaming() },
                        onRegenerateLast = { viewModel.regenerateLastResponse() },
                        onAddAttachment = { viewModel.addAttachment(it) },
                        onRemoveAttachment = { viewModel.removeAttachment(it) },
                        onShowBanner = { viewModel.showBannerMessage(it) },
                        onDismissBanner = { viewModel.clearBannerMessage() }
                    )
                }
                AppRoute.NOTEBOOK -> {
                    GeminiNotebookScreen(
                        notebooks = notebooks,
                        activeNotebook = activeNotebook,
                        settings = settings,
                        isOffline = isEffectiveOffline,
                        isGeneratingPodcast = isGeneratingPodcast,
                        isGeneratingArtifact = isGeneratingNotebookArtifact,
                        isStreamingChat = isStreamingNotebookChat,
                        streamingChatText = streamingNotebookChatText,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onCreateNotebook = { viewModel.createNotebook(it) },
                        onSelectNotebook = { viewModel.selectNotebook(it) },
                        onDeleteNotebook = { viewModel.deleteNotebook(it) },
                        onAddNotebookSource = { nbId, src -> viewModel.addNotebookSource(nbId, src) },
                        onRemoveNotebookSource = { nbId, src -> viewModel.removeNotebookSource(nbId, src) },
                        onGeneratePodcast = { viewModel.generateNotebookPodcast(it) },
                        onGenerateArtifact = { nbId, type -> viewModel.generateNotebookArtifact(nbId, type) },
                        onSendNotebookChat = { nbId, q -> viewModel.sendNotebookChat(nbId, q) },
                        onStopNotebookChat = { viewModel.stopNotebookChat() }
                    )
                }
                AppRoute.STUDIO -> {
                    CreativeStudioScreen(
                        initialPrompt = studioInitialPrompt,
                        initialTab = studioInitialTab,
                        imageGenState = imageGenState,
                        videoGenState = videoGenState,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onOpenApiConfig = { currentRoute = AppRoute.API_CONFIG },
                        onGeneratePicture = { prompt, ref -> viewModel.generatePicture(prompt, ref) },
                        onGenerateVideo = { ctx, prompt -> viewModel.generateVideo(ctx, prompt) }
                    )
                }
                AppRoute.HSC_HUB -> {
                    HscKnowledgeScreen(
                        onBack = { currentRoute = AppRoute.CHAT },
                        onAskTopicInChat = { prompt, mode ->
                            viewModel.recordHscTopicStudied()
                            currentRoute = AppRoute.CHAT
                            viewModel.sendMessage(prompt, mode)
                        },
                        onGenerateDiagram = { diagramPrompt ->
                            viewModel.recordHscTopicStudied()
                            studioInitialTab = 0
                            studioInitialPrompt = diagramPrompt
                            currentRoute = AppRoute.STUDIO
                            viewModel.generatePicture(diagramPrompt, null)
                        }
                    )
                }
                AppRoute.FLASHCARDS -> {
                    FlashcardsScreen(
                        flashcards = flashcards,
                        isOffline = isEffectiveOffline,
                        isGeneratingAiDeck = isGeneratingFlashcards,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onReviewCard = { id, mastered -> viewModel.reviewFlashcard(id, mastered) },
                        onAddCustomCard = { subj, front, back, note ->
                            viewModel.addCustomFlashcard(subj, front, back, note)
                        },
                        onGenerateAiCards = { subj, type, topic, count, instr, cb ->
                            viewModel.generateAiFlashcards(subj, type, topic, count, instr, cb)
                        },
                        onDeleteCard = { viewModel.deleteFlashcard(it) }
                    )
                }
                AppRoute.PROGRESS -> {
                    ProgressDashboardScreen(
                        stats = progressStats,
                        flashcards = flashcards,
                        appLimits = appLimits,
                        totalSessionsCount = sessions.size,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onOpenFlashcards = { currentRoute = AppRoute.FLASHCARDS },
                        onOpenHscLab = { currentRoute = AppRoute.HSC_HUB },
                        onResetStats = { viewModel.resetProgressStats() }
                    )
                }
                AppRoute.STUDY_TIMER -> {
                    StudyTimerScreen(
                        selectedSubject = studyTimerSubject,
                        totalSeconds = studyTimerTotalSeconds,
                        remainingSeconds = studyTimerRemainingSeconds,
                        isRunning = isStudyTimerRunning,
                        isBreakMode = isStudyTimerBreakMode,
                        studySessions = studyTimerSessions,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onSelectSubject = { viewModel.selectStudyTimerSubject(it) },
                        onSelectDurationMinutes = { mins, isBreak ->
                            viewModel.setStudyTimerPreset(mins, isBreak)
                        },
                        onToggleStartPause = { viewModel.toggleStudyTimerRunning() },
                        onResetTimer = { viewModel.resetStudyTimer() },
                        onCompleteEarly = { viewModel.completeStudyTimerEarly() }
                    )
                }
                AppRoute.ADMISSION -> {
                    AdmissionDetailsScreen(
                        onBack = { currentRoute = AppRoute.CHAT },
                        onAskAdmissionInChat = { prompt, mode ->
                            currentRoute = AppRoute.CHAT
                            viewModel.sendMessage(prompt, mode)
                        }
                    )
                }
                AppRoute.FOCUS_LOCK -> {
                    FocusLimitScreen(
                        appLimits = appLimits,
                        unlockRecords = unlockRecords,
                        isEvaluatingReason = isEvaluatingUnlockReason,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onAddAppLimit = { name, pkg, mins -> viewModel.addAppLimit(name, pkg, mins) },
                        onToggleTracking = { viewModel.toggleAppTracking(it) },
                        onSimulateExpire = { viewModel.simulateTimeExpiredLock(it) },
                        onRemoveRule = { viewModel.removeAppLimit(it) },
                        onGenerateUnlockCode = { appId, extra, reason, contact, cb ->
                            viewModel.generateUnlockCode(appId, extra, reason, contact, cb)
                        },
                        onApplyUnlockCode = { ruleId, code, cb ->
                            viewModel.applyUnlockCode(ruleId, code, cb)
                        }
                    )
                }
                AppRoute.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        userEmail = userEmail,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onUpdateTheme = { viewModel.updateThemeMode(it) },
                        onUpdateOfflineMode = { viewModel.updateOfflineMode(it) },
                        onUpdateLanguage = { viewModel.updateLanguagePreference(it) },
                        onUpdateAcademicProfile = { viewModel.updateAcademicProfile(it) },
                        onUpdateTemperature = { viewModel.updateTemperature(it) },
                        onUpdateAutoSpeak = { viewModel.updateAutoSpeak(it) },
                        onUpdateVoiceLang = { viewModel.updateVoiceInputLang(it) },
                        onUpdateSpeechRate = { viewModel.updateSpeechRate(it) },
                        onSelectAiVoice = { viewModel.selectAiVoice(it) },
                        onUpdateTtsPitch = { viewModel.updateTtsPitch(it) },
                        onSignOut = onSignOut
                    )
                }
                AppRoute.API_CONFIG -> {
                    ApiConfigScreen(
                        settings = settings,
                        connectionTestState = connectionTestState,
                        onBack = { currentRoute = AppRoute.CHAT },
                        onSelectModel = { viewModel.updateModelId(it) },
                        onToggleUseCustomKey = { viewModel.updateUseCustomApiKey(it) },
                        onSaveCustomKey = { raw, cb -> viewModel.saveCustomApiKey(raw, cb) },
                        onClearCustomKey = { viewModel.clearCustomApiKey() },
                        onTestConnection = { viewModel.testApiConnection(it) }
                    )
                }
            }
        }

        if (showLiveVoiceOverlay) {
            LiveVoiceOverlay(
                settings = settings,
                liveVoiceMode = liveVoiceMode,
                liveVoiceState = liveVoiceState,
                onSelectMode = { viewModel.setLiveVoiceMode(it) },
                onSelectAiVoice = { viewModel.selectAiVoice(it) },
                onStartGeminiLive = { viewModel.startGeminiLiveSession(context) },
                onStopGeminiLive = { viewModel.stopGeminiLiveSession() },
                onSendVoiceLoopTurn = { spoken, onState, onReply ->
                    viewModel.askInVoiceLoop(spoken, onState, onReply)
                },
                onClose = { showLiveVoiceOverlay = false }
            )
        }

        // Self-lock overlay when Study Update AI time limit expires
        if (selfLockedRule != null && currentRoute != AppRoute.FOCUS_LOCK) {
            SelfTimeLimitLockOverlay(
                rule = selfLockedRule,
                onUnlock = { code, cb -> viewModel.applyUnlockCode(selfLockedRule.id, code, cb) },
                onOpenFocusManager = { currentRoute = AppRoute.FOCUS_LOCK }
            )
        }
    }
}

@Composable
private fun SelfTimeLimitLockOverlay(
    rule: AppLimitRule,
    onUnlock: (String, (Boolean, String) -> Unit) -> Unit,
    onOpenFocusManager: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .padding(24.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "${rule.appName} Time Limit Reached",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your daily limit (${rule.dailyLimitMinutes} min) has expired. Enter an unlock code generated with a reason.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it },
                    label = { Text("Unlock Code (SU-XXXXXX)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMonoFamily),
                    modifier = Modifier.fillMaxWidth()
                )
                if (feedback != null) {
                    Text(
                        text = feedback!!.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feedback!!.first) EmeraldSuccess else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        onUnlock(codeInput) { ok, msg -> feedback = ok to msg }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock Now")
                }
                TextButton(onClick = onOpenFocusManager) {
                    Text("Open Unlock Code & Reason Generator")
                }
            }
        }
    }
}
