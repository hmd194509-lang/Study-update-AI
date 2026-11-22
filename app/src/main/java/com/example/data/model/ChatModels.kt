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
    HSC_PHYSICS("HSC Physics", "পদার্থবিজ্ঞান", "Formulas, CQ, MCQ & labeled diagrams"),
    HSC_CHEMISTRY("HSC Chemistry", "রসায়ন", "Reactions, organic, formulas & diagrams"),
    HSC_MATH("HSC Math", "উচ্চতর গণিত", "Step-by-step calculus, vectors & algebra"),
    HSC_BIOLOGY("HSC Biology", "জীববিজ্ঞান", "Botany, zoology, CQ & labeled diagrams"),
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
    val aiVoiceName: String = "AOEDE",
    val ttsPitch: Float = 1.0f,
    val selectedModelId: String = DEFAULT_MODEL_ID,
    val useCustomApiKey: Boolean = false,
    val usingDefaultOwnerKey: Boolean = false,
    val maskedCustomApiKey: String? = null,
    val offlineModeEnabled: Boolean = false
)

data class GeminiModelOption(
    val id: String,
    val title: String,
    val subtitle: String
)

const val DEFAULT_MODEL_ID = "gemini-3.8-flash"
const val GEMINI_API_KEY_PORTAL_URL = "https://aistudio.google.com/apikey"

