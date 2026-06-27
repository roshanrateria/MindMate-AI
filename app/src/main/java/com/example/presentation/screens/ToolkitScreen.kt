package com.example.presentation.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ToolkitScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedTool by remember { mutableStateOf("breathing") } // breathing, pmr, grounding, sleep

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(
                    text = "Wellness Toolkit",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = PrimaryIndigo
                )
                Text(
                    text = "Evidence-based tools to soothe study exhaustion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Tabs / Navigation
            ScrollableTabRow(
                selectedTabIndex = when (selectedTool) {
                    "breathing" -> 0
                    "pmr" -> 1
                    "grounding" -> 2
                    else -> 3
                },
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                contentColor = PrimaryIndigo,
                divider = { Divider(color = GlassBorder) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTool == "breathing",
                    onClick = { selectedTool = "breathing" },
                    text = { Text("Box Breathing") }
                )
                Tab(
                    selected = selectedTool == "pmr",
                    onClick = { selectedTool = "pmr" },
                    text = { Text("PMR Relaxation") }
                )
                Tab(
                    selected = selectedTool == "grounding",
                    onClick = { selectedTool = "grounding" },
                    text = { Text("5-4-3-2-1 Panic Grounding") }
                )
                Tab(
                    selected = selectedTool == "sleep",
                    onClick = { selectedTool = "sleep" },
                    text = { Text("Sleep Checklist") }
                )
            }

            // Screen Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .animateContentSize()
            ) {
                when (selectedTool) {
                    "breathing" -> BoxBreathingSection()
                    "pmr" -> PMRSection()
                    "grounding" -> GroundingSection()
                    else -> SleepChecklistSection()
                }
            }
        }
    }
}

// ==================== Box Breathing Section ====================

@Composable
private fun BoxBreathingSection() {
    var isRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(4) }
    var phase by remember { mutableStateOf("Inhale") } // Inhale, Hold (Full), Exhale, Hold (Empty)

    // Breathing Animation size logic
    val bubbleSize by animateDpAsState(
        targetValue = when (phase) {
            "Inhale" -> 220.dp
            "Hold (Full)" -> 220.dp
            "Exhale" -> 120.dp
            else -> 120.dp // Hold (Empty)
        },
        animationSpec = tween(durationMillis = 4000)
    )

    val bubbleColor = when (phase) {
        "Inhale" -> ResilientGreen
        "Hold (Full)" -> PrimaryIndigo
        "Exhale" -> TertiaryBlue
        else -> AtRiskOrange
    }

    LaunchedEffect(isRunning, phase, secondsLeft) {
        if (isRunning) {
            delay(1000)
            if (secondsLeft > 1) {
                secondsLeft -= 1
            } else {
                secondsLeft = 4
                phase = when (phase) {
                    "Inhale" -> "Hold (Full)"
                    "Hold (Full)" -> "Exhale"
                    "Exhale" -> "Hold (Empty)"
                    else -> "Inhale"
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "4-4-4-4 Box Breathing Cycle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Navy SEAL breathing technique to abort acute mock panic or stress in <3 minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Interactive Synced Breathing Bubble
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                ) {
                    // Expanding pulse background ring
                    Box(
                        modifier = Modifier
                            .size(bubbleSize)
                            .background(bubbleColor.copy(alpha = 0.15f), CircleShape)
                            .border(2.dp, bubbleColor, CircleShape)
                    )

                    // Inner rating
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = phase,
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = bubbleColor,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$secondsLeft Sec",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White,
                            fontSize = 42.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        isRunning = !isRunning
                        if (!isRunning) {
                            secondsLeft = 4
                            phase = "Inhale"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) AtRiskOrange else ResilientGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_breathing_button")
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isRunning) "Pause Breathing" else "Start Deep Breathing",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ==================== PMR Section ====================

@Composable
private fun PMRSection() {
    val pmrSteps = listOf(
        "1. Forehead: Wrinkle your forehead, squeezing tightly for 5 seconds... and release. Feel the relaxation.",
        "2. Jaw: Clench your teeth and squeeze your jaw tightly for 5 seconds... exhale and release completely.",
        "3. Shoulders: Shrug your shoulders up toward your ears, squeezing for 5 seconds... let them drop heavy.",
        "4. Chest & Back: Tighten your chest muscles and pull your shoulder blades back for 5 seconds... release.",
        "5. Arms & Hands: Make tight fists, squeezing your forearms and biceps for 5 seconds... let them go limp.",
        "6. Thighs & Feet: Clench your thigh muscles and curl your toes downward tightly for 5 seconds... release completely."
    )

    Column {
        Text(
            text = "Progressive Muscle Relaxation (PMR)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Systematically tensing and releasing muscle groups drains stored pre-exam physical trauma and tension.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        pmrSteps.forEach { step ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// ==================== Grounding Section ====================

@Composable
private fun GroundingSection() {
    val groundingItems = listOf(
        "👀 5 things you can see around your study desk (e.g. your pen, notebook, ceiling fan, clock, sunlight).",
        "🧎 4 things you can feel physically (e.g. the chair under you, the keyboard keys, cold water on your face, floor).",
        "👂 3 things you can hear (e.g. traffic outside, clock ticking, humming of the refrigerator, birds).",
        "👃 2 things you can smell (e.g. tea, old textbooks, incense, oil).",
        "👅 1 thing you can taste (e.g. water, mint, trace of toothpaste)."
    )

    Column {
        Text(
            text = "5-4-3-2-1 Sensory Grounding",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Use this when experiencing extreme pre-mock overload, sudden heart racing, or exam catastrophizing.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        groundingItems.forEach { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextPrimary
                )
            }
        }
    }
}

// ==================== Sleep Checklist Section ====================

@Composable
private fun SleepChecklistSection() {
    val initialItems = listOf(
        "No textbooks or smartphone inside the bed area",
        "Finish caffeine / tea / coffee at least 6 hours before bedtime",
        "No backlit screens (laptops, phones) after 11 PM",
        "Dimmish your desk lights 30 minutes before winding down",
        "Aim for a consistent bedtime, even on weekends"
    )

    val checklistState = remember { mutableStateMapOf<String, Boolean>().apply {
        initialItems.forEach { put(it, false) }
    }}

    Column {
        Text(
            text = "Sleep Hygiene Checklist",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Inverted sleep cycles cause 40% memory retention loss. Check off what you completed today:",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        initialItems.forEach { item ->
            val isChecked = checklistState[item] ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isChecked) PrimaryIndigo.copy(alpha = 0.1f) else DarkSurface)
                    .border(1.dp, if (isChecked) PrimaryIndigo else GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { checklistState[item] = !isChecked }
                    .padding(16.dp)
                    .testTag("sleep_checklist_row_${item.take(4)}")
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { checklistState[item] = !isChecked },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryIndigo)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isChecked) PrimaryIndigo else TextPrimary
                )
            }
        }
    }
}
