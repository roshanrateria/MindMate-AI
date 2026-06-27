package com.example.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MindMateApplication
import com.example.domain.model.*
import com.example.domain.repository.AIRepository
import com.example.domain.repository.WellnessRepository
import com.example.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ==================== Onboarding ViewModel ====================

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("mindmate_prefs", Context.MODE_PRIVATE)

    private val _nickname = MutableStateFlow(prefs.getString("nickname", "") ?: "")
    val nickname = _nickname.asStateFlow()

    private val _examType = MutableStateFlow(prefs.getString("exam_type", "JEE") ?: "JEE")
    val examType = _examType.asStateFlow()

    private val _biggestWorry = MutableStateFlow(prefs.getString("biggest_worry", "Parental Expectations") ?: "Parental Expectations")
    val biggestWorry = _biggestWorry.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("onboarding_completed", false))
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    fun updateNickname(name: String) {
        _nickname.value = name
    }

    fun updateExamType(type: String) {
        _examType.value = type
    }

    fun updateBiggestWorry(worry: String) {
        _biggestWorry.value = worry
    }

    fun completeOnboarding() {
        prefs.edit()
            .putString("nickname", _nickname.value)
            .putString("exam_type", _examType.value)
            .putString("biggest_worry", _biggestWorry.value)
            .putBoolean("onboarding_completed", true)
            .apply()
        _isOnboardingCompleted.value = true
    }

    fun resetOnboarding() {
        prefs.edit().clear().apply()
        _nickname.value = ""
        _examType.value = "JEE"
        _biggestWorry.value = "Parental Expectations"
        _isOnboardingCompleted.value = false
    }
}

// ==================== Daily Check-In ViewModel ====================

sealed interface CheckInUiState {
    object Idle : CheckInUiState
    object Loading : CheckInUiState
    data class Success(val analysis: AIAnalysis) : CheckInUiState
    data class Error(val message: String) : CheckInUiState
}

