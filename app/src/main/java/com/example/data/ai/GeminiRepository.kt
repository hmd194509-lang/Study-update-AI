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
import com.example.data.model.FlashcardType
import com.example.data.model.LanguagePreference
import com.example.data.model.MessageRole
import com.example.data.model.NotebookArtifactType
import com.example.data.model.NotebookMessage
import com.example.data.model.PodcastTurn
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyMode
import com.example.security.SecretSanitizer
import com.example.util.AttachmentReader
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private fun resolveModelCandidates(requestedModelId: String): List<String> {
        val clean = requestedModelId.trim().ifBlank { "gemini-3.8-flash" }
        val baseFallbacks = when (clean) {
            "gemini-4-hydrogen" -> listOf("gemini-4-hydrogen", "gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash")
            "gemini-3.8-flash" -> listOf("gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
            "gemini-3.5-flash-lite" -> listOf("gemini-3.5-flash-lite", "gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.0-flash")
            "gemini-4-argon" -> listOf("gemini-4-argon", "gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash")
            else -> listOf(clean, "gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-2.0-flash")
        }
        return baseFallbacks.distinct()
    }

    private fun buildRestEndpoints(apiKey: String, modelId: String, action: String): List<Pair<String, Boolean>> {
        val cleanKey = apiKey.trim()
        val encodedKey = URLEncoder.encode(cleanKey, "UTF-8")
        val querySuffix = if (action.contains("?")) "&key=$encodedKey" else "?key=$encodedKey"
        return if (cleanKey.startsWith("AQ.")) {
            listOf(
                "https://aiplatform.googleapis.com/v1/publishers/google/models/$modelId:$action$querySuffix" to false,
                "https://aiplatform.googleapis.com/v1beta1/publishers/google/models/$modelId:$action$querySuffix" to false,
                "https://generativelanguage.googleapis.com/v1beta/models/$modelId:$action$querySuffix" to true
            )
        } else {
            listOf(
                "https://generativelanguage.googleapis.com/v1beta/models/$modelId:$action" to true,
                "https://aiplatform.googleapis.com/v1/publishers/google/models/$modelId:$action$querySuffix" to false
            )
        }
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
            var succeeded = false
            var customFailure: Throwable? = null
            try {
                streamViaCustomApiKey(
                    apiKey = customApiKey!!,
                    modelId = modelId,
                    systemInstruction = systemPrompt,
                    history = history,
                    userPrompt = userPrompt,
                    attachments = attachments,
                    temperature = settings.temperature
                ).collect { chunk ->
                    succeeded = true
                    emit(SecretSanitizer.redact(chunk, customApiKey))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                customFailure = e
                Log.w(TAG, "Custom key stream fallback triggered: ${e.message}")
            }

            if (!succeeded) {
                try {
                    streamViaFirebaseAiLogic(
                        modelId = modelId,
                        systemInstruction = systemPrompt,
                        history = history,
                        userPrompt = userPrompt,
                        attachments = attachments,
                        temperature = settings.temperature
                    ).collect { chunk ->
                        emit(SecretSanitizer.redact(chunk, customApiKey))
                    }
                } catch (fbErr: CancellationException) {
                    throw fbErr
                } catch (_: Exception) {
                    throw (customFailure ?: IllegalStateException("Could not reach Study AI."))
                }
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
        cardType: FlashcardType = FlashcardType.MIXED,
        count: Int = 10,
        customInstruction: String = "",
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): List<StudyFlashcard> = withContext(Dispatchers.IO) {
        val cleanTopic = SecretSanitizer.redact(topic.trim(), customApiKey)
        require(cleanTopic.isNotBlank()) { "Please enter a study topic or instruction for flashcards." }
        val safeCount = count.coerceIn(3, 30)
        val cleanExtra = SecretSanitizer.redact(customInstruction.trim(), customApiKey)

        val flashcardItemSchema = Schema.obj(
            properties = mapOf(
                "front" to Schema.string(description = "Concise study question, prompt, or concept title"),
                "back" to Schema.string(description = "Clear answer, formula, definition, or explanation"),
                "admissionNote" to Schema.string(description = "High-yield HSC or University/Medical admission shortcut or trap")
            )
        )
        val deckSchema = Schema.obj(
            properties = mapOf(
                "cards" to Schema.array(
                    items = flashcardItemSchema,
                    description = "Exactly $safeCount study flashcards"
                )
            )
        )

        val extraLine = if (cleanExtra.isNotBlank()) "\nStudent's custom requirement: $cleanExtra" else ""
        val prompt = """
            Create $safeCount high-yield ${subject.label} (${subject.bnLabel}) study flashcards for HSC & Bangladesh University/Medical/BUET Admission prep.
            Topic: "$cleanTopic"
            Card Type Focus: ${cardType.label} (${cardType.bnLabel}) — prioritize ${cardType.promptHint}.
            $extraLine
            Use clear Bengali & English formulas/terms so students memorize fast.
        """.trimIndent()

        val rawJson = withTimeout(75_000L) {
            executeTextPromptWithFallback(
                prompt = "$prompt\nReturn strict JSON: {\"cards\":[{\"front\":\"...\",\"back\":\"...\",\"admissionNote\":\"...\"}]}",
                firebasePrompt = prompt,
                attachments = emptyList(),
                requestedModelId = "gemini-3.8-flash",
                responseMimeType = "application/json",
                firebaseSchema = deckSchema,
                temperature = 0.45f,
                useCustomKey = useCustomKey,
                customApiKey = customApiKey
            )
        }

        val cleanedJson = cleanJsonEnvelope(rawJson)
        val root = JSONObject(cleanedJson)
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
                        type = cardType,
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
            LanguagePreference.AUTO -> "Speak in an engaging bilingual Bangla + English mix (natural Bangladeshi student/podcast style so two speakers explain everything clearly)."
        }

        val turnSchema = Schema.obj(
            properties = mapOf(
                "speaker" to Schema.string(description = "Either HOST_A or HOST_B"),
                "speakerName" to Schema.string(description = "Ayesha (Host) or Rafi (Co-Host)"),
                "text" to Schema.string(description = "Natural conversational podcast dialogue line (2-4 sentences explaining a concept clearly)")
            )
        )
        val podcastSchema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Catchy episode title based on the uploaded PDF/notes"),
                "turns" to Schema.array(
                    items = turnSchema,
                    description = "10 to 14 alternating podcast dialogue turns between HOST_A and HOST_B"
                )
            )
        )

        val prompt = """
            You are creating an interactive 2-Host Audio Explanation Podcast (NotebookLM style) for a student's notebook titled "$notebookTitle".
            Carefully examine all attached PDF documents, rendered PDF pages, and notes, then produce an engaging two-person conversation explaining the chapter concepts, formulas, examples, and exam traps simply.
            - Hosts: HOST_A = "Ayesha (Host)" (asks intuitive questions & explains step-by-step) and HOST_B = "Rafi (Co-Host)" (breaks down formulas, real-world analogies & exam shortcuts).
            - Alternate naturally between HOST_A and HOST_B across 10 to 14 dialogue turns so it sounds like two tutors discussing and teaching the PDF aloud.
            - $langInstruction
            - Ground explanations in the provided PDF/notes.
        """.trimIndent()

        val rawJson = withTimeout(110_000L) {
            executeTextPromptWithFallback(
                prompt = "$prompt\nReturn strict JSON: {\"title\":\"...\",\"turns\":[{\"speaker\":\"HOST_A\",\"speakerName\":\"Ayesha (Host)\",\"text\":\"...\"}]}",
                firebasePrompt = prompt,
                attachments = attachments,
                requestedModelId = "gemini-3.8-flash",
                responseMimeType = "application/json",
                firebaseSchema = podcastSchema,
                temperature = 0.6f,
                useCustomKey = useCustomKey,
                customApiKey = customApiKey
            )
        }

        val cleanedJson = cleanJsonEnvelope(rawJson)
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

        withTimeout(90_000L) {
            executeTextPromptWithFallback(
                prompt = fullPrompt,
                firebasePrompt = fullPrompt,
                attachments = attachments,
                requestedModelId = "gemini-3.8-flash",
                responseMimeType = null,
                firebaseSchema = null,
                temperature = 0.35f,
                useCustomKey = useCustomKey,
                customApiKey = customApiKey
            ).trim()
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
            You are Study AI Notebook Assistant (like NotebookLM) inside Study AI.
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
            var emitted = false
            var customErr: Throwable? = null
            try {
                streamViaCustomApiKey(
                    apiKey = customApiKey!!,
                    modelId = modelId,
                    systemInstruction = sysInstruction,
                    history = syntheticHistory,
                    userPrompt = question,
                    attachments = attachments,
                    temperature = 0.35f
                ).collect {
                    emitted = true
                    emit(SecretSanitizer.redact(it, customApiKey))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                customErr = e
            }
            if (!emitted) {
                try {
                    streamViaFirebaseAiLogic(
                        modelId = modelId,
                        systemInstruction = sysInstruction,
                        history = syntheticHistory,
                        userPrompt = question,
                        attachments = attachments,
                        temperature = 0.35f
                    ).collect { emit(SecretSanitizer.redact(it, customApiKey)) }
                } catch (fbErr: CancellationException) {
                    throw fbErr
                } catch (_: Exception) {
                    throw (customErr ?: IllegalStateException("Notebook AI request failed."))
                }
            }
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

    private suspend fun executeTextPromptWithFallback(
        prompt: String,
        firebasePrompt: String,
        attachments: List<AttachmentPayload>,
        requestedModelId: String,
        responseMimeType: String?,
        firebaseSchema: Schema?,
        temperature: Float,
        useCustomKey: Boolean,
        customApiKey: String?
    ): String {
        var customError: Throwable? = null
        if (useCustomKey && !customApiKey.isNullOrBlank()) {
            try {
                return generateTextViaRestCascade(
                    apiKey = customApiKey.trim(),
                    requestedModelId = requestedModelId,
                    prompt = prompt,
                    attachments = attachments,
                    responseMimeType = responseMimeType,
                    temperature = temperature
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                customError = e
                Log.w(TAG, "REST generation fallback to Firebase AI Logic: ${e.message}")
            }
        }

        // Try Firebase AI Logic across compatible models
        val fbCandidates = resolveModelCandidates(requestedModelId)
        var lastFbErr: Throwable? = null
        for (modelName in fbCandidates) {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        this.temperature = temperature
                        if (responseMimeType != null) this.responseMimeType = responseMimeType
                        if (firebaseSchema != null) this.responseSchema = firebaseSchema
                    }
                )
                val responseText = if (attachments.isEmpty()) {
                    model.generateContent(firebasePrompt).text
                } else {
                    val outgoing = content(role = "user") {
                        appendAttachmentsToFirebaseContent(this, attachments)
                        text(firebasePrompt)
                    }
                    model.generateContent(outgoing).text
                }
                if (!responseText.isNullOrBlank()) {
                    return responseText
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastFbErr = e
            }
        }

        throw (customError ?: lastFbErr ?: IllegalStateException("AI returned an empty response."))
    }

    private fun generateTextViaRestCascade(
        apiKey: String,
        requestedModelId: String,
        prompt: String,
        attachments: List<AttachmentPayload>,
        responseMimeType: String?,
        temperature: Float
    ): String {
        val candidates = resolveModelCandidates(requestedModelId)
        var lastErr = "AI request failed."

        for (modelId in candidates) {
            val endpoints = buildRestEndpoints(apiKey, modelId, "generateContent")
            for ((url, supportsFilesApi) in endpoints) {
                try {
                    val partsArr = JSONArray()
                    appendAttachmentsToJsonParts(partsArr, attachments, apiKey, supportsFilesApi)
                    partsArr.put(JSONObject().put("text", prompt))

                    val genConfig = JSONObject().apply {
                        put("temperature", temperature.toDouble())
                        if (!responseMimeType.isNullOrBlank()) {
                            put("responseMimeType", responseMimeType)
                        }
                    }
                    val payload = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", partsArr)))
                        put("generationConfig", genConfig)
                    }
                    val req = Request.Builder()
                        .url(url)
                        .apply { if (!url.contains("key=")) addHeader("x-goog-api-key", apiKey) }
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    okHttpClient.newCall(req).execute().use { res ->
                        val rawBody = res.body?.string().orEmpty()
                        if (res.isSuccessful) {
                            val parts = JSONObject(rawBody)
                                .getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                            val text = buildString {
                                for (i in 0 until parts.length()) {
                                    append(parts.getJSONObject(i).optString("text"))
                                }
                            }.trim()
                            if (text.isNotBlank()) return text
                        } else {
                            lastErr = mapHttpError(res.code, SecretSanitizer.redact(rawBody, apiKey))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastErr = SecretSanitizer.redact(e.localizedMessage ?: "Network error", apiKey)
                }
            }
        }
        throw IllegalStateException(lastErr)
    }

    private fun appendAttachmentsToFirebaseContent(
        builder: Content.Builder,
        attachments: List<AttachmentPayload>
    ) {
        for (att in attachments) {
            when (att) {
                is AttachmentPayload.ImageAttachment -> builder.image(att.bitmap)
                is AttachmentPayload.PdfAttachment -> {
                    if (att.bytes.isNotEmpty()) {
                        builder.inlineData(att.bytes, "application/pdf")
                    } else if (att.localFilePath.isNotBlank()) {
                        val sizeMb = (att.sizeBytes / (1024L * 1024L)).coerceAtLeast(1L)
                        builder.text("[Large PDF Document Attached: ${att.name} ($sizeMb MB) — key pages rendered below]")
                        AttachmentReader.renderSamplePdfPages(att.localFilePath, maxPages = 10)
                            .forEach { pageBitmap -> builder.image(pageBitmap) }
                    }
                }
                is AttachmentPayload.TextDocumentAttachment ->
                    builder.text("[Source Note: ${att.name}]\n```\n${att.textContent}\n```")
            }
        }
    }

    private fun appendAttachmentsToJsonParts(
        partsArr: JSONArray,
        attachments: List<AttachmentPayload>,
        apiKey: String,
        supportsFilesApi: Boolean
    ) {
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
                    if (att.bytes.isNotEmpty()) {
                        partsArr.put(
                            JSONObject().put(
                                "inlineData",
                                JSONObject().apply {
                                    put("mimeType", "application/pdf")
                                    put("data", Base64.encodeToString(att.bytes, Base64.NO_WRAP))
                                }
                            )
                        )
                    } else if (att.localFilePath.isNotBlank()) {
                        val uploadedUri = if (supportsFilesApi) {
                            runCatching { uploadLargePdfToGeminiFilesApi(File(att.localFilePath), att.name, apiKey) }.getOrNull()
                        } else null

                        if (!uploadedUri.isNullOrBlank()) {
                            partsArr.put(
                                JSONObject().put(
                                    "fileData",
                                    JSONObject().apply {
                                        put("mimeType", "application/pdf")
                                        put("fileUri", uploadedUri)
                                    }
                                )
                            )
                        } else {
                            val sizeMb = (att.sizeBytes / (1024L * 1024L)).coerceAtLeast(1L)
                            partsArr.put(
                                JSONObject().put(
                                    "text",
                                    "[Large PDF Document: ${att.name} ($sizeMb MB) — key pages rendered for analysis below]"
                                )
                            )
                            val pages = AttachmentReader.renderSamplePdfPages(att.localFilePath, maxPages = 10)
                            for (bmp in pages) {
                                val baos = ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                                partsArr.put(
                                    JSONObject().put(
                                        "inlineData",
                                        JSONObject().apply {
                                            put("mimeType", "image/jpeg")
                                            put("data", Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP))
                                        }
                                    )
                                )
                            }
                        }
                    }
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

    private fun uploadLargePdfToGeminiFilesApi(file: File, displayName: String, apiKey: String): String? {
        if (!file.exists() || file.length() <= 0L) return null
        val encodedKey = URLEncoder.encode(apiKey.trim(), "UTF-8")
        val initUrl = "https://generativelanguage.googleapis.com/upload/v1beta/files?key=$encodedKey"
        val metaJson = JSONObject().put("file", JSONObject().put("display_name", displayName.take(120))).toString()

        val initReq = Request.Builder()
            .url(initUrl)
            .addHeader("X-Goog-Upload-Protocol", "resumable")
            .addHeader("X-Goog-Upload-Command", "start")
            .addHeader("X-Goog-Upload-Header-Content-Length", file.length().toString())
            .addHeader("X-Goog-Upload-Header-Content-Type", "application/pdf")
            .addHeader("Content-Type", "application/json")
            .post(metaJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val uploadUrl = okHttpClient.newCall(initReq).execute().use { res ->
            if (!res.isSuccessful) return null
            res.header("X-Goog-Upload-URL") ?: res.header("x-goog-upload-url")
        } ?: return null

        // Stream directly from File in 64 KB chunks -> zero OOM up to 1 GB!
        val fileBody = object : RequestBody() {
            override fun contentType() = "application/pdf".toMediaType()
            override fun contentLength() = file.length()
            override fun writeTo(sink: BufferedSink) {
                FileInputStream(file).use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        sink.write(buf, 0, read)
                    }
                }
            }
        }

        val uploadReq = Request.Builder()
            .url(uploadUrl)
            .addHeader("X-Goog-Upload-Offset", "0")
            .addHeader("X-Goog-Upload-Command", "upload, finalize")
            .post(fileBody)
            .build()

        val fileObj = okHttpClient.newCall(uploadReq).execute().use { res ->
            if (!res.isSuccessful) return null
            JSONObject(res.body?.string().orEmpty()).optJSONObject("file")
        } ?: return null

        val fileName = fileObj.optString("name")
        var state = fileObj.optString("state", "ACTIVE")
        var fileUri = fileObj.optString("uri")
        var polls = 0
        while (state == "PROCESSING" && polls < 20 && fileName.isNotBlank()) {
            polls++
            Thread.sleep(1500L)
            val checkReq = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/$fileName?key=$encodedKey")
                .get()
                .build()
            okHttpClient.newCall(checkReq).execute().use { res ->
                if (res.isSuccessful) {
                    val checked = JSONObject(res.body?.string().orEmpty())
                    state = checked.optString("state", state)
                    fileUri = checked.optString("uri", fileUri)
                }
            }
        }
        return fileUri.takeIf { it.isNotBlank() && state != "FAILED" }
    }

    suspend fun generateImage(
        prompt: String,
        referenceBitmap: Bitmap? = null,
        useCustomKey: Boolean = false,
        customApiKey: String? = null
    ): Bitmap = withContext(Dispatchers.IO) {
        val safePrompt = SecretSanitizer.redact(prompt.trim(), customApiKey)
        require(safePrompt.isNotBlank()) { "Please enter an image prompt." }

        val errors = mutableListOf<String>()

        if (useCustomKey && !customApiKey.isNullOrBlank()) {
            try {
                return@withContext generateImageViaRestCascade(
                    apiKey = customApiKey.trim(),
                    prompt = safePrompt,
                    referenceBitmap = referenceBitmap
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add(SecretSanitizer.redact(e.localizedMessage ?: "REST image error", customApiKey))
            }
        }

        // Firebase AI Logic Image Generation Cascade
        try {
            return@withContext generateImageViaFirebaseCascade(safePrompt, referenceBitmap)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errors.add(SecretSanitizer.redact(e.localizedMessage ?: "Firebase image error", customApiKey))
        }

        // If user was on Firebase mode but has an owner/fallback key available, try REST too
        if (!useCustomKey && !customApiKey.isNullOrBlank()) {
            try {
                return@withContext generateImageViaRestCascade(
                    apiKey = customApiKey.trim(),
                    prompt = safePrompt,
                    referenceBitmap = referenceBitmap
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errors.add(SecretSanitizer.redact(e.localizedMessage ?: "Fallback key image error", customApiKey))
            }
        }

        throw IllegalStateException(
            "Image generation failed (${errors.firstOrNull() ?: "model unavailable"}). Try saving a standard Google AI Studio key (AIza...) or check Firebase Blaze billing."
        )
    }

    private suspend fun generateImageViaFirebaseCascade(
        safePrompt: String,
        referenceBitmap: Bitmap?
    ): Bitmap = withTimeout(75_000L) {
        val fbImageModels = listOf(
            "gemini-nano-banana-2.1",
            "gemini-3.1-flash-lite-image",
            "gemini-2.5-flash-image",
            "gemini-3-pro-image"
        )
        var lastError: Throwable? = null
        for (modelName in fbImageModels) {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = modelName,
                    generationConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.IMAGE, ResponseModality.TEXT)
                    }
                )
                val response = if (referenceBitmap != null) {
                    model.generateContent(
                        content {
                            image(referenceBitmap)
                            text(safePrompt)
                        }
                    )
                } else {
                    model.generateContent(safePrompt)
                }
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                if (imagePart?.image != null) {
                    return@withTimeout imagePart.image
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw (lastError ?: IllegalStateException("No image returned by Firebase AI Logic."))
    }

    private suspend fun generateImageViaRestCascade(
        apiKey: String,
        prompt: String,
        referenceBitmap: Bitmap?
    ): Bitmap = withTimeout(75_000L) {
        val restImageModels = listOf(
            "gemini-2.5-flash-image",
            "gemini-2.0-flash-exp-image-generation",
            "gemini-3-pro-image-preview",
            "gemini-nano-banana-2.1"
        )
        var lastErr = "No REST image model responded with an image."

        // 1. Try Gemini Multimodal generateContent image models
        for (modelId in restImageModels) {
            val endpoints = buildRestEndpoints(apiKey, modelId, "generateContent")
            for ((url, _) in endpoints) {
                try {
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
                        put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
                        put(
                            "generationConfig",
                            JSONObject().put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
                        )
                    }

                    val req = Request.Builder()
                        .url(url)
                        .apply { if (!url.contains("key=")) addHeader("x-goog-api-key", apiKey) }
                        .addHeader("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    okHttpClient.newCall(req).execute().use { res ->
                        val rawBody = res.body?.string().orEmpty()
                        if (res.isSuccessful) {
                            decodeBitmapFromGenerateContentJson(rawBody)?.let { return@withTimeout it }
                        } else {
                            lastErr = mapHttpError(res.code, SecretSanitizer.redact(rawBody, apiKey))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastErr = SecretSanitizer.redact(e.localizedMessage ?: "Network error", apiKey)
                }
            }
        }

        // 2. Fallback to Imagen 3 `:predict` endpoint if no reference image edit is needed
        if (referenceBitmap == null) {
            val imagenModels = listOf("imagen-3.0-generate-002", "imagen-3.0-fast-generate-001")
            for (imagenId in imagenModels) {
                val predictEndpoints = buildRestEndpoints(apiKey, imagenId, "predict")
                for ((url, _) in predictEndpoints) {
                    try {
                        val payload = JSONObject().apply {
                            put("instances", JSONArray().put(JSONObject().put("prompt", prompt)))
                            put("parameters", JSONObject().put("sampleCount", 1))
                        }
                        val req = Request.Builder()
                            .url(url)
                            .apply { if (!url.contains("key=")) addHeader("x-goog-api-key", apiKey) }
                            .addHeader("Content-Type", "application/json")
                            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                            .build()
                        okHttpClient.newCall(req).execute().use { res ->
                            val rawBody = res.body?.string().orEmpty()
                            if (res.isSuccessful) {
                                val preds = JSONObject(rawBody).optJSONArray("predictions")
                                if (preds != null && preds.length() > 0) {
                                    val b64 = preds.getJSONObject(0).optString("bytesBase64Encoded")
                                    if (b64.isNotBlank()) {
                                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            ?.let { return@withTimeout it }
                                    }
                                }
                            } else {
                                lastErr = mapHttpError(res.code, SecretSanitizer.redact(rawBody, apiKey))
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastErr = SecretSanitizer.redact(e.localizedMessage ?: "Imagen error", apiKey)
                    }
                }
            }
        }

        throw IllegalStateException(lastErr)
    }

    private fun decodeBitmapFromGenerateContentJson(rawBody: String): Bitmap? {
        val root = JSONObject(rawBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        for (c in 0 until candidates.length()) {
            val respParts = candidates.getJSONObject(c)
                .optJSONObject("content")
                ?.optJSONArray("parts") ?: continue
            for (p in 0 until respParts.length()) {
                val part = respParts.getJSONObject(p)
                val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data") ?: continue
                val b64 = inline.optString("data", "")
                if (b64.isNotBlank()) {
                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
                }
            }
        }
        return null
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
                "Video generation uses Gemini Veo. Please configure your Gemini API Key in API Configuration first."
            )
        }

        val cleanPrompt = SecretSanitizer.redact(prompt.trim(), key)
        require(cleanPrompt.isNotBlank()) { "Please enter a video prompt." }

        val veoModels = listOf(
            "veo-2.0-generate-001",
            "veo-3.0-fast-generate-001",
            "veo-3.0-generate-001"
        )
        var lastErrorMsg = "Could not start Veo video generation."

        for (veoModel in veoModels) {
            val endpoints = buildRestEndpoints(key, veoModel, "predictLongRunning")
            for ((startUrl, isDeveloperApi) in endpoints) {
                try {
                    onProgress("Submitting prompt to $veoModel...")
                    val bodyJson = JSONObject().apply {
                        put("instances", JSONArray().put(JSONObject().put("prompt", cleanPrompt)))
                        put("parameters", JSONObject().apply { put("aspectRatio", "16:9") })
                    }

                    val startReq = Request.Builder()
                        .url(startUrl)
                        .apply { if (!startUrl.contains("key=")) addHeader("x-goog-api-key", key) }
                        .addHeader("Content-Type", "application/json")
                        .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    val operationName = okHttpClient.newCall(startReq).execute().use { res ->
                        val raw = res.body?.string().orEmpty()
                        if (!res.isSuccessful) {
                            lastErrorMsg = mapHttpError(res.code, SecretSanitizer.redact(raw, key))
                            null
                        } else {
                            JSONObject(raw).optString("name").takeIf { it.isNotBlank() }
                        }
                    } ?: continue

                    return@withContext pollAndSaveVeoOperation(
                        context = context,
                        operationName = operationName,
                        apiKey = key,
                        isDeveloperApi = isDeveloperApi,
                        onProgress = onProgress
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastErrorMsg = SecretSanitizer.redact(e.localizedMessage ?: "Veo error", key)
                }
            }
        }

        throw IllegalStateException(
            "$lastErrorMsg (Note: Google Veo video generation requires a Google AI Studio / Google Cloud billed key with Veo enabled.)"
        )
    }

    private suspend fun pollAndSaveVeoOperation(
        context: Context,
        operationName: String,
        apiKey: String,
        isDeveloperApi: Boolean,
        onProgress: (String) -> Unit
    ): File = withTimeout(180_000L) {
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val baseHost = if (isDeveloperApi || !operationName.startsWith("projects/")) {
            "https://generativelanguage.googleapis.com/v1beta/$operationName?key=$encodedKey"
        } else {
            "https://aiplatform.googleapis.com/v1/$operationName?key=$encodedKey"
        }

        var pollCount = 0
        while (true) {
            pollCount++
            onProgress("Rendering video with Gemini Veo (step $pollCount)...")
            delay(5_000L)

            val pollReq = Request.Builder()
                .url(baseHost)
                .addHeader("x-goog-api-key", apiKey)
                .get()
                .build()

            val opObj = okHttpClient.newCall(pollReq).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    throw IllegalStateException(mapHttpError(res.code, SecretSanitizer.redact(raw, apiKey)))
                }
                JSONObject(raw)
            }

            if (opObj.has("error")) {
                val errMsg = opObj.optJSONObject("error")?.optString("message") ?: "Video generation failed."
                throw IllegalStateException(SecretSanitizer.redact(errMsg, apiKey))
            }

            if (opObj.optBoolean("done", false)) {
                val base64Video = extractVideoBase64(opObj)
                if (!base64Video.isNullOrBlank()) {
                    onProgress("Saving rendered MP4 video...")
                    val dir = File(context.cacheDir, "generated_videos").apply { mkdirs() }
                    val outFile = File(dir, "veo_${System.currentTimeMillis()}.mp4")
                    outFile.writeBytes(Base64.decode(base64Video, Base64.DEFAULT))
                    return@withTimeout outFile
                }

                val videoUri = extractVideoUri(opObj)
                    ?: throw IllegalStateException("Video completed, but no downloadable video URI or bytes were returned.")
                onProgress("Downloading MP4 video...")
                return@withTimeout downloadVideoToCache(context, videoUri, apiKey)
            }
        }
        @Suppress("UNREACHABLE_CODE")
        throw IllegalStateException("Video generation ended unexpectedly.")
    }

    private fun extractVideoBase64(opObj: JSONObject): String? {
        val responseObj = opObj.optJSONObject("response") ?: return null
        val genResponse = responseObj.optJSONObject("generateVideoResponse")
        val samples = genResponse?.optJSONArray("generatedSamples")
            ?: responseObj.optJSONArray("generatedSamples")
        if (samples != null && samples.length() > 0) {
            val videoObj = samples.getJSONObject(0).optJSONObject("video")
            val b64 = videoObj?.optString("bytesBase64Encoded")?.takeIf { it.isNotBlank() }
                ?: videoObj?.optString("encodedVideo")?.takeIf { it.isNotBlank() }
            if (!b64.isNullOrBlank()) return b64
        }
        val videos = responseObj.optJSONArray("videos")
        if (videos != null && videos.length() > 0) {
            val b64 = videos.getJSONObject(0).optString("bytesBase64Encoded")
            if (b64.isNotBlank()) return b64
        }
        return null
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
        val finalUrl = if (videoUri.contains("key=")) videoUri else {
            val sep = if (videoUri.contains("?")) "&" else "?"
            "$videoUri${sep}key=${URLEncoder.encode(apiKey, "UTF-8")}"
        }
        val req = Request.Builder()
            .url(finalUrl)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()
        okHttpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                throw IllegalStateException("Failed to download generated video (${res.code}).")
            }
            val stream = res.body?.byteStream() ?: throw IllegalStateException("Empty video download stream.")
            outFile.outputStream().use { out -> stream.copyTo(out) }
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
        val candidates = resolveModelCandidates(modelId)
        var lastError: Throwable? = null

        for (candidateModel in candidates) {
            try {
                val model = Firebase.ai.generativeModel(
                    modelName = candidateModel,
                    generationConfig = generationConfig {
                        this.temperature = temperature
                        maxOutputTokens = 8192
                    },
                    systemInstruction = content { text(systemInstruction) }
                )

                val chatHistory = buildFirebaseHistory(history)
                val chat = model.startChat(history = chatHistory)

                val outgoingContent = content(role = "user") {
                    appendAttachmentsToFirebaseContent(this, attachments)
                    val effectiveText = userPrompt.ifBlank {
                        "Please analyze the attached file(s) and explain clearly."
                    }
                    text(effectiveText)
                }

                var emittedAny = false
                withTimeout(75_000L) {
                    chat.sendMessageStream(outgoingContent).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            emittedAny = true
                            emit(chunkText)
                        }
                    }
                }
                if (emittedAny) return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Firebase model $candidateModel failed: ${e.message}")
            }
        }
        throw (lastError ?: IllegalStateException("Model returned an empty response."))
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
        val cleanKey = apiKey.trim()
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
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                }
            )
            expectingUser = !expectingUser
        }
        if (!expectingUser && contentsArray.length() > 0) {
            contentsArray.remove(contentsArray.length() - 1)
        }

        val candidates = resolveModelCandidates(modelId)
        var lastError = "Failed to connect with API key."

        for (candidateModel in candidates) {
            val endpoints = buildRestEndpoints(cleanKey, candidateModel, "streamGenerateContent?alt=sse")
            for ((url, supportsFilesApi) in endpoints) {
                try {
                    val currentParts = JSONArray()
                    appendAttachmentsToJsonParts(currentParts, attachments, cleanKey, supportsFilesApi)
                    currentParts.put(
                        JSONObject().put(
                            "text",
                            userPrompt.ifBlank { "Please analyze the attached file(s) and explain clearly." }
                        )
                    )
                    val fullContents = JSONArray(contentsArray.toString()).put(
                        JSONObject().apply {
                            put("role", "user")
                            put("parts", currentParts)
                        }
                    )

                    val requestBodyJson = JSONObject().apply {
                        put(
                            "systemInstruction",
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                        )
                        put("contents", fullContents)
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
                        .apply { if (!url.contains("key=")) addHeader("x-goog-api-key", cleanKey) }
                        .addHeader("Content-Type", "application/json")
                        .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    var emitted = false
                    withTimeout(75_000L) {
                        okHttpClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                val errBody = response.body?.string().orEmpty()
                                lastError = mapHttpError(response.code, SecretSanitizer.redact(errBody, cleanKey))
                                return@use
                            }

                            val body = response.body ?: return@use
                            val reader = BufferedReader(InputStreamReader(body.byteStream()))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val trimmed = line?.trim().orEmpty()
                                if (!trimmed.startsWith("data:")) continue
                                val jsonStr = trimmed.removePrefix("data:").trim()
                                if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue

                                val chunkObj = JSONObject(jsonStr)
                                val candidatesArr = chunkObj.optJSONArray("candidates") ?: continue
                                if (candidatesArr.length() == 0) continue
                                val parts = candidatesArr.getJSONObject(0)
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
                        }
                    }
                    if (emitted) return@flow
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = SecretSanitizer.redact(e.localizedMessage ?: "Connection failed", cleanKey)
                }
            }
        }
        throw IllegalStateException(lastError)
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
                        "No API key configured. Enter your personal key or switch to Firebase AI Logic."
                    )
                }
                val candidates = resolveModelCandidates(modelId)
                var lastErr = "Connection failed."
                for (candidateModel in candidates) {
                    for ((url, _) in buildRestEndpoints(key, candidateModel, "generateContent")) {
                        val payload = JSONObject().apply {
                            put(
                                "contents",
                                JSONArray().put(
                                    JSONObject().put("role", "user")
                                        .put("parts", JSONArray().put(JSONObject().put("text", "Reply with OK")))
                                )
                            )
                            put("generationConfig", JSONObject().put("maxOutputTokens", 16))
                        }
                        val req = Request.Builder()
                            .url(url)
                            .apply { if (!url.contains("key=")) addHeader("x-goog-api-key", key) }
                            .addHeader("Content-Type", "application/json")
                            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                            .build()

                        val connectedResult = withTimeout(30_000L) {
                            okHttpClient.newCall(req).execute().use { res ->
                                if (res.isSuccessful) {
                                    val latency = System.currentTimeMillis() - startMs
                                    ConnectionTestState.Connected(
                                        model = candidateModel,
                                        latencyMs = latency,
                                        engineLabel = if (key.startsWith("AQ.")) "API Key (Vertex Express / Cloud)" else "Gemini API Key (Encrypted)"
                                    )
                                } else {
                                    val errBody = SecretSanitizer.redact(res.body?.string().orEmpty(), key)
                                    lastErr = mapHttpError(res.code, errBody)
                                    null
                                }
                            }
                        }
                        if (connectedResult != null) return@withContext connectedResult
                    }
                }
                // Automatic fallback verification to Firebase AI Logic so app stays usable
                for (fbModel in candidates) {
                    try {
                        val response = Firebase.ai.generativeModel(modelName = fbModel).generateContent("Reply with OK")
                        if (!response.text.isNullOrBlank()) {
                            val latency = System.currentTimeMillis() - startMs
                            return@withContext ConnectionTestState.Connected(
                                model = fbModel,
                                latencyMs = latency,
                                engineLabel = "Study AI Fallback (Firebase AI Logic)"
                            )
                        }
                    } catch (_: Exception) {
                    }
                }
                return@withContext ConnectionTestState.Failed(lastErr)
            } else {
                val candidates = resolveModelCandidates(modelId)
                var lastErr = "Connection failed."
                for (candidateModel in candidates) {
                    try {
                        val model = Firebase.ai.generativeModel(modelName = candidateModel)
                        val connectedResult = withTimeout(30_000L) {
                            val response = model.generateContent("Reply with OK")
                            if (!response.text.isNullOrBlank()) {
                                val latency = System.currentTimeMillis() - startMs
                                ConnectionTestState.Connected(
                                    model = candidateModel,
                                    latencyMs = latency,
                                    engineLabel = "Firebase AI Logic (App Check)"
                                )
                            } else {
                                null
                            }
                        }
                        if (connectedResult != null) return@withContext connectedResult
                    } catch (e: Exception) {
                        lastErr = e.localizedMessage ?: "Firebase AI error"
                    }
                }
                ConnectionTestState.Failed(friendlyErrorMessage(SecretSanitizer.redact(lastErr)))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection test timed out", e)
            ConnectionTestState.Failed("Connection test timed out after 30 seconds.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Connection test failed", e)
            val cleaned = SecretSanitizer.redact(e.localizedMessage ?: "Connection failed.", customApiKey)
            ConnectionTestState.Failed(friendlyErrorMessage(cleaned))
        }
    }

    private fun cleanJsonEnvelope(raw: String): String {
        val t = raw.trim()
        val unwrapped = t.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = unwrapped.indexOf('{')
        val end = unwrapped.lastIndexOf('}')
        return if (start >= 0 && end > start) unwrapped.substring(start, end + 1) else unwrapped
    }

    fun mapExceptionToFriendlyMessage(throwable: Throwable, customKey: String? = null): String {
        if (throwable is TimeoutCancellationException) {
            return "Request timed out. Please check your internet connection and try again."
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
            401, 403 -> "Authentication failed ($code). Please verify your API key in API Configuration."
            404 -> "Selected model or endpoint was not found (404). ${body.take(120)}"
            429 -> "Rate limit or quota exceeded (429). Please wait a moment and try again."
            503 -> "Service is temporarily busy (503). Please try again shortly."
            else -> "API error ($code): ${body.take(180)}"
        }
    }

    private fun friendlyErrorMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("api_key_invalid") || lower.contains("api key not valid") || lower.contains("401") ->
                "Invalid API key. Please check your key in API Configuration."
            lower.contains("403") || lower.contains("app check") || lower.contains("attestation") ->
                "API authorization or App Check verification failed (403). Check your API configuration or network."
            lower.contains("429") || lower.contains("quota") || lower.contains("resource_exhausted") ->
                "Rate limit or quota reached (429). Please wait a few seconds and retry."
            lower.contains("503") || lower.contains("overloaded") || lower.contains("unavailable") ->
                "AI service is temporarily unavailable (503). Please try again in a moment."
            lower.contains("safety") || lower.contains("blocked") ->
                "Response was blocked by safety filters. Please rephrase your question."
            else -> raw
        }
    }

    class MissingVideoApiKeyException(message: String) : IllegalStateException(message)

    private companion object {
        const val TAG = "GeminiRepository"
    }
}
