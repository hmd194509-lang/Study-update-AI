package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.data.model.AppSettings
import com.example.data.model.AttachmentPayload
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectionTestState
import com.example.data.model.FlashcardSubject
import com.example.data.model.LanguagePreference
import com.example.data.model.MessageRole
import com.example.data.model.NotebookArtifactType
import com.example.data.model.NotebookMessage
import com.example.data.model.PodcastTurn
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyMode
import com.example.security.SecretSanitizer
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val imageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_IMAGE_MODEL_ID,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
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

    suspend fun generateFlashcards(
        topic: String,
        subject: FlashcardSubject,
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): List<StudyFlashcard> = withContext(Dispatchers.IO) {
        val cleanTopic = SecretSanitizer.redact(topic.trim(), customApiKey)
        require(cleanTopic.isNotBlank()) { "Please enter a study topic for flashcards." }

        val flashcardItemSchema = Schema.obj(
            properties = mapOf(
                "front" to Schema.string(description = "Concise study question or concept prompt"),
                "back" to Schema.string(description = "Clear answer, formula, or definition"),
                "admissionNote" to Schema.string(description = "High-yield HSC or University/Medical admission shortcut or trap")
            )
        )
        val deckSchema = Schema.obj(
            properties = mapOf(
                "cards" to Schema.array(
                    items = flashcardItemSchema,
                    description = "Exactly 5 study flashcards"
                )
            )
        )

        val prompt = "Create 5 high-yield ${subject.label} (${subject.bnLabel}) study flashcards for HSC & Admission prep on the topic: \"$cleanTopic\". Use clear Bengali & English formulas/terms where helpful."

        val rawJson = withTimeout(60_000L) {
            if (useCustomKey && !customApiKey.isNullOrBlank()) {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
                val payload = JSONObject().apply {
                    put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put(
                                        "text",
                                        "$prompt Return strict JSON: {\"cards\":[{\"front\":\"...\",\"back\":\"...\",\"admissionNote\":\"...\"}]}"
                                    )
                                )
                            )
                        )
                    )
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseMimeType", "application/json")
                            put("temperature", 0.4)
                        }
                    )
                }
                val req = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", customApiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                okHttpClient.newCall(req).execute().use { res ->
                    val rawBody = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(rawBody, customApiKey)))
                    }
                    JSONObject(rawBody)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                }
            } else {
                val flashcardModel = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = deckSchema
                        temperature = 0.4f
                    }
                )
                flashcardModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Model returned an empty flashcard response.")
            }
        }

        val root = JSONObject(rawJson)
        val arr = root.getJSONArray("cards")
        val list = mutableListOf<StudyFlashcard>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val front = item.optString("front").trim()
            val back = item.optString("back").trim()
            if (front.isNotEmpty() && back.isNotEmpty()) {
                list.add(
                    StudyFlashcard(
                        id = "fc_ai_${UUID.randomUUID()}",
                        subject = subject,
                        front = front,
                        back = back,
                        admissionNote = item.optString("admissionNote").trim(),
                        isCustom = true
                    )
                )
            }
        }
        require(list.isNotEmpty()) { "No flashcards could be parsed from AI output." }
        list
    }

    suspend fun generateNotebookPodcast(
        notebookTitle: String,
        attachments: List<AttachmentPayload>,
        languagePreference: LanguagePreference,
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): Pair<String, List<PodcastTurn>> = withContext(Dispatchers.IO) {
        require(attachments.isNotEmpty()) { "Please upload a PDF or add notes first." }

        val langInstruction = when (languagePreference) {
            LanguagePreference.BN -> "Speak in natural, conversational Bengali (বাংলা) with clear English technical terms where needed."
            LanguagePreference.EN -> "Speak in clear, engaging English."
            LanguagePreference.AUTO -> "Speak in an engaging bilingual Bangla + English mix (natural Bangladeshi student/podcast style)."
        }

        val turnSchema = Schema.obj(
            properties = mapOf(
                "speaker" to Schema.string(description = "Either HOST_A or HOST_B"),
                "speakerName" to Schema.string(description = "Ayesha (Host) or Rafi (Co-Host)"),
                "text" to Schema.string(description = "Natural conversational podcast dialogue line (2-4 sentences)")
            )
        )
        val podcastSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Catchy episode title based on the uploaded PDF/notes"),
                "turns" to Schema.array(
                    items = turnSchema,
                    description = "8 to 12 alternating podcast dialogue turns between HOST_A and HOST_B"
                )
            )
        )

        val prompt = """
            You are creating an Audio Overview Podcast episode (like Google NotebookLM) for a student's notebook titled "$notebookTitle".
            Carefully read all attached PDF documents and notes, then produce an engaging 2-host audio podcast script explaining the core concepts, formulas, examples, and exam insights from the source material.
            - Hosts: HOST_A = "Ayesha (Host)" (curious, clear explainer) and HOST_B = "Rafi (Co-Host)" (connects real-world examples & exam traps).
            - Alternate between HOST_A and HOST_B across 8 to 12 rich conversational turns so it sounds wonderful when read aloud.
            - $langInstruction
            - Do NOT invent facts outside the provided PDF/notes.
        """.trimIndent()

        val rawJson = withTimeout(90_000L) {
            if (useCustomKey && !customApiKey.isNullOrBlank()) {
                val partsArr = JSONArray()
                appendAttachmentsToJsonParts(partsArr, attachments)
                partsArr.put(
                    JSONObject().put(
                        "text",
                        "$prompt\nReturn strict JSON: {\"title\":\"...\",\"turns\":[{\"speaker\":\"HOST_A\",\"speakerName\":\"Ayesha (Host)\",\"text\":\"...\"}]}"
                    )
                )
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
                val payload = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", partsArr)))
                    put(
                        "generationConfig",
                        JSONObject().apply {
                            put("responseMimeType", "application/json")
                            put("temperature", 0.6)
                        }
                    )
                }
                val req = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", customApiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                okHttpClient.newCall(req).execute().use { res ->
                    val rawBody = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(rawBody, customApiKey)))
                    }
                    JSONObject(rawBody)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                }
            } else {
                val podcastModel = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                        responseSchema = podcastSchema
                        temperature = 0.6f
                    }
                )
                val outgoing = content(role = "user") {
                    appendAttachmentsToFirebaseContent(this, attachments)
                    text(prompt)
                }
                podcastModel.generateContent(outgoing).text
                    ?: throw IllegalStateException("Model returned an empty podcast script.")
            }
        }

        val cleanedJson = rawJson.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val root = JSONObject(cleanedJson)
        val epTitle = root.optString("title").trim().ifBlank { "$notebookTitle Audio Overview" }
        val turnsArr = root.getJSONArray("turns")
        val turns = mutableListOf<PodcastTurn>()
        for (i in 0 until turnsArr.length()) {
            val item = turnsArr.getJSONObject(i)
            val speakerRaw = item.optString("speaker", if (i % 2 == 0) "HOST_A" else "HOST_B").uppercase()
            val speaker = if (speakerRaw.contains("B")) "HOST_B" else "HOST_A"
            val defaultName = if (speaker == "HOST_A") "Ayesha (Host)" else "Rafi (Co-Host)"
            val speakerName = item.optString("speakerName").trim().ifBlank { defaultName }
            val lineText = item.optString("text").trim()
            if (lineText.isNotEmpty()) {
                turns.add(PodcastTurn(speaker = speaker, speakerName = speakerName, text = lineText))
            }
        }
        require(turns.isNotEmpty()) { "Podcast script had no spoken lines." }
        epTitle to turns
    }

    suspend fun generateNotebookArtifact(
        artifactType: NotebookArtifactType,
        notebookTitle: String,
        attachments: List<AttachmentPayload>,
        languagePreference: LanguagePreference,
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        require(attachments.isNotEmpty()) { "Please upload a PDF or add notes first." }

        val langLine = when (languagePreference) {
            LanguagePreference.BN -> "Write primarily in clear Bengali (বাংলা) with English formulas/scientific terms."
            LanguagePreference.EN -> "Write in clear English."
            LanguagePreference.AUTO -> "Write in a clean Bangla + English study mix."
        }
        val taskInstruction = when (artifactType) {
            NotebookArtifactType.STUDY_GUIDE ->
                "Create a structured Study Guide for \"$notebookTitle\" based ONLY on the attached PDF/notes: include Overview, Key Concepts, Important Formulas/Definitions, and Quick Revision Bullets."
            NotebookArtifactType.EXAM_FAQ ->
                "Extract the 6-8 most important Exam Questions, Viva/CQ answers, Formula shortcuts, and common student traps directly from the attached PDF/notes."
            NotebookArtifactType.PRACTICE_QUIZ ->
                "Create 5 multiple-choice practice questions (MCQ) grounded strictly in the attached PDF/notes. Format each with A/B/C/D options, then provide an Answer Key with brief 1-line explanations at the bottom."
        }
        val fullPrompt = "$taskInstruction\n$langLine\nFormat cleanly in Markdown."

        withTimeout(75_000L) {
            if (useCustomKey && !customApiKey.isNullOrBlank()) {
                val partsArr = JSONArray()
                appendAttachmentsToJsonParts(partsArr, attachments)
                partsArr.put(JSONObject().put("text", fullPrompt))
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
                val payload = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", partsArr)))
                    put("generationConfig", JSONObject().put("temperature", 0.35))
                }
                val req = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", customApiKey.trim())
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                okHttpClient.newCall(req).execute().use { res ->
                    val rawBody = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(rawBody, customApiKey)))
                    }
                    val parts = JSONObject(rawBody)
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                    buildString {
                        for (i in 0 until parts.length()) {
                            append(parts.getJSONObject(i).optString("text"))
                        }
                    }.trim()
                }
            } else {
                val model = Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    generationConfig = generationConfig { temperature = 0.35f }
                )
                val outgoing = content(role = "user") {
                    appendAttachmentsToFirebaseContent(this, attachments)
                    text(fullPrompt)
                }
                model.generateContent(outgoing).text?.trim()
                    ?: throw IllegalStateException("Empty response while generating ${artifactType.label}.")
            }
        }
    }

    fun streamNotebookReply(
        notebookTitle: String,
        history: List<NotebookMessage>,
        question: String,
        attachments: List<AttachmentPayload>,
        settings: AppSettings,
        customApiKey: String?
    ): Flow<String> = flow {
        val sysInstruction = """
            You are Gemini Notebook Assistant (like NotebookLM) inside Study Update AI.
            You are answering questions grounded in the sources inside the notebook "$notebookTitle".
            - Always cite which attached PDF or note supports your answer (e.g., [Source: chapter1.pdf]).
            - If the answer is not in the uploaded sources, clearly say what is missing and avoid hallucinating.
            - Language preference: ${settings.languagePreference.label}.
        """.trimIndent()

        val syntheticHistory = history.map {
            ChatMessage(
                id = it.id,
                role = it.role.name,
                content = it.content,
                errorFlag = false
            )
        }
        val modelId = settings.selectedModelId.ifBlank { "gemini-3.8-flash" }
        val useCustom = settings.useCustomApiKey && !customApiKey.isNullOrBlank()
        if (useCustom) {
            streamViaCustomApiKey(
                apiKey = customApiKey!!,
                modelId = modelId,
                systemInstruction = sysInstruction,
                history = syntheticHistory,
                userPrompt = question,
                attachments = attachments,
                temperature = 0.35f
            ).collect { emit(SecretSanitizer.redact(it, customApiKey)) }
        } else {
            streamViaFirebaseAiLogic(
                modelId = modelId,
                systemInstruction = sysInstruction,
                history = syntheticHistory,
                userPrompt = question,
                attachments = attachments,
                temperature = 0.35f
            ).collect { emit(SecretSanitizer.redact(it)) }
        }
    }.flowOn(Dispatchers.IO)

    private fun appendAttachmentsToFirebaseContent(
        builder: com.google.firebase.ai.type.Content.Builder,
        attachments: List<AttachmentPayload>
    ) {
        for (att in attachments) {
            when (att) {
                is AttachmentPayload.ImageAttachment -> builder.image(att.bitmap)
                is AttachmentPayload.PdfAttachment -> builder.inlineData(att.bytes, "application/pdf")
                is AttachmentPayload.TextDocumentAttachment ->
                    builder.text("[Source Note: ${att.name}]\n```\n${att.textContent}\n```")
            }
        }
    }

    private fun appendAttachmentsToJsonParts(partsArr: JSONArray, attachments: List<AttachmentPayload>) {
        for (att in attachments) {
            when (att) {
                is AttachmentPayload.ImageAttachment -> {
                    partsArr.put(
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
                    partsArr.put(
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
                    partsArr.put(
                        JSONObject().put(
                            "text",
                            "[Source Note: ${att.name}]\n```\n${att.textContent}\n```"
                        )
                    )
                }
            }
        }
    }

    suspend fun generateImage(
        prompt: String,
        referenceBitmap: Bitmap? = null,
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val safePrompt = SecretSanitizer.redact(prompt.trim(), customApiKey)
        require(safePrompt.isNotBlank()) { "Please enter an image prompt." }

        if (useCustomKey && !customApiKey.isNullOrBlank()) {
            return@withContext generateImageViaCustomKey(
                apiKey = customApiKey.trim(),
                prompt = safePrompt,
                referenceBitmap = referenceBitmap
            )
        }

        withTimeout(60_000L) {
            val response = if (referenceBitmap != null) {
                imageModel.generateContent(
                    content {
                        image(referenceBitmap)
                        text(safePrompt)
                    }
                )
            } else {
                imageModel.generateContent(safePrompt)
            }

            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            imagePart?.image ?: throw IllegalStateException(
                "No image was returned by $DEFAULT_IMAGE_MODEL_ID. Note: Gemini Nano Banana image generation requires the Firebase Blaze plan or a custom Gemini API Key."
            )
        }
    }

    private suspend fun generateImageViaCustomKey(
        apiKey: String,
        prompt: String,
        referenceBitmap: Bitmap?
    ): Bitmap = withTimeout(60_000L) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_IMAGE_MODEL_ID:generateContent"
        val parts = JSONArray()
        if (referenceBitmap != null) {
            val baos = ByteArrayOutputStream()
            referenceBitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            parts.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                    }
                )
            )
        }
        parts.put(JSONObject().put("text", prompt))

        val payload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            put(
                "generationConfig",
                JSONObject().put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
            )
        }

        val req = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(req).execute().use { res ->
            val rawBody = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(rawBody, apiKey)))
            }
            val root = JSONObject(rawBody)
            val candidates = root.optJSONArray("candidates")
                ?: throw IllegalStateException("No candidates returned by image model.")
            for (c in 0 until candidates.length()) {
                val respParts = candidates.getJSONObject(c)
                    .optJSONObject("content")
                    ?.optJSONArray("parts") ?: continue
                for (p in 0 until respParts.length()) {
                    val inline = respParts.getJSONObject(p).optJSONObject("inlineData") ?: continue
                    val b64 = inline.optString("data", "")
                    if (b64.isNotBlank()) {
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        return@withTimeout BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            ?: throw IllegalStateException("Failed to decode generated image.")
                    }
                }
            }
            throw IllegalStateException("Image model did not include image bytes in its response.")
        }
    }

    suspend fun generateVideo(
        context: Context,
        prompt: String,
        customApiKey: String?,
        onProgress: (String) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val key = customApiKey?.trim().orEmpty()
        if (key.isEmpty()) {
            throw MissingVideoApiKeyException(
                "Video generation uses the Gemini Veo API (`predictLongRunning`). Please save your Gemini API Key in API Configuration first."
            )
        }

        val cleanPrompt = SecretSanitizer.redact(prompt.trim(), key)
        require(cleanPrompt.isNotBlank()) { "Please enter a video prompt." }

        onProgress("Submitting prompt to Gemini Veo...")

        val startUrl = "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_VIDEO_MODEL_ID:predictLongRunning"
        val bodyJson = JSONObject().apply {
            put(
                "instances",
                JSONArray().put(JSONObject().put("prompt", cleanPrompt))
            )
            put(
                "parameters",
                JSONObject().apply {
                    put("aspectRatio", "16:9")
                }
            )
        }

        val startReq = Request.Builder()
            .url(startUrl)
            .addHeader("x-goog-api-key", key)
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val operationName = okHttpClient.newCall(startReq).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(raw, key)))
            }
            JSONObject(raw).optString("name").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Veo operation did not return an operation name.")
        }

        withTimeout(180_000L) {
            var pollCount = 0
            while (true) {
                pollCount++
                onProgress("Rendering video with Gemini Veo (step $pollCount)...")
                delay(5_000L)

                val pollUrl = "https://generativelanguage.googleapis.com/v1beta/$operationName"
                val pollReq = Request.Builder()
                    .url(pollUrl)
                    .addHeader("x-goog-api-key", key)
                    .get()
                    .build()

                val opObj = okHttpClient.newCall(pollReq).execute().use { res ->
                    val raw = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(raw, key)))
                    }
                    JSONObject(raw)
                }

                if (opObj.has("error")) {
                    val errMsg = opObj.optJSONObject("error")?.optString("message")
                        ?: "Video generation failed."
                    throw IllegalStateException(SecretSanitizer.redact(errMsg, key))
                }

                if (opObj.optBoolean("done", false)) {
                    val videoUri = extractVideoUri(opObj)
                        ?: throw IllegalStateException("Video completed, but no downloadable video URI was returned.")
                    onProgress("Downloading MP4 video...")
                    return@withTimeout downloadVideoToCache(context, videoUri, key)
                }
            }
        }
        @Suppress("UNREACHABLE_CODE")
        throw IllegalStateException("Video generation ended unexpectedly.")
    }

    private fun extractVideoUri(opObj: JSONObject): String? {
        val responseObj = opObj.optJSONObject("response") ?: return null
        val genResponse = responseObj.optJSONObject("generateVideoResponse")
        val samples = genResponse?.optJSONArray("generatedSamples")
            ?: responseObj.optJSONArray("generatedSamples")
        if (samples != null && samples.length() > 0) {
            val uri = samples.getJSONObject(0).optJSONObject("video")?.optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }
        val videos = responseObj.optJSONArray("videos")
        if (videos != null && videos.length() > 0) {
            val uri = videos.getJSONObject(0).optString("uri")
            if (!uri.isNullOrBlank()) return uri
        }
        return null
    }

    private fun downloadVideoToCache(context: Context, videoUri: String, apiKey: String): File {
        val dir = File(context.cacheDir, "generated_videos").apply { mkdirs() }
        val outFile = File(dir, "veo_${System.currentTimeMillis()}.mp4")
        val req = Request.Builder()
            .url(videoUri)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()
        okHttpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                throw IllegalStateException("Failed to download generated video (${res.code}).")
            }
            val stream = res.body?.byteStream()
                ?: throw IllegalStateException("Empty video download stream.")
            outFile.outputStream().use { out ->
                stream.copyTo(out)
            }
        }
        return outFile
    }

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
                continue
            }
            result.add(content(role = roleStr) { text(msg.content) })
        }
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
        if (throwable is QuotaExceededException) {
            return "Rate limit or quota reached. Please wait a few seconds and retry."
        }
        val raw = SecretSanitizer.redact(
            throwable.localizedMessage ?: throwable.message ?: "Unexpected AI error.",
            customKey
        )
        return friendlyErrorMessage(raw)
    }

    private fun mapHttpError(code: Int, body: String): String {
        return when (code) {
            400 -> "Bad request (400). Verify the selected model ID and prompt/attachments. ${body.take(120)}"
            401, 403 -> "Authentication failed ($code). Please verify your Gemini API key in API Configuration."
            404 -> "Selected model or endpoint was not found (404). ${body.take(120)}"
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

    class MissingVideoApiKeyException(message: String) : IllegalStateException(message)

    private companion object {
        const val TAG = "GeminiRepository"
        const val DEFAULT_IMAGE_MODEL_ID = "gemini-nano-banana-2.1"
        const val DEFAULT_VIDEO_MODEL_ID = "veo-2.0-generate-001"
    }
}
