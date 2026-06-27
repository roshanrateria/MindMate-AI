package com.example.domain.usecase

import com.example.domain.model.*
import com.example.domain.repository.AIRepository
import com.example.domain.repository.WellnessRepository
import kotlinx.coroutines.flow.firstOrNull

/**
 * UseCase to evaluate if a journal text or check-in indicates a state of crisis.
 * Checks against 15+ severe distress phrases specifically customized for Indian exam students.
 */
class DetectCrisisUseCase {
    private val crisisKeywords = listOf(
        "no point", "worthless", "can't do this", "end it", "give up on life",
        "disappear", "kill myself", "suicide", "better off dead", "want to die",
        "ending my life", "no hope", "cannot go on", "sleeping forever",
        "nothing matters anymore", "everyone hates me", "disappoint my parents",
        "fail my family", "worthless student"
    )

    operator fun invoke(text: String): CrisisResult {
        if (text.isBlank()) return CrisisResult(false, CrisisLevel.NONE, emptyList())
        val lowercaseText = text.lowercase()
        val matched = crisisKeywords.filter { keyword ->
            lowercaseText.contains(keyword)
        }
        
        return if (matched.isNotEmpty()) {
            val level = if (matched.any { it in listOf("kill myself", "suicide", "better off dead", "want to die", "end it") }) {
                CrisisLevel.HIGH
            } else {
                CrisisLevel.MEDIUM
            }
            CrisisResult(
                isCrisis = true,
                level = level,
                matchedKeywords = matched
            )
        } else {
            CrisisResult(false, CrisisLevel.NONE, emptyList())
        }
    }
}

/**
 * UseCase to calculate a 0-100 overall student wellness score from a CheckIn.
 * Formula is evidence-aligned:
 * - Mood average (25%)
 * - Sleep quality and duration (20%)
 * - Stress trend (25% - lower stress increases score)
 * - Rest compliance / study breaks (15%)
 * - Self-confidence and meals eaten (15%)
 */
class GetWellnessScoreUseCase {
    operator fun invoke(checkIn: CheckIn): Int {
        val moodPoints = checkIn.mood * 10f // 0-100
        
        val sleepQualityPoints = when (checkIn.sleepQuality) {
            "Restful" -> 100f
            "Disturbed" -> 50f
            else -> 0f
        }
        // Ideal sleep: 7 to 9 hours
        val sleepDurationPoints = when {
            checkIn.sleepHours >= 7 && checkIn.sleepHours <= 9 -> 100f
            checkIn.sleepHours >= 5 -> 70f
            checkIn.sleepHours > 9 -> 60f
            else -> 30f
        }
        val sleepPoints = (sleepQualityPoints * 0.5f) + (sleepDurationPoints * 0.5f)

        val stressPoints = (10 - checkIn.stress) * 10f // 0-100 (low stress = high points)

        val breakPoints = if (checkIn.studyBreaks) 100f else 0f

        val mealPoints = when (checkIn.mealsEaten) {
            "Yes" -> 100f
            "Skipped some" -> 50f
            else -> 10f
        }
        val confidencePoints = checkIn.confidence * 10f
        val lifestylePoints = (mealPoints * 0.5f) + (confidencePoints * 0.5f)

        val finalScore = (moodPoints * 0.25f) +
                (sleepPoints * 0.20f) +
                (stressPoints * 0.25f) +
                (breakPoints * 0.15f) +
                (lifestylePoints * 0.15f)

        return finalScore.coerceIn(0f, 100f).toInt()
    }
}

/**
 * UseCase to map a burnout score to a defined BurnoutLevel.
 * Categories:
 * - 0 to 30: RESILIENT
 * - 31 to 55: CAUTION
 * - 56 to 75: AT_RISK
 * - 76 to 100: CRISIS_ALERT
 */
class GetBurnoutScoreUseCase {
    operator fun invoke(intrapersonal: Int, interpersonal: Int, academic: Int): Pair<Int, BurnoutLevel> {
        val total = (intrapersonal + interpersonal + academic).coerceIn(0, 100)
        val level = when {
            total >= 76 -> BurnoutLevel.CRISIS_ALERT
            total >= 56 -> BurnoutLevel.AT_RISK
            total >= 31 -> BurnoutLevel.CAUTION
            else -> BurnoutLevel.RESILIENT
        }
        return Pair(total, level)
    }
}

/**
 * UseCase to trigger NVIDIA NIM / Gemini journal analysis, or provide an empathetic local offline fallback.
 * Guarantees zero "cold errors" for offline student use.
 */
