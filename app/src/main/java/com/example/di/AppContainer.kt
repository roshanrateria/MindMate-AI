package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.repository.AIRepositoryImpl
import com.example.data.repository.WellnessRepositoryImpl
import com.example.domain.repository.AIRepository
import com.example.domain.repository.WellnessRepository
import com.example.domain.usecase.*

/**
 * Dependency Container representing simple Service Locator/Manual DI.
 * Provides singleton dependencies for repositories and use cases.
 */
class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    val wellnessRepository: WellnessRepository by lazy {
        WellnessRepositoryImpl(
            checkInDao = database.checkInDao(),
            aiAnalysisDao = database.aiAnalysisDao(),
            chatMessageDao = database.chatMessageDao()
        )
    }

    val aiRepository: AIRepository by lazy {
        AIRepositoryImpl()
    }

    // Use cases
    val detectCrisisUseCase: DetectCrisisUseCase by lazy {
        DetectCrisisUseCase()
    }

    val getWellnessScoreUseCase: GetWellnessScoreUseCase by lazy {
        GetWellnessScoreUseCase()
    }

    val getBurnoutScoreUseCase: GetBurnoutScoreUseCase by lazy {
        GetBurnoutScoreUseCase()
    }

    val analyzeJournalUseCase: AnalyzeJournalUseCase by lazy {
        AnalyzeJournalUseCase(
            aiRepository = aiRepository,
            wellnessRepository = wellnessRepository,
            detectCrisisUseCase = detectCrisisUseCase,
            getBurnoutScoreUseCase = getBurnoutScoreUseCase
        )
    }
}
