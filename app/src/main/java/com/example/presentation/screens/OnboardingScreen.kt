package com.example.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.OnboardingViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nickname by viewModel.nickname.collectAsState()
    val examType by viewModel.examType.collectAsState()
    val biggestWorry by viewModel.biggestWorry.collectAsState()

    val scrollState = rememberScrollState()

    val exams = listOf("JEE (Engineering)", "NEET (Medical)", "UPSC (Civil Services)", "GATE", "CAT", "CUET", "CBSE/ICSE Boards", "Other")
    val worries = listOf("Parental Expectations", "Fear of Failure", "Peer Comparison", "Volume of Mock Tests", "Syllabus Backlog", "Homesickness & Loneliness")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Aesthetic ambient background gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.15f), Color.Transparent),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Brand title
            Text(
                text = "MindMate AI",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                color = PrimaryIndigo,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = "Your safe, stigma-free exam wellness space",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nickname card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "1. What should we call you?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Your data stays entirely on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { viewModel.updateNickname(it) },
                        placeholder = { Text("Enter nickname / pseudonym") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nickname_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Exam target card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = SecondaryLavender)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Select your target exam",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exams.chunked(2).forEach { rowExams ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowExams.forEach { exam ->
                                    val isSelected = examType == exam
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.2f) else DarkBackground)
                                            .border(
                                                1.dp,
                                                if (isSelected) PrimaryIndigo else GlassBorder,
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.updateExamType(exam) }
                                            .testTag("exam_chip_${exam.take(4).trim()}")
                                    ) {
                                        Text(
                                            text = exam,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) PrimaryIndigo else TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Worry Selection card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AtRiskOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. What's your biggest stressor?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        worries.forEach { worry ->
                            val isSelected = biggestWorry == worry
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AtRiskOrange.copy(alpha = 0.15f) else DarkBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) AtRiskOrange else GlassBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.updateBiggestWorry(worry) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("worry_row_${worry.take(4).trim()}")
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.updateBiggestWorry(worry) },
                                    colors = RadioButtonDefaults.colors(selectedColor = AtRiskOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = worry,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) AtRiskOrange else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue Button
            Button(
                onClick = {
                    if (nickname.isNotBlank()) {
                        viewModel.completeOnboarding()
                        onOnboardingCompleted()
                    }
                },
                enabled = nickname.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    disabledContainerColor = GlassBorder
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("submit_onboarding")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Enter MindMate Space",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (nickname.isNotBlank()) Color.White else TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
