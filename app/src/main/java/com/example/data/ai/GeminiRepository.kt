package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.data.model.AppSettings
import com.example.data.model.AttachmentPayload
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectionTestState
import com.example.data.model.MessageRole
import com.example.data.model.StudyMode
import com.example.security.SecretSanitizer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun streamReply(
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<AttachmentPayload>,
        mode: StudyMode,
        settings: AppSettings,
        customApiKey: String?
    ): Flow<String> = flow {
        val systemPrompt = PromptBuilder.buildSystemInstruction(mode, settings)
        val modelId = settings.selectedModelId.ifBlank { "gemini-3.8-flash" }
        val useCustom = settings.useCustomApiKey && !customApiKey.isNullOrBlank()

        if (useCustom) {
            streamViaCustomApiKey(
                apiKey = customApiKey!!,
                modelId = modelId,
                systemInstruction = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                attachments = attachments,
                temperature = settings.temperature
            ).collect { chunk ->
                emit(SecretSanitizer.redact(chunk, customApiKey))
            }
        } else {
            streamViaFirebaseAiLogic(
                modelId = modelId,
                systemInstruction = systemPrompt,
                history = history,
                userPrompt = userPrompt,
                attachments = attachments,
                temperature = settings.temperature
            ).collect { chunk ->
                emit(SecretSanitizer.redact(chunk))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun streamViaFirebaseAiLogic(
        modelId: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<AttachmentPayload>,
        temperature: Float
    ): Flow<String> = flow {
        val model = Firebase.ai.generativeModel(
            modelName = modelId,
            generationConfig = generationConfig {
                this.temperature = temperature
                maxOutputTokens = 8192
            },
            systemInstruction = content { text(systemInstruction) }
        )

        val chatHistory = buildFirebaseHistory(history)
        val chat = model.startChat(history = chatHistory)

        val outgoingContent = content(role = "user") {
            for (att in attachments) {
                when (att) {
                    is AttachmentPayload.ImageAttachment -> {
                        image(att.bitmap)
                    }
                    is AttachmentPayload.PdfAttachment -> {
                        inlineData(att.bytes, "application/pdf")
                    }
                    is AttachmentPayload.TextDocumentAttachment -> {
                        text("[Attached Document: ${att.name}]\n```\n${att.textContent}\n```")
                    }
                }
            }
            val effectiveText = userPrompt.ifBlank {
                "Please analyze the attached file(s) and explain clearly."
            }
            text(effectiveText)
        }

        withTimeout(60_000L) {
            var emitted = false
            chat.sendMessageStream(outgoingContent).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    emitted = true
                    emit(chunkText)
                }
            }
            if (!emitted) {
                throw IllegalStateException("Model returned an empty response.")
            }
        }
    }

    private fun buildFirebaseHistory(messages: List<ChatMessage>): List<Content> {
        val cleaned = messages.filter { !it.errorFlag && it.content.isNotBlank() }.takeLast(24)
        val result = mutableListOf<Content>()
        for (msg in cleaned) {
            val roleStr = if (MessageRole.fromRaw(msg.role) == MessageRole.USER) "user" else "model"
            if (result.isEmpty() && roleStr != "user") continue
            val last = result.lastOrNull()
            if (last != null && last.role == roleStr) {
                // Keep alternating user/model turns
                continue
            }
            result.add(content(role = roleStr) { text(msg.content) })
        }
        // Ensure history ends with a model turn before sending the new user message
        if (result.lastOrNull()?.role == "user") {
            result.removeAt(result.lastIndex)
        }
        return result
    }

    private fun streamViaCustomApiKey(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userPrompt: String,
        attachments: List<AttachmentPayload>,
        temperature: Float
    ): Flow<String> = flow {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:streamGenerateContent?alt=sse"

        val contentsArray = JSONArray()
        val recent = history.filter { !it.errorFlag && it.content.isNotBlank() }.takeLast(24)
        var expectingUser = true
        for (msg in recent) {
            val roleStr = if (MessageRole.fromRaw(msg.role) == MessageRole.USER) "user" else "model"
            if (expectingUser && roleStr != "user") continue
            if (!expectingUser && roleStr != "model") continue
            contentsArray.put(
                JSONObject().apply {
                    put("role", roleStr)
                    put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", msg.content))
                    )
                }
            )
            expectingUser = !expectingUser
        }
        if (!expectingUser && contentsArray.length() > 0) {
            contentsArray.remove(contentsArray.length() - 1)
        }

        val currentParts = JSONArray()
        for (att in attachments) {
            when (att) {
                is AttachmentPayload.ImageAttachment -> {
                    currentParts.put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", Base64.encodeToString(att.jpegBytes, Base64.NO_WRAP))
                            }
                        )
                    )
                }
                is AttachmentPayload.PdfAttachment -> {
                    currentParts.put(
                        JSONObject().put(
                            "inlineData",
                            JSONObject().apply {
                                put("mimeType", "application/pdf")
                                put("data", Base64.encodeToString(att.bytes, Base64.NO_WRAP))
                            }
                        )
                    )
                }
                is AttachmentPayload.TextDocumentAttachment -> {
                    currentParts.put(
                        JSONObject().put(
                            "text",
                            "[Attached Document: ${att.name}]\n```\n${att.textContent}\n```"
                        )
                    )
                }
            }
        }
        currentParts.put(
            JSONObject().put(
                "text",
                userPrompt.ifBlank { "Please analyze the attached file(s) and explain clearly." }
            )
        )
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            }
        )

        val requestBodyJson = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction))
                )
            )
            put("contents", contentsArray)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", temperature.toDouble())
                    put("maxOutputTokens", 8192)
                }
            )
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey.trim())
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        withTimeout(60_000L) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string().orEmpty()
                    throw IllegalStateException(
                        mapHttpError(response.code, SecretSanitizer.redact(errBody, apiKey))
                    )
                }

                val body = response.body ?: throw IllegalStateException("Empty stream body.")
                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var emitted = false
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim().orEmpty()
                    if (!trimmed.startsWith("data:")) continue
                    val jsonStr = trimmed.removePrefix("data:").trim()
                    if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                    val chunkObj = JSONObject(jsonStr)
                    val candidates = chunkObj.optJSONArray("candidates") ?: continue
                    if (candidates.length() == 0) continue
                    val parts = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts") ?: continue
                    for (i in 0 until parts.length()) {
                        val txt = parts.getJSONObject(i).optString("text", "")
                        if (txt.isNotEmpty()) {
                            emitted = true
                            emit(txt)
                        }
                    }
                }
                if (!emitted) {
                    throw IllegalStateException("Model returned an empty response.")
                }
            }
        }
    }

    suspend fun testConnection(
        modelId: String,
        useCustomKey: Boolean,
        customApiKey: String?
    ): ConnectionTestState = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        try {
            if (useCustomKey) {
                val key = customApiKey?.trim().orEmpty()
                if (key.isEmpty()) {
                    return@withContext ConnectionTestState.Failed(
                        "No custom API key saved yet. Enter and save your Gemini API key first."
                    )
                }
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", "Reply with OK"))
                            )
                        )
                    )
                    put("generationConfig", JSONObject().put("maxOutputTokens", 16))
                }
                val req = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", key)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                withTimeout(30_000L) {
                    okHttpClient.newCall(req).execute().use { res ->
                        if (!res.isSuccessful) {
                            val errBody = SecretSanitizer.redact(res.body?.string().orEmpty(), key)
                            return@withTimeout ConnectionTestState.Failed(mapHttpError(res.code, errBody))
                        }
                        val latency = System.currentTimeMillis() - startMs
                        ConnectionTestState.Connected(
                            model = modelId,
                            latencyMs = latency,
                            engineLabel = "Custom Gemini API Key (Encrypted)"
                        )
                    }
                }
            } else {
                val model = Firebase.ai.generativeModel(modelName = modelId)
                withTimeout(30_000L) {
                    val response = model.generateContent("Reply with OK")
                    if (response.text.isNullOrBlank()) {
                        ConnectionTestState.Failed("Connected, but model returned an empty response.")
                    } else {
                        val latency = System.currentTimeMillis() - startMs
                        ConnectionTestState.Connected(
                            model = modelId,
                            latencyMs = latency,
                            engineLabel = "Firebase AI Logic (App Check)"
                        )
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection test timed out", e)
            ConnectionTestState.Failed("Connection test timed out after 30 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            val cleaned = SecretSanitizer.redact(
                e.localizedMessage ?: "Connection failed.",
                customApiKey
            )
            ConnectionTestState.Failed(friendlyErrorMessage(cleaned))
        }
    }

    fun mapExceptionToFriendlyMessage(throwable: Throwable, customKey: String? = null): String {
        if (throwable is TimeoutCancellationException) {
            return "Request timed out after 60 seconds. Please check your connection and try again."
        }
        val raw = SecretSanitizer.redact(
            throwable.localizedMessage ?: throwable.message ?: "Unexpected AI error.",
            customKey
        )
        return friendlyErrorMessage(raw)
    }

    private fun mapHttpError(code: Int, body: String): String {
        return when (code) {
            400 -> "Bad request (400). Verify the selected model ID and attachments."
            401, 403 -> "Authentication failed ($code). Please verify your Gemini API key in API Configuration."
            404 -> "Selected model was not found (404). Try switching to gemini-3.8-flash."
            429 -> "Rate limit or quota exceeded (429). Please wait a moment and try again."
            503 -> "Gemini service is temporarily busy (503). Please try again shortly."
            else -> "Gemini API error ($code): ${body.take(180)}"
        }
    }

    private fun friendlyErrorMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("api_key_invalid") || lower.contains("api key not valid") || lower.contains("401") ->
                "Invalid Gemini API key. Please check your key in API Configuration."
            lower.contains("403") || lower.contains("app check") || lower.contains("attestation") ->
                "API authorization or App Check verification failed (403). Check your API configuration or network."
            lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
                "Rate limit or quota reached (429). Please wait a few seconds and retry."
            lower.contains("503") || lower.contains("overloaded") || lower.contains("unavailable") ->
                "Gemini service is temporarily unavailable (503). Please try again in a moment."
            lower.contains("safety") || lower.contains("blocked") ->
                "Response was blocked by safety filters. Please rephrase your question."
            else -> raw
        }
    }

    private companion object {
        const val TAG = "GeminiRepository"
    }
}
