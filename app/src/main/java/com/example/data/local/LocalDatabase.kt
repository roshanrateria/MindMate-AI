package com.example.data.local

import android.content.Context
import androidx.room.*
import com.example.domain.model.AIAnalysis
import com.example.domain.model.BurnoutLevel
import com.example.domain.model.ChatMessage
import com.example.domain.model.CheckIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ==================== Room Entities ====================

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val mood: Int,
    val stress: Int,
    val anxiety: Int,
    val energy: Int,
    val motivation: Int,
    val confidence: Int,
    val sleepHours: Float,
    val sleepQuality: String,
    val studyHours: Float,
    val studyBreaks: Boolean,
    val mealsEaten: String,
    val triggers: String, // Comma-separated list
    val journalText: String
)

@Entity(
    tableName = "ai_analyses",
    foreignKeys = [
        ForeignKey(
            entity = CheckInEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkInId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["checkInId"])]
)
data class AIAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val checkInId: Int,
    val timestamp: Long,
    val primaryEmotion: String,
    val detectedTriggers: String, // Comma-separated list
    val intrapersonalScore: Int,
    val interpersonalScore: Int,
    val academicScore: Int,
    val totalBurnoutScore: Int,
    val burnoutLevel: String, // Maps to enum
    val suggestion: String,
    val companionMessage: String
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val sender: String,
    val message: String
)

// ==================== Mappers ====================

object LocalMappers {
    fun CheckInEntity.toDomain(): CheckIn {
        return CheckIn(
            id = id,
            timestamp = timestamp,
            mood = mood,
            stress = stress,
            anxiety = anxiety,
            energy = energy,
            motivation = motivation,
            confidence = confidence,
            sleepHours = sleepHours,
            sleepQuality = sleepQuality,
            studyHours = studyHours,
            studyBreaks = studyBreaks,
            mealsEaten = mealsEaten,
            triggers = if (triggers.isBlank()) emptyList() else triggers.split(","),
            journalText = journalText
        )
    }

    fun CheckIn.toEntity(): CheckInEntity {
        return CheckInEntity(
            id = id,
            timestamp = timestamp,
            mood = mood,
            stress = stress,
            anxiety = anxiety,
            energy = energy,
            motivation = motivation,
            confidence = confidence,
            sleepHours = sleepHours,
            sleepQuality = sleepQuality,
            studyHours = studyHours,
            studyBreaks = studyBreaks,
            mealsEaten = mealsEaten,
            triggers = triggers.joinToString(","),
            journalText = journalText
        )
    }

    fun AIAnalysisEntity.toDomain(): AIAnalysis {
        return AIAnalysis(
            id = id,
            checkInId = checkInId,
            timestamp = timestamp,
            primaryEmotion = primaryEmotion,
            detectedTriggers = if (detectedTriggers.isBlank()) emptyList() else detectedTriggers.split(","),
            intrapersonalScore = intrapersonalScore,
            interpersonalScore = interpersonalScore,
            academicScore = academicScore,
            totalBurnoutScore = totalBurnoutScore,
            burnoutLevel = try { BurnoutLevel.valueOf(burnoutLevel) } catch (e: Exception) { BurnoutLevel.RESILIENT },
            suggestion = suggestion,
            companionMessage = companionMessage
        )
    }

    fun AIAnalysis.toEntity(): AIAnalysisEntity {
        return AIAnalysisEntity(
            id = id,
            checkInId = checkInId,
            timestamp = timestamp,
            primaryEmotion = primaryEmotion,
            detectedTriggers = detectedTriggers.joinToString(","),
            intrapersonalScore = intrapersonalScore,
            interpersonalScore = interpersonalScore,
            academicScore = academicScore,
            totalBurnoutScore = totalBurnoutScore,
            burnoutLevel = burnoutLevel.name,
            suggestion = suggestion,
            companionMessage = companionMessage
        )
    }

    fun ChatMessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = id,
            timestamp = timestamp,
            sender = sender,
            message = message
        )
    }

    fun ChatMessage.toEntity(): ChatMessageEntity {
        return ChatMessageEntity(
            id = id,
            timestamp = timestamp,
            sender = sender,
            message = message
        )
    }
}

// ==================== Room DAOs ====================

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE id = :id LIMIT 1")
    fun getCheckInById(id: Int): Flow<CheckInEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckInEntity): Long

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteCheckIn(id: Int)
}

@Dao
interface AIAnalysisDao {
    @Query("SELECT * FROM ai_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<AIAnalysisEntity>>

    @Query("SELECT * FROM ai_analyses WHERE checkInId = :checkInId LIMIT 1")
    fun getAnalysisByCheckInId(checkInId: Int): Flow<AIAnalysisEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: AIAnalysisEntity)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

// ==================== App Database ====================

@Database(
    entities = [CheckInEntity::class, AIAnalysisEntity::class, ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun checkInDao(): CheckInDao
    abstract fun aiAnalysisDao(): AIAnalysisDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindmate_wellness.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
