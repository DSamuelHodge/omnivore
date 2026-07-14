package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TranscriptNote
import com.example.ui.viewmodel.AudioScribeViewModel
import com.example.ui.viewmodel.SummaryUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AudioScribeViewModel,
    onNavigateToRecord: () -> Unit
) {
    val notes by viewModel.allNotes.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.rawText.contains(searchQuery, ignoreCase = true) ||
                    it.summary.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "My Notes",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Capture, transcribe, and synthesize your thoughts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_search_input"),
                placeholder = { Text("Search title, transcript, or summary...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Microphone illustration",
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No recordings yet" else "No matching notes found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Press the microphone button or navigate to the record tab to capture real-time transcription." else "Try adjusting your keywords or search query.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onNavigateToRecord,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("home_record_button")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Transcribing")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredNotes) { note ->
                        NoteCard(note = note, onClick = { viewModel.selectNote(note) })
                    }
                }
            }
        }

        // Expanded Note Detail View
        val context = LocalContext.current
        selectedNote?.let { note ->
            NoteDetailDialog(
                note = note,
                summaryState = summaryState,
                onDismiss = { viewModel.selectNote(null) },
                onSummarize = { viewModel.summarizeNote(note) },
                onExport = { format -> viewModel.shareNote(context, note, format) },
                onDelete = {
                    viewModel.deleteNote(note)
                    viewModel.selectNote(null)
                }
            )
        }
    }
}

@Composable
fun NoteCard(
    note: TranscriptNote,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(note.timestamp))
    val minutes = note.durationSeconds / 60
    val seconds = note.durationSeconds % 60
    val durationText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (note.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = if (note.isSynced) "Synced to Cloud" else "Local Only",
                    tint = if (note.isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Duration",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val snippet = if (note.summary.isNotEmpty()) note.summary else note.rawText
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (note.summary.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Summary Indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI Summarized",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailDialog(
    note: TranscriptNote,
    summaryState: SummaryUiState,
    onDismiss: () -> Unit,
    onSummarize: () -> Unit,
    onExport: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showExportMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                        }

                        Row {
                            IconButton(
                                onClick = onSummarize,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Summarize")
                            }

                            Box {
                                IconButton(onClick = { showExportMenu = true }) {
                                    Icon(Icons.Default.Share, contentDescription = "Export option")
                                }
                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export PDF") },
                                        onClick = {
                                            showExportMenu = false
                                            onExport("pdf")
                                        },
                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Markdown") },
                                        onClick = {
                                            showExportMenu = false
                                            onExport("md")
                                        },
                                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export Plain Text") },
                                        onClick = {
                                            showExportMenu = false
                                            onExport("txt")
                                        },
                                        leadingIcon = { Icon(Icons.Default.FormatAlignLeft, contentDescription = null) }
                                    )
                                }
                            }

                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete recording", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            when (summaryState) {
                                is SummaryUiState.Loading -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("Summarizing with Gemini...", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Creating outline, chapters, and major takeaways.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                                is SummaryUiState.Error -> {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            text = "Summarization Failed: ${summaryState.message}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        if (note.summary.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AI Smart Summary",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = note.summary,
                                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        val takeaways = note.getKeyTakeawaysList()
                        if (takeaways.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Key Takeaways",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        takeaways.forEach { item ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("• ", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = item,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Full Transcript",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.rawText,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 24.dp)
                            )
                        }
                    }

                    if (note.summary.isEmpty() && summaryState !is SummaryUiState.Loading) {
                        Button(
                            onClick = onSummarize,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("generate_summary_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Summary & Insights")
                        }
                    }
                }
            }
        }
    )
}
