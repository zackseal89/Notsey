package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var amplitudeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun startRecording(): Boolean {
        try {
            val audioDir = File(context.filesDir, "voice_notes")
            if (!audioDir.exists()) audioDir.mkdirs()

            val fileName = "voice_note_${System.currentTimeMillis()}.m4a"
            val file = File(audioDir, fileName)
            currentOutputFile = file

            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _elapsedSeconds.value = 0
            _liveTranscript.value = ""

            // Start live timer and amplitude tracking
            amplitudeJob = scope.launch {
                var secondsCounter = 0
                while (isActive && _isRecording.value) {
                    delay(100)
                    mediaRecorder?.let {
                        try {
                            val maxAmp = it.maxAmplitude
                            _amplitude.value = (maxAmp / 32767f).coerceIn(0f, 1f)
                        } catch (e: Exception) {
                            _amplitude.value = 0f
                        }
                    }
                    secondsCounter += 100
                    _elapsedSeconds.value = secondsCounter / 1000
                }
            }

            // Start SpeechRecognizer for real-time transcription if available
            startSpeechRecognition()

            return true
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start recording", e)
            cleanUp()
            return false
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        Log.d("SpeechRecognizer", "Error code: $error")
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _liveTranscript.value = matches[0]
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _liveTranscript.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                startListening(intent)
            }
        } catch (e: Exception) {
            Log.w("SpeechRecognizer", "Could not init speech recognizer", e)
        }
    }

    fun stopRecording(): RecordResult? {
        if (!_isRecording.value) return null
        return try {
            amplitudeJob?.cancel()
            amplitudeJob = null

            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioRecorderManager", "Error stopping recorder", e)
                }
                release()
            }
            mediaRecorder = null

            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                // Ignore
            }

            _isRecording.value = false
            _amplitude.value = 0f

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                RecordResult(
                    filePath = file.absolutePath,
                    durationMs = (_elapsedSeconds.value * 1000L).coerceAtLeast(1000L),
                    transcript = _liveTranscript.value.trim()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping recording", e)
            cleanUp()
            null
        }
    }

    fun cancelRecording() {
        amplitudeJob?.cancel()
        amplitudeJob = null
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore
        }
        currentOutputFile?.delete()
        currentOutputFile = null
        _isRecording.value = false
        _amplitude.value = 0f
        _elapsedSeconds.value = 0
        _liveTranscript.value = ""
    }

    private fun cleanUp() {
        amplitudeJob?.cancel()
        mediaRecorder?.release()
        mediaRecorder = null
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {}
        _isRecording.value = false
        _amplitude.value = 0f
    }
}

data class RecordResult(
    val filePath: String,
    val durationMs: Long,
    val transcript: String
)
