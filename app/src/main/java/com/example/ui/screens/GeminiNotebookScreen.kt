package com.example.ui.screens

import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AppSettings
import com.example.data.model.GeminiNotebook
import com.example.data.model.MessageRole
import com.example.data.model.NotebookArtifactType
import com.example.data.model.NotebookSource
import com.example.data.model.NotebookSourceType
import com.example.data.model.PodcastTurn
import com.example.ui.components.MarkdownMessageView
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import com.example.util.AttachmentReader
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiNotebookScreen(
    notebooks: List<GeminiNotebook>,
    activeNotebook: GeminiNotebook?,
    settings: AppSettings,
    isOffline: Boolean,
    isGeneratingPodcast: Boolean,
    isGeneratingArtifact: NotebookArtifactType?,
    isStreamingChat: Boolean,
    streamingChatText: String?,
    onBack: () -> Unit,
    onCreateNotebook: (String) -> Unit,
    onSelectNotebook: (String) -> Unit,
    onDeleteNotebook: (String) -> Unit,
    onAddNotebookSource: (String, NotebookSource) -> Unit,
    onRemoveNotebookSource: (String, NotebookSource) -> Unit,
    onGeneratePodcast: (String) -> Unit,
    onGenerateArtifact: (String, NotebookArtifactType) -> Unit,
    onSendNotebookChat: (String, String) -> Unit,
    onStopNotebookChat: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewNotebookDialog by remember { mutableStateOf(false) }
    var newNotebookTitle by remember { mutableStateOf("") }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteBodyInput by remember { mutableStateOf("") }
    var sourceStatusMsg by remember { mutableStateOf<String?>(null) }

    // Dual-host Podcast Audio Player state powered by Android TTS
    var ttsReady by remember { mutableStateOf(false) }
    var isPlayingPodcast by remember { mutableStateOf(false) }
    var currentTurnIndex by remember { mutableIntStateOf(0) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.speechRate.coerceIn(0.8f, 1.5f)) }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var speakTurnAction by remember { mutableStateOf<(Int) -> Unit>({}) }

    val ttsEngine = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
        engine
    }

    DisposableEffect(ttsEngine) {
        ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.removePrefix("podcast_turn_")?.toIntOrNull() ?: return
                mainHandler.post {
                    val turns = activeNotebook?.podcastTurns.orEmpty()
                    if (isPlayingPodcast && idx + 1 < turns.size) {
                        currentTurnIndex = idx + 1
                        speakTurnAction(idx + 1)
                    } else {
                        isPlayingPodcast = false
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isPlayingPodcast = false }
            }
        })
        onDispose {
            isPlayingPodcast = false
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    val speakTurn: (Int) -> Unit = { index ->
        val turns = activeNotebook?.podcastTurns.orEmpty()
        val turn = turns.getOrNull(index)
        if (ttsReady && turn != null) {
            val hasBengali = turn.text.any { it in '\u0980'..'\u09FF' }
            ttsEngine.language = if (hasBengali) Locale("bn", "BD") else Locale.US
            ttsEngine.setSpeechRate(playbackSpeed)
            // Distinct voices for Host A vs Co-Host B
            val hostPitch = if (turn.speaker == "HOST_A") {
                (settings.ttsPitch + 0.08f).coerceIn(0.7f, 1.4f)
            } else {
                (settings.ttsPitch - 0.12f).coerceIn(0.7f, 1.4f)
            }
            ttsEngine.setPitch(hostPitch)
            ttsEngine.speak(
                turn.text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "podcast_turn_$index"
            )
        }
    }
    speakTurnAction = speakTurn

    // Stop podcast when switching notebooks
    LaunchedEffect(activeNotebook?.id) {
        isPlayingPodcast = false
        currentTurnIndex = 0
        ttsEngine.stop()
    }

    // Jump directly to Podcast tab when podcast generation completes
    val turnCount = activeNotebook?.podcastTurns?.size ?: 0
    LaunchedEffect(turnCount) {
        if (turnCount > 0 && isGeneratingPodcast) {
            selectedTab = 1
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && activeNotebook != null) {
            scope.launch {
                AttachmentReader.savePdfForNotebook(context, uri)
                    .onSuccess { source ->
                        onAddNotebookSource(activeNotebook.id, source)
                        sourceStatusMsg = "Added PDF: ${source.title}"
                    }
                    .onFailure { err ->
                        sourceStatusMsg = err.localizedMessage ?: "Could not load PDF."
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Study AI Notebook",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        isPlayingPodcast = false
                        ttsEngine.stop()
                        onBack()
                    },
                    modifier = Modifier.testTag("notebook_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to chat"
                    )
                }
            },
            actions = {
                OutlinedButton(
                    onClick = { showNewNotebookDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .testTag("new_notebook_button")
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        // Notebook selector row
        if (notebooks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                notebooks.forEach { nb ->
                    val selected = nb.id == activeNotebook?.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectNotebook(nb.id) },
                        label = {
                            Text(
                                text = "${nb.title} (${nb.sources.size})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = if (notebooks.size > 1 && selected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete notebook",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { onDeleteNotebook(nb.id) }
                                )
                            }
                        } else null
                    )
                }
            }
        }

        if (activeNotebook == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.widthIn(max = 500.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Study AI Notebook & 1GB PDF Podcast",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload any PDF chapter up to 1 GB, lecture slide, or study notes. Two AI hosts (Ayesha & Rafi) discuss and explain the entire PDF aloud, plus Study Guides, MCQ Quizzes & Q&A.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showNewNotebookDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("create_first_notebook_button")
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Notebook")
                        }
                    }
                }
            }
        } else {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sources (${activeNotebook.sources.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Podcast & Studio") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Notebook Chat") }
                )
            }

            when (selectedTab) {
                0 -> {
                    NotebookSourcesTab(
                        notebook = activeNotebook,
                        isOffline = isOffline,
                        isGeneratingPodcast = isGeneratingPodcast,
                        statusMsg = sourceStatusMsg,
                        onUploadPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        onOpenAddNote = { showAddNoteDialog = true },
                        onRemoveSource = { src -> onRemoveNotebookSource(activeNotebook.id, src) },
                        onGeneratePodcast = {
                            selectedTab = 1
                            onGeneratePodcast(activeNotebook.id)
                        }
                    )
                }

                1 -> {
                    NotebookPodcastStudioTab(
                        notebook = activeNotebook,
                        isOffline = isOffline,
                        isGeneratingPodcast = isGeneratingPodcast,
                        isGeneratingArtifact = isGeneratingArtifact,
                        ttsReady = ttsReady,
                        isPlayingPodcast = isPlayingPodcast,
                        currentTurnIndex = currentTurnIndex,
                        playbackSpeed = playbackSpeed,
                        onGeneratePodcast = { onGeneratePodcast(activeNotebook.id) },
                        onTogglePlayPause = {
                            val turns = activeNotebook.podcastTurns
                            if (turns.isEmpty() || !ttsReady) return@NotebookPodcastStudioTab
                            if (isPlayingPodcast) {
                                isPlayingPodcast = false
                                ttsEngine.stop()
                            } else {
                                val safeIdx = currentTurnIndex.coerceIn(0, turns.lastIndex)
                                currentTurnIndex = safeIdx
                                isPlayingPodcast = true
                                speakTurn(safeIdx)
                            }
                        },
                        onStopPodcast = {
                            isPlayingPodcast = false
                            currentTurnIndex = 0
                            ttsEngine.stop()
                        },
                        onSelectTurn = { idx ->
                            currentTurnIndex = idx
                            isPlayingPodcast = true
                            speakTurn(idx)
                        },
                        onSkipPrev = {
                            val nextIdx = (currentTurnIndex - 1).coerceAtLeast(0)
                            currentTurnIndex = nextIdx
                            if (isPlayingPodcast) speakTurn(nextIdx)
                        },
                        onSkipNext = {
                            val last = activeNotebook.podcastTurns.lastIndex
                            if (last >= 0) {
                                val nextIdx = (currentTurnIndex + 1).coerceAtMost(last)
                                currentTurnIndex = nextIdx
                                if (isPlayingPodcast) speakTurn(nextIdx)
                            }
                        },
                        onChangeSpeed = { nextSpeed ->
                            playbackSpeed = nextSpeed
                            if (isPlayingPodcast) speakTurn(currentTurnIndex)
                        },
                        onCopyPodcastScript = {
                            val script = activeNotebook.podcastTurns.joinToString("\n\n") {
                                "${it.speakerName}: ${it.text}"
                            }
                            clipboard.setText(AnnotatedString(script))
                        },
                        onGenerateArtifact = { type ->
                            onGenerateArtifact(activeNotebook.id, type)
                        },
                        onCopyMarkdown = { md ->
                            clipboard.setText(AnnotatedString(md))
                        }
                    )
                }

                2 -> {
                    NotebookChatTab(
                        notebook = activeNotebook,
                        isStreaming = isStreamingChat,
                        streamingText = streamingChatText,
                        onSendMessage = { question ->
                            onSendNotebookChat(activeNotebook.id, question)
                        },
                        onStopStreaming = onStopNotebookChat,
                        onSwitchToSources = { selectedTab = 0 }
                    )
                }
            }
        }
    }

    if (showNewNotebookDialog) {
        AlertDialog(
            onDismissRequest = { showNewNotebookDialog = false },
            title = { Text("New Gemini Notebook") },
            text = {
                OutlinedTextField(
                    value = newNotebookTitle,
                    onValueChange = { newNotebookTitle = it },
                    label = { Text("Notebook Title (e.g., HSC Physics Vector)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newNotebookTitle.trim().ifBlank { "Study Notebook" }
                        onCreateNotebook(title)
                        newNotebookTitle = ""
                        showNewNotebookDialog = false
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewNotebookDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddNoteDialog && activeNotebook != null) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("Add Study Note to Notebook") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteBodyInput,
                        onValueChange = { noteBodyInput = it },
                        label = { Text("Paste chapter notes, formulas, or text") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = noteBodyInput.trim().isNotEmpty(),
                    onClick = {
                        val title = noteTitleInput.trim().ifBlank { "Lecture Note" }
                        val body = noteBodyInput.trim()
                        onAddNotebookSource(
                            activeNotebook.id,
                            NotebookSource(
                                id = "note_${System.currentTimeMillis()}",
                                title = title,
                                type = NotebookSourceType.NOTE,
                                sizeBytes = body.length.toLong(),
                                textContent = body
                            )
                        )
                        noteTitleInput = ""
                        noteBodyInput = ""
                        showAddNoteDialog = false
                    }
                ) { Text("Save Source") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotebookSourcesTab(
    notebook: GeminiNotebook,
    isOffline: Boolean,
    isGeneratingPodcast: Boolean,
    statusMsg: String?,
    onUploadPdf: () -> Unit,
    onOpenAddNote: () -> Unit,
    onRemoveSource: (NotebookSource) -> Unit,
    onGeneratePodcast: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 960.dp)
    ) {
        // Hero Upload PDF -> Podcast Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary, CyanTertiary)))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Upload PDF (Up to 1 GB) & Listen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "সর্বোচ্চ ১ জিবি পিডিএফ আপলোড করুন — দুইজন AI হোস্ট কথা বলে পুরো পিডিএফ বুঝিয়ে দিবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onUploadPdf,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_pdf_notebook_button")
                    ) {
                        Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload PDF")
                    }
                    OutlinedButton(
                        onClick = onOpenAddNote,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_note_notebook_button")
                    ) {
                        Icon(Icons.Default.NoteAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Note")
                    }
                }

                if (!statusMsg.isNullOrBlank()) {
                    Text(
                        text = statusMsg,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (notebook.sources.isNotEmpty()) {
                    Button(
                        onClick = onGeneratePodcast,
                        enabled = !isGeneratingPodcast,
                        colors = ButtonDefaults.buttonColors(containerColor = VioletSecondary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_pdf_podcast_cta")
                    ) {
                        if (isGeneratingPodcast) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating 2-Host Podcast from PDF...")
                        } else {
                            Icon(Icons.Default.GraphicEq, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isOffline) "Create Offline Podcast Overview"
                                else "Generate Audio Podcast (পডকাস্ট শুনুন)"
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "UPLOADED SOURCES (${notebook.sources.size})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (notebook.sources.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No PDFs or notes in \"${notebook.title}\" yet. Tap 'Upload PDF' above to add a textbook chapter, lecture sheet, or admission guide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            notebook.sources.forEach { source ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = if (source.type == NotebookSourceType.PDF) {
                                Icons.Default.PictureAsPdf
                            } else {
                                Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = if (source.type == NotebookSourceType.PDF) MaterialTheme.colorScheme.error else IndigoPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val sizeLabel = if (source.type == NotebookSourceType.PDF) {
                                val kb = (source.sizeBytes / 1024L).coerceAtLeast(1L)
                                if (kb >= 1024) "${"%.1f".format(kb / 1024f)} MB • PDF" else "$kb KB • PDF"
                            } else {
                                "${source.textContent.length} chars • Note"
                            }
                            Text(
                                text = sizeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onRemoveSource(source) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove source",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotebookPodcastStudioTab(
    notebook: GeminiNotebook,
    isOffline: Boolean,
    isGeneratingPodcast: Boolean,
    isGeneratingArtifact: NotebookArtifactType?,
    ttsReady: Boolean,
    isPlayingPodcast: Boolean,
    currentTurnIndex: Int,
    playbackSpeed: Float,
    onGeneratePodcast: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopPodcast: () -> Unit,
    onSelectTurn: (Int) -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onCopyPodcastScript: () -> Unit,
    onGenerateArtifact: (NotebookArtifactType) -> Unit,
    onCopyMarkdown: (String) -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "podcast_wave")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "pulse"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 960.dp)
    ) {
        // Audio Overview Podcast Player Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .scale(if (isPlayingPodcast) pulseScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(IndigoPrimary, VioletSecondary, CyanTertiary)
                                    )
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = notebook.podcastTitle.ifBlank { "PDF Audio Podcast Overview" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (notebook.podcastTurns.isEmpty()) {
                                    "2-Host conversation explaining your PDF"
                                } else {
                                    "Ayesha & Rafi • ${notebook.podcastTurns.size} dialogue turns"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (notebook.podcastTurns.isNotEmpty()) {
                        IconButton(onClick = onCopyPodcastScript) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy script")
                        }
                    }
                }

                if (notebook.sources.isEmpty()) {
                    Text(
                        text = "Upload a PDF or add notes in the Sources tab first to generate a Podcast.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Button(
                        onClick = onGeneratePodcast,
                        enabled = !isGeneratingPodcast,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_generate_podcast_button")
                    ) {
                        if (isGeneratingPodcast) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating Podcast Dialogue...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    notebook.podcastTurns.isNotEmpty() -> "Regenerate Audio Podcast"
                                    isOffline -> "Create Offline Podcast Script"
                                    else -> "Generate Podcast from PDF"
                                }
                            )
                        }
                    }
                }

                if (notebook.podcastTurns.isNotEmpty()) {
                    val safeIdx = currentTurnIndex.coerceIn(0, notebook.podcastTurns.lastIndex)
                    val progress = (safeIdx + 1).toFloat() / notebook.podcastTurns.size.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Turn ${safeIdx + 1} / ${notebook.podcastTurns.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0.9f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                                FilterChip(
                                    selected = kotlin.math.abs(playbackSpeed - spd) < 0.05f,
                                    onClick = { onChangeSpeed(spd) },
                                    label = { Text("${spd}x") }
                                )
                            }
                        }
                    }

                    // Audio Transport Bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = onSkipPrev,
                            enabled = safeIdx > 0,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous turn")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onTogglePlayPause,
                            enabled = ttsReady,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(IndigoPrimary, VioletSecondary)))
                                .testTag("podcast_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlayingPodcast) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingPodcast) "Pause podcast" else "Play podcast",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSkipNext,
                            enabled = safeIdx < notebook.podcastTurns.lastIndex,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next turn")
                        }
                        IconButton(
                            onClick = onStopPodcast,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop podcast")
                        }
                    }

                    // Interactive Transcript (Tap any dialogue card to listen from there)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        notebook.podcastTurns.forEachIndexed { idx, turn ->
                            val isActive = idx == safeIdx && isPlayingPodcast
                            val isHostA = turn.speaker == "HOST_A"
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (isActive) {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(14.dp)
                                            )
                                        } else Modifier
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTurn(idx) }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = turn.speakerName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHostA) IndigoPrimary else VioletSecondary
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = turn.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // NotebookLM Studio Artifact Generators
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Notebook Studio Guides",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotebookArtifactType.entries.forEach { type ->
                        val busy = isGeneratingArtifact == type
                        OutlinedButton(
                            onClick = { onGenerateArtifact(type) },
                            enabled = notebook.sources.isNotEmpty() && isGeneratingArtifact == null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("artifact_btn_${type.name}")
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = when (type) {
                                        NotebookArtifactType.STUDY_GUIDE -> Icons.Default.AutoStories
                                        NotebookArtifactType.EXAM_FAQ -> Icons.Default.School
                                        NotebookArtifactType.PRACTICE_QUIZ -> Icons.Default.Quiz
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(type.label)
                        }
                    }
                }
            }
        }

        if (notebook.studyGuideMarkdown.isNotBlank()) {
            NotebookArtifactCard(
                title = "Study Guide (${notebook.title})",
                markdown = notebook.studyGuideMarkdown,
                accentColor = IndigoPrimary,
                onCopy = { onCopyMarkdown(notebook.studyGuideMarkdown) }
            )
        }
        if (notebook.faqMarkdown.isNotBlank()) {
            NotebookArtifactCard(
                title = "Exam FAQ & Key Formulas",
                markdown = notebook.faqMarkdown,
                accentColor = EmeraldSuccess,
                onCopy = { onCopyMarkdown(notebook.faqMarkdown) }
            )
        }
        if (notebook.quizMarkdown.isNotBlank()) {
            NotebookArtifactCard(
                title = "MCQ Practice Quiz",
                markdown = notebook.quizMarkdown,
                accentColor = VioletSecondary,
                onCopy = { onCopyMarkdown(notebook.quizMarkdown) }
            )
        }
    }
}

