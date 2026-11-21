package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.data.model.AiVoicePreset
import com.example.data.model.AppSettings
import com.example.data.model.LiveVoiceMode
import com.example.data.model.LiveVoiceState
import com.example.ui.theme.CyanTertiary
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import java.util.Locale

@Composable
fun LiveVoiceOverlay(
    settings: AppSettings,
    liveVoiceMode: LiveVoiceMode,
    liveVoiceState: LiveVoiceState,
    onSelectMode: (LiveVoiceMode) -> Unit,
    onSelectAiVoice: (AiVoicePreset) -> Unit,
    onStartGeminiLive: () -> Unit,
    onStopGeminiLive: () -> Unit,
    onSendVoiceLoopTurn: (String, (LiveVoiceState) -> Unit, (String) -> Unit) -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        onStopGeminiLive()
        onClose()
    }

    val context = LocalContext.current
    var loopState by remember { mutableStateOf<LiveVoiceState>(LiveVoiceState.Idle) }
    var isLoopActive by remember { mutableStateOf(false) }
    var lastAiReply by remember { mutableStateOf("") }

    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
        }
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    fun startLoopListening() {
        if (recognizer == null) {
            loopState = LiveVoiceState.Error("Speech recognition service unavailable on this device.")
            isLoopActive = false
            return
        }
        val lang = settings.voiceInputLang.ifBlank { "bn-BD" }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                loopState = LiveVoiceState.Active("Listening ($lang)... Speak now")
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (isLoopActive && error == SpeechRecognizer.ERROR_NO_MATCH) {
                    startLoopListening()
                } else {
                    isLoopActive = false
                    loopState = LiveVoiceState.Error("Voice capture stopped (code $error). Tap Mic to resume.")
                }
            }
            override fun onResults(results: Bundle?) {
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (spoken.isNullOrBlank()) {
                    if (isLoopActive) startLoopListening()
                    return
                }
                onSendVoiceLoopTurn(
                    spoken,
                    { nextState -> loopState = nextState },
                    { reply ->
                        lastAiReply = reply
                        loopState = LiveVoiceState.Active(
                            statusText = "Speaking response...",
                            isModelSpeaking = true,
                            lastTranscript = spoken
                        )
                        if (ttsReady) {
                            val hasBn = reply.any { it in '\u0980'..'\u09FF' }
                            tts.language = if (hasBn) Locale.forLanguageTag("bn-BD") else Locale.US
                            tts.setSpeechRate(settings.speechRate)
                            tts.setPitch(settings.ttsPitch)
                            val clean = reply.replace(Regex("[#*_`]"), " ").take(2500)
                            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "live_loop_${System.currentTimeMillis()}")
                        } else if (isLoopActive) {
                            startLoopListening()
                        }
                    }
                )
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    loopState = LiveVoiceState.Active("Listening: $partial", lastTranscript = partial)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    DisposableEffect(tts, recognizer) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (isLoopActive && liveVoiceMode == LiveVoiceMode.BILINGUAL_LOOP) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        startLoopListening()
                    }
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}
        })
        onDispose {
            tts.stop()
            tts.shutdown()
            recognizer?.destroy()
            onStopGeminiLive()
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (liveVoiceMode == LiveVoiceMode.GEMINI_LIVE) {
                onStartGeminiLive()
            } else {
                isLoopActive = true
                startLoopListening()
            }
        } else {
            loopState = LiveVoiceState.Error("Microphone permission is required for Live Voice Chat.")
        }
    }

    val activeState = if (liveVoiceMode == LiveVoiceMode.GEMINI_LIVE) liveVoiceState else loopState

    val infiniteTransition = rememberInfiniteTransition(label = "live_orb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090D1A),
                        Color(0xFF131B36),
                        Color(0xFF0A0E1B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Mode Switcher
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Study Update Live",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveVoiceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = liveVoiceMode == mode,
                            onClick = {
                                tts.stop()
                                recognizer?.stopListening()
                                isLoopActive = false
                                onStopGeminiLive()
                                onSelectMode(mode)
                            },
                            label = { Text(mode.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    AiVoicePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = settings.aiVoiceName.equals(preset.geminiVoiceId, ignoreCase = true),
                            onClick = { onSelectAiVoice(preset) },
                            label = { Text("Voice: ${preset.displayName}") }
                        )
                    }
                }
            }

            // Pulsing Gemini Live Orb + Transcript
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val isTalkingOrListening = activeState is LiveVoiceState.Active || activeState is LiveVoiceState.Connecting
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(190.dp)
                        .scale(if (isTalkingOrListening) pulse else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(CyanTertiary, IndigoPrimary, VioletSecondary.copy(alpha = 0.7f))
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Live Voice Orb",
                        tint = Color.White,
                        modifier = Modifier.size(68.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val statusLabel = when (activeState) {
                    LiveVoiceState.Idle -> "Tap the Microphone below to start Live Voice Chat"
                    LiveVoiceState.Connecting -> "Connecting to Gemini Live..."
                    is LiveVoiceState.Active -> activeState.statusText
                    is LiveVoiceState.Error -> activeState.message
                }

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (activeState is LiveVoiceState.Error) Color(0xFFFCA5A5) else Color.White,
                    textAlign = TextAlign.Center
                )

                if (lastAiReply.isNotBlank() && liveVoiceMode == LiveVoiceMode.BILINGUAL_LOOP) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.09f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastAiReply.take(280),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Bottom Call Controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isRunning = activeState is LiveVoiceState.Active || activeState is LiveVoiceState.Connecting
                IconButton(
                    onClick = {
                        if (isRunning) {
                            tts.stop()
                            recognizer?.stopListening()
                            isLoopActive = false
                            loopState = LiveVoiceState.Idle
                            onStopGeminiLive()
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else if (liveVoiceMode == LiveVoiceMode.GEMINI_LIVE) {
                                onStartGeminiLive()
                            } else {
                                isLoopActive = true
                                startLoopListening()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) IndigoPrimary else CyanTertiary)
                        .testTag("live_voice_mic_toggle")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isRunning) "Pause live mic" else "Start live voice",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Button(
                    onClick = {
                        tts.stop()
                        recognizer?.stopListening()
                        isLoopActive = false
                        onStopGeminiLive()
                        onClose()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .height(64.dp)
                        .testTag("live_voice_end_button")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End live voice", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End", color = Color.White)
                }
            }
        }
    }
}
