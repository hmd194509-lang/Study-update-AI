package com.example.data.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.AppSettings
import com.example.data.model.StudyMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(PublicPreviewAPI::class)
class LiveVoiceManager {

    private var activeSession: LiveSession? = null

    @SuppressLint("MissingPermission")
    suspend fun startLiveAudioSession(
        context: Context,
        mode: StudyMode,
        settings: AppSettings
    ) = withContext(Dispatchers.IO) {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            throw SecurityException("Microphone permission is required for Live Voice Chat.")
        }

        stopLiveSession()

        val systemInstruction = content {
            text(
                PromptBuilder.buildSystemInstruction(mode, settings) +
                    "\nKeep spoken answers conversational, concise, and clear for live audio conversation."
            )
        }

        val voiceId = com.example.data.model.AiVoicePreset.fromId(settings.aiVoiceName).geminiVoiceId
        val liveModel = Firebase.ai.liveModel(
            modelName = LIVE_MODEL_ID,
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                speechConfig = SpeechConfig(voice = Voice(voiceId))
            },
            systemInstruction = systemInstruction
        )

        val session = withTimeout(25_000L) {
            liveModel.connect()
        }
        activeSession = session
        session.startAudioConversation()
    }

    suspend fun stopLiveSession() = withContext(Dispatchers.IO) {
        try {
            activeSession?.stopAudioConversation()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping audio conversation: ${e.message}")
        }
        try {
            activeSession?.close()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Error closing live session: ${e.message}")
        }
        activeSession = null
    }

    private companion object {
        const val TAG = "LiveVoiceManager"
        const val LIVE_MODEL_ID = "gemini-2.5-flash-native-audio-preview-12-2025"
    }
}
