package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    private var accumulatedText = ""

    fun startListening() {
        if (_isRecording.value) {
            // Already recording, try to restart if idle
            restartListening()
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _errorState.value = null
                    }

                    override fun onBeginningOfSpeech() {
                        _isRecording.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _amplitude.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                            else -> "Unknown error"
                        }
                        Log.e("SpeechRecognizerHelper", "Error code $error: $message")
                        
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            if (_isRecording.value) {
                                restartListening()
                            }
                        } else {
                            _errorState.value = message
                            _isRecording.value = false
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val resultText = matches[0]
                            if (accumulatedText.isEmpty()) {
                                accumulatedText = resultText
                            } else {
                                accumulatedText += " " + resultText
                            }
                            _transcribedText.value = accumulatedText
                            _partialText.value = ""
                        }
                        
                        if (_isRecording.value) {
                            restartListening()
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _partialText.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }

        try {
            _isRecording.value = true
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            _errorState.value = "Failed to start: ${e.localizedMessage}"
            _isRecording.value = false
        }
    }

    private fun restartListening() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            speechRecognizer?.startListening(recognizerIntent)
        } catch (e: Exception) {
            Log.e("SpeechRecognizerHelper", "Failed to restart: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        _isRecording.value = false
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("SpeechRecognizerHelper", "Stop error: ${e.localizedMessage}")
        }
        _amplitude.value = 0f
        _partialText.value = ""
    }

    fun reset() {
        accumulatedText = ""
        _transcribedText.value = ""
        _partialText.value = ""
        _amplitude.value = 0f
        _errorState.value = null
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