val SUPPORTED_GEMINI_MODELS = listOf(
    GeminiModelOption(
        id = "gemini-3.8-flash",
        title = "Study AI 3.8 Flash (Recommended)",
        subtitle = "Fast frontier intelligence for study, coding, Bengali/English, images & 1GB PDFs."
    ),
    GeminiModelOption(
        id = "gemini-4-hydrogen",
        title = "Study AI 4 Hydrogen (New)",
        subtitle = "Next-gen fast reasoning & bilingual HSC/Admission clarity."
    ),
    GeminiModelOption(
        id = "gemini-3.5-flash-lite",
        title = "Study AI 3.5 Flash-Lite",
        subtitle = "Ultra-low latency & high efficiency for quick Q&A and summaries."
    ),
    GeminiModelOption(
        id = "gemini-4-argon",
        title = "Study AI 4 Argon",
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
    val generatedImagePath: String = "",
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
        val bytes: ByteArray = ByteArray(0),
        val localFilePath: String = "",
        val sizeBytes: Long = bytes.size.toLong()
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

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val bitmap: Bitmap, val prompt: String, val caption: String = "") : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

sealed interface VideoGenUiState {
    data object Idle : VideoGenUiState
    data class Loading(val statusMessage: String) : VideoGenUiState
    data class Success(val videoFilePath: String, val prompt: String) : VideoGenUiState
    data class Error(val message: String, val needsApiKey: Boolean = false) : VideoGenUiState
}

enum class LiveVoiceMode(val label: String) {
    GEMINI_LIVE("Live Voice Audio"),
    BILINGUAL_LOOP("Voice Loop (বাংলা & EN)")
}

sealed interface LiveVoiceState {
    data object Idle : LiveVoiceState
    data object Connecting : LiveVoiceState
    data class Active(
        val statusText: String,
        val isMuted: Boolean = false,
        val isModelSpeaking: Boolean = false,
        val lastTranscript: String = ""
    ) : LiveVoiceState
    data class Error(val message: String) : LiveVoiceState
}

data class AppLimitRule(
    val id: String,
    val appName: String,
    val packageName: String = "",
    val dailyLimitMinutes: Int,
    val remainingSeconds: Int,
    val isTrackingActive: Boolean = false,
    val isLocked: Boolean = false,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

data class UnlockCodeRecord(
    val id: String,
    val code: String,
    val targetAppId: String,
    val targetAppName: String,
    val extraMinutes: Int,
    val reason: String,
    val recipientContact: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val isUsed: Boolean = false
)

enum class HscSubject(val label: String, val bnLabel: String, val studyMode: StudyMode) {
    PHYSICS("Physics", "পদার্থবিজ্ঞান", StudyMode.HSC_PHYSICS),
    CHEMISTRY("Chemistry", "রসায়ন", StudyMode.HSC_CHEMISTRY),
    BIOLOGY("Biology", "জীববিজ্ঞান", StudyMode.HSC_BIOLOGY)
}

data class HscTopicCard(
    val id: String,
    val subject: HscSubject,
    val title: String,
    val bnTitle: String,
    val levelBadge: String,
    val keyFormulasOrFacts: List<String>,
    val admissionTip: String,
    val explainPrompt: String,
    val diagramPrompt: String
)

enum class FlashcardSubject(val label: String, val bnLabel: String) {
    PHYSICS("Physics", "পদার্থ"),
    CHEMISTRY("Chemistry", "রসায়ন"),
    BIOLOGY("Biology", "জীববিজ্ঞান"),
    MATH("Math", "গণিত"),
    GENERAL("General", "সাধারণ");

    companion object {
        fun fromRaw(raw: String?): FlashcardSubject =
            entries.firstOrNull { it.name == raw } ?: GENERAL
    }
}

enum class FlashcardType(val label: String, val bnLabel: String, val promptHint: String) {
    MIXED("Mixed Exam", "মিশ্র", "mix of core concepts, formulas, definitions and exam traps"),
    FORMULA("Formulas", "সূত্রাবলি", "mathematical & scientific formulas, SI units, and derivations"),
    DEFINITION("Definitions & Laws", "সংজ্ঞা ও সূত্র", "crystal-clear definitions, laws, and scientific principles"),
    MCQ_TRAP("MCQ Traps", "এমসিকিউ ট্র্যাপ", "tricky admission MCQ facts, exceptions, and common mistakes"),
    CQ_SHORT("CQ (ক ও খ)", "সৃজনশীল ক-খ", "HSC Creative Question জ্ঞানমূলক (ক) and অনুধাবনমূলক (খ) answers"),
    REACTIONS("Reactions & Cycles", "বিক্রিয়া ও চক্র", "important chemical reactions, reagents, or biological cycles");

    companion object {
        fun fromRaw(raw: String?): FlashcardType =
            entries.firstOrNull { it.name == raw } ?: MIXED
    }
}

data class StudyFlashcard(
    val id: String,
    val subject: FlashcardSubject,
    val type: FlashcardType = FlashcardType.MIXED,
    val front: String,
    val back: String,
    val admissionNote: String = "",
    val isMastered: Boolean = false,
    val reviewCount: Int = 0,
    val isCustom: Boolean = false,
    val lastReviewedMs: Long = 0L
)

data class StudyProgressStats(
    val totalQuestionsAsked: Int = 0,
    val offlineQuestionsAnswered: Int = 0,
    val flashcardsReviewed: Int = 0,
    val hscTopicsStudied: Int = 0,
    val studyStreakDays: Int = 0,
    val lastStudyDayKey: String = "",
    val subjectQuestionCounts: Map<String, Int> = emptyMap(),
    val dailyActivityCounts: Map<String, Int> = emptyMap(),
    val unlockRequestsApproved: Int = 0,
    val unlockRequestsRejected: Int = 0
)

data class UnlockReasonEvaluation(
    val approved: Boolean,
    val score: Int,
    val feedback: String,
    val evaluatedByAi: Boolean
)

enum class AiVoicePreset(
    val geminiVoiceId: String,
    val displayName: String,
    val toneDesc: String,
    val defaultPitch: Float
) {
    AOEDE("AOEDE", "Aoede", "Warm & Clear (Female)", 1.05f),
    KORE("KORE", "Kore", "Calm Tutor (Female)", 1.0f),
    PUCK("PUCK", "Puck", "Upbeat & Friendly (Male)", 0.98f),
    CHARON("CHARON", "Charon", "Deep & Steady (Male)", 0.85f),
    FENRIR("FENRIR", "Fenrir", "Crisp & Bold (Male)", 0.90f);

    companion object {
        fun fromId(id: String?): AiVoicePreset =
            entries.firstOrNull { it.geminiVoiceId.equals(id, ignoreCase = true) } ?: AOEDE
    }
}

data class StudyTimerSession(
    val id: String,
    val subject: FlashcardSubject,
    val durationMinutes: Int,
    val completedAtMs: Long = System.currentTimeMillis()
)

enum class AdmissionCategory(val label: String) {
    MEDICAL("Medical & Dental"),
    ENGINEERING("Engineering"),
    UNIVERSITY("Public & GST"),
    AGRI_OTHER("Agri & Special"),
    PRIVATE("Private Universities")
}

data class AdmissionUniversityInfo(
    val id: String,
    val name: String,
    val bnName: String,
    val category: AdmissionCategory,
    val circularSchedule: String,
    val examSchedule: String,
    val eligibilityAndMarks: String,
    val registrationUrl: String,
    val noticeUrl: String,
    val aiPrepPrompt: String
)

enum class NotebookSourceType {
    PDF,
    NOTE;

    companion object {
        fun fromRaw(raw: String?): NotebookSourceType =
            entries.firstOrNull { it.name == raw } ?: NOTE
    }
}

data class NotebookSource(
    val id: String,
    val title: String,
    val type: NotebookSourceType,
    val sizeBytes: Long = 0L,
    val localFilePath: String = "",
    val textContent: String = "",
    val addedAtMs: Long = System.currentTimeMillis()
)

data class PodcastTurn(
    val speaker: String, // "HOST_A" or "HOST_B"
    val speakerName: String,
    val text: String
)

data class NotebookMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class NotebookArtifactType(val label: String, val bnLabel: String) {
    STUDY_GUIDE("Study Guide", "স্টাডি গাইড"),
    EXAM_FAQ("Exam FAQ & Formulas", "পরীক্ষার প্রশ্নোত্তর ও সূত্র"),
    PRACTICE_QUIZ("MCQ Practice Quiz", "এমসিকিউ কুইজ")
}

data class GeminiNotebook(
    val id: String,
    val title: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val sources: List<NotebookSource> = emptyList(),
    val podcastTitle: String = "",
    val podcastTurns: List<PodcastTurn> = emptyList(),
    val studyGuideMarkdown: String = "",
    val faqMarkdown: String = "",
    val quizMarkdown: String = "",
    val messages: List<NotebookMessage> = emptyList()
)

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
