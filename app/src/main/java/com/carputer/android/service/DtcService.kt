package com.carputer.android.service

import android.util.Log
import com.carputer.android.data.model.DtcCode
import com.carputer.android.data.model.DtcHistoryEntry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

class DtcService {

    companion object {
        private const val TAG = "DtcService"
        private const val ESP_IP = "192.168.4.20"
        private const val PORT = 5001
        private const val TIMEOUT_MS = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _dtcCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcCodes: StateFlow<List<DtcCode>> = _dtcCodes.asStateFlow()

    private val _dtcCount = MutableStateFlow(0)
    val dtcCount: StateFlow<Int> = _dtcCount.asStateFlow()

    private val _dtcHistory = MutableStateFlow<List<DtcHistoryEntry>>(emptyList())
    val dtcHistory: StateFlow<List<DtcHistoryEntry>> = _dtcHistory.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _statusText = MutableStateFlow("Ready")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _lastScanMode = MutableStateFlow("")
    val lastScanMode: StateFlow<String> = _lastScanMode.asStateFlow()

    private val history = mutableListOf<DtcHistoryEntry>()

    fun scanDtc() {
        _busy.value = true
        _lastScanMode.value = "Standard"
        _statusText.value = "Scanning for DTC codes..."
        scope.launch {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = TIMEOUT_MS.toInt()
                val cmd = """{"cmd":"dtc_scan"}""".toByteArray()
                val packet = DatagramPacket(cmd, cmd.size, InetAddress.getByName(ESP_IP), PORT)
                socket.send(packet)

                val buffer = ByteArray(4096)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val data = String(response.data, 0, response.length, Charsets.UTF_8)
                parseDtcResponse(data)
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "DTC scan error", e)
                _statusText.value = "Scan failed: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    fun scanDtcTestMode() {
        _busy.value = true
        _lastScanMode.value = "Test Mode"
        _statusText.value = "Running DTC test mode..."
        scope.launch {
            try {
                val socket = DatagramSocket()
                socket.soTimeout = TIMEOUT_MS.toInt()
                val cmd = """{"cmd":"dtc_test"}""".toByteArray()
                val packet = DatagramPacket(cmd, cmd.size, InetAddress.getByName(ESP_IP), PORT)
                socket.send(packet)

                val buffer = ByteArray(4096)
                val response = DatagramPacket(buffer, buffer.size)
                socket.receive(response)

                val data = String(response.data, 0, response.length, Charsets.UTF_8)
                parseDtcResponse(data)
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "DTC test mode error", e)
                _statusText.value = "Test mode failed: ${e.message}"
            } finally {
                _busy.value = false
            }
        }
    }

    private fun parseDtcResponse(data: String) {
        try {
            val lines = data.split("\n").filter { it.startsWith("DTC:") }
            val codes = lines.mapNotNull { line ->
                val codeStr = line.substringAfter("DTC:").trim()
                codeStr.toIntOrNull()?.let { DtcCode(code = it, description = describeCode(it)) }
            }
            _dtcCodes.value = codes
            _dtcCount.value = codes.size
            _statusText.value = "Found ${codes.size} DTC code(s)"

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            codes.forEach { code ->
                history.add(DtcHistoryEntry(code = code.code, description = code.description, timestamp = dateStr))
            }
            _dtcHistory.value = history.toList()

            if (codes.isEmpty()) {
                _statusText.value = "No DTC codes found"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse DTC response error", e)
            _statusText.value = "Failed to parse DTC data"
        }
    }

    fun clearHistory() {
        history.clear()
        _dtcHistory.value = emptyList()
    }

    fun describeCode(code: Int): String {
        return when (code) {
            // Toyota/standard OBD2 P-codes
            12 -> "Code 12  RPM signal (NE/NE+) — No RPM signal to ECU"
            13 -> "Code 13  RPM signal (NE/G+) — No G signal to ECU"
            14 -> "Code 14  Ignition signal (IGf) — No IGF signal to ECU"
            21 -> "Code 21  Oxygen sensor signal"
            22 -> "Code 22  Coolant temperature sensor (THW) — Circuit malfunction"
            24 -> "Code 24  Intake air temperature sensor"
            31 -> "Code 31  Manifold absolute pressure (MAP) sensor"
            41 -> "Code 41  Throttle position sensor (VTA) — Circuit malfunction"
            42 -> "Code 42  Vehicle speed sensor (SPD) — No signal"
            43 -> "Code 43  Starter signal"
            51 -> "Code 51  Switch condition signal"
            52 -> "Code 52  Knock sensor signal"
            71 -> "Code 71  EGR system malfunction"
            in 1000..1999 -> "P${code}  Manufacturer-specific diagnostic code"
            in 2000..2999 -> "P${code}  Generic OBD2 powertrain code"
            else -> "Code $code  Unknown diagnostic code"
        }
    }
}
