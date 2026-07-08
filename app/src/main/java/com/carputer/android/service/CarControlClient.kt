package com.carputer.android.service

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import kotlin.coroutines.coroutineContext

enum class JoypadEvent {
    UP, DOWN, LEFT, RIGHT, SELECT, EXIT
}

class CarControlClient {

    companion object {
        private const val TAG = "CarControlClient"
        const val DEFAULT_HOST = "192.168.4.1"
        const val DEFAULT_PORT = 5000
        private const val RETRY_MS = 3000L
    }

    private var socket: Socket? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _statusText = MutableStateFlow("Not connected")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    // HVAC
    private val _hvacEnabled = MutableStateFlow(false)
    val hvacEnabled: StateFlow<Boolean> = _hvacEnabled.asStateFlow()
    private val _fanSpeed = MutableStateFlow(0)
    val fanSpeed: StateFlow<Int> = _fanSpeed.asStateFlow()
    private val _acEnabled = MutableStateFlow(false)
    val acEnabled: StateFlow<Boolean> = _acEnabled.asStateFlow()

    // Vehicle
    private val _doorsLocked = MutableStateFlow(false)
    val doorsLocked: StateFlow<Boolean> = _doorsLocked.asStateFlow()
    private val _remoteStartActive = MutableStateFlow(false)
    val remoteStartActive: StateFlow<Boolean> = _remoteStartActive.asStateFlow()
    private val _fanRelay = MutableStateFlow(0)
    val fanRelay: StateFlow<Int> = _fanRelay.asStateFlow()

    // Extra relays
    private val _extra = MutableStateFlow(listOf(false, false))
    val extra: StateFlow<List<Boolean>> = _extra.asStateFlow()

    // Joypad events (SharedFlow because these are discrete events, not state)
    private val _joypadEvents = MutableSharedFlow<JoypadEvent>(extraBufferCapacity = 8)
    val joypadEvents: SharedFlow<JoypadEvent> = _joypadEvents.asSharedFlow()

    private var host = DEFAULT_HOST
    private var port = DEFAULT_PORT

    fun connect(address: String = "$DEFAULT_HOST:$DEFAULT_PORT") {
        val parts = address.split(":")
        host = parts.getOrElse(0) { DEFAULT_HOST }
        port = parts.getOrElse(1) { DEFAULT_PORT.toString() }.toIntOrNull() ?: DEFAULT_PORT
        startConnection()
    }