@Composable
private fun NotebookArtifactCard(
    title: String,
    markdown: String,
    accentColor: Color,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy guide")
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            MarkdownMessageView(
                content = markdown,
                textColor = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotebookChatTab(
    notebook: GeminiNotebook,
    isStreaming: Boolean,
    streamingText: String?,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit,
    onSwitchToSources: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (notebook.sources.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Add a PDF or Note Source First",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Notebook Chat answers questions directly from the PDFs and notes inside this notebook.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onSwitchToSources) {
                                Text("Go to Sources")
                            }
                        }
                    }
                }
            } else if (notebook.messages.isEmpty() && !isStreaming) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ask Study AI grounded in ${notebook.sources.size} source(s):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Summarize this PDF in easy Bangla + English",
                                "Extract all formulas and key definitions",
                                "What HSC / Admission questions come from this?"
                            ).forEach { prompt ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onSendMessage(prompt) },
                                    label = { Text(prompt) }
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(notebook.messages, key = { _, msg -> msg.id }) { _, msg ->
                val isUser = msg.role == MessageRole.USER
                Row(
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        modifier = Modifier.widthIn(max = 340.dp)
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            if (isUser) {
                                Text(
                                    text = msg.content,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                MarkdownMessageView(
                                    content = msg.content,
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (isStreaming && !streamingText.isNullOrBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            MarkdownMessageView(
                                content = "$streamingText ▍",
                                textColor = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Composer Row
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about your uploaded PDF...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isStreaming && inputText.isNotBlank() && notebook.sources.isNotEmpty()) {
                                val q = inputText
                                inputText = ""
                                onSendMessage(q)
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notebook_chat_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isStreaming) {
                    IconButton(onClick = onStopStreaming) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    IconButton(
                        enabled = inputText.isNotBlank() && notebook.sources.isNotEmpty(),
                        onClick = {
                            val q = inputText
                            inputText = ""
                            onSendMessage(q)
                        },
                        modifier = Modifier.testTag("notebook_chat_send")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
