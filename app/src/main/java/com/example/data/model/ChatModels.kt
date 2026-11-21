package com.example.data.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StudyMode(
    val label: String,
    val bnLabel: String,
    val shortDesc: String
) {
    GENERAL("Personal AI", "সাধারণ সহায়তা", "General Q&A, math, ideas & daily tasks"),
    HSC_PHYSICS("HSC Physics", "পদার্থবিজ্ঞান", "Formulas, CQ, MCQ & derivations"),
    HSC_CHEMISTRY("HSC Chemistry", "রসায়ন", "Reactions, organic & stoichiometry"),
    HSC_MATH("HSC Math", "উচ্চতর গণিত", "Step-by-step calculus, vectors & algebra"),
    HSC_BIOLOGY("HSC Biology", "জীববিজ্ঞান", "Botany, zoology, CQ & scientific names"),
    QUIZ("Quiz & Practice", "কুইজ ও অনুশীলন", "Generate MCQs, CQs & mock tests"),
    ROUTINE("Study Routine", "পড়ার রুটিন", "Custom daily & weekly schedules"),
    ADMISSION("Admission Prep", "ভর্তি প্রস্তুতি", "BUET, DU, Medical & GST prep"),
    CODING("Code & Debug", "কোডিং ও ডিবাগ", "Generate, explain & debug code"),
    WRITER("Email & Writer", "চিঠি ও আবেদনপত্র", "Emails, applications & formal documents");

    companion object {
        fun fromRaw(raw: String?): StudyMode =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class MessageRole {
    USER,
    MODEL;

    companion object {
        fun fromRaw(raw: String?): MessageRole =
            entries.firstOrNull { it.name == raw } ?: MODEL
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Dark Mode"),
    LIGHT("Light Mode");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

enum class LanguagePreference(val label: String) {
    AUTO("Auto-Detect (বাংলা & English)"),
    BN("Prefer Bengali (বাংলা)"),
    EN("Prefer English");

    companion object {
        fun fromRaw(raw: String?): LanguagePreference =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class AcademicProfile(val label: String) {
    HSC_SCIENCE("HSC Science (NCTB)"),
    ADMISSION("University / Medical / Engineering Admission"),
    GENERAL("General Learner & Developer");

    companion object {
        fun fromRaw(raw: String?): AcademicProfile =
            entries.firstOrNull { it.name == raw } ?: HSC_SCIENCE
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val languagePreference: LanguagePreference = LanguagePreference.AUTO,
    val academicProfile: AcademicProfile = AcademicProfile.HSC_SCIENCE,
    val temperature: Float = 0.7f,
    val autoSpeak: Boolean = false,
    val voiceInputLang: String = "bn-BD",
    val speechRate: Float = 1.0f,
    val selectedModelId: String = DEFAULT_MODEL_ID,
    val useCustomApiKey: Boolean = false,
    val maskedCustomApiKey: String? = null
)

data class GeminiModelOption(
    val id: String,
    val title: String,
    val subtitle: String
)

const val DEFAULT_MODEL_ID = "gemini-3.8-flash"

val SUPPORTED_GEMINI_MODELS = listOf(
    GeminiModelOption(
        id = "gemini-3.8-flash",
        title = "Gemini 3.8 Flash (Recommended)",
        subtitle = "Fast frontier intelligence for study, coding, Bengali/English, images & PDFs."
    ),
    GeminiModelOption(
        id = "gemini-3.5-flash-lite",
        title = "Gemini 3.5 Flash-Lite",
        subtitle = "Ultra-low latency & high efficiency for quick Q&A and summaries."
    ),
    GeminiModelOption(
        id = "gemini-4-argon",
        title = "Gemini 4 Argon",
        subtitle = "Advanced STEM, higher math, physics & complex coding reasoning."
    )
)

@Keep
@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Conversation",
    val mode: String = StudyMode.GENERAL.name,
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val role: String = MessageRole.USER.name,
    val content: String = "",
    val mode: String = StudyMode.GENERAL.name,
    val attachmentNames: List<String> = emptyList(),
    val errorFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface AttachmentPayload {
    data class ImageAttachment(
        val id: String,
        val name: String,
        val bitmap: Bitmap,
        val jpegBytes: ByteArray
    ) : AttachmentPayload

    data class PdfAttachment(
        val id: String,
        val name: String,
        val bytes: ByteArray
    ) : AttachmentPayload

    data class TextDocumentAttachment(
        val id: String,
        val name: String,
        val textContent: String
    ) : AttachmentPayload
}

val AttachmentPayload.displayName: String
    get() = when (this) {
        is AttachmentPayload.ImageAttachment -> name
        is AttachmentPayload.PdfAttachment -> name
        is AttachmentPayload.TextDocumentAttachment -> name
    }

val AttachmentPayload.idValue: String
    get() = when (this) {
        is AttachmentPayload.ImageAttachment -> id
        is AttachmentPayload.PdfAttachment -> id
        is AttachmentPayload.TextDocumentAttachment -> id
    }

sealed interface ConnectionTestState {
    data object Idle : ConnectionTestState
    data object Testing : ConnectionTestState
    data class Connected(val model: String, val latencyMs: Long, val engineLabel: String) : ConnectionTestState
    data class Failed(val errorMessage: String) : ConnectionTestState
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
