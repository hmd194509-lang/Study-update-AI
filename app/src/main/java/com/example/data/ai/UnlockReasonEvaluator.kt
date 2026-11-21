package com.example.data.ai

import android.util.Log
import com.example.data.model.UnlockReasonEvaluation
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UnlockReasonEvaluator {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private val reasonSchema = Schema.obj(
        properties = mapOf(
            "approved" to Schema.boolean(description = "True ONLY if the reason is genuinely useful, productive, educational, work-related, or an urgent real need. False if lazy, entertainment, gaming, vague, gibberish, or time-wasting."),
            "score" to Schema.integer(description = "Usefulness score from 0 to 100. Must be >= 65 to approve."),
            "feedback" to Schema.string(description = "Clear 1-2 sentence explanation (in Bengali or English matching the user) telling why the request was approved or why the code was denied.")
        )
    )

    private val aiEvaluatorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reasonSchema
                temperature = 0.1f
            },
            systemInstruction = content {
                text(
                    "You are a strict digital-wellbeing gatekeeper. A user has locked an app due to daily time limit expiry and is asking for an unlock code. " +
                        "Approve ONLY genuinely useful, productive, educational, exam-prep, assignment, coding, medical, or urgent family/teacher communication reasons (score >= 65). " +
                        "REJECT vague requests ('give code', 'please', 'need app', 'test'), entertainment/scrolling ('watch reels', 'shorts', 'game khelbo', 'bored', 'timepass', 'movie', 'emni', 'সময় কাটানো', 'গেম'), or gibberish."
                )
            }
        )
    }

    suspend fun evaluate(
        appName: String,
        extraMinutes: Int,
        rawReason: String,
        isOffline: Boolean,
        useCustomKey: Boolean,
        customApiKey: String?
    ): UnlockReasonEvaluation = withContext(Dispatchers.IO) {
        val reason = rawReason.trim()

        // Step 1: Hard pre-validation (runs always before anything else)
        val hardReject = preValidateReason(reason)
        if (hardReject != null) {
            return@withContext hardReject
        }

        // Step 2: If offline, use strict deterministic usefulness heuristic
        if (isOffline) {
            return@withContext evaluateLocally(reason, evaluatedByAi = false)
        }

        // Step 3: Evaluate via Gemini AI for nuanced English / Bengali / Banglish understanding
        try {
            val prompt = "App to unlock: $appName\nExtra minutes requested: $extraMinutes\nUser reason: \"$reason\""
            val jsonStr = if (useCustomKey && !customApiKey.isNullOrBlank()) {
                evaluateViaCustomKey(customApiKey.trim(), prompt)
            } else {
                withTimeout(30_000L) {
                    aiEvaluatorModel.generateContent(prompt).text
                        ?: throw IllegalStateException("Empty evaluation response.")
                }
            }

            val obj = JSONObject(jsonStr)
            val score = obj.optInt("score", 0).coerceIn(0, 100)
            val aiApproved = obj.optBoolean("approved", false) && score >= 65
            val feedback = obj.optString("feedback").trim().ifBlank {
                if (aiApproved) "Request verified as useful."
                else "কারণটি যথেষ্ট দরকারি নয় — উপযুক্ত শিক্ষা বা জরুরি কারণ লিখুন।"
            }
            UnlockReasonEvaluation(
                approved = aiApproved,
                score = score,
                feedback = feedback,
                evaluatedByAi = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI unlock reason check failed, using strict local validator", e)
            evaluateLocally(reason, evaluatedByAi = false)
        }
    }

    private fun evaluateViaCustomKey(apiKey: String, prompt: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"
        val systemText = "You are a strict digital-wellbeing gatekeeper. Approve ONLY genuinely useful, educational, study, work, or urgent reasons (score >= 65). Reject lazy, timepass, gaming, reels, or vague reasons. Return JSON with keys approved (boolean), score (integer 0-100), feedback (string)."
        val body = JSONObject().apply {
            put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText)))
            )
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                }
            )
        }
        val req = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        okHttpClient.newCall(req).execute().use { res ->
            if (!res.isSuccessful) error("HTTP ${res.code}")
            val root = JSONObject(res.body?.string().orEmpty())
            return root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }

    private fun preValidateReason(reason: String): UnlockReasonEvaluation? {
        val words = reason.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (reason.length < 12 || words.size < 3) {
            return UnlockReasonEvaluation(
                approved = false,
                score = 15,
                feedback = "কারণটি অনেক ছোট বা অস্পষ্ট। অন্তত ৩+ শব্দে স্পষ্ট ও দরকারি কারণ লিখুন (যেমন: HSC ফিজিক্স ক্লাস বা জরুরি অ্যাসাইনমেন্ট)।",
                evaluatedByAi = false
            )
        }

        val lower = reason.lowercase()
        val uniqueChars = lower.filter { !it.isWhitespace() }.toSet().size
        if (uniqueChars < 5 || Regex("(.)\\1{4,}").containsMatchIn(lower)) {
            return UnlockReasonEvaluation(
                approved = false,
                score = 10,
                feedback = "ভুয়া বা অর্থহীন লেখা গ্রহণযোগ্য নয়। সঠিক ও দরকারি কারণ না দিলে আনলক কোড দেওয়া হবে না।",
                evaluatedByAi = false
            )
        }

        val uselessPhrases = listOf(
            "timepass", "time pass", "bored", "bore lagche", "just like that", "emni",
            "game khelbo", "play game", "gaming", "free fire", "pubg", "bgmi",
            "watch reels", "shorts", "tiktok", "funny video", "movie", "natok",
            "give code", "code dao", "unlock please", "pls give", "test reason",
            "এমনি", "সময় কাটানো", "বিরক্ত লাগছে", "গেম খেলব", "মজা করব", "কোড দাও", "রিলস দেখব"
        )
        if (uselessPhrases.any { lower.contains(it) }) {
            return UnlockReasonEvaluation(
                approved = false,
                score = 20,
                feedback = "বিনোদন, গেম বা অপ্রয়োজনীয় কারণে আনলক কোড দেওয়া হবে না। শুধুমাত্র পড়াশোনা, কাজ বা জরুরি প্রয়োজনে আবেদন করুন।",
                evaluatedByAi = false
            )
        }
        return null
    }

    private fun evaluateLocally(reason: String, evaluatedByAi: Boolean): UnlockReasonEvaluation {
        val lower = reason.lowercase()
        val usefulKeywords = listOf(
            "study", "class", "lecture", "exam", "hsc", "ssc", "admission", "physics",
            "chemistry", "biology", "math", "homework", "assignment", "notes", "pdf",
            "book", "teacher", "sir", "madam", "parent", "mom", "dad", "father", "mother",
            "urgent", "emergency", "medical", "doctor", "project", "coding", "submit",
            "quiz", "revision", "tutorial", "research", "college", "university", "work",
            "পড়া", "পড়া", "ক্লাস", "লেকচার", "পরীক্ষা", "অ্যাসাইনমেন্ট", "নোট", "বই",
            "স্যার", "শিক্ষক", "অভিভাবক", "বাবা", "মা", "জরুরি", "চিকিৎসা", "প্রজেক্ট", "জমা"
        )
        val matched = usefulKeywords.count { lower.contains(it) }
        return if (matched >= 1 && reason.length >= 15) {
            UnlockReasonEvaluation(
                approved = true,
                score = (70 + matched * 8).coerceAtMost(95),
                feedback = "কারণটি উপযুক্ত ও দরকারি হিসেবে যাচাই করা হয়েছে। আনলক কোড তৈরি সম্পন্ন।",
                evaluatedByAi = evaluatedByAi
            )
        } else {
            UnlockReasonEvaluation(
                approved = false,
                score = 38,
                feedback = "কারণটি পড়াশোনা বা জরুরি প্রয়োজনের মতো যথেষ্ট দরকারি মনে হয়নি। স্পষ্টভাবে কোন ক্লাস/নোট/কাজ বা জরুরি কারণে সময় প্রয়োজন তা লিখুন।",
                evaluatedByAi = evaluatedByAi
            )
        }
    }

    private companion object {
        const val TAG = "UnlockReasonEvaluator"
    }
}
