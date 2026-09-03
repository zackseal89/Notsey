package com.example.audio

import android.media.MediaPlayer
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

class AudioPlayerManager {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playingNoteId = MutableStateFlow<Long?>(null)
    val playingNoteId: StateFlow<Long?> = _playingNoteId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    fun playNoteAudio(noteId: Long, audioPath: String) {
        if (_playingNoteId.value == noteId && _isPlaying.value) {
            pause()
            return
        }

        if (_playingNoteId.value == noteId && mediaPlayer != null) {
            resume()
            return
        }

        stop()

        val file = File(audioPath)
        if (!file.exists()) {
            Log.w("AudioPlayerManager", "Audio file not found: $audioPath")
            return
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    stop()
                }
            }

            mediaPlayer = player
            _playingNoteId.value = noteId
            _totalDurationMs.value = player.duration.toLong()
            _currentPositionMs.value = 0L

            player.start()
            _isPlaying.value = true

            startProgressTracker()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to play audio", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
        progressJob?.cancel()
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null
        _isPlaying.value = false
        _playingNoteId.value = null
        _currentPositionMs.value = 0L
        _totalDurationMs.value = 0L
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let {
                    try {
                        _currentPositionMs.value = it.currentPosition.toLong()
                    } catch (e: Exception) {
                        // Player might be releasing
                    }
                }
                delay(100)
            }
        }
    }
}
