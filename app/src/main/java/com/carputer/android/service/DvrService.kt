package com.carputer.android.service

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DvrService {

    companion object {
        private const val TAG = "DvrService"
    }

    private var recorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private val _currentFile = MutableStateFlow("")
    val currentFile: StateFlow<String> = _currentFile.asStateFlow()

    private val _cameraSource = MutableStateFlow("0")
    val cameraSource: StateFlow<String> = _cameraSource.asStateFlow()

    private val _dvrDir = MutableStateFlow("/storage/emulated/0/dvr")
    val dvrDir: StateFlow<String> = _dvrDir.asStateFlow()

    private val _recordings = MutableStateFlow<List<String>>(emptyList())
    val recordings: StateFlow<List<String>> = _recordings.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _playingFile = MutableStateFlow("")
    val playingFile: StateFlow<String> = _playingFile.asStateFlow()

    private val _playPosition = MutableStateFlow(0L)
    val playPosition: StateFlow<Long> = _playPosition.asStateFlow()

    private val _playDuration = MutableStateFlow(0L)
    val playDuration: StateFlow<Long> = _playDuration.asStateFlow()

    fun ensureDvrDir() {
        val dir = File(_dvrDir.value)
        if (!dir.exists()) dir.mkdirs()
    }

    fun startRecording() {
        if (_recording.value) return
        ensureDvrDir()
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(_dvrDir.value, "dashcam_$dateStr.mp4")
            _currentFile.value = file.absolutePath

            recorder = MediaRecorder().apply {
                setVideoSource(MediaRecorder.VideoSource.CAMERA)
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoSize(640, 480)
                setVideoFrameRate(30)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _recording.value = true
            _recordingSeconds.value = 0

            recJob = scope.launch {
                while (isActive && _recording.value) {
                    delay(1000)
                    _recordingSeconds.value = _recordingSeconds.value + 1
                }
            }

            Log.i(TAG, "Recording started: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            _recording.value = false
        }
    }

    fun stopRecording() {
        if (!_recording.value) return
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
        } catch (e: Exception) {
            Log.e(TAG, "Stop recording error", e)
        }
        _recording.value = false
        recJob?.cancel()
        scanRecordings()
        Log.i(TAG, "Recording stopped")
    }

    fun playFile(path: String) {
        stopPlayback()
        _playingFile.value = path
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                setOnPreparedListener { mp ->
                    _playDuration.value = mp.duration.toLong()
                    mp.start()
                    _playing.value = true
                }
                setOnCompletionListener {
                    _playing.value = false
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "Playback error: $what $extra")
                    _playing.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play file", e)
            _playing.value = false
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) { }
        mediaPlayer = null
        _playing.value = false
        _playingFile.value = ""
        _playPosition.value = 0
    }

    fun togglePause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playing.value = false
            } else {
                it.start()
                _playing.value = true
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        val result = file.delete()
        if (result) scanRecordings()
        return result
    }

    fun scanRecordings() {
        val dir = File(_dvrDir.value)
        if (!dir.isDirectory) {
            _recordings.value = emptyList()
            return
        }
        _recordings.value = dir.listFiles()
            ?.filter { it.isFile && it.extension in listOf("mp4", "mkv") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    fun setCameraSource(source: String) {
        _cameraSource.value = source
    }

    fun setDvrDirectory(dir: String) {
        _dvrDir.value = dir
        ensureDvrDir()
    }

    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
        else "%d:%02d".format(minutes, seconds)
    }

    fun fileLabel(path: String): String {
        return File(path).name
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
