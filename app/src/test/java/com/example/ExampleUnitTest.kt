package com.example

import com.example.domain.model.BurnoutLevel
import com.example.domain.model.CheckIn
import com.example.domain.model.CrisisLevel
import com.example.domain.usecase.DetectCrisisUseCase
import com.example.domain.usecase.GetBurnoutScoreUseCase
import com.example.domain.usecase.GetWellnessScoreUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Robust Unit Test Suite verifying safety-critical systems, burnout scores, and wellness scaling.
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCrisisDetector_recognizesHighCrisisKeywords() {
        val detector = DetectCrisisUseCase()
        
        // High crisis triggers
        val text1 = "I can't go on, I want to commit suicide"
        val text2 = "physics is so hard I feel like ending my life and want to die"
        
        val result1 = detector(text1)
        val result2 = detector(text2)
        
        assertTrue(result1.isCrisis)
        assertEquals(CrisisLevel.HIGH, result1.level)
        assertTrue(result1.matchedKeywords.contains("suicide"))
        
        assertTrue(result2.isCrisis)
        assertEquals(CrisisLevel.HIGH, result2.level)
        assertTrue(result2.matchedKeywords.contains("want to die"))
    }

    @Test
    fun testCrisisDetector_recognizesMediumCrisisKeywords() {
        val detector = DetectCrisisUseCase()
        
        // Medium crisis triggers
        val text1 = "there's no point in prep, I'm a worthless student"
        val text2 = "I feel so hopeless I want to disappear"
        
        val result1 = detector(text1)
        val result2 = detector(text2)
        
        assertTrue(result1.isCrisis)
        assertEquals(CrisisLevel.MEDIUM, result1.level)
        assertTrue(result1.matchedKeywords.contains("no point"))
        
        assertTrue(result2.isCrisis)
        assertEquals(CrisisLevel.MEDIUM, result2.level)
        assertTrue(result2.matchedKeywords.contains("disappear"))
    }

    @Test
    fun testCrisisDetector_ignoresNonCrisisText() {
        val detector = DetectCrisisUseCase()
        
        val text = "I am studying organic chemistry today, physics is hard but I'm trying to resolve backlogs."
        val result = detector(text)
        
        assertEquals(false, result.isCrisis)
        assertEquals(CrisisLevel.NONE, result.level)
        assertTrue(result.matchedKeywords.isEmpty())
    }

    @Test
    fun testBurnoutScoreCalculator_mapsCorrectLevels() {
        val calculator = GetBurnoutScoreUseCase()
        
        // Green / Resilient boundary: 0-30
        val (score1, level1) = calculator(5, 10, 10) // total 25
        assertEquals(25, score1)
        assertEquals(BurnoutLevel.RESILIENT, level1)
        
        // Yellow / Caution boundary: 31-55
        val (score2, level2) = calculator(15, 15, 15) // total 45
        assertEquals(45, score2)
        assertEquals(BurnoutLevel.CAUTION, level2)
        
        // Orange / At Risk boundary: 56-75
        val (score3, level3) = calculator(20, 20, 25) // total 65
        assertEquals(65, score3)
        assertEquals(BurnoutLevel.AT_RISK, level3)
        
        // Red / Crisis boundary: 76-100
        val (score4, level4) = calculator(30, 28, 30) // total 88
        assertEquals(88, score4)
        assertEquals(BurnoutLevel.CRISIS_ALERT, level4)
    }

    @Test
    fun testWellnessScoreCalculator_scalesCorrectly() {
        val calculator = GetWellnessScoreUseCase()
        
        // Ideal peaceful day: high mood, 8h sleep, restful, low stress, took breaks, eaten, confident
        val peacefulCheckIn = CheckIn(
            mood = 9,
            stress = 2,
            anxiety = 1,
            energy = 8,
            motivation = 9,
            confidence = 8,
            sleepHours = 8f,
            sleepQuality = "Restful",
            studyHours = 6f,
            studyBreaks = true,
            mealsEaten = "Yes",
            triggers = emptyList(),
            journalText = "I had a great study session today."
        )
        
        val score = calculator(peacefulCheckIn)
        // Score should be highly positive (> 80)
        assertTrue(score >= 80)
        
        // Overwhelmed stressful day: low mood, poor sleep, high stress, skipped breaks, skipped meals, worthless
        val distressedCheckIn = CheckIn(
            mood = 2,
            stress = 9,
            anxiety = 8,
            energy = 3,
            motivation = 2,
            confidence = 1,
            sleepHours = 4f,
            sleepQuality = "Couldn't sleep",
            studyHours = 12f,
            studyBreaks = false,
            mealsEaten = "Barely ate",
            triggers = listOf("Family pressure"),
            journalText = "Feeling like a total failure today."
        )
        
        val badScore = calculator(distressedCheckIn)
        // Score should be very low (< 35)
        assertTrue(badScore < 35)
    }
}