class AnalyzeJournalUseCase(
    private val aiRepository: AIRepository,
    private val wellnessRepository: WellnessRepository,
    private val detectCrisisUseCase: DetectCrisisUseCase,
    private val getBurnoutScoreUseCase: GetBurnoutScoreUseCase
) {
    suspend operator fun invoke(checkIn: CheckIn, examType: String): AIAnalysis {
        // First check for crisis
        val crisisResult = detectCrisisUseCase(checkIn.journalText)
        
        // Prepare context summary for AI
        val metricsSummary = "Mood: ${checkIn.mood}/10, Stress: ${checkIn.stress}/10, Anxiety: ${checkIn.anxiety}/10, Energy: ${checkIn.energy}/10, Motivation: ${checkIn.motivation}/10, Confidence: ${checkIn.confidence}/10, Sleep: ${checkIn.sleepHours}h (${checkIn.sleepQuality}), Study: ${checkIn.studyHours}h (Breaks: ${checkIn.studyBreaks})"

        val aiResult = aiRepository.analyzeJournal(
            journalText = checkIn.journalText,
            examType = examType,
            checkInMetricsSummary = metricsSummary
        )

        val analysis = aiResult.getOrElse {
            // Robust Offline Fallback Analysis Generator
            val detectedTriggers = mutableListOf<String>()
            if (checkIn.triggers.isNotEmpty()) {
                detectedTriggers.addAll(checkIn.triggers)
            } else {
                // Heuristically extract triggers from text
                val text = checkIn.journalText.lowercase()
                if (text.contains("parents") || text.contains("father") || text.contains("mother") || text.contains("expectations")) detectedTriggers.add("Parental Expectations")
                if (text.contains("mock") || text.contains("test") || text.contains("score") || text.contains("marks")) detectedTriggers.add("Mock Test Scores")
                if (text.contains("rank") || text.contains("peer") || text.contains("friend") || text.contains("comparison")) detectedTriggers.add("Peer Comparison")
                if (text.contains("syllabus") || text.contains("backlog") || text.contains("study") || text.contains("chemistry") || text.contains("physics") || text.contains("math")) detectedTriggers.add("Cognitive Overload")
                if (text.contains("alone") || text.contains("hostel") || text.contains("room") || text.contains("lonely")) detectedTriggers.add("Social Isolation")
            }
            if (detectedTriggers.isEmpty()) detectedTriggers.add("General Exam Tension")

            // Local heuristic calculation for Burnout Score
            // Domain 1: Intrapersonal (Fatigue, sleep, anxiety, energy)
            val intra = ((10 - checkIn.energy) * 3.3f + (10 - checkIn.confidence) * 1.5f + (if (checkIn.sleepQuality != "Restful") 15 else 0)).toInt().coerceIn(5, 33)
            // Domain 2: Interpersonal (Irritability, withdrawal, triggers containing family/friends)
            val inter = (checkIn.stress * 2.5f + (if (checkIn.triggers.contains("Feeling Alone") || checkIn.triggers.contains("Family Pressure")) 15 else 5)).toInt().coerceIn(5, 33)
            // Domain 3: Academic/Occupational (Motivation, study hours without breaks, backlog)
            val academic = ((10 - checkIn.motivation) * 2.5f + (if (!checkIn.studyBreaks) 12 else 0) + (if (checkIn.studyHours > 12) 10 else 5)).toInt().coerceIn(5, 33)

            val (totalBurnout, burnoutLevel) = getBurnoutScoreUseCase(intra, inter, academic)

            val offlineSuggestion = when {
                totalBurnout >= 76 -> "Please take a complete break today. Log off from mock prep, step out of your study desk, and speak to a trusted friend, family member, or counselor. Your safety and peace are more important than any mock test."
                checkIn.stress >= 7 || !checkIn.studyBreaks -> "You logged ${checkIn.studyHours} hours of study, but your stress is high. Try implementing the 50-10 Pomodoro routine: 50 minutes of studying followed by a mandatory 10-minute deep-breathing break outside your room."
                checkIn.sleepHours < 6 -> "Your sleep duration (${checkIn.sleepHours} hours) is below the threshold for memory consolidation. Prioritize getting at least 7 hours of undisturbed sleep tonight to boost retention tomorrow."
                else -> "Your routine is structured well today. Maintain this momentum but ensure you dedicate at least 30 minutes to winding down before bed without your phone or textbooks."
            }

            val offlineCompanionMsg = when {
                crisisResult.isCrisis -> "I am right here with you, and I hear how incredibly heavy this is. Please know that you do not have to carry this load alone. You are valuable far beyond your target rank, and help is always available."
                burnoutLevel == BurnoutLevel.CRISIS_ALERT -> "The pressure you've been putting on yourself is immense, and it's completely normal to feel exhausted. Please be gentle with yourself today. You are a person, not a mock scorecard."
                checkIn.mood <= 3 -> "I am sorry today felt so hard. It is completely okay to have low days, and admitting it is a sign of strength. Take it one hour at a time."
                else -> "You showed up for your wellness check-in today, which is a wonderful step of self-care. Let's keep focusing on small, healthy steps forward."
            }

            AIAnalysis(
                checkInId = checkIn.id,
                timestamp = System.currentTimeMillis(),
                primaryEmotion = if (checkIn.mood <= 3) "Overwhelmed" else if (checkIn.anxiety >= 6) "Anxious" else if (checkIn.motivation >= 7) "Determined" else "Reflective",
                detectedTriggers = detectedTriggers,
                intrapersonalScore = intra,
                interpersonalScore = inter,
                academicScore = academic,
                totalBurnoutScore = totalBurnout,
                burnoutLevel = burnoutLevel,
                suggestion = offlineSuggestion,
                companionMessage = offlineCompanionMsg
            )
        }

        // Save to DB
        val finalAnalysis = analysis.copy(checkInId = checkIn.id)
        wellnessRepository.insertAnalysis(finalAnalysis)
        return finalAnalysis
    }
}
