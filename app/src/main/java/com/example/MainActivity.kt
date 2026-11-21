package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.remote.ChatRepository
import com.example.security.configureAppCheck
import com.example.ui.components.AppRoute
import com.example.ui.components.ChatDrawerContent
import com.example.ui.screens.ApiConfigScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.StudyUpdateAiTheme
import com.example.ui.viewmodel.MainViewModel
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
    userEmail: String?,
    settingsDataStore: SettingsDataStore,
    onSignOut: () -> Unit
) {
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
                    chatRepository = ChatRepository(db),
                    geminiRepository = GeminiRepository(),
                    settingsDataStore = settingsDataStore,
                    currentUserId = currentUserId
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isStreaming.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingMessageText.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()
    val connectionTestState by viewModel.connectionTestState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf(AppRoute.CHAT) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val activeSession = sessions.firstOrNull { it.id == activeSessionId }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == AppRoute.CHAT,
        drawerContent = {
            ChatDrawerContent(
                sessionsState = sessionsState,
                activeSessionId = activeSessionId,
                currentRoute = currentRoute,
                settings = settings,
                userEmail = userEmail,
                onNewChat = { viewModel.startNewChat() },
                onSelectSession = { viewModel.selectSession(it) },
                onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onNavigate = { route ->
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
                    messagesState = messagesState,
                    activeMode = activeMode,
                    settings = settings,
                    isStreaming = isStreaming,
                    streamingText = streamingText,
                    pendingAttachments = pendingAttachments,
                    bannerMessage = bannerMessage,
                    autoSpeakEvents = viewModel.autoSpeakEvents,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenApiConfig = { currentRoute = AppRoute.API_CONFIG },
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
                    onStopStreaming = { viewModel.stopStreaming() },
                    onRegenerateLast = { viewModel.regenerateLastResponse() },
                    onAddAttachment = { viewModel.addAttachment(it) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    onShowBanner = { viewModel.showBannerMessage(it) },
                    onDismissBanner = { viewModel.clearBannerMessage() }
                )
            }
            AppRoute.SETTINGS -> {
                SettingsScreen(
                    settings = settings,
                    userEmail = userEmail,
                    onBack = { currentRoute = AppRoute.CHAT },
                    onUpdateTheme = { viewModel.updateThemeMode(it) },
                    onUpdateLanguage = { viewModel.updateLanguagePreference(it) },
                    onUpdateAcademicProfile = { viewModel.updateAcademicProfile(it) },
                    onUpdateTemperature = { viewModel.updateTemperature(it) },
                    onUpdateAutoSpeak = { viewModel.updateAutoSpeak(it) },
                    onUpdateVoiceLang = { viewModel.updateVoiceInputLang(it) },
                    onUpdateSpeechRate = { viewModel.updateSpeechRate(it) },
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
}
