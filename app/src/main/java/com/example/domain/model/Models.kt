package com.example.domain.model

import java.io.Serializable

/**
 * Core Domain Model representing a student's daily wellness check-in.
 */
data class CheckIn(
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mood: Int,          // 0 to 10
    val stress: Int,        // 0 to 10
    val anxiety: Int,       // 0 to 10
    val energy: Int,        // 0 to 10
    val motivation: Int,    // 0 to 10
    val confidence: Int,    // 0 to 10
    val sleepHours: Float,
    val sleepQuality: String, // Restful, Disturbed, Couldn't sleep
    val studyHours: Float,
    val studyBreaks: Boolean,
    val mealsEaten: String,   // Yes, Skipped some, Barely ate
    val triggers: List<String>, // List of selected stressors (parental, peer, score, etc.)
    val journalText: String
)

/**
 * Domain model representing the AI's analysis of a daily journal entry.
 */
data class AIAnalysis(
    val id: Int = 0,
    val checkInId: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val primaryEmotion: String,
    val detectedTriggers: List<String>,
    val intrapersonalScore: Int, // 0 to 33
    val interpersonalScore: Int,  // 0 to 33
    val academicScore: Int,       // 0 to 33
    val totalBurnoutScore: Int,   // 0 to 100
    val burnoutLevel: BurnoutLevel,
    val suggestion: String,
    val companionMessage: String
)

/**
 * Domain model representing a single chat message in the AI Companion Chat.
 */
data class ChatMessage(
    val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String, // "user" or "ai"
    val message: String
)

/**
 * Burnout Level representing the four-tier evidence-based classification.
 */
enum class BurnoutLevel {
    RESILIENT,  // 0-30: Green
    CAUTION,    // 31-55: Yellow
    AT_RISK,    // 56-75: Orange
    CRISIS_ALERT // 76-100: Red
}

/**
 * Crisis Level representing the student's current psychological emergency status.
 */
enum class CrisisLevel {
    NONE,
    MEDIUM,
    HIGH
}

/**
 * Result of the Crisis Detection check.
 */
data class CrisisResult(
    val isCrisis: Boolean,
    val level: CrisisLevel,
    val matchedKeywords: List<String>
)
