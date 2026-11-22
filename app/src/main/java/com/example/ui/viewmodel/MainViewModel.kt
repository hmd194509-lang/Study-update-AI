package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiRepository
import com.example.data.ai.LiveVoiceManager
import com.example.data.ai.OfflineStudyEngine
import com.example.data.ai.StudyFlashcardBank
import com.example.data.ai.UnlockReasonEvaluator
import com.example.data.local.SettingsDataStore
import com.example.data.model.AcademicProfile
import com.example.data.model.AiVoicePreset
import com.example.data.model.AppLimitRule
import com.example.data.model.AppSettings
import com.example.data.model.AttachmentPayload
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.ConnectionTestState
import com.example.data.model.FlashcardSubject
import com.example.data.model.FlashcardType
import com.example.data.model.GeminiNotebook
import com.example.data.model.ImageGenUiState
import com.example.data.model.LanguagePreference
import com.example.data.model.LiveVoiceMode
import com.example.data.model.LiveVoiceState
import com.example.data.model.MessageRole
import com.example.data.model.NotebookArtifactType
import com.example.data.model.NotebookMessage
import com.example.data.model.NotebookSource
import com.example.data.model.PodcastTurn
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyMode
import com.example.data.model.StudyProgressStats
import com.example.data.model.StudyTimerSession
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.UnlockCodeRecord
import com.example.data.model.UnlockReasonEvaluation
import com.example.data.model.VideoGenUiState
import com.example.data.model.displayName
import com.example.data.model.idValue
import com.example.data.remote.ChatRepository
import com.example.security.SecretSanitizer
import com.example.util.AttachmentReader
import com.example.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val appContext: Context,
    private val chatRepository: ChatRepository,
    private val geminiRepository: GeminiRepository,
    private val settingsDataStore: SettingsDataStore,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val liveVoiceManager: LiveVoiceManager = LiveVoiceManager(),
    private val unlockReasonEvaluator: UnlockReasonEvaluator = UnlockReasonEvaluator()
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppSettings()
        )

    val appLimits: StateFlow<List<AppLimitRule>> = settingsDataStore.appLimitsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val unlockRecords: StateFlow<List<UnlockCodeRecord>> = settingsDataStore.unlockRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val flashcards: StateFlow<List<StudyFlashcard>> = settingsDataStore.flashcardsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StudyFlashcardBank.defaultCards
        )

    val progressStats: StateFlow<StudyProgressStats> = settingsDataStore.progressStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StudyProgressStats()
        )

    val studyTimerSessions: StateFlow<List<StudyTimerSession>> = settingsDataStore.studyTimerSessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val notebooks: StateFlow<List<GeminiNotebook>> = settingsDataStore.notebooksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _activeNotebookId = MutableStateFlow<String?>(null)
    val activeNotebook: StateFlow<GeminiNotebook?> = combine(notebooks, _activeNotebookId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    val isDeviceOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = networkMonitor.isCurrentlyOnline()
        )

    val isEffectiveOffline: StateFlow<Boolean> = combine(settings, isDeviceOnline) { s, online ->
        s.offlineModeEnabled || !online
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = !networkMonitor.isCurrentlyOnline()
    )

    val sessionsState: StateFlow<UiState<List<ChatSession>>> =
        chatRepository.observeSessions(currentUserId)
            .map<List<ChatSession>, UiState<List<ChatSession>>> { sessions ->
                UiState.Success(sessions)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing sessions", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load chat history."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _isTemporaryChat = MutableStateFlow(false)
    val isTemporaryChat: StateFlow<Boolean> = _isTemporaryChat.asStateFlow()

    private val _temporaryMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    val messagesState: StateFlow<UiState<List<ChatMessage>>> =
        combine(_isTemporaryChat, _activeSessionId) { isTemp, sessionId -> isTemp to sessionId }
            .flatMapLatest { (isTemp, sessionId) ->
                if (isTemp) {
                    _temporaryMessages.map { UiState.Success(it) }
                } else if (sessionId.isNullOrBlank()) {
                    flowOf<UiState<List<ChatMessage>>>(UiState.Success(emptyList()))
                } else {
                    chatRepository.observeMessages(currentUserId, sessionId)
                        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(TAG, "Error observing messages", rootError)
                            emit(UiState.Error(rootError.message ?: "Failed to load messages."))
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Success(emptyList())
            )

    private val _activeMode = MutableStateFlow(StudyMode.GENERAL)
    val activeMode: StateFlow<StudyMode> = _activeMode.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingMessageText = MutableStateFlow<String?>(null)
    val streamingMessageText: StateFlow<String?> = _streamingMessageText.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<AttachmentPayload>>(emptyList())
    val pendingAttachments: StateFlow<List<AttachmentPayload>> = _pendingAttachments.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _imageGenState = MutableStateFlow<ImageGenUiState>(ImageGenUiState.Idle)
    val imageGenState: StateFlow<ImageGenUiState> = _imageGenState.asStateFlow()

    private val _videoGenState = MutableStateFlow<VideoGenUiState>(VideoGenUiState.Idle)
    val videoGenState: StateFlow<VideoGenUiState> = _videoGenState.asStateFlow()

    private val _liveVoiceMode = MutableStateFlow(LiveVoiceMode.BILINGUAL_LOOP)
    val liveVoiceMode: StateFlow<LiveVoiceMode> = _liveVoiceMode.asStateFlow()

    private val _liveVoiceState = MutableStateFlow<LiveVoiceState>(LiveVoiceState.Idle)
    val liveVoiceState: StateFlow<LiveVoiceState> = _liveVoiceState.asStateFlow()

    private val _isGeneratingFlashcards = MutableStateFlow(false)
    val isGeneratingFlashcards: StateFlow<Boolean> = _isGeneratingFlashcards.asStateFlow()

    private val _isEvaluatingUnlockReason = MutableStateFlow(false)
    val isEvaluatingUnlockReason: StateFlow<Boolean> = _isEvaluatingUnlockReason.asStateFlow()

    private val _studyTimerSubject = MutableStateFlow(FlashcardSubject.PHYSICS)
    val studyTimerSubject: StateFlow<FlashcardSubject> = _studyTimerSubject.asStateFlow()

    private val _studyTimerTotalSeconds = MutableStateFlow(25 * 60)
    val studyTimerTotalSeconds: StateFlow<Int> = _studyTimerTotalSeconds.asStateFlow()

    private val _studyTimerRemainingSeconds = MutableStateFlow(25 * 60)
    val studyTimerRemainingSeconds: StateFlow<Int> = _studyTimerRemainingSeconds.asStateFlow()

    private val _isStudyTimerRunning = MutableStateFlow(false)
    val isStudyTimerRunning: StateFlow<Boolean> = _isStudyTimerRunning.asStateFlow()

    private val _isStudyTimerBreakMode = MutableStateFlow(false)
    val isStudyTimerBreakMode: StateFlow<Boolean> = _isStudyTimerBreakMode.asStateFlow()

    private val _isGeneratingPodcast = MutableStateFlow(false)
    val isGeneratingPodcast: StateFlow<Boolean> = _isGeneratingPodcast.asStateFlow()

    private val _isGeneratingNotebookArtifact = MutableStateFlow<NotebookArtifactType?>(null)
    val isGeneratingNotebookArtifact: StateFlow<NotebookArtifactType?> = _isGeneratingNotebookArtifact.asStateFlow()

    private val _isStreamingNotebookChat = MutableStateFlow(false)
    val isStreamingNotebookChat: StateFlow<Boolean> = _isStreamingNotebookChat.asStateFlow()

    private val _streamingNotebookChatText = MutableStateFlow<String?>(null)
    val streamingNotebookChatText: StateFlow<String?> = _streamingNotebookChatText.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _autoSpeakEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val autoSpeakEvents: SharedFlow<String> = _autoSpeakEvents.asSharedFlow()

    private var activeGenerationJob: Job? = null
    private var notebookChatJob: Job? = null
    private var liveVoiceJob: Job? = null
    private val secureRandom = SecureRandom()

    init {
        viewModelScope.launch {
            sessionsState.collect { state ->
                if (!_isTemporaryChat.value && state is UiState.Success && _activeSessionId.value == null && state.data.isNotEmpty()) {
                    val first = state.data.first()
                    _activeSessionId.value = first.id
                    _activeMode.value = StudyMode.fromRaw(first.mode)
                }
            }
        }

        // Countdown ticker for active App Time Limits
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                val current = appLimits.value
                if (current.any { it.isTrackingActive && !it.isLocked }) {
                    val updated = current.map { rule ->
                        if (rule.isTrackingActive && !rule.isLocked) {
                            val nextSec = (rule.remainingSeconds - 1).coerceAtLeast(0)
                            rule.copy(
                                remainingSeconds = nextSec,
                                isLocked = nextSec == 0,
                                isTrackingActive = nextSec > 0,
                                lastUpdatedEpochMs = System.currentTimeMillis()
                            )
                        } else {
                            rule
                        }
                    }
                    settingsDataStore.saveAppLimits(updated)
                }

                // Study Timer countdown ticker
                if (_isStudyTimerRunning.value) {
                    val nextRemaining = (_studyTimerRemainingSeconds.value - 1).coerceAtLeast(0)
                    _studyTimerRemainingSeconds.value = nextRemaining
                    if (nextRemaining == 0) {
                        _isStudyTimerRunning.value = false
                        val completedMinutes = (_studyTimerTotalSeconds.value / 60).coerceAtLeast(1)
                        if (!_isStudyTimerBreakMode.value) {
                            recordCompletedStudyTimer(
                                subject = _studyTimerSubject.value,
                                minutes = completedMinutes
                            )
                            _bannerMessage.value =
                                "Completed $completedMinutes min ${_studyTimerSubject.value.label} study session!"
                        } else {
                            _bannerMessage.value = "Break finished! Ready for your next study block."
                        }
                    }
                }
            }
        }
    }

    fun selectSession(session: ChatSession) {
        if (_isStreaming.value) stopStreaming()
        _isTemporaryChat.value = false
        _temporaryMessages.value = emptyList()
        _activeSessionId.value = session.id
        _activeMode.value = StudyMode.fromRaw(session.mode)
    }

    fun startNewChat() {
        if (_isStreaming.value) stopStreaming()
        _isTemporaryChat.value = false
        _temporaryMessages.value = emptyList()
        _activeSessionId.value = null
        _streamingMessageText.value = null
        _pendingAttachments.value = emptyList()
    }

    fun toggleTemporaryChat(enable: Boolean? = null) {
        val next = enable ?: !_isTemporaryChat.value
        if (_isStreaming.value) stopStreaming()
        _isTemporaryChat.value = next
        _temporaryMessages.value = emptyList()
        _streamingMessageText.value = null
        _pendingAttachments.value = emptyList()
        if (next) {
            _activeSessionId.value = null
        }
    }

    fun selectMode(mode: StudyMode) {
        _activeMode.value = mode
        val sid = _activeSessionId.value
        if (!_isTemporaryChat.value && !sid.isNullOrBlank()) {
            viewModelScope.launch {
                chatRepository.updateSessionMetadata(sessionId = sid, mode = mode)
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            chatRepository.updateSessionMetadata(sessionId = sessionId, title = clean)
                .onFailure { _bannerMessage.value = "Could not rename conversation." }
        }
    }

    fun deleteSession(sessionId: String) {
        if (_activeSessionId.value == sessionId) {
            if (_isStreaming.value) stopStreaming()
            _activeSessionId.value = null
        }
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
                .onFailure { _bannerMessage.value = "Could not delete conversation." }
        }
    }

    fun addAttachment(payload: AttachmentPayload) {
        val current = _pendingAttachments.value
        if (current.size >= 4) {
            _bannerMessage.value = "You can attach up to 4 files per message."
            return
        }
        _pendingAttachments.value = current + payload
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.value = _pendingAttachments.value.filterNot { it.idValue == id }
    }

    fun clearBannerMessage() {
        _bannerMessage.value = null
    }

    fun showBannerMessage(message: String) {
        _bannerMessage.value = message
    }

    fun sendMessage(rawPrompt: String, overrideMode: StudyMode? = null) {
        if (_isStreaming.value) return
        val attachments = _pendingAttachments.value
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() && attachments.isEmpty()) return

        if (overrideMode != null) {
            _activeMode.value = overrideMode
        }
        val mode = _activeMode.value
        val currentSettings = settings.value
        val isTemp = _isTemporaryChat.value

        activeGenerationJob = viewModelScope.launch {
            var customKey: String? = null
            try {
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null

                if (currentSettings.useCustomApiKey && customKey.isNullOrBlank()) {
                    _bannerMessage.value =
                        "Custom API Key mode is enabled, but no key is saved. Please add your key in API Configuration."
                    return@launch
                }

                val safePrompt = SecretSanitizer.redact(prompt, customKey)
                val attachmentNames = attachments.map { it.displayName }
                val effectiveUserContent = safePrompt.ifBlank {
                    "Analyze attached: ${attachmentNames.joinToString(", ")}"
                }

                val existingMessages = (messagesState.value as? UiState.Success)?.data.orEmpty()
                var sessionId: String? = null

                if (isTemp) {
                    val userMsg = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.USER.name,
                        content = effectiveUserContent,
                        mode = mode.name,
                        attachmentNames = attachmentNames,
                        errorFlag = false
                    )
                    _temporaryMessages.value = _temporaryMessages.value + userMsg
                    _pendingAttachments.value = emptyList()
                } else {
                    sessionId = _activeSessionId.value
                    if (sessionId.isNullOrBlank()) {
                        val autoTitle = (safePrompt.ifBlank { attachmentNames.firstOrNull() ?: "New Conversation" })
                            .take(48)
                        val createResult = chatRepository.createSession(
                            title = autoTitle,
                            mode = mode
                        )
                        sessionId = createResult.getOrElse { err ->
                            _bannerMessage.value = err.localizedMessage ?: "Failed to create conversation."
                            return@launch
                        }
                        _activeSessionId.value = sessionId
                    }

                    _pendingAttachments.value = emptyList()

                    chatRepository.addMessage(
                        sessionId = sessionId,
                        role = MessageRole.USER,
                        content = effectiveUserContent,
                        mode = mode,
                        attachmentNames = attachmentNames
                    ).onFailure { err ->
                        _bannerMessage.value = err.localizedMessage ?: "Failed to save message."
                        return@launch
                    }

                    chatRepository.updateSessionMetadata(
                        sessionId = sessionId,
                        mode = mode,
                        lastMessagePreview = effectiveUserContent.take(120)
                    )
                }

                val effectiveOffline = currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()
                recordQuestionActivity(mode = mode, isOffline = effectiveOffline)

                _isStreaming.value = true
                _streamingMessageText.value = ""
                val accumulator = StringBuilder()

                if (effectiveOffline) {
                    val offlineReply = OfflineStudyEngine.buildOfflineReply(
                        prompt = effectiveUserContent,
                        mode = mode,
                        flashcards = flashcards.value
                    )
                    accumulator.append(offlineReply)
                    _streamingMessageText.value = offlineReply
                } else {
                    geminiRepository.streamReply(
                        history = existingMessages,
                        userPrompt = effectiveUserContent,
                        attachments = attachments,
                        mode = mode,
                        settings = currentSettings,
                        customApiKey = customKey
                    ).collect { chunk ->
                        accumulator.append(chunk)
                        _streamingMessageText.value = SecretSanitizer.redact(accumulator.toString(), customKey)
                    }
                }

                val finalReply = SecretSanitizer.redact(accumulator.toString().trim(), customKey)
                if (finalReply.isNotEmpty()) {
                    if (isTemp) {
                        val modelMsg = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            sessionId = "temp",
                            role = MessageRole.MODEL.name,
                            content = finalReply,
                            mode = mode.name,
                            errorFlag = false
                        )
                        _temporaryMessages.value = _temporaryMessages.value + modelMsg
                    } else if (!sessionId.isNullOrBlank()) {
                        chatRepository.addMessage(
                            sessionId = sessionId,
                            role = MessageRole.MODEL,
                            content = finalReply,
                            mode = mode,
                            errorFlag = false
                        )
                        chatRepository.updateSessionMetadata(
                            sessionId = sessionId,
                            lastMessagePreview = finalReply.take(120)
                        )
                    }
                    if (currentSettings.autoSpeak) {
                        _autoSpeakEvents.tryEmit(finalReply)
                    }
                }
            } catch (e: CancellationException) {
                val partial = _streamingMessageText.value?.trim().orEmpty()
                val sid = _activeSessionId.value
                if (partial.isNotEmpty()) {
                    if (_isTemporaryChat.value) {
                        _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            sessionId = "temp",
                            role = MessageRole.MODEL.name,
                            content = partial,
                            mode = mode.name,
                            errorFlag = false
                        )
                    } else if (!sid.isNullOrBlank()) {
                        viewModelScope.launch {
                            chatRepository.addMessage(
                                sessionId = sid,
                                role = MessageRole.MODEL,
                                content = partial,
                                mode = mode,
                                errorFlag = false
                            )
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                val friendlyError = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                if (_isTemporaryChat.value) {
                    _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.MODEL.name,
                        content = friendlyError,
                        mode = mode.name,
                        errorFlag = true
                    )
                } else {
                    val sid = _activeSessionId.value
                    if (!sid.isNullOrBlank()) {
                        chatRepository.addMessage(
                            sessionId = sid,
                            role = MessageRole.MODEL,
                            content = friendlyError,
                            mode = mode,
                            errorFlag = true
                        )
                    } else {
                        _bannerMessage.value = friendlyError
                    }
                }
            } finally {
                _isStreaming.value = false
                _streamingMessageText.value = null
                activeGenerationJob = null
            }
        }
    }

    fun sendImageInChat(rawPrompt: String) {
        if (_isStreaming.value) return
        val prompt = rawPrompt.trim()
        val attachments = _pendingAttachments.value
        val refBitmap = attachments.filterIsInstance<AttachmentPayload.ImageAttachment>().firstOrNull()?.bitmap
        if (prompt.isEmpty()) {
            _bannerMessage.value = "Describe the picture or diagram you want Study AI to create."
            return
        }

        val currentSettings = settings.value
        val isTemp = _isTemporaryChat.value
        val mode = _activeMode.value

        activeGenerationJob = viewModelScope.launch {
            var customKey: String? = null
            try {
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null

                val safePrompt = SecretSanitizer.redact(prompt, customKey)
                val userText = "🎨 Generate picture: $safePrompt"
                var sessionId: String? = null

                if (isTemp) {
                    _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.USER.name,
                        content = userText,
                        mode = mode.name
                    )
                    _pendingAttachments.value = emptyList()
                } else {
                    sessionId = _activeSessionId.value
                    if (sessionId.isNullOrBlank()) {
                        val createResult = chatRepository.createSession(
                            title = "Picture: ${safePrompt.take(36)}",
                            mode = mode
                        )
                        sessionId = createResult.getOrElse { err ->
                            _bannerMessage.value = err.localizedMessage ?: "Failed to create conversation."
                            return@launch
                        }
                        _activeSessionId.value = sessionId
                    }
                    _pendingAttachments.value = emptyList()
                    chatRepository.addMessage(
                        sessionId = sessionId,
                        role = MessageRole.USER,
                        content = userText,
                        mode = mode
                    ).onFailure { err ->
                        _bannerMessage.value = err.localizedMessage ?: "Failed to save message."
                        return@launch
                    }
                    chatRepository.updateSessionMetadata(
                        sessionId = sessionId,
                        mode = mode,
                        lastMessagePreview = userText.take(120)
                    )
                }

                if (currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()) {
                    throw IllegalStateException("Internet connection is required to generate pictures.")
                }

                _isStreaming.value = true
                _streamingMessageText.value = "🎨 Study AI is creating your picture..."

                val bitmap = geminiRepository.generateImage(
                    prompt = safePrompt,
                    referenceBitmap = refBitmap,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )

                val imgDir = java.io.File(appContext.filesDir, "chat_images").apply { mkdirs() }
                val fileName = "chat_img_${UUID.randomUUID()}.png"
                val outFile = java.io.File(imgDir, fileName)
                java.io.FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
                }

                val caption = "Generated picture for **$safePrompt** _(Tap image to view fullscreen or share)_"
                if (isTemp) {
                    _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.MODEL.name,
                        content = caption,
                        mode = mode.name,
                        attachmentNames = listOf("IMG_GEN:$fileName"),
                        generatedImagePath = outFile.absolutePath
                    )
                } else if (!sessionId.isNullOrBlank()) {
                    chatRepository.addMessage(
                        sessionId = sessionId,
                        role = MessageRole.MODEL,
                        content = caption,
                        mode = mode,
                        attachmentNames = listOf("IMG_GEN:$fileName")
                    )
                    chatRepository.updateSessionMetadata(
                        sessionId = sessionId,
                        lastMessagePreview = "🎨 $safePrompt".take(120)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "In-chat picture generation failed", e)
                val friendlyError = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                if (_isTemporaryChat.value) {
                    _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.MODEL.name,
                        content = friendlyError,
                        mode = mode.name,
                        errorFlag = true
                    )
                } else {
                    val sid = _activeSessionId.value
                    if (!sid.isNullOrBlank()) {
                        chatRepository.addMessage(
                            sessionId = sid,
                            role = MessageRole.MODEL,
                            content = friendlyError,
                            mode = mode,
                            errorFlag = true
                        )
                    } else {
                        _bannerMessage.value = friendlyError
                    }
                }
            } finally {
                _isStreaming.value = false
                _streamingMessageText.value = null
                activeGenerationJob = null
            }
        }
    }

    fun stopStreaming() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _isStreaming.value = false
        _streamingMessageText.value = null
    }

    fun regenerateLastResponse() {
        if (_isStreaming.value) return
        val isTemp = _isTemporaryChat.value
        val sid = _activeSessionId.value
        if (!isTemp && sid == null) return

        val messages = (messagesState.value as? UiState.Success)?.data ?: return
        if (messages.isEmpty()) return

        val last = messages.last()
        val lastUser = if (MessageRole.fromRaw(last.role) == MessageRole.MODEL) {
            messages.dropLast(1).lastOrNull { MessageRole.fromRaw(it.role) == MessageRole.USER }
        } else {
            last
        } ?: return

        activeGenerationJob = viewModelScope.launch {
            var customKey: String? = null
            try {
                if (MessageRole.fromRaw(last.role) == MessageRole.MODEL) {
                    if (isTemp) {
                        _temporaryMessages.value = _temporaryMessages.value.dropLast(1)
                    } else if (sid != null) {
                        chatRepository.deleteMessage(sid, last.id)
                    }
                }
                val historyWithoutLast = messages
                    .dropLastWhile { MessageRole.fromRaw(it.role) == MessageRole.MODEL }
                    .dropLast(1)

                val currentSettings = settings.value
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null

                val mode = _activeMode.value
                _isStreaming.value = true
                _streamingMessageText.value = ""
                val accumulator = StringBuilder()

                geminiRepository.streamReply(
                    history = historyWithoutLast,
                    userPrompt = lastUser.content,
                    attachments = emptyList(),
                    mode = mode,
                    settings = currentSettings,
                    customApiKey = customKey
                ).collect { chunk ->
                    accumulator.append(chunk)
                    _streamingMessageText.value = SecretSanitizer.redact(accumulator.toString(), customKey)
                }

                val finalReply = SecretSanitizer.redact(accumulator.toString().trim(), customKey)
                if (finalReply.isNotEmpty()) {
                    if (isTemp) {
                        _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                            id = UUID.randomUUID().toString(),
                            userId = currentUserId,
                            sessionId = "temp",
                            role = MessageRole.MODEL.name,
                            content = finalReply,
                            mode = mode.name,
                            errorFlag = false
                        )
                    } else if (sid != null) {
                        chatRepository.addMessage(
                            sessionId = sid,
                            role = MessageRole.MODEL,
                            content = finalReply,
                            mode = mode,
                            errorFlag = false
                        )
                        chatRepository.updateSessionMetadata(
                            sessionId = sid,
                            lastMessagePreview = finalReply.take(120)
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val friendlyError = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                if (isTemp) {
                    _temporaryMessages.value = _temporaryMessages.value + ChatMessage(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        sessionId = "temp",
                        role = MessageRole.MODEL.name,
                        content = friendlyError,
                        mode = _activeMode.value.name,
                        errorFlag = true
                    )
                } else if (sid != null) {
                    chatRepository.addMessage(
                        sessionId = sid,
                        role = MessageRole.MODEL,
                        content = friendlyError,
                        mode = _activeMode.value,
                        errorFlag = true
                    )
                }
            } finally {
                _isStreaming.value = false
                _streamingMessageText.value = null
                activeGenerationJob = null
            }
        }
    }

    // Picture Generation Skill
    fun generatePicture(prompt: String, referenceBitmap: Bitmap? = null) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _imageGenState.value = ImageGenUiState.Loading
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null

                val bitmap = geminiRepository.generateImage(
                    prompt = clean,
                    referenceBitmap = referenceBitmap,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )
                _imageGenState.value = ImageGenUiState.Success(
                    bitmap = bitmap,
                    prompt = clean
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Picture generation failed", e)
                _imageGenState.value = ImageGenUiState.Error(
                    geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                )
            }
        }
    }

    fun clearImageGenState() {
        _imageGenState.value = ImageGenUiState.Idle
    }

    // Video Generation Skill
    fun generateVideo(context: Context, prompt: String) {
        val clean = prompt.trim()
        if (clean.isEmpty()) return

        viewModelScope.launch {
            _videoGenState.value = VideoGenUiState.Loading("Preparing Gemini Veo request...")
            var customKey: String? = null
            try {
                customKey = settingsDataStore.getDecryptedCustomApiKey()
                val file = geminiRepository.generateVideo(
                    context = context.applicationContext,
                    prompt = clean,
                    customApiKey = customKey,
                    onProgress = { msg ->
                        _videoGenState.value = VideoGenUiState.Loading(msg)
                    }
                )
                _videoGenState.value = VideoGenUiState.Success(
                    videoFilePath = file.absolutePath,
                    prompt = clean
                )
            } catch (e: GeminiRepository.MissingVideoApiKeyException) {
                _videoGenState.value = VideoGenUiState.Error(
                    message = e.message ?: "Gemini API Key required for Veo video generation.",
                    needsApiKey = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Video generation failed", e)
                _videoGenState.value = VideoGenUiState.Error(
                    message = geminiRepository.mapExceptionToFriendlyMessage(e, customKey),
                    needsApiKey = false
                )
            }
        }
    }

    fun clearVideoGenState() {
        _videoGenState.value = VideoGenUiState.Idle
    }

    // Live Voice Chat (Gemini Live API + Bilingual Loop)
    fun setLiveVoiceMode(mode: LiveVoiceMode) {
        _liveVoiceMode.value = mode
    }

    fun startGeminiLiveSession(context: Context) {
        liveVoiceJob?.cancel()
        liveVoiceJob = viewModelScope.launch {
            _liveVoiceState.value = LiveVoiceState.Connecting
            try {
                liveVoiceManager.startLiveAudioSession(
                    context = context,
                    mode = _activeMode.value,
                    settings = settings.value
                )
                _liveVoiceState.value = LiveVoiceState.Active(
                    statusText = "Connected to Gemini Live Audio — speak naturally",
                    isMuted = false,
                    isModelSpeaking = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini Live session failed", e)
                _liveVoiceState.value = LiveVoiceState.Error(
                    geminiRepository.mapExceptionToFriendlyMessage(e)
                )
            }
        }
    }

    fun stopGeminiLiveSession() {
        liveVoiceJob?.cancel()
        liveVoiceJob = null
        viewModelScope.launch {
            liveVoiceManager.stopLiveSession()
            _liveVoiceState.value = LiveVoiceState.Idle
        }
    }

    fun askInVoiceLoop(
        spokenText: String,
        onStatusUpdate: (LiveVoiceState) -> Unit,
        onReplyReady: (String) -> Unit
    ) {
        val clean = spokenText.trim()
        if (clean.isEmpty()) return

        liveVoiceJob?.cancel()
        liveVoiceJob = viewModelScope.launch {
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null

                onStatusUpdate(
                    LiveVoiceState.Active(
                        statusText = "Thinking...",
                        lastTranscript = clean
                    )
                )
                val acc = StringBuilder()
                geminiRepository.streamReply(
                    history = emptyList(),
                    userPrompt = "$clean\n(Keep your answer conversational and clear for voice playback.)",
                    attachments = emptyList(),
                    mode = _activeMode.value,
                    settings = currentSettings,
                    customApiKey = customKey
                ).collect { chunk -> acc.append(chunk) }

                val finalReply = SecretSanitizer.redact(acc.toString().trim(), customKey)
                if (finalReply.isNotEmpty()) {
                    onReplyReady(finalReply)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onStatusUpdate(
                    LiveVoiceState.Error(
                        geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                    )
                )
            }
        }
    }

    // Any-App Time Limit & Unlock Code + Reason System
    fun addAppLimit(appName: String, packageName: String = "", dailyLimitMinutes: Int) {
        val cleanName = appName.trim()
        if (cleanName.isEmpty()) return
        val safeMinutes = dailyLimitMinutes.coerceIn(1, 720)
        viewModelScope.launch {
            val current = appLimits.value
            val newRule = AppLimitRule(
                id = UUID.randomUUID().toString(),
                appName = cleanName,
                packageName = packageName.trim(),
                dailyLimitMinutes = safeMinutes,
                remainingSeconds = safeMinutes * 60,
                isTrackingActive = false,
                isLocked = false
            )
            settingsDataStore.saveAppLimits(listOf(newRule) + current)
        }
    }

    fun toggleAppTracking(ruleId: String) {
        viewModelScope.launch {
            val updated = appLimits.value.map { rule ->
                if (rule.id == ruleId) {
                    if (rule.isLocked || rule.remainingSeconds <= 0) {
                        rule.copy(isLocked = true, isTrackingActive = false)
                    } else {
                        rule.copy(
                            isTrackingActive = !rule.isTrackingActive,
                            lastUpdatedEpochMs = System.currentTimeMillis()
                        )
                    }
                } else {
                    rule
                }
            }
            settingsDataStore.saveAppLimits(updated)
        }
    }

    fun simulateTimeExpiredLock(ruleId: String) {
        viewModelScope.launch {
            val updated = appLimits.value.map { rule ->
                if (rule.id == ruleId) {
                    rule.copy(remainingSeconds = 0, isLocked = true, isTrackingActive = false)
                } else {
                    rule
                }
            }
            settingsDataStore.saveAppLimits(updated)
        }
    }

    fun removeAppLimit(ruleId: String) {
        viewModelScope.launch {
            settingsDataStore.saveAppLimits(appLimits.value.filterNot { it.id == ruleId })
        }
    }

    fun generateUnlockCode(
        targetAppId: String,
        extraMinutes: Int,
        reason: String,
        recipientContact: String,
        onResult: (Boolean, UnlockCodeRecord?, String, UnlockReasonEvaluation?) -> Unit
    ) {
        if (_isEvaluatingUnlockReason.value) return
        val cleanReason = reason.trim()
        val targetApp = appLimits.value.firstOrNull { it.id == targetAppId }
        if (targetApp == null) {
            onResult(false, null, "Please select an app to generate the unlock code for.", null)
            return
        }

        viewModelScope.launch {
            _isEvaluatingUnlockReason.value = true
            try {
                val currentSettings = settings.value
                val customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null
                val offline = currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()

                val evaluation = unlockReasonEvaluator.evaluate(
                    appName = targetApp.appName,
                    extraMinutes = extraMinutes,
                    rawReason = cleanReason,
                    isOffline = offline,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )

                if (!evaluation.approved) {
                    val curStats = progressStats.value
                    settingsDataStore.saveProgressStats(
                        curStats.copy(unlockRequestsRejected = curStats.unlockRequestsRejected + 1)
                    )
                    onResult(false, null, evaluation.feedback, evaluation)
                    return@launch
                }

                val num = 100000 + secureRandom.nextInt(900000)
                val record = UnlockCodeRecord(
                    id = UUID.randomUUID().toString(),
                    code = "SU-$num",
                    targetAppId = targetApp.id,
                    targetAppName = targetApp.appName,
                    extraMinutes = extraMinutes.coerceIn(5, 240),
                    reason = cleanReason,
                    recipientContact = recipientContact.trim(),
                    createdAtMs = System.currentTimeMillis(),
                    isUsed = false
                )
                val next = listOf(record) + unlockRecords.value
                settingsDataStore.saveUnlockRecords(next)
                val curStats = progressStats.value
                settingsDataStore.saveProgressStats(
                    curStats.copy(unlockRequestsApproved = curStats.unlockRequestsApproved + 1)
                )
                onResult(
                    true,
                    record,
                    "Unlock code ${record.code} generated (${evaluation.score}/100). ${evaluation.feedback}",
                    evaluation
                )
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isEvaluatingUnlockReason.value = false
            }
        }
    }

    fun applyUnlockCode(
        ruleId: String,
        enteredCode: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanCode = enteredCode.trim().uppercase()
        if (cleanCode.isEmpty()) {
            onResult(false, "Please enter the unlock code.")
            return
        }

        val matching = unlockRecords.value.firstOrNull {
            !it.isUsed &&
                it.code.equals(cleanCode, ignoreCase = true) &&
                it.targetAppId == ruleId
        }
        if (matching == null) {
            onResult(false, "Invalid or already used unlock code for this app.")
            return
        }

        viewModelScope.launch {
            val updatedRules = appLimits.value.map { rule ->
                if (rule.id == ruleId) {
                    rule.copy(
                        isLocked = false,
                        isTrackingActive = false,
                        remainingSeconds = rule.remainingSeconds + (matching.extraMinutes * 60),
                        lastUpdatedEpochMs = System.currentTimeMillis()
                    )
                } else {
                    rule
                }
            }
            val updatedRecords = unlockRecords.value.map { rec ->
                if (rec.id == matching.id) rec.copy(isUsed = true) else rec
            }
            settingsDataStore.saveAppLimits(updatedRules)
            settingsDataStore.saveUnlockRecords(updatedRecords)
            onResult(
                true,
                "Unlocked ${matching.targetAppName} (+${matching.extraMinutes} min). Reason: ${matching.reason}"
            )
        }
    }

    // Study Flashcards & Progress Dashboard
    fun reviewFlashcard(cardId: String, mastered: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = flashcards.value.map { card ->
                if (card.id == cardId) {
                    card.copy(
                        isMastered = mastered,
                        reviewCount = card.reviewCount + 1,
                        lastReviewedMs = now
                    )
                } else {
                    card
                }
            }
            settingsDataStore.saveFlashcards(updated)

            val stats = progressStats.value
            val (nextStreak, todayKey) = computeUpdatedStreak(stats.lastStudyDayKey, stats.studyStreakDays)
            val updatedDaily = stats.dailyActivityCounts.toMutableMap().apply {
                this[todayKey] = (this[todayKey] ?: 0) + 1
            }
            settingsDataStore.saveProgressStats(
                stats.copy(
                    flashcardsReviewed = stats.flashcardsReviewed + 1,
                    studyStreakDays = nextStreak,
                    lastStudyDayKey = todayKey,
                    dailyActivityCounts = updatedDaily
                )
            )
        }
    }

    fun addCustomFlashcard(
        subject: FlashcardSubject,
        front: String,
        back: String,
        admissionNote: String
    ) {
        val cleanFront = front.trim()
        val cleanBack = back.trim()
        if (cleanFront.isEmpty() || cleanBack.isEmpty()) return
        viewModelScope.launch {
            val newCard = StudyFlashcard(
                id = "fc_custom_${UUID.randomUUID()}",
                subject = subject,
                front = cleanFront,
                back = cleanBack,
                admissionNote = admissionNote.trim(),
                isCustom = true
            )
            settingsDataStore.saveFlashcards(listOf(newCard) + flashcards.value)
        }
    }

    fun generateAiFlashcards(
        subject: FlashcardSubject,
        cardType: FlashcardType,
        topic: String,
        count: Int,
        customInstruction: String,
        onResult: (Boolean, String) -> Unit
    ) {
        if (_isGeneratingFlashcards.value) return
        if (isEffectiveOffline.value) {
            onResult(false, "AI card generation needs internet. Use '+ Add Card' in Offline Mode!")
            return
        }
        viewModelScope.launch {
            _isGeneratingFlashcards.value = true
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null
                val generated = geminiRepository.generateFlashcards(
                    topic = topic,
                    subject = subject,
                    cardType = cardType,
                    count = count,
                    customInstruction = customInstruction,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )
                settingsDataStore.saveFlashcards(generated + flashcards.value)
                onResult(true, "Added ${generated.size} ${cardType.label} cards for \"$topic\" (Unlimited deck)!")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(false, geminiRepository.mapExceptionToFriendlyMessage(e, customKey))
            } finally {
                _isGeneratingFlashcards.value = false
            }
        }
    }

    fun deleteFlashcard(cardId: String) {
        viewModelScope.launch {
            settingsDataStore.saveFlashcards(flashcards.value.filterNot { it.id == cardId })
        }
    }

    fun recordHscTopicStudied() {
        viewModelScope.launch {
            val stats = progressStats.value
            val (nextStreak, todayKey) = computeUpdatedStreak(stats.lastStudyDayKey, stats.studyStreakDays)
            val updatedDaily = stats.dailyActivityCounts.toMutableMap().apply {
                this[todayKey] = (this[todayKey] ?: 0) + 1
            }
            settingsDataStore.saveProgressStats(
                stats.copy(
                    hscTopicsStudied = stats.hscTopicsStudied + 1,
                    studyStreakDays = nextStreak,
                    lastStudyDayKey = todayKey,
                    dailyActivityCounts = updatedDaily
                )
            )
        }
    }

    fun resetProgressStats() {
        viewModelScope.launch {
            settingsDataStore.saveProgressStats(StudyProgressStats())
        }
    }

    private suspend fun recordQuestionActivity(mode: StudyMode, isOffline: Boolean) {
        val stats = progressStats.value
        val (nextStreak, todayKey) = computeUpdatedStreak(stats.lastStudyDayKey, stats.studyStreakDays)
        val updatedSubjects = stats.subjectQuestionCounts.toMutableMap().apply {
            this[mode.name] = (this[mode.name] ?: 0) + 1
        }
        val updatedDaily = stats.dailyActivityCounts.toMutableMap().apply {
            this[todayKey] = (this[todayKey] ?: 0) + 1
        }
        settingsDataStore.saveProgressStats(
            stats.copy(
                totalQuestionsAsked = stats.totalQuestionsAsked + 1,
                offlineQuestionsAnswered = stats.offlineQuestionsAnswered + if (isOffline) 1 else 0,
                studyStreakDays = nextStreak,
                lastStudyDayKey = todayKey,
                subjectQuestionCounts = updatedSubjects,
                dailyActivityCounts = updatedDaily
            )
        )
    }

    private fun computeUpdatedStreak(lastDayKey: String, currentStreak: Int): Pair<Int, String> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val todayKey = fmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = fmt.format(cal.time)

        val nextStreak = when (lastDayKey) {
            todayKey -> currentStreak.coerceAtLeast(1)
            yesterdayKey -> currentStreak + 1
            else -> 1
        }
        return nextStreak to todayKey
    }

    // Study Timer Controls
    fun selectStudyTimerSubject(subject: FlashcardSubject) {
        _studyTimerSubject.value = subject
    }

    fun setStudyTimerPreset(minutes: Int, isBreak: Boolean) {
        val safeMinutes = minutes.coerceIn(1, 180)
        _isStudyTimerRunning.value = false
        _isStudyTimerBreakMode.value = isBreak
        _studyTimerTotalSeconds.value = safeMinutes * 60
        _studyTimerRemainingSeconds.value = safeMinutes * 60
    }

    fun toggleStudyTimerRunning() {
        if (_studyTimerRemainingSeconds.value <= 0) {
            _studyTimerRemainingSeconds.value = _studyTimerTotalSeconds.value
        }
        _isStudyTimerRunning.value = !_isStudyTimerRunning.value
    }

    fun resetStudyTimer() {
        _isStudyTimerRunning.value = false
        _studyTimerRemainingSeconds.value = _studyTimerTotalSeconds.value
    }

    fun completeStudyTimerEarly() {
        val elapsedSeconds = (_studyTimerTotalSeconds.value - _studyTimerRemainingSeconds.value).coerceAtLeast(0)
        val elapsedMinutes = (elapsedSeconds / 60).coerceAtLeast(1)
        _isStudyTimerRunning.value = false
        _studyTimerRemainingSeconds.value = _studyTimerTotalSeconds.value
        if (!_isStudyTimerBreakMode.value) {
            viewModelScope.launch {
                recordCompletedStudyTimer(_studyTimerSubject.value, elapsedMinutes)
                _bannerMessage.value = "Logged $elapsedMinutes min ${_studyTimerSubject.value.label} study session!"
            }
        }
    }

    private suspend fun recordCompletedStudyTimer(subject: FlashcardSubject, minutes: Int) {
        val session = StudyTimerSession(
            id = "timer_${UUID.randomUUID()}",
            subject = subject,
            durationMinutes = minutes,
            completedAtMs = System.currentTimeMillis()
        )
        settingsDataStore.saveStudyTimerSessions(listOf(session) + studyTimerSessions.value)

        val stats = progressStats.value
        val (nextStreak, todayKey) = computeUpdatedStreak(stats.lastStudyDayKey, stats.studyStreakDays)
        val updatedDaily = stats.dailyActivityCounts.toMutableMap().apply {
            this[todayKey] = (this[todayKey] ?: 0) + 1
        }
        settingsDataStore.saveProgressStats(
            stats.copy(
                studyStreakDays = nextStreak,
                lastStudyDayKey = todayKey,
                dailyActivityCounts = updatedDaily
            )
        )
    }

    // Gemini Notebook & PDF Podcast Controls
    fun createNotebook(title: String) {
        val clean = title.trim().ifBlank { "Study Notebook" }
        val newNb = GeminiNotebook(
            id = "nb_${UUID.randomUUID()}",
            title = clean
        )
        _activeNotebookId.value = newNb.id
        viewModelScope.launch {
            settingsDataStore.saveNotebooks(listOf(newNb) + notebooks.value)
        }
    }

    fun selectNotebook(notebookId: String) {
        _activeNotebookId.value = notebookId
    }

    fun deleteNotebook(notebookId: String) {
        val target = notebooks.value.firstOrNull { it.id == notebookId } ?: return
        target.sources.forEach { AttachmentReader.deleteStoredNotebookSourceFile(it) }
        val remaining = notebooks.value.filterNot { it.id == notebookId }
        if (_activeNotebookId.value == notebookId) {
            _activeNotebookId.value = remaining.firstOrNull()?.id
        }
        viewModelScope.launch {
            settingsDataStore.saveNotebooks(remaining)
        }
    }

    fun addNotebookSource(notebookId: String, source: NotebookSource) {
        viewModelScope.launch {
            val updated = notebooks.value.map { nb ->
                if (nb.id == notebookId) {
                    nb.copy(
                        sources = nb.sources + source,
                        updatedAtMs = System.currentTimeMillis()
                    )
                } else nb
            }
            settingsDataStore.saveNotebooks(updated)
        }
    }

    fun removeNotebookSource(notebookId: String, source: NotebookSource) {
        AttachmentReader.deleteStoredNotebookSourceFile(source)
        viewModelScope.launch {
            val updated = notebooks.value.map { nb ->
                if (nb.id == notebookId) {
                    nb.copy(
                        sources = nb.sources.filterNot { it.id == source.id },
                        updatedAtMs = System.currentTimeMillis()
                    )
                } else nb
            }
            settingsDataStore.saveNotebooks(updated)
        }
    }

    fun generateNotebookPodcast(notebookId: String) {
        if (_isGeneratingPodcast.value) return
        val nb = notebooks.value.firstOrNull { it.id == notebookId } ?: return
        if (nb.sources.isEmpty()) {
            _bannerMessage.value = "Upload a PDF or note source first to generate a Podcast."
            return
        }

        viewModelScope.launch {
            _isGeneratingPodcast.value = true
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                val offline = currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()
                if (offline) {
                    val sourceTitles = nb.sources.joinToString(", ") { it.title }
                    val noteSummary = nb.sources
                        .mapNotNull { it.textContent.trim().takeIf { t -> t.isNotEmpty() } }
                        .joinToString(" ")
                        .take(350)
                    val offlineTurns = listOf(
                        PodcastTurn(
                            speaker = "HOST_A",
                            speakerName = "Ayesha (Host)",
                            text = "Welcome to Study Update Notebook Podcast! Today we are reviewing ${nb.title}, using sources: $sourceTitles."
                        ),
                        PodcastTurn(
                            speaker = "HOST_B",
                            speakerName = "Rafi (Co-Host)",
                            text = if (noteSummary.isNotBlank()) {
                                "Here is the key highlight from your saved notes: $noteSummary"
                            } else {
                                "You are currently in Offline Mode, so PDF binary parsing is paused. Connect to internet and regenerate to get a full deep-dive explanation of every page in your PDF!"
                            }
                        ),
                        PodcastTurn(
                            speaker = "HOST_A",
                            speakerName = "Ayesha (Host)",
                            text = "Even offline, you can listen to this briefing, review saved Study Guides, and test yourself with HSC Flashcards."
                        )
                    )
                    updateNotebookById(notebookId) { cur ->
                        cur.copy(
                            podcastTitle = "${nb.title} (Offline Briefing)",
                            podcastTurns = offlineTurns,
                            updatedAtMs = System.currentTimeMillis()
                        )
                    }
                    return@launch
                }

                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null
                val attachments = AttachmentReader.loadNotebookAttachments(nb.sources)
                val (epTitle, turns) = geminiRepository.generateNotebookPodcast(
                    notebookTitle = nb.title,
                    attachments = attachments,
                    languagePreference = currentSettings.languagePreference,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )
                updateNotebookById(notebookId) { cur ->
                    cur.copy(
                        podcastTitle = epTitle,
                        podcastTurns = turns,
                        updatedAtMs = System.currentTimeMillis()
                    )
                }
                _bannerMessage.value = "Podcast ready! Press Play to listen to your PDF."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
            } finally {
                _isGeneratingPodcast.value = false
            }
        }
    }

    fun generateNotebookArtifact(notebookId: String, type: NotebookArtifactType) {
        if (_isGeneratingNotebookArtifact.value != null) return
        val nb = notebooks.value.firstOrNull { it.id == notebookId } ?: return
        if (nb.sources.isEmpty()) return

        viewModelScope.launch {
            _isGeneratingNotebookArtifact.value = type
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                if (currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()) {
                    _bannerMessage.value = "Connect to the internet or disable Offline Mode to analyze PDFs with Gemini Notebook."
                    return@launch
                }
                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null
                val attachments = AttachmentReader.loadNotebookAttachments(nb.sources)
                val md = geminiRepository.generateNotebookArtifact(
                    artifactType = type,
                    notebookTitle = nb.title,
                    attachments = attachments,
                    languagePreference = currentSettings.languagePreference,
                    useCustomKey = currentSettings.useCustomApiKey,
                    customApiKey = customKey
                )
                updateNotebookById(notebookId) { cur ->
                    when (type) {
                        NotebookArtifactType.STUDY_GUIDE -> cur.copy(studyGuideMarkdown = md, updatedAtMs = System.currentTimeMillis())
                        NotebookArtifactType.EXAM_FAQ -> cur.copy(faqMarkdown = md, updatedAtMs = System.currentTimeMillis())
                        NotebookArtifactType.PRACTICE_QUIZ -> cur.copy(quizMarkdown = md, updatedAtMs = System.currentTimeMillis())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
            } finally {
                _isGeneratingNotebookArtifact.value = null
            }
        }
    }

    fun sendNotebookChat(notebookId: String, rawQuestion: String) {
        if (_isStreamingNotebookChat.value) return
        val clean = rawQuestion.trim()
        if (clean.isEmpty()) return
        val nb = notebooks.value.firstOrNull { it.id == notebookId } ?: return

        notebookChatJob = viewModelScope.launch {
            var customKey: String? = null
            try {
                val currentSettings = settings.value
                val userMsg = NotebookMessage(
                    id = "nbm_${UUID.randomUUID()}",
                    role = MessageRole.USER,
                    content = clean
                )
                updateNotebookById(notebookId) { cur ->
                    cur.copy(messages = cur.messages + userMsg, updatedAtMs = System.currentTimeMillis())
                }

                if (currentSettings.offlineModeEnabled || !networkMonitor.isCurrentlyOnline()) {
                    val offlineReply = OfflineStudyEngine.buildOfflineReply(clean, StudyMode.GENERAL, flashcards.value)
                    val modelMsg = NotebookMessage(
                        id = "nbm_${UUID.randomUUID()}",
                        role = MessageRole.MODEL,
                        content = offlineReply
                    )
                    updateNotebookById(notebookId) { cur ->
                        cur.copy(messages = cur.messages + modelMsg, updatedAtMs = System.currentTimeMillis())
                    }
                    return@launch
                }

                customKey = if (currentSettings.useCustomApiKey) {
                    settingsDataStore.getDecryptedCustomApiKey()
                } else null
                val attachments = AttachmentReader.loadNotebookAttachments(nb.sources)

                _isStreamingNotebookChat.value = true
                _streamingNotebookChatText.value = ""
                val acc = StringBuilder()
                geminiRepository.streamNotebookReply(
                    notebookTitle = nb.title,
                    history = nb.messages,
                    question = clean,
                    attachments = attachments,
                    settings = currentSettings,
                    customApiKey = customKey
                ).collect { chunk ->
                    acc.append(chunk)
                    _streamingNotebookChatText.value = SecretSanitizer.redact(acc.toString(), customKey)
                }

                val finalReply = SecretSanitizer.redact(acc.toString().trim(), customKey)
                if (finalReply.isNotEmpty()) {
                    val modelMsg = NotebookMessage(
                        id = "nbm_${UUID.randomUUID()}",
                        role = MessageRole.MODEL,
                        content = finalReply
                    )
                    updateNotebookById(notebookId) { cur ->
                        cur.copy(messages = cur.messages + modelMsg, updatedAtMs = System.currentTimeMillis())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errMsg = NotebookMessage(
                    id = "nbm_${UUID.randomUUID()}",
                    role = MessageRole.MODEL,
                    content = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                )
                updateNotebookById(notebookId) { cur ->
                    cur.copy(messages = cur.messages + errMsg, updatedAtMs = System.currentTimeMillis())
                }
            } finally {
                _isStreamingNotebookChat.value = false
                _streamingNotebookChatText.value = null
                notebookChatJob = null
            }
        }
    }

    fun stopNotebookChat() {
        notebookChatJob?.cancel()
        notebookChatJob = null
        _isStreamingNotebookChat.value = false
        _streamingNotebookChatText.value = null
    }

    private suspend fun updateNotebookById(
        notebookId: String,
        transform: (GeminiNotebook) -> GeminiNotebook
    ) {
        val next = notebooks.value.map { nb -> if (nb.id == notebookId) transform(nb) else nb }
        settingsDataStore.saveNotebooks(next)
    }

    // Settings mutations
    fun updateOfflineMode(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateOfflineMode(enabled) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.updateThemeMode(mode) }
    }

    fun updateLanguagePreference(pref: LanguagePreference) {
        viewModelScope.launch { settingsDataStore.updateLanguagePreference(pref) }
    }

    fun updateAcademicProfile(profile: AcademicProfile) {
        viewModelScope.launch { settingsDataStore.updateAcademicProfile(profile) }
    }

    fun updateTemperature(value: Float) {
        viewModelScope.launch { settingsDataStore.updateTemperature(value) }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoSpeak(enabled) }
    }

    fun updateVoiceInputLang(lang: String) {
        viewModelScope.launch { settingsDataStore.updateVoiceInputLang(lang) }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch { settingsDataStore.updateSpeechRate(rate) }
    }

    fun selectAiVoice(preset: AiVoicePreset) {
        viewModelScope.launch { settingsDataStore.updateAiVoice(preset.geminiVoiceId, preset.defaultPitch) }
    }

    fun updateTtsPitch(pitch: Float) {
        viewModelScope.launch { settingsDataStore.updateTtsPitch(pitch) }
    }

    fun updateModelId(modelId: String) {
        viewModelScope.launch { settingsDataStore.updateModelId(modelId) }
    }

    fun updateUseCustomApiKey(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateUseCustomApiKey(enabled)
            _connectionTestState.value = ConnectionTestState.Idle
        }
    }

    fun saveCustomApiKey(rawKey: String, onSaved: (Boolean, String) -> Unit) {
        val trimmed = rawKey.trim()
        if (!SecretSanitizer.looksLikeValidApiKey(trimmed)) {
            onSaved(false, "Invalid API key format. Keys should be 24+ characters without spaces.")
            return
        }
        viewModelScope.launch {
            try {
                settingsDataStore.saveCustomApiKey(trimmed)
                _connectionTestState.value = ConnectionTestState.Idle
                onSaved(true, "API key encrypted with Android Keystore & saved securely.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to encrypt API key", e)
                onSaved(false, "Could not encrypt API key on this device.")
            }
        }
    }

    fun clearCustomApiKey() {
        viewModelScope.launch {
            settingsDataStore.clearCustomApiKey()
            _connectionTestState.value = ConnectionTestState.Idle
            _bannerMessage.value = "Personal API key removed. Switched back to Owner Default / Firebase AI Logic."
        }
    }

    fun testApiConnection(overrideCandidateKey: String? = null) {
        viewModelScope.launch {
            _connectionTestState.value = ConnectionTestState.Testing
            val currentSettings = settings.value
            val useCustom = currentSettings.useCustomApiKey || !overrideCandidateKey.isNullOrBlank()
            val key = overrideCandidateKey?.trim()?.takeIf { it.isNotEmpty() }
                ?: if (currentSettings.useCustomApiKey) settingsDataStore.getDecryptedCustomApiKey() else null

            val result = geminiRepository.testConnection(
                modelId = currentSettings.selectedModelId,
                useCustomKey = useCustom,
                customApiKey = key
            )
            _connectionTestState.value = result
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveVoiceJob?.cancel()
    }

    private companion object {
        const val TAG = "MainViewModel"
    }
}
