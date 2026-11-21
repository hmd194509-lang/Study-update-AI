package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiRepository
import com.example.data.local.SettingsDataStore
import com.example.data.model.AcademicProfile
import com.example.data.model.AppSettings
import com.example.data.model.AttachmentPayload
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.ConnectionTestState
import com.example.data.model.LanguagePreference
import com.example.data.model.MessageRole
import com.example.data.model.StudyMode
import com.example.data.model.ThemeMode
import com.example.data.model.UiState
import com.example.data.model.displayName
import com.example.data.model.idValue
import com.example.data.remote.ChatRepository
import com.example.security.SecretSanitizer
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val chatRepository: ChatRepository,
    private val geminiRepository: GeminiRepository,
    private val settingsDataStore: SettingsDataStore,
    private val currentUserId: String
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppSettings()
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

    val messagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeSessionId
            .flatMapLatest { sessionId ->
                if (sessionId.isNullOrBlank()) {
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

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _autoSpeakEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val autoSpeakEvents: SharedFlow<String> = _autoSpeakEvents.asSharedFlow()

    private var activeGenerationJob: Job? = null

    init {
        viewModelScope.launch {
            sessionsState.collect { state ->
                if (state is UiState.Success && _activeSessionId.value == null && state.data.isNotEmpty()) {
                    val first = state.data.first()
                    _activeSessionId.value = first.id
                    _activeMode.value = StudyMode.fromRaw(first.mode)
                }
            }
        }
    }

    fun selectSession(session: ChatSession) {
        if (_isStreaming.value) stopStreaming()
        _activeSessionId.value = session.id
        _activeMode.value = StudyMode.fromRaw(session.mode)
    }

    fun startNewChat() {
        if (_isStreaming.value) stopStreaming()
        _activeSessionId.value = null
        _streamingMessageText.value = null
        _pendingAttachments.value = emptyList()
    }

    fun selectMode(mode: StudyMode) {
        _activeMode.value = mode
        val sid = _activeSessionId.value
        if (!sid.isNullOrBlank()) {
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

                // Redact any accidental API key pasted by the user before storing or sending
                val safePrompt = SecretSanitizer.redact(prompt, customKey)
                val attachmentNames = attachments.map { it.displayName }

                var sessionId = _activeSessionId.value
                val existingMessages = (messagesState.value as? UiState.Success)?.data.orEmpty()

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

                val effectiveUserContent = safePrompt.ifBlank {
                    "Analyze attached: ${attachmentNames.joinToString(", ")}"
                }

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

                _isStreaming.value = true
                _streamingMessageText.value = ""
                val accumulator = StringBuilder()

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

                val finalReply = SecretSanitizer.redact(accumulator.toString().trim(), customKey)
                if (finalReply.isNotEmpty()) {
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
                    if (currentSettings.autoSpeak) {
                        _autoSpeakEvents.tryEmit(finalReply)
                    }
                }
            } catch (e: CancellationException) {
                val partial = _streamingMessageText.value?.trim().orEmpty()
                val sid = _activeSessionId.value
                if (partial.isNotEmpty() && !sid.isNullOrBlank()) {
                    // Save partial reply gracefully if user stopped generation
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
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI generation failed", e)
                val friendlyError = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
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
        val sid = _activeSessionId.value ?: return
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
                    chatRepository.deleteMessage(sid, last.id)
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val friendlyError = geminiRepository.mapExceptionToFriendlyMessage(e, customKey)
                chatRepository.addMessage(
                    sessionId = sid,
                    role = MessageRole.MODEL,
                    content = friendlyError,
                    mode = _activeMode.value,
                    errorFlag = true
                )
            } finally {
                _isStreaming.value = false
                _streamingMessageText.value = null
                activeGenerationJob = null
            }
        }
    }

    // Settings mutations
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
            _bannerMessage.value = "Stored custom API key removed. Switched back to Firebase AI Logic."
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

    private companion object {
        const val TAG = "MainViewModel"
    }
}
