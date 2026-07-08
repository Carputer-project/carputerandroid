package com.carputer.android.service

import android.util.Log
import com.carputer.android.data.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class DataLoggerService {

    companion object {
        private const val TAG = "DataLogger"
        private const val FLUSH_INTERVAL_MS = 5000L
        private const val LOG_DIR = "carputer_logs"
    }

    private var writer: FileWriter? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val _logging = MutableStateFlow(false)
    val logging: StateFlow<Boolean> = _logging.asStateFlow()

    private val _logPath = MutableStateFlow("")
    val logPath: StateFlow<String> = _logPath.asStateFlow()

    private var rowCount = 0

    fun startLogging(basePath: String = "/storage/emulated/0") {
        if (_logging.value) return
        try {
            val dir = File(basePath, LOG_DIR)
            if (!dir.exists()) dir.mkdirs()

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "sensor_log_$dateStr.csv")
            _logPath.value = file.absolutePath

            writer = FileWriter(file, true)
            writeHeader()
            _logging.value = true

            // Periodic flush timer to prevent data loss on crash
            job = scope.launch {
                while (isActive) {
                    delay(FLUSH_INTERVAL_MS)
                    writer?.flush()
                }
            }

            Log.i(TAG, "Logging started: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start logging", e)
        }
    }

    fun stopLogging() {
        _logging.value = false
        job?.cancel()
        job = null
        closeFile()
        Log.i(TAG, "Logging stopped, $rowCount rows written")
    }

    fun onSensorData(data: SensorData) {
        if (!_logging.value) return
        writeRow(data)
    }

    private fun writeHeader() {
        writer?.write("timestamp,speed,rpm,throttle,map,coolant,oil,ambient,intake,fuel,oilPressure,brakeFluid,battery\n")
        writer?.flush()
    }

    private fun writeRow(data: SensorData) {
        try {
            val line = buildString {
                append(dateFormat.format(Date()))
                append(",${data.speed}")
                append(",${data.rpm}")
                append(",${data.throttle}")
                append(",${data.map}")
                append(",${data.coolant}")
                append(",${data.oil}")
                append(",${data.ambient}")
                append(",${data.intake}")
                append(",${data.fuel}")
                append(",${data.oilPressure}")
                append(",${data.brakeFluid}")
                append(",${data.battery}")
                append("\n")
            }
            writer?.write(line)
            rowCount++

            if (rowCount % 10 == 0) writer?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Write error", e)
        }
    }

    private fun closeFile() {
        try {
            writer?.flush()
            writer?.close()
        } catch (_: Exception) { }
        writer = null
    }
}
