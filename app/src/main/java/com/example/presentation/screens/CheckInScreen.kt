package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.AIAnalysisCard
import com.example.presentation.components.WellnessSlider
import com.example.presentation.viewmodel.CheckInUiState
import com.example.presentation.viewmodel.CheckInViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    onNavigateToCompanion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val mood by viewModel.mood.collectAsState()
    val stress by viewModel.stress.collectAsState()
    val anxiety by viewModel.anxiety.collectAsState()
    val energy by viewModel.energy.collectAsState()
    val motivation by viewModel.motivation.collectAsState()
    val confidence by viewModel.confidence.collectAsState()

    val sleepHours by viewModel.sleepHours.collectAsState()
    val sleepQuality by viewModel.sleepQuality.collectAsState()
    val studyHours by viewModel.studyHours.collectAsState()
    val studyBreaks by viewModel.studyBreaks.collectAsState()
    val mealsEaten by viewModel.mealsEaten.collectAsState()

    val selectedTriggers by viewModel.selectedTriggers.collectAsState()
    val journalText by viewModel.journalText.collectAsState()

    val uiState by viewModel.uiState.collectAsState()

    val allTriggers = listOf(
        "📞 Family pressure / Call from parents",
        "👥 Compared myself to peers",
        "📊 Mock test marks / Bad ranks",
        "😴 Didn't study as planned / backlog",
        "🏠 Homesickness / Coaching hostel loneliness",
        "💸 Fees stress / Financial concerns",
        "🧪 Upcoming major test tomorrow",
        "🔇 Feeling lonely / No one to talk to",
        "😶 Feeling numb / Empty inside"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Daily Wellness Log",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                color = PrimaryIndigo
            )
            Text(
                text = "Track your patterns in under 3 minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SLIDERS SECTION
            Text(
                text = "How are you feeling right now?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            WellnessSlider(
                value = mood,
                onValueChange = { viewModel.mood.value = it },
                title = "Mood",
                lowLabel = "Overwhelmed / Sad",
                highLabel = "Calm / Peaceful",
                icon = Icons.Default.Info,
                color = ResilientGreen,
                contentDescriptionText = "Mood slider"
            )

            WellnessSlider(
                value = stress,
                onValueChange = { viewModel.stress.value = it },
                title = "Stress Level",
                lowLabel = "Completely Calm",
                highLabel = "Extremely Stressed",
                icon = Icons.Default.Warning,
                color = AtRiskOrange,
                contentDescriptionText = "Stress level slider"
            )

            WellnessSlider(
                value = anxiety,
                onValueChange = { viewModel.anxiety.value = it },
                title = "Anxiety / Panic Level",
                lowLabel = "Relaxed",
                highLabel = "High Anxiety / Panic",
                icon = Icons.Default.Warning,
                color = CrisisRed,
                contentDescriptionText = "Anxiety level slider"
            )

            WellnessSlider(
                value = energy,
                onValueChange = { viewModel.energy.value = it },
                title = "Physical Energy",
                lowLabel = "Completely Exhausted",
                highLabel = "Energized",
                icon = Icons.Default.Info,
                color = TertiaryBlue,
                contentDescriptionText = "Energy slider"
            )

            WellnessSlider(
                value = motivation,
                onValueChange = { viewModel.motivation.value = it },
                title = "Motivation to Study",
                lowLabel = "Hopeless / Zero motivation",
                highLabel = "Highly Driven",
                icon = Icons.Default.ArrowForward,
                color = PrimaryIndigo,
                contentDescriptionText = "Motivation level slider"
            )

            WellnessSlider(
                value = confidence,
                onValueChange = { viewModel.confidence.value = it },
                title = "Academic Confidence",
                lowLabel = "Catastrophic (Worthless)",
                highLabel = "Highly Confident",
                icon = Icons.Default.Check,
                color = SecondaryLavender,
                contentDescriptionText = "Confidence level slider"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // STUDY & SLEEP TRACKERS
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Sleep & Study Cycles",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sleep hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sleep Last Night:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${sleepHours.toInt()} Hours", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryIndigo)
                    }
                    Slider(
                        value = sleepHours,
                        onValueChange = { viewModel.sleepHours.value = it },
                        valueRange = 0f..16f,
                        steps = 15,
                        colors = SliderDefaults.colors(activeTrackColor = PrimaryIndigo)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sleep Quality
                    Text("Sleep Quality:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Restful", "Disturbed", "Couldn't sleep").forEach { quality ->
                            val isSelected = sleepQuality == quality
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground)
                                    .border(1.dp, if (isSelected) PrimaryIndigo else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.sleepQuality.value = quality }
                                    .testTag("sleep_quality_${quality.take(4)}")
                            ) {
                                Text(quality, style = MaterialTheme.typography.bodySmall, color = if (isSelected) PrimaryIndigo else TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Study Hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Study Hours Today:", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Text("${studyHours.toInt()} Hours", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SecondaryLavender)
                    }
                    Slider(
                        value = studyHours,
                        onValueChange = { viewModel.studyHours.value = it },
                        valueRange = 0f..18f,
                        steps = 17,
                        colors = SliderDefaults.colors(activeTrackColor = SecondaryLavender)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Did you take breaks?
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Took regular study breaks?", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Switch(
                            checked = studyBreaks,
                            onCheckedChange = { viewModel.studyBreaks.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryIndigo)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Meals
                    Text("Eaten meals properly?", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Yes", "Skipped some", "Barely ate").forEach { meals ->
                            val isSelected = mealsEaten == meals
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SecondaryLavender.copy(alpha = 0.2f) else DarkBackground)
                                    .border(1.dp, if (isSelected) SecondaryLavender else GlassBorder, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.mealsEaten.value = meals }
                                    .testTag("meals_${meals.take(4)}")
                            ) {
                                Text(meals, style = MaterialTheme.typography.bodySmall, color = if (isSelected) SecondaryLavender else TextPrimary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CONTEXTUAL STRESS TRIGGERS
            Text(
                text = "What was on your mind today?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allTriggers.forEach { trigger ->
                    val isChecked = selectedTriggers.contains(trigger)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChecked) PrimaryIndigo.copy(alpha = 0.1f) else DarkSurface)
                            .border(1.dp, if (isChecked) PrimaryIndigo else GlassBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleTrigger(trigger) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("trigger_row_${trigger.take(4).trim()}")
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggleTrigger(trigger) },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = trigger, style = MaterialTheme.typography.bodyMedium, color = if (isChecked) PrimaryIndigo else TextPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // OPEN ENDED REFLECTION / JOURNAL
            Text(
                text = "Daily Journal / Free-Form Reflection",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Our AI uncovers hidden stress triggers and trends from what you write here.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = journalText,
                onValueChange = { viewModel.journalText.value = it },
                placeholder = { Text("Write freely... (e.g., 'Today mock physics went bad, feeling backlog guilt and missing home. My parents have so many expectations...')") },
                minLines = 4,
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_input")
            )

            Spacer(modifier = Modifier.height(30.dp))

            // SUBMIT BUTTON OR ANALYSIS LOADER
            when (uiState) {
                is CheckInUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Analyzing journal emotional patterns via Gemini AI...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is CheckInUiState.Success -> {
                    val analysis = (uiState as CheckInUiState.Success).analysis
                    AIAnalysisCard(analysis = analysis)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.resetState()
                                onNavigateToCompanion()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Discuss in Chat")
                        }
                        OutlinedButton(
                            onClick = { viewModel.resetState() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Log Another Entry")
                        }
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.submitCheckIn() },
                        enabled = journalText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo, disabledContainerColor = GlassBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("submit_check_in")
                    ) {
                        Text(
                            text = "Analyze with MindMate AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (journalText.isNotBlank()) Color.White else TextMuted
                        )
                    }
                    if (uiState is CheckInUiState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (uiState as CheckInUiState.Error).message,
                            color = CrisisRed,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
