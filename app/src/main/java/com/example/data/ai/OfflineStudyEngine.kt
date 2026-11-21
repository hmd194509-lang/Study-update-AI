package com.example.data.ai

import com.example.data.model.HscSubject
import com.example.data.model.StudyFlashcard
import com.example.data.model.StudyMode

object OfflineStudyEngine {

    fun buildOfflineReply(
        prompt: String,
        mode: StudyMode,
        flashcards: List<StudyFlashcard>
    ): String {
        val clean = prompt.trim()
        val tokens = clean.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 2 }

        val targetSubject = when (mode) {
            StudyMode.HSC_PHYSICS -> HscSubject.PHYSICS
            StudyMode.HSC_CHEMISTRY -> HscSubject.CHEMISTRY
            StudyMode.HSC_BIOLOGY -> HscSubject.BIOLOGY
            else -> null
        }

        val scoredTopics = HscKnowledgeBank.topics.map { topic ->
            val haystack = (
                "${topic.title} ${topic.bnTitle} ${topic.levelBadge} " +
                    topic.keyFormulasOrFacts.joinToString(" ") +
                    " ${topic.admissionTip}"
                ).lowercase()
            var score = tokens.count { haystack.contains(it) } * 3
            if (targetSubject == topic.subject) score += 2
            topic to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(2)

        val scoredCards = flashcards.map { card ->
            val haystack = "${card.subject.label} ${card.subject.bnLabel} ${card.front} ${card.back} ${card.admissionNote}".lowercase()
            val score = tokens.count { haystack.contains(it) }
            card to score
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(3)

        return buildString {
            appendLine("> **Offline Mode** — Answered from your on-device HSC Knowledge Base & Flashcard decks (no internet used).")
            appendLine()

            if (scoredTopics.isNotEmpty() || scoredCards.isNotEmpty()) {
                if (scoredTopics.isNotEmpty()) {
                    scoredTopics.forEach { (topic, _) ->
                        appendLine("### ${topic.title} (${topic.bnTitle})")
                        appendLine("**${topic.levelBadge}**")
                        topic.keyFormulasOrFacts.forEach { fact ->
                            appendLine("- `$fact`")
                        }
                        appendLine("- **Admission Tip:** ${topic.admissionTip}")
                        appendLine()
                    }
                }
                if (scoredCards.isNotEmpty()) {
                    appendLine("### Matching Study Flashcards")
                    scoredCards.forEach { (card, _) ->
                        appendLine("**Q:** ${card.front}")
                        appendLine("**A:** ${card.back}")
                        if (card.admissionNote.isNotBlank()) {
                            appendLine("*Tip:* ${card.admissionNote}")
                        }
                        appendLine()
                    }
                }
            } else {
                val fallbackTopics = HscKnowledgeBank.topics.filter {
                    targetSubject == null || it.subject == targetSubject
                }.take(2)

                appendLine("No exact keyword match was found in the local offline bank for **\"$clean\"**, so here are high-yield offline notes for **${mode.label}**:")
                appendLine()
                fallbackTopics.forEach { topic ->
                    appendLine("### ${topic.title} (${topic.bnTitle})")
                    topic.keyFormulasOrFacts.take(3).forEach { fact ->
                        appendLine("- `$fact`")
                    }
                    appendLine("- **Tip:** ${topic.admissionTip}")
                    appendLine()
                }
                appendLine("You can also open **HSC & Admission Lab** or **Study Flashcards** from the menu — both work 100% offline. Turn off Offline Mode or connect to the internet for full Gemini AI reasoning.")
            }
        }.trim()
    }
}
