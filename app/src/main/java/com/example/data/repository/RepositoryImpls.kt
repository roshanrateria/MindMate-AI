package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.local.LocalMappers.toDomain
import com.example.data.local.LocalMappers.toEntity
import com.example.data.remote.*
import com.example.domain.model.*
import com.example.domain.repository.AIRepository
import com.example.domain.repository.WellnessRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.Serializable
/**
 * Concrete implementation of the local WellnessRepository using Room DB.
 */
class WellnessRepositoryImpl(
    private val checkInDao: CheckInDao,
    private val aiAnalysisDao: AIAnalysisDao,
    private val chatMessageDao: ChatMessageDao
) : WellnessRepository {

    override fun getAllCheckIns(): Flow<List<CheckIn>> {
        return checkInDao.getAllCheckIns().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getCheckInById(id: Int): Flow<CheckIn?> {
        return checkInDao.getCheckInById(id).map { it?.toDomain() }
    }

    override fun getAllAnalyses(): Flow<List<AIAnalysis>> {
        return aiAnalysisDao.getAllAnalyses().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAnalysisForCheckIn(checkInId: Int): Flow<AIAnalysis?> {
        return aiAnalysisDao.getAnalysisByCheckInId(checkInId).map { it?.toDomain() }
    }

    override suspend fun insertCheckIn(checkIn: CheckIn): Long {
        return checkInDao.insertCheckIn(checkIn.toEntity())
    }

    override suspend fun deleteCheckIn(id: Int) {
        checkInDao.deleteCheckIn(id)
    }

    override suspend fun insertAnalysis(analysis: AIAnalysis) {
        aiAnalysisDao.insertAnalysis(analysis.toEntity())
    }

    override fun getChatMessages(): Flow<List<ChatMessage>> {
        return chatMessageDao.getChatMessages().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertChatMessage(message: ChatMessage) {
        chatMessageDao.insertMessage(message.toEntity())
    }

    override suspend fun clearChatHistory() {
        chatMessageDao.clearHistory()
    }
}

/**
 * Concrete implementation of AIRepository communicating with the Gemini API.
 */
class AIRepositoryImpl : AIRepository {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val jsonAdapter = moshi.adapter(AIAnalysisResultSchema::class.java)

    override suspend fun analyzeJournal(
        journalText: String,
        examType: String,
        checkInMetricsSummary: String
    ): Result<AIAnalysis> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        val systemPrompt = """
            You are an expert student mental wellness assistant specialized in analyzing daily journal reflections for Indian competitive exam aspirants (JEE, NEET, UPSC, GATE, CAT, CUET, Board exams).
            Analyze the provided student's journal and core wellness metrics.
            You MUST return a JSON object with EXACTLY the following fields:
            {
              "primaryEmotion": "Anxious" / "Overwhelmed" / "Determined" / "Exhausted" / "Lonely" / "Reflective" etc. Choose the most fitting one.
              "detectedTriggers": ["Parental expectations", "Mock test score", "Peer comparison", "Syllabus backlog", "Social isolation", "Homesickness", "Financial pressure" etc. Extract up to 3 based on text and selected metrics],
              "intrapersonalScore": 0-33 score representing domain 1 (Persistent fatigue, poor sleep, anxiety, impaired focus),
              "interpersonalScore": 0-33 score representing domain 2 (Irritability, social withdrawal, home sickness, lack of support),
              "academicScore": 0-33 score representing domain 3 (Study avoidance, backlog stress, helplessness, loss of interest),
              "suggestion": "A highly practical, personalized, CBT-aligned wellness suggestion under 120 words. Enforce realistic rest cycles, healthy sleep hygiene, or study strategies. Align with the student's exam type and current triggers.",
              "companionMessage": "A warm, deeply empathetic, peer-like supportive message under 80 words. Validate their exam struggles. Emphasize that their worth is independent of their mock score or AIR rank. Use friendly, direct tone."
            }
            Do NOT include any markdown blocks (like ```json) or explanation. Return raw JSON string only.
        """.trimIndent()

        val userPrompt = """
            Student Exam: $examType
            Daily Metrics Summary: $checkInMetricsSummary
            Journal Entry text: "$journalText"
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userPrompt)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from AI service")

            val parsedSchema = jsonAdapter.fromJson(jsonText)
                ?: throw IllegalStateException("Failed to parse AI response JSON schema")

            val totalScore = (parsedSchema.intrapersonalScore + parsedSchema.interpersonalScore + parsedSchema.academicScore).coerceIn(0, 100)
            val level = when {
                totalScore >= 76 -> BurnoutLevel.CRISIS_ALERT
                totalScore >= 56 -> BurnoutLevel.AT_RISK
                totalScore >= 31 -> BurnoutLevel.CAUTION
                else -> BurnoutLevel.RESILIENT
            }

            Result.success(
                AIAnalysis(
                    timestamp = System.currentTimeMillis(),
                    primaryEmotion = parsedSchema.primaryEmotion,
                    detectedTriggers = parsedSchema.detectedTriggers,
                    intrapersonalScore = parsedSchema.intrapersonalScore,
                    interpersonalScore = parsedSchema.interpersonalScore,
                    academicScore = parsedSchema.academicScore,
                    totalBurnoutScore = totalScore,
                    burnoutLevel = level,
                    suggestion = parsedSchema.suggestion,
                    companionMessage = parsedSchema.companionMessage
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCompanionResponse(
        userInput: String,
        examType: String,
        recentCheckInsSummary: String,
        messageHistory: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        val systemPrompt = """
            You are MindMate AI, a warm, non-clinical, 24/7 mental wellness companion for Indian competitive exam aspirants (JEE, NEET, UPSC, GATE, CAT, CUET, Boards).
            
            Personality:
            - Warm, empathetic, and friend-like. NOT clinical, NOT robotic.
            - Uses the student's target exam context to ground suggestions (e.g. "NEET syllabus is huge", "Mock series can be brutal").
            - Validate their feelings before offering any structured tools. Never give toxic positive hustle advice.
            - Code-switch naturally between English and friendly Hinglish occasionally (e.g., "Take a break, sleep is important haina?").
            - Validate that their self-worth is NOT tied to an exam scorecard.
            
            Safety Rules:
            - Never diagnose conditions (e.g., "you have clinical depression"). Suggest speaking with a mental health professional or counselor for complex mental state.
            - If student mentions suicide, self-harm, or severe crisis (e.g., "end my life", "give up"), immediately and gently remind them they are not alone and direct them to connect with helplines.
            - Keep responses scannable, engaging, and under 150 words.
        """.trimIndent()

        // Compile chat content representation
        val promptParts = mutableListOf<GeminiPart>()
        
        // Add context summaries
        promptParts.add(GeminiPart(text = "[Student Context]\nExam Target: $examType\nRecent 7-day Check-In Summary: $recentCheckInsSummary"))
        
        // Add last 6 turns of history to prevent token clutter
        messageHistory.takeLast(6).forEach { msg ->
            val senderLabel = if (msg.sender == "user") "Student" else "MindMate AI"
            promptParts.add(GeminiPart(text = "$senderLabel: ${msg.message}"))
        }
        
        // Add active user input
        promptParts.add(GeminiPart(text = "Student: $userInput\nMindMate AI:"))

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = promptParts)),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty reply from AI companion")
            
            Result.success(replyText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun tutorSolveProblem(
        userInput: String,
        imageBytesBase64: String?,
        mimeType: String?,
        examType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("API key not configured"))
        }

        val systemPrompt = """
            You are MindMate AI Tutor, an exceptionally clear, empathetic, and encouraging expert educator specializing in Indian competitive exams ($examType).
            You are helping a highly stressed aspirant. Solve the problem they sent (which may be a typed question, or an uploaded textbook problem image, or both) step-by-step with absolute accuracy, deep conceptual clarity, and friendly guidance.
            
            Guidelines:
            - Explain the core concept first (e.g., "This JEE Physics problem uses the Conservation of Angular Momentum...").
            - Provide a highly structured, simple, step-by-step mathematical or conceptual breakdown.
            - End with a brief word of encouragement to keep their morale high!
            - Keep your response friendly, clear, scannable, and under 220 words so it fits beautifully on mobile.
        """.trimIndent()

        val promptParts = mutableListOf<GeminiPart>()
        if (imageBytesBase64 != null && mimeType != null) {
            promptParts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = imageBytesBase64)))
        }
        promptParts.add(GeminiPart(text = "Student Question: $userInput"))

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = promptParts)),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.3f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val solutionText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty reply from AI Tutor")
            
            Result.success(solutionText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
