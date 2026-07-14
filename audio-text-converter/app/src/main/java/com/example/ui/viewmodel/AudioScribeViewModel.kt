package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GeminiSummaryResponse
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.ResponseFormat
import com.example.api.ResponseFormatText
import com.example.api.RetrofitClient
import com.example.audio.SpeechRecognizerHelper
import com.example.data.TranscriptNote
import com.example.data.TranscriptRepository
import com.example.utils.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    Home, Record, Settings
}

sealed interface SummaryUiState {
    object Idle : SummaryUiState
    object Loading : SummaryUiState
    data class Success(val note: TranscriptNote) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

class AudioScribeViewModel(
    application: Application,
    private val repository: TranscriptRepository
) : AndroidViewModel(application) {

    private val speechHelper = SpeechRecognizerHelper(application)

    // Current Active Screen
    private val _currentScreen = MutableStateFlow(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Notes List
    val allNotes: StateFlow<List<TranscriptNote>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Selected Note for detail viewing
    private val _selectedNote = MutableStateFlow<TranscriptNote?>(null)
    val selectedNote: StateFlow<TranscriptNote?> = _selectedNote.asStateFlow()

    // Speech-to-Text States
    val isRecording: StateFlow<Boolean> = speechHelper.isRecording
    val liveText: StateFlow<String> = speechHelper.transcribedText
    val partialText: StateFlow<String> = speechHelper.partialText
    val amplitude: StateFlow<Float> = speechHelper.amplitude
    val speechError: StateFlow<String?> = speechHelper.errorState

    // Live Recording Duration Timer
    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()
    private var timerJob: Job? = null

    // Gemini API Summarization state
    private val _summaryState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val summaryState: StateFlow<SummaryUiState> = _summaryState.asStateFlow()

    // Cloud Sync States
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f)
    val syncProgress: StateFlow<Float> = _syncProgress.asStateFlow()

    private val _lastSyncedTime = MutableStateFlow(System.currentTimeMillis())
    val lastSyncedTime: StateFlow<Long> = _lastSyncedTime.asStateFlow()

    // Cloud Sync Preference
    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
    }

    fun selectNote(note: TranscriptNote?) {
        _selectedNote.value = note
        _summaryState.value = SummaryUiState.Idle
    }

    // --- Recording Control ---
    fun startRecording() {
        speechHelper.reset()
        _durationSeconds.value = 0L
        speechHelper.startListening()
        
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isRecording.value) {
                    _durationSeconds.value += 1
                }
            }
        }
    }

    fun stopRecording() {
        speechHelper.stopListening()
        timerJob?.cancel()
        timerJob = null
    }

    fun discardRecording() {
        stopRecording()
        speechHelper.reset()
        _durationSeconds.value = 0L
    }

    fun saveRecording(title: String) {
        val rawText = (liveText.value + " " + partialText.value).trim()
        if (rawText.isEmpty()) return

        val finalTitle = title.ifBlank { "Recorded Session" }
        viewModelScope.launch {
            val note = TranscriptNote(
                title = finalTitle,
                rawText = rawText,
                durationSeconds = _durationSeconds.value,
                timestamp = System.currentTimeMillis(),
                isSynced = false,
                lastModified = System.currentTimeMillis()
            )
            val noteId = repository.insert(note)
            
            // Auto sync check
            if (_autoSyncEnabled.value) {
                syncNoteInBg(noteId.toInt())
            }

            // Reset
            speechHelper.reset()
            _durationSeconds.value = 0L
            _currentScreen.value = Screen.Home
        }
    }

    // --- AI Summarization ---
    fun summarizeNote(note: TranscriptNote) {
        _summaryState.value = SummaryUiState.Loading

        viewModelScope.launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                _summaryState.value = SummaryUiState.Error("Gemini API key is not configured. Please register it in AI Studio Secrets.")
                return@launch
            }

            val systemInstruction = """
                You are an expert audio transcription analyzer. Your job is to analyze transcripts of spoken audio, and return a clean, structured JSON response with the exact fields: "title", "summary", and "keyTakeaways".
                - "title": A short, highly-engaging descriptive title for the session (max 6 words). Do NOT use generic names.
                - "summary": A concise paragraph summary of the key discussion points (max 120 words).
                - "keyTakeaways": A list of up to 5 concise points representing the highlights, action items, or decisions.
                Return ONLY valid JSON.
            """.trimIndent()

            val prompt = """
                Please analyze and summarize the following transcript:
                \"\"\"
                ${note.rawText}
                \"\"\"
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    responseFormat = ResponseFormat(
                        text = ResponseFormatText(mimeType = "application/json")
                    ),
                    temperature = 0.4f
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (jsonText != null) {
                    val summaryAdapter = RetrofitClient.moshi.adapter(GeminiSummaryResponse::class.java)
                    val summaryData = summaryAdapter.fromJson(jsonText)

                    if (summaryData != null) {
                        val updatedNote = note.copy(
                            title = summaryData.title,
                            summary = summaryData.summary,
                            keyTakeawaysJson = summaryData.keyTakeaways.joinToString("\n"),
                            lastModified = System.currentTimeMillis()
                        )
                        repository.update(updatedNote)
                        _selectedNote.value = updatedNote
                        _summaryState.value = SummaryUiState.Success(updatedNote)
                        
                        // Auto sync update
                        if (_autoSyncEnabled.value) {
                            syncNoteInBg(updatedNote.id)
                        }
                    } else {
                        _summaryState.value = SummaryUiState.Error("Failed to parse AI Summary structure.")
                    }
                } else {
                    _summaryState.value = SummaryUiState.Error("No response received from Gemini AI.")
                }
            } catch (e: Exception) {
                Log.e("AudioScribeViewModel", "Gemini API error", e)
                _summaryState.value = SummaryUiState.Error(e.localizedMessage ?: "Network or API call failed.")
            }
        }
    }

    // --- Sharing and Exporting ---
    fun shareNote(context: Context, note: TranscriptNote, format: String) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                when (format.lowercase()) {
                    "pdf" -> Exporter.exportToPdf(context, note)
                    "md" -> Exporter.exportToMarkdown(context, note)
                    else -> Exporter.exportToTxt(context, note)
                }
            }

            val mimeType = when (format.lowercase()) {
                "pdf" -> "application/pdf"
                "md" -> "text/markdown"
                else -> "text/plain"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, "Shared from Audio Text Converter: ${note.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Export Note as ${format.uppercase()}"))
        }
    }

    // --- Delete ---
    fun deleteNote(note: TranscriptNote) {
        viewModelScope.launch {
            repository.delete(note.id)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    // --- Sync Operations ---
    fun setAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
    }

    fun triggerCloudSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncProgress.value = 0f

        viewModelScope.launch {
            for (progress in 1..10) {
                delay(200)
                _syncProgress.value = progress / 10f
            }

            allNotes.value.forEach { note ->
                if (!note.isSynced) {
                    repository.updateSyncStatus(note.id, true)
                }
            }

            _lastSyncedTime.value = System.currentTimeMillis()
            _isSyncing.value = false
        }
    }

    private fun syncNoteInBg(noteId: Int) {
        viewModelScope.launch {
            delay(1500)
            repository.updateSyncStatus(noteId, true)
            _lastSyncedTime.value = System.currentTimeMillis()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.destroy()
    }
}

class AudioScribeViewModelFactory(
    private val application: Application,
    private val repository: TranscriptRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioScribeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioScribeViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
