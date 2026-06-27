package com.example.domain.repository

import com.example.domain.model.CheckIn
import com.example.domain.model.AIAnalysis
import com.example.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Interface representing the local storage repository for Check-ins, AI analysis, and chat history.
 */
interface WellnessRepository {
    // Check-in methods
    fun getAllCheckIns(): Flow<List<CheckIn>>
    fun getCheckInById(id: Int): Flow<CheckIn?>
    suspend fun insertCheckIn(checkIn: CheckIn): Long
    suspend fun deleteCheckIn(id: Int)

    // AI Analysis methods
    fun getAllAnalyses(): Flow<List<AIAnalysis>>
    fun getAnalysisForCheckIn(checkInId: Int): Flow<AIAnalysis?>
    suspend fun insertAnalysis(analysis: AIAnalysis)

    // Chat methods
    fun getChatMessages(): Flow<List<ChatMessage>>
    suspend fun insertChatMessage(message: ChatMessage)
    suspend fun clearChatHistory()
}

/**
 * Interface representing the AI Service Repository which interfaces with Gemini API / NVIDIA NIM.
 */
interface AIRepository {
    /**
     * Analyzes a journal text along with selected wellness metrics, extracting:
     * - Primary/Secondary emotions
     * - Key stress triggers
     * - Burnout risk score (across intrapersonal, interpersonal, and academic domains)
     * - Personalized wellness recommendation
     * - Warm companion message
     */
    suspend fun analyzeJournal(
        journalText: String,
        examType: String,
        checkInMetricsSummary: String
    ): Result<AIAnalysis>

    /**
     * Generates a conversational response from the AI companion, taking into account
     * current context and past messaging history.
     */
    suspend fun generateCompanionResponse(
        userInput: String,
        examType: String,
        recentCheckInsSummary: String,
        messageHistory: List<ChatMessage>
    ): Result<String>

    /**
     * Solves a tutoring question with optional multimodal image input.
     */
    suspend fun tutorSolveProblem(
        userInput: String,
        imageBytesBase64: String?,
        mimeType: String?,
        examType: String
    ): Result<String>
}