class CheckInViewModel(
    private val wellnessRepository: WellnessRepository,
    private val analyzeJournalUseCase: AnalyzeJournalUseCase,
    private val detectCrisisUseCase: DetectCrisisUseCase,
    private val examTypeProvider: () -> String
) : ViewModel() {

    // Inputs
    var mood = MutableStateFlow(5)
    var stress = MutableStateFlow(5)
    var anxiety = MutableStateFlow(5)
    var energy = MutableStateFlow(5)
    var motivation = MutableStateFlow(5)
    var confidence = MutableStateFlow(5)
    
    var sleepHours = MutableStateFlow(7f)
    var sleepQuality = MutableStateFlow("Restful") // Restful, Disturbed, Couldn't sleep
    var studyHours = MutableStateFlow(8f)
    var studyBreaks = MutableStateFlow(true)
    var mealsEaten = MutableStateFlow("Yes") // Yes, Skipped some, Barely ate
    
    var selectedTriggers = MutableStateFlow<Set<String>>(emptySet())
    var journalText = MutableStateFlow("")

    private val _uiState = MutableStateFlow<CheckInUiState>(CheckInUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isCrisisDetected = MutableSharedFlow<CrisisResult>()
    val isCrisisDetected = _isCrisisDetected.asSharedFlow()

    fun toggleTrigger(trigger: String) {
        val current = selectedTriggers.value
        if (current.contains(trigger)) {
            selectedTriggers.value = current - trigger
        } else {
            selectedTriggers.value = current + trigger
        }
    }

    fun submitCheckIn() {
        viewModelScope.launch {
            _uiState.value = CheckInUiState.Loading
            try {
                // 1. Create check-in domain model
                val checkIn = CheckIn(
                    mood = mood.value,
                    stress = stress.value,
                    anxiety = anxiety.value,
                    energy = energy.value,
                    motivation = motivation.value,
                    confidence = confidence.value,
                    sleepHours = sleepHours.value,
                    sleepQuality = sleepQuality.value,
                    studyHours = studyHours.value,
                    studyBreaks = studyBreaks.value,
                    mealsEaten = mealsEaten.value,
                    triggers = selectedTriggers.value.toList(),
                    journalText = journalText.value
                )

                // 2. Insert into local DB to get valid ID
                val checkInId = wellnessRepository.insertCheckIn(checkIn)
                val checkInWithId = checkIn.copy(id = checkInId.toInt())

                // 3. Scan for active crisis keywords immediately
                val crisisResult = detectCrisisUseCase(checkIn.journalText)
                if (crisisResult.isCrisis) {
                    _isCrisisDetected.emit(crisisResult)
                }

                // 4. Perform AI / fallback analysis and save to DB
                val analysis = analyzeJournalUseCase(checkInWithId, examTypeProvider())
                
                _uiState.value = CheckInUiState.Success(analysis)
            } catch (e: Exception) {
                _uiState.value = CheckInUiState.Error(e.message ?: "Failed to process check-in")
            }
        }
    }

    fun resetState() {
        _uiState.value = CheckInUiState.Idle
        mood.value = 5
        stress.value = 5
        anxiety.value = 5
        energy.value = 5
        motivation.value = 5
        confidence.value = 5
        sleepHours.value = 7f
        sleepQuality.value = "Restful"
        studyHours.value = 8f
        studyBreaks.value = true
        mealsEaten.value = "Yes"
        selectedTriggers.value = emptySet()
        journalText.value = ""
    }
}

// ==================== Dashboard ViewModel ====================

data class DashboardUiState(
    val recentCheckIns: List<CheckIn> = emptyList(),
    val overallWellnessScore: Int = 0,
    val burnoutScore: Int = 0,
    val burnoutLevel: BurnoutLevel = BurnoutLevel.RESILIENT,
    val latestAnalysis: AIAnalysis? = null,
    val streakCount: Int = 0,
    val intrapersonalBurnout: Int = 0,
    val interpersonalBurnout: Int = 0,
    val academicBurnout: Int = 0,
    val triggerCounts: Map<String, Int> = emptyMap(),
    val hasCrisisScore: Boolean = false
)

class DashboardViewModel(
    private val wellnessRepository: WellnessRepository,
    private val getWellnessScoreUseCase: GetWellnessScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeWellnessData()
    }

    private fun observeWellnessData() {
        viewModelScope.launch {
            combine(
                wellnessRepository.getAllCheckIns(),
                wellnessRepository.getAllAnalyses()
            ) { checkIns, analyses ->
                if (checkIns.isEmpty()) {
                    return@combine DashboardUiState()
                }

                val recent = checkIns.take(30)
                val latestCheckIn = recent.firstOrNull()
                
                // Calculate wellness score of latest check-in
                val wellnessScore = latestCheckIn?.let { getWellnessScoreUseCase(it) } ?: 0

                // Late analysis
                val latestAnalysis = analyses.firstOrNull()
                val burnoutScore = latestAnalysis?.totalBurnoutScore ?: 0
                val burnoutLevel = latestAnalysis?.burnoutLevel ?: BurnoutLevel.RESILIENT

                // Compile streak count (how many consecutive days with check-ins)
                val streak = calculateStreak(checkIns)

                // Trigger frequency breakdown
                val triggerMap = mutableMapOf<String, Int>()
                checkIns.forEach { ci ->
                    ci.triggers.forEach { trig ->
                        triggerMap[trig] = (triggerMap[trig] ?: 0) + 1
                    }
                }

                DashboardUiState(
                    recentCheckIns = recent,
                    overallWellnessScore = wellnessScore,
                    burnoutScore = burnoutScore,
                    burnoutLevel = burnoutLevel,
                    latestAnalysis = latestAnalysis,
                    streakCount = streak,
                    intrapersonalBurnout = latestAnalysis?.intrapersonalScore ?: 0,
                    interpersonalBurnout = latestAnalysis?.interpersonalScore ?: 0,
                    academicBurnout = latestAnalysis?.academicScore ?: 0,
                    triggerCounts = triggerMap,
                    hasCrisisScore = (burnoutScore >= 76)
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun calculateStreak(checkIns: List<CheckIn>): Int {
        if (checkIns.isEmpty()) return 0
        
        // Simple day-based difference check
        var streak = 1
        val oneDayMillis = 24 * 60 * 60 * 1000L
        
        for (i in 0 until checkIns.size - 1) {
            val diff = checkIns[i].timestamp - checkIns[i+1].timestamp
            if (diff <= oneDayMillis * 1.5) { // within 36 hours is consecutive
                streak++
            } else {
                break
            }
        }
        return streak
    }
}

// ==================== Companion Chat ViewModel ====================

enum class CompanionMode { CHAT, VENT, TUTOR }

sealed interface TutorUiState {
    object Idle : TutorUiState
    object Solving : TutorUiState
    data class Solved(val solution: String) : TutorUiState
    data class Error(val message: String) : TutorUiState
}

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Sending : ChatUiState
    data class Error(val message: String) : ChatUiState
}

class CompanionViewModel(
    private val wellnessRepository: WellnessRepository,
    private val aiRepository: AIRepository,
    private val detectCrisisUseCase: DetectCrisisUseCase,
    private val examTypeProvider: () -> String
) : ViewModel() {

    // Mode management
    private val _activeMode = MutableStateFlow(CompanionMode.CHAT)
    val activeMode = _activeMode.asStateFlow()

    // Vent Mode States
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    // Tutor Mode States
    private val _tutorUiState = MutableStateFlow<TutorUiState>(TutorUiState.Idle)
    val tutorUiState = _tutorUiState.asStateFlow()

    fun setMode(mode: CompanionMode) {
        _activeMode.value = mode
        if (mode != CompanionMode.TUTOR) {
            _tutorUiState.value = TutorUiState.Idle
        }
    }

    fun toggleListening() {
        _isListening.value = !_isListening.value
    }

    fun solveProblem(question: String, imageBase64: String?, mimeType: String?) {
        viewModelScope.launch {
            _tutorUiState.value = TutorUiState.Solving
            val result = aiRepository.tutorSolveProblem(
                userInput = question,
                imageBytesBase64 = imageBase64,
                mimeType = mimeType,
                examType = examTypeProvider()
            )
            result.fold(
                onSuccess = { solution ->
                    _tutorUiState.value = TutorUiState.Solved(solution)
                },
                onFailure = { error ->
                    _tutorUiState.value = TutorUiState.Error(error.message ?: "Failed to generate solution")
                }
            )
        }
    }

    fun setTutorSolving() {
        _tutorUiState.value = TutorUiState.Solving
    }

    fun setTutorError(message: String) {
        _tutorUiState.value = TutorUiState.Error(message)
    }

    val chatMessages: StateFlow<List<ChatMessage>> = wellnessRepository.getChatMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _isCrisisDetected = MutableSharedFlow<CrisisResult>()
    val isCrisisDetected = _isCrisisDetected.asSharedFlow()

    init {
        // Seed initial greeting message if history is empty
        viewModelScope.launch {
            val messages = wellnessRepository.getChatMessages().firstOrNull() ?: emptyList()
            if (messages.isEmpty()) {
                wellnessRepository.insertChatMessage(
                    ChatMessage(
                        sender = "ai",
                        message = "Hello! I am MindMate AI, your wellness companion. Exam prep can be a long, exhausting journey—but remember, you don't have to walk it alone. I am here for you 24/7. What's on your mind today?"
                    )
                )
            }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return
        
        viewModelScope.launch {
            _uiState.value = ChatUiState.Sending
            
            // 1. Insert user message
            val userMsg = ChatMessage(sender = "user", message = messageText)
            wellnessRepository.insertChatMessage(userMsg)

            // 2. Immediate crisis check on chat input
            val crisisResult = detectCrisisUseCase(messageText)
            if (crisisResult.isCrisis) {
                _isCrisisDetected.emit(crisisResult)
            }

            // 3. Compile check-in history summary for context memory
            val recentCheckIns = wellnessRepository.getAllCheckIns().firstOrNull()?.take(5) ?: emptyList()
            val metricsSummary = if (recentCheckIns.isNotEmpty()) {
                recentCheckIns.joinToString("; ") { ci ->
                    "Mood: ${ci.mood}/10, Stress: ${ci.stress}/10, Anxiety: ${ci.anxiety}/10, Energy: ${ci.energy}/10"
                }
            } else {
                "No recent check-ins recorded."
            }

            // 4. Generate companion reply
            val result = aiRepository.generateCompanionResponse(
                userInput = messageText,
                examType = examTypeProvider(),
                recentCheckInsSummary = metricsSummary,
                messageHistory = chatMessages.value
            )

            result.fold(
                onSuccess = { reply ->
                    wellnessRepository.insertChatMessage(ChatMessage(sender = "ai", message = reply))
                    _uiState.value = ChatUiState.Idle
                },
                onFailure = { error ->
                    // Robust local fallback reply
                    val fallbackReply = when {
                        crisisResult.isCrisis -> "I'm right here. I hear how overwhelmed you are. Please know your worth isn't defined by an exam rank. Let's take a slow deep breath together. Please check out our 'Emergency Help' page to connect with some friendly counselors who want to support you right now."
                        messageText.lowercase().contains("parent") || messageText.lowercase().contains("family") -> "Parental expectations can feel like an incredibly heavy mountain to carry. It is completely natural to feel anxious about disappointing them. Remember, their dreams for you come from love, but your peace of mind and health are what truly matter most."
                        messageText.lowercase().contains("backlog") || messageText.lowercase().contains("study") -> "Dealing with syllabus backlogs is one of the most draining parts of prep. Instead of looking at the whole pile, can you try breaking it down? Just pick one small topic to focus on for 20 minutes. You've got this."
                        messageText.lowercase().contains("mock") || messageText.lowercase().contains("test") || messageText.lowercase().contains("score") -> "A mock test scorecard measures speed and rote recall under strict timing; it does NOT measure your intelligence, your potential, or your ultimate value. Try analyzing what went wrong objectively, then close the notebook and get some restful sleep."
                        else -> "I hear you, and I am right here. The exam journey has high ups and low downs. It's completely normal to feel like this sometimes. Let's focus on taking small, bite-sized steps. What is one small thing we can do right now to make you feel slightly more relaxed?"
                    }
                    wellnessRepository.insertChatMessage(ChatMessage(sender = "ai", message = fallbackReply))
                    _uiState.value = ChatUiState.Idle
                }
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            wellnessRepository.clearChatHistory()
            wellnessRepository.insertChatMessage(
                ChatMessage(
                    sender = "ai",
                    message = "Message history cleared. I am ready to start fresh! How are you doing today?"
                )
            )
        }
    }
}

// ==================== Unified ViewModel Factory ====================

class ViewModelFactory(
    private val application: Application,
    private val wellnessRepository: WellnessRepository,
    private val analyzeJournalUseCase: AnalyzeJournalUseCase,
    private val detectCrisisUseCase: DetectCrisisUseCase,
    private val getWellnessScoreUseCase: GetWellnessScoreUseCase,
    private val examTypeProvider: () -> String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(application) as T
            }
            modelClass.isAssignableFrom(CheckInViewModel::class.java) -> {
                CheckInViewModel(wellnessRepository, analyzeJournalUseCase, detectCrisisUseCase, examTypeProvider) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(wellnessRepository, getWellnessScoreUseCase) as T
            }
            modelClass.isAssignableFrom(CompanionViewModel::class.java) -> {
                CompanionViewModel(wellnessRepository, (application as MindMateApplication).container.aiRepository, detectCrisisUseCase, examTypeProvider) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
