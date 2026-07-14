package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.viewmodel.AudioScribeViewModel
import java.util.Locale

@Composable
fun RecordScreen(
    viewModel: AudioScribeViewModel
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val liveText by viewModel.liveText.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val durationSeconds by viewModel.durationSeconds.collectAsState()
    val errorState by viewModel.speechError.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRecording) "Recording Conversation..." else "Ready to Record",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 64.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    RealTimeSoundVisualizer(amplitude = amplitude)
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(15) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                val scrollState = rememberScrollState()

                LaunchedEffect(liveText, partialText) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }

                if (liveText.isEmpty() && partialText.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isRecording) "Listening... speak into your microphone" else "Transcribed words will flow here in real time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (liveText.isNotEmpty()) {
                            Text(
                                text = liveText,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (partialText.isNotEmpty()) {
                            if (liveText.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = partialText,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = 18.sp,
                                    lineHeight = 26.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                ),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            errorState?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = isRecording || liveText.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = { viewModel.discardRecording() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Discard Recording")
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(92.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isRecording) 1.15f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    val brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isRecording) 0.3f else 0.1f),
                            Color.Transparent
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(brush)
                    )

                    Button(
                        onClick = {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isRecording) {
                                    viewModel.stopRecording()
                                } else {
                                    viewModel.startRecording()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("record_trigger_button"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Pause Recording" else "Record Button",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isRecording && liveText.isNotEmpty(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            customTitle = ""
                            showSaveDialog = true
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .testTag("save_note_fab"),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Recording")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Transcription") },
            text = {
                Column {
                    Text("Give a title to this audio note:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        placeholder = { Text("e.g. Weekly Synopses Meeting") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_note_title_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        viewModel.saveRecording(customTitle)
                    },
                    modifier = Modifier.testTag("confirm_save_note_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RealTimeSoundVisualizer(amplitude: Float) {
    val barCount = 15
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_wiggle")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val randomFactor = remember { (0.6f + Math.random() * 0.8f).toFloat() }
            val animateHeight by infiniteTransition.animateFloat(
                initialValue = 0.1f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = (180 * randomFactor).toInt(),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_wiggle"
            )

            val currentBarScale = (amplitude * animateHeight * 55.dp.value).coerceIn(6.dp.value, 65.dp.value).dp

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(currentBarScale)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}
