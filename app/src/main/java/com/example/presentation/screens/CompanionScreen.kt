package com.example.presentation.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.domain.model.ChatMessage
import com.example.presentation.viewmodel.ChatUiState
import com.example.presentation.viewmodel.CompanionMode
import com.example.presentation.viewmodel.CompanionViewModel
import com.example.presentation.viewmodel.TutorUiState
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CompanionScreen(
    viewModel: CompanionViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val activeMode by viewModel.activeMode.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val tutorUiState by viewModel.tutorUiState.collectAsState()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val recordAudioPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Speech Recognizer setup
    var partialText by remember { mutableStateOf("") }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                partialText = "Listening..."
            }
            override fun onBeginningOfSpeech() {
                partialText = "I'm listening..."
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                partialText = "Processing voice..."
            }
            override fun onError(error: Int) {
                partialText = ""
                if (viewModel.isListening.value) {
                    viewModel.toggleListening()
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    partialText = ""
                    viewModel.sendMessage(text)
                }
                if (viewModel.isListening.value) {
                    viewModel.toggleListening()
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    partialText = matches[0]
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(isListening) {
        if (isListening) {
            if (recordAudioPermissionState.status.isGranted) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer.setRecognitionListener(recognitionListener)
                speechRecognizer.startListening(intent)
            } else {
                recordAudioPermissionState.launchPermissionRequest()
                viewModel.toggleListening() // Reset listen state
            }
        } else {
            speechRecognizer.stopListening()
        }
    }

    // Initialize TextToSpeech engine
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.US
            }
        }
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    fun speak(text: String) {
        if (isTtsReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Auto-speak AI's new responses in Vent mode
    LaunchedEffect(messages.size, activeMode) {
        if (activeMode == CompanionMode.VENT && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg.sender == "ai") {
                speak(lastMsg.message)
            }
        }
    }

    var inputMessage by remember { mutableStateOf("") }

    val starterPrompts = listOf(
        "📊 Stress about bad mock scores",
        "📞 Parental expectations/calls",
        "😴 Dealing with huge backlog",
        "🏠 Feeling lonely in coaching hostel"
    )

    // Scroll to bottom when list updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MindMate AI",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Empathetic, multi-modal co-solving companion",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                
                // Clear button
                IconButton(
                    onClick = { 
                        viewModel.clearHistory()
                        tts?.stop()
                    },
                    modifier = Modifier.testTag("clear_chat_history")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = TextSecondary)
                }
            }

            Divider(color = GlassBorder)

            // Segmented Mode Selector at Top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val modes = listOf(
                    Triple(CompanionMode.CHAT, "Chat", Icons.Default.Send),
                    Triple(CompanionMode.VENT, "Voice Vent", Icons.Default.PlayArrow),
                    Triple(CompanionMode.TUTOR, "AI Tutor", Icons.Default.Search)
                )
                modes.forEach { (mode, label, icon) ->
                    val isSelected = activeMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            viewModel.setMode(mode)
                            tts?.stop()
                        },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryIndigo,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceCard,
                            labelColor = TextSecondary
                        ),
                        border = null,
                        modifier = Modifier.testTag("mode_chip_${mode.name}")
                    )
                }
            }

            Divider(color = GlassBorder)

            // Dynamic view based on mode
            when (activeMode) {
                CompanionMode.CHAT -> {
                    // Chat Scrollable area
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages) { message ->
                            ChatBubble(message = message)
                        }
                        
                        // Typing Indicator
                        if (uiState is ChatUiState.Sending) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = "MindMate is reflecting...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Starter chips
                    if (messages.size <= 2) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(starterPrompts) { prompt ->
                                AssistChip(
                                    onClick = {
                                        viewModel.sendMessage(prompt.substring(2)) // strip emoji
                                    },
                                    label = { Text(prompt, fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = DarkSurfaceCard,
                                        labelColor = TextSecondary
                                    ),
                                    border = null,
                                    modifier = Modifier.testTag("prompt_chip_${prompt.take(6)}")
                                )
                            }
                        }
                    }

                    // Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = { Text("How can I support you?") },
                            maxLines = 3,
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputMessage.isNotBlank()) {
                                        viewModel.sendMessage(inputMessage)
                                        inputMessage = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = GlassBorder,
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_text_field")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = {
                                if (inputMessage.isNotBlank()) {
                                    viewModel.sendMessage(inputMessage)
                                    inputMessage = ""
                                }
                            },
                            enabled = inputMessage.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (inputMessage.isNotBlank()) PrimaryIndigo else GlassBorder,
                                    CircleShape
                                )
                                .testTag("send_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send Message",
                                tint = if (inputMessage.isNotBlank()) Color.White else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                CompanionMode.VENT -> {
                    // Voice Vent Mode Layout
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Real-Time Voice Venting",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Talk your worries out. AI listens and speaks back to soothe you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                        )

                        // Soundwave Animation
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(220.dp)
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = if (isListening) 1.35f else 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(if (isListening) 800 else 1800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(if (isListening) 800 else 1800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )

                            // Pulsing gradient backgrounds
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                if (isListening) CrisisRed.copy(alpha = pulseAlpha) else PrimaryIndigo.copy(alpha = pulseAlpha),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .align(Alignment.Center)
                            )

                            // Main central floating mic orb
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = if (isListening) {
                                                listOf(CrisisRed, AtRiskOrange)
                                            } else {
                                                listOf(PrimaryIndigo, SecondaryLavender)
                                            }
                                        )
                                    )
                                    .clickable { viewModel.toggleListening() }
                                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "Mic button",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Real-time transcript display
                        if (partialText.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = partialText,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Listening Status / Action guide
                        Text(
                            text = if (isListening) "MindMate is listening... speak your heart out!" else "Tap the Orb to Start Voice Session",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isListening) CrisisRed else Color.White
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        // Preset venting quick prompts
                        Text(
                            text = "No mic? Tap to instantly speak a preset worry:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val ventPrompts = listOf(
                            "I feel completely exhausted and isolated tonight.",
                            "I'm terrified of failing the mock exam this Sunday.",
                            "The backlog is so huge, I feel frozen and can't start.",
                            "I got a call from home and feel so homesick."
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ventPrompts.forEach { prompt ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.sendMessage(prompt)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SecondaryLavender, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(text = prompt, style = MaterialTheme.typography.bodySmall, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                CompanionMode.TUTOR -> {
                    // Live AI Tutor Mode Layout
                    var selectedProblemIndex by remember { mutableStateOf(0) }
                    var triggerFlash by remember { mutableStateOf(false) }

                    val sampleProblems = listOf(
                        Triple(
                            "JEE Physics - Inclined Mechanics",
                            "Determine the velocity of a 5kg block at the bottom of a 30-degree incline plane (length = 4m) with friction coefficient μ = 0.1.",
                            "A block diagram of mass sliding on theta ramp"
                        ),
                        Triple(
                            "NEET Biology - Cell Chloroplast Structure",
                            "Identify the labeled structures in the double-membraned organelle containing internal thylakoids stacked into grana.",
                            "Chloroplast drawing with labeled sections A (Outer Membrane), B (Grana), C (Stroma)"
                        ),
                        Triple(
                            "UPSC Polity - Basic Structure Doctrine",
                            "Explain the origin, historical landmark judgments, and key constituents of the 'Basic Structure Doctrine' in the Constitution.",
                            "UPSC polity textbook questions list with Article 368 details"
                        )
                    )

                    val cameraGranted = cameraPermissionState.status.isGranted
                    var customTutorQuestion by remember { mutableStateOf("") }
                    var tutorMicListening by remember { mutableStateOf(false) }
                    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
                    val lifecycleOwner = LocalLifecycleOwner.current

                    val startTutorSpeechToText = {
                        if (recordAudioPermissionState.status.isGranted) {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                            }
                            val tutorSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                            tutorSpeechRecognizer.setRecognitionListener(object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) {
                                    tutorMicListening = true
                                }
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() {
                                    tutorMicListening = false
                                }
                                override fun onError(error: Int) {
                                    tutorMicListening = false
                                    tutorSpeechRecognizer.destroy()
                                }
                                override fun onResults(results: Bundle?) {
                                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    if (!matches.isNullOrEmpty()) {
                                        customTutorQuestion = matches[0]
                                    }
                                    tutorMicListening = false
                                    tutorSpeechRecognizer.destroy()
                                }
                                override fun onPartialResults(partialResults: Bundle?) {}
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })
                            tutorSpeechRecognizer.startListening(intent)
                        } else {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Live Multimodal AI Tutor",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Align study questions in viewfinder to co-solve with Gemini API",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        // 1. Live Camera / Simulator Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black)
                                .border(2.dp, if (triggerFlash) Color.White else GlassBorder, RoundedCornerShape(16.dp))
                        ) {
                            if (cameraGranted) {
                                AndroidView(
                                    factory = { ctx ->
                                        val previewView = PreviewView(ctx).apply {
                                            scaleType = PreviewView.ScaleType.FILL_CENTER
                                        }
                                        val executor = ContextCompat.getMainExecutor(ctx)
                                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                        cameraProviderFuture.addListener({
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .build()
                                            imageCapture = capture

                                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                            try {
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview,
                                                    capture
                                                )
                                            } catch (e: Exception) {
                                                Log.e("CameraPreview", "Camera binding failed", e)
                                            }
                                        }, executor)
                                        previewView
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Camera Required",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Camera access is needed to scan study materials.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { cameraPermissionState.launchPermissionRequest() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Grant Camera Permission", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            if (triggerFlash) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.White))
                                LaunchedEffect(Unit) {
                                    delay(100)
                                    triggerFlash = false
                                }
                            }

                            // Viewfinder Gridlines overlay
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeColor = Color.White.copy(alpha = 0.15f)
                                val strokeWidth = 1.dp.toPx()
                                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(size.width / 3, 0f), end = androidx.compose.ui.geometry.Offset(size.width / 3, size.height), strokeWidth = strokeWidth)
                                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(size.width * 2 / 3, 0f), end = androidx.compose.ui.geometry.Offset(size.width * 2 / 3, size.height), strokeWidth = strokeWidth)
                                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(0f, size.height / 3), end = androidx.compose.ui.geometry.Offset(size.width, size.height / 3), strokeWidth = strokeWidth)
                                drawLine(strokeColor, start = androidx.compose.ui.geometry.Offset(0f, size.height * 2 / 3), end = androidx.compose.ui.geometry.Offset(size.width, size.height * 2 / 3), strokeWidth = strokeWidth)
                            }

                            // Active targeted question info card overlay inside Camera Viewfinder
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "📷 Viewfinder Target: ${sampleProblems[selectedProblemIndex].first}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = SecondaryLavender
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = sampleProblems[selectedProblemIndex].second,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            // Focus Indicator Ring
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .align(Alignment.Center)
                                    .border(1.5.dp, SecondaryLavender.copy(alpha = 0.6f), CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom question query input with speech button
                        Text(
                            text = "Ask Tutor a Specific Question (or use preset sheet):",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTutorQuestion,
                                onValueChange = { customTutorQuestion = it },
                                placeholder = { Text("What do you want me to explain here?", fontSize = 13.sp) },
                                maxLines = 2,
                                singleLine = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = DarkSurfaceCard,
                                    unfocusedContainerColor = DarkSurfaceCard
                                ),
                                modifier = Modifier
                                    .weight(1f)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { startTutorSpeechToText() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (tutorMicListening) CrisisRed else GlassBorder,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = if (tutorMicListening) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = "Speak Question",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Textbook Selector Carousel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Or Choose Study Problem Preset:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            sampleProblems.forEachIndexed { index, (title, _, _) ->
                                val isSelected = selectedProblemIndex == index
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PrimaryIndigo.copy(alpha = 0.3f) else DarkSurfaceCard
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) PrimaryIndigo else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .clickable { selectedProblemIndex = index }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = title.split(" - ").first(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                        Text(
                                            text = title.split(" - ").last(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Shutter snap button
                        Button(
                            onClick = {
                                triggerFlash = true
                                val item = sampleProblems[selectedProblemIndex]
                                val baseQuestion = customTutorQuestion.ifBlank { item.second }
                                val finalQuestion = "Solve this step-by-step with formulas and clear structures: $baseQuestion"
                                
                                val currentCapture = imageCapture
                                if (cameraGranted && currentCapture != null) {
                                    viewModel.setTutorSolving()
                                    currentCapture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                try {
                                                    val buffer = image.planes[0].buffer
                                                    val bytes = ByteArray(buffer.remaining())
                                                    buffer.get(bytes)
                                                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                                    viewModel.solveProblem(finalQuestion, base64, "image/jpeg")
                                                } catch (e: Exception) {
                                                    viewModel.setTutorError("Failed to convert image: ${e.localizedMessage}")
                                                } finally {
                                                    image.close()
                                                }
                                            }
                                            
                                            override fun onError(exception: ImageCaptureException) {
                                                viewModel.setTutorError("Camera capture failed: ${exception.localizedMessage}")
                                            }
                                        }
                                    )
                                } else {
                                    viewModel.solveProblem(finalQuestion, null, null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Snap & Solve Problem", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Results area
                        when (val solverState = tutorUiState) {
                            is TutorUiState.Solving -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = PrimaryIndigo)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Gemini is analyzing diagram & solving problem...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            is TutorUiState.Solved -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "✍️ Step-by-Step Solution",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = ResilientGreen,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Speak explanation button
                                            IconButton(
                                                onClick = { speak(solverState.solution) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = "Speak Solution",
                                                    tint = SecondaryLavender
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = solverState.solution,
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            is TutorUiState.Error -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CrisisRed)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = solverState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            else -> {
                                // Idle state guide
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Choose a study problem card above and click Snap & Solve to launch active tutor session.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == "user"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        val bubbleShape = if (isUser) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        }

        val containerColor = if (isUser) PrimaryIndigo else DarkSurfaceCard
        val borderModifier = if (isUser) Modifier else Modifier.border(1.dp, GlassBorder, bubbleShape)

        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = bubbleShape,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(borderModifier)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = Color.White
                )
            }
        }
    }
}
