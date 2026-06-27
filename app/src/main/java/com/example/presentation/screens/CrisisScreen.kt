package com.example.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.components.HelplineCard
import com.example.ui.theme.*

@Composable
fun CrisisScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

            // Safety Warning Header
            Card(
                colors = CardDefaults.cardColors(containerColor = CrisisRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = CrisisRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "You are not alone.",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We see you're going through something really hard right now. Exam stress can feel crushing, but please remember: your life is immensely precious. Your target rank doesn't define your future.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // HELPLINES LIST
            Text(
                text = "Confidential, Free, 24/7 Human Help",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "Tap to speak with a warm counselor who understands student stress:",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))

            HelplineCard(
                name = "Tele-MANAS (Govt Indian Helpline)",
                phone = "14416",
                hours = "24x7",
                languages = "20+ Regional languages"
            )

            HelplineCard(
                name = "Vandrevala Foundation Helpline",
                phone = "18602662345",
                hours = "24x7",
                languages = "Hindi, English, 8 Regional"
            )

            HelplineCard(
                name = "Manodarpan (MHRD Student Helpline)",
                phone = "8448440632",
                hours = "8 AM to 8 PM",
                languages = "Hindi, English"
            )

            HelplineCard(
                name = "iCALL (TATA Institute of Social Sci.)",
                phone = "02225521111",
                hours = "Mon-Sat, 8 AM - 10 PM",
                languages = "English, Hindi"
            )

            HelplineCard(
                name = "AASRA Support",
                phone = "9820466726",
                hours = "24x7",
                languages = "Hindi, English"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // COGNITIVE COPING SCRIPTS
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Grounding coping statements:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• \"This mock score measures memory recall in a speed run; it does not measure my core capacity or future career paths.\"",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• \"I am studying away from home in a high-pressure coaching environment. Missing home is a strength, not a weakness. I can take today off to recover.\"",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• \"An exam is just one gate. There are multiple, beautiful alternate pathways (Plan B) to success and happiness in fields of technology, humanities, medical sciences, and arts.\"",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // NON-JUDGMENTAL PLAN B PLANNER
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TertiaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "De-escalating the Fear of Failure",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Research proves that having a secondary plan increases performance and reduces chronic stress. You don't have to study with a knife to your neck. Take a piece of paper right now and sketch out 3 alternate things you would love to do if this exam did not exist. Discuss this list with a trusted friend or our AI Companion.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
