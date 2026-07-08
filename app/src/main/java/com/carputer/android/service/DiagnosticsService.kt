package com.carputer.android.service

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticsService {

    companion object {
        private const val TAG = "DiagnosticsService"
    }

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _lastReport = MutableStateFlow("")
    val lastReport: StateFlow<String> = _lastReport.asStateFlow()

    private val _issueCount = MutableStateFlow(0)
    val issueCount: StateFlow<Int> = _issueCount.asStateFlow()

    private val issues = mutableListOf<String>()

    // Service references
    var sensorClient: SensorClient? = null
    var carControlClient: CarControlClient? = null
    var wifiService: WifiService? = null
    var mediaPlayerService: MediaPlayerService? = null

    fun runDiagnostics() {
        _running.value = true
        issues.clear()

        checkSensorManager()
        checkCarControlManager()
        checkWifi()
        checkMediaPlayer()
        checkGStreamerPlugins()

        generateReport()
        _running.value = false
    }

    private fun checkSensorManager() {
        val sc = sensorClient
        if (sc == null) {
            addIssue("SensorClient", "Not initialized")
            return
        }
        if (!sc.connected.value) {
            addIssue("SensorClient", "Not connected — check UDP port 5001")
        }
    }

    private fun checkCarControlManager() {
        val cc = carControlClient
        if (cc == null) {
            addIssue("CarControlClient", "Not initialized")
            return
        }
        if (!cc.connected.value) {
            addIssue("CarControlClient", "Not connected to ${CarControlClient.DEFAULT_HOST}:${CarControlClient.DEFAULT_PORT}")
        }
    }

    private fun checkWifi() {
        val ws = wifiService
        if (ws == null) {
            addIssue("WifiService", "Not initialized")
            return
        }
        if (!ws.connected.value) {
            addIssue("WifiService", "Not connected to any network")
        }
    }

    private fun checkMediaPlayer() {
        val mp = mediaPlayerService
        if (mp == null) {
            addIssue("MediaPlayerService", "Not initialized")
            return
        }
        if (mp.playlist.value.isEmpty()) {
            addIssue("MediaPlayerService", "No media files loaded")
        }
    }

    private fun checkGStreamerPlugins() {
        val gstPlugins = File("/system/lib").listFiles { f ->
            f.name.startsWith("libgst") && f.name.endsWith(".so")
        }
        if (gstPlugins == null || gstPlugins.isEmpty()) {
            Log.d(TAG, "No GStreamer plugins (expected on Android)")
        }
    }

    private fun addIssue(component: String, issue: String) {
        issues.add("[$component] $issue")
        _issueCount.value = issues.size
        Log.w(TAG, "Issue: [$component] $issue")
    }

    private fun generateReport() {
        val sb = StringBuilder()
        sb.appendLine("=== Carputer Diagnostics Report ===")
        sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine()

        if (issues.isEmpty()) {
            sb.appendLine("No issues found — all systems nominal.")
        } else {
            sb.appendLine("Issues found: ${issues.size}")
            sb.appendLine()
            issues.forEach { sb.appendLine("  • $it") }
        }

        _lastReport.value = sb.toString()
        Log.i(TAG, "Diagnostics complete, ${issues.size} issues")
    }

    fun saveReport(path: String = "/storage/emulated/0/carputer_debug_report.txt") {
        try {
            File(path).writeText(_lastReport.value)
            Log.i(TAG, "Report saved to $path")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save report", e)
        }
    }
}