    private fun startConnection() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                try {
                    _statusText.value = "Connecting to $host:$port..."
                    socket = Socket(host, port)
                    writer = OutputStreamWriter(socket!!.getOutputStream(), Charsets.UTF_8)
                    reader = BufferedReader(InputStreamReader(socket!!.getInputStream(), Charsets.UTF_8))
                    _connected.value = true
                    _statusText.value = "Connected on $host:$port"
                    Log.i(TAG, "Connected to $host:$port")

                    queryStatus()
                    readLoop()
                } catch (e: Exception) {
                    Log.w(TAG, "Connection failed, retrying in ${RETRY_MS}ms", e)
                    _connected.value = false
                    _statusText.value = "Retrying $host:$port..."
                    delay(RETRY_MS)
                }
            }
        }
    }

    private suspend fun readLoop() {
        try {
            val buf = StringBuilder()
            while (coroutineContext.isActive && socket?.isConnected == true) {
                val c = reader?.read() ?: break
                if (c == -1) break
                val ch = c.toChar()
                if (ch == '\n') {
                    val line = buf.toString().trim()
                    buf.clear()
                    if (line.isNotEmpty()) processLine(line)
                } else {
                    buf.append(ch)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Read loop error", e)
        } finally {
            _connected.value = false
            _statusText.value = "Disconnected"
            close()
        }
    }

    private fun processLine(line: String) {
        Log.d(TAG, "Received: $line")
        if (line.startsWith("H:")) parseStatus(line)
        else if (line == "OK") Log.d(TAG, "Command acknowledged")
        else if (line.startsWith("ERR:")) Log.w(TAG, "Error: ${line.substring(4)}")
        else if (line.startsWith("INFO:")) Log.i(TAG, "Info: ${line.substring(5)}")
        else if (line.startsWith("WARN:")) Log.w(TAG, "Warning: ${line.substring(5)}")
        else if (line.startsWith("J:")) parseJoypad(line)
    }

    private fun parseJoypad(line: String) {
        if (line.length < 3) return
        val dir = line.substring(2)
        val event = when (dir) {
            "U" -> JoypadEvent.UP
            "D" -> JoypadEvent.DOWN
            "L" -> JoypadEvent.LEFT
            "R" -> JoypadEvent.RIGHT
            "S" -> JoypadEvent.SELECT
            "E" -> JoypadEvent.EXIT
            else -> return
        }
        Log.d(TAG, "Joypad: $dir")
        _joypadEvents.tryEmit(event)
    }

    private fun parseStatus(line: String) {
        val parts = line.split(" ")
        for (part in parts) {
            when {
                part.startsWith("H:") -> _hvacEnabled.value = part.substring(2) == "1"
                part.startsWith("S:") -> _fanSpeed.value = part.substring(2).toIntOrNull() ?: 0
                part.startsWith("A:") -> _acEnabled.value = part.substring(2) == "1"
                part.startsWith("L:") -> _doorsLocked.value = part.substring(2) == "1"
                part.startsWith("R:") -> _remoteStartActive.value = part.substring(2) == "1"
                part.startsWith("F:") -> _fanRelay.value = part.substring(2).toIntOrNull() ?: 0
                part.startsWith("P:") -> {
                    val vals = part.substring(2).split(",")
                    _extra.value = listOf(
                        vals.getOrElse(0) { "0" } == "1",
                        vals.getOrElse(1) { "0" } == "1"
                    )
                }
            }
        }
    }

    private suspend fun sendCommand(cmd: Char, value: Int) {
        if (!_connected.value) return
        try {
            val data = "$cmd$value\n"
            writer?.write(data)
            writer?.flush()
            Log.d(TAG, "Sent: $cmd$value")
        } catch (e: Exception) {
            Log.e(TAG, "Send error", e)
        }
    }

    fun setHvacEnabled(enabled: Boolean) {
        scope.launch { sendCommand('H', if (enabled) 1 else 0) }
    }

    fun setFanSpeed(speed: Int) {
        scope.launch { sendCommand('S', speed.coerceIn(0, 5)) }
    }

    fun setAcEnabled(enabled: Boolean) {
        scope.launch { sendCommand('A', if (enabled) 1 else 0) }
    }

    fun lockDoors() {
        scope.launch { sendCommand('L', 1) }
    }

    fun unlockDoors() {
        scope.launch { sendCommand('L', 0) }
    }

    fun windowsUp() {
        scope.launch { sendCommand('W', 1) }
    }

    fun windowsDown() {
        scope.launch { sendCommand('W', 0) }
    }

    fun startRemote() {
        scope.launch { sendCommand('R', 1) }
    }

    fun stopRemote() {
        scope.launch { sendCommand('R', 0) }
    }

    fun setFanRelay(level: Int) {
        val clamped = level.coerceIn(0, 2)
        _fanRelay.value = clamped
        scope.launch { sendCommand('F', clamped) }
    }

    fun setExtra(index: Int, on: Boolean) {
        if (index !in 1..2) return
        val encoded = index * 10 + if (on) 1 else 0
        scope.launch { sendCommand('P', encoded) }
    }

    fun playChime() {
        scope.launch { sendCommand('C', 1) }
    }

    fun queryStatus() {
        scope.launch {
            try {
                writer?.write("?\n")
                writer?.flush()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    fun disconnect() {
        job?.cancel()
        close()
    }

    private fun close() {
        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) { }
        reader = null
        writer = null
        socket = null
        _connected.value = false
    }
}
