package com.carputer.android.service

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import com.carputer.android.data.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SensorClient(private val context: Context) {

    companion object {
        private const val TAG = "SensorClient"
        private const val PORT = 5001
        private const val BUFFER_SIZE = 4096
        private const val SENSOR_IP = "192.168.4.3"
        private const val SENSOR_MODULE_IP = "192.168.4.20"
        private const val CMD_PORT = 5002
        private const val GATEWAY = "192.168.4.1"
        private const val EXPECTED_SSID = "Carputer_ECU"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private var socket: DatagramSocket? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _statusText = MutableStateFlow("Not connected")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    fun start() {
        Log.e(TAG, "=== SensorClient.start() called ===")
        if (job?.isActive == true) return
        _statusText.value = "Starting sensor listener on UDP $PORT"
        job = scope.launch {
            ensureCorrectIp()
            try {
                socket = DatagramSocket(PORT).also { it.soTimeout = 0 }
                _statusText.value = "Listening on UDP $PORT"
                val buffer = ByteArray(BUFFER_SIZE)

                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket?.receive(packet)
                        val data = packet.data.copyOfRange(0, packet.length)
                        val text = String(data, Charsets.UTF_8)
                        Log.d(TAG, "Received ${data.size} bytes from ${packet.address}:${packet.port}")
                        parseData(text)
                        _connected.value = true
                        _statusText.value = "Receiving from ${packet.address}"
                    } catch (e: Exception) {
                        if (isActive) {
                            Log.e(TAG, "Receive error", e)
                            _connected.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket error", e)
                _statusText.value = "Failed to bind UDP socket"
            }
        }
    }

    fun stop() {
        job?.cancel()
        socket?.close()
        socket = null
        _connected.value = false
        _statusText.value = "Stopped"
    }

    private fun parseData(text: String) {
        try {
            val event = json.decodeFromString<com.carputer.android.data.model.SensorEvent>(text)
            if (event.event == "sensors") {
                _sensorData.value = event.data
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse error: ${e.message}")
        }
    }

    fun reconnect() {
        stop()
        start()
    }

    fun sendQuery() {
        scope.launch {
            try {
                val query = """{"cmd":"query"}""".toByteArray()
                val addr = InetAddress.getByName(SENSOR_MODULE_IP)
                val packet = DatagramPacket(query, query.size, addr, CMD_PORT)
                socket?.send(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send query", e)
            }
        }
    }

    private fun ensureCorrectIp() {
        try {
            val wifi = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            var info = wifi.connectionInfo
            var retries = 0
            while (info == null && retries < 10) {
                Thread.sleep(500)
                info = wifi.connectionInfo
                retries++
            }
            if (info == null) return
            val currentIpInt = info.ipAddress
            val currentIp = String.format(
                "%d.%d.%d.%d",
                currentIpInt and 0xff,
                currentIpInt shr 8 and 0xff,
                currentIpInt shr 16 and 0xff,
                currentIpInt shr 24 and 0xff
            )
            Log.e(TAG, "ensureCorrectIp: current IP=$currentIp")
            if (currentIp == SENSOR_IP) { Log.e(TAG, "ensureCorrectIp: already correct"); return }

            Log.e(TAG, "ensureCorrectIp: scanning configured networks")
            for (config in wifi.configuredNetworks) {
                if (config.networkId == info.networkId) {
                    Log.e(TAG, "ensureCorrectIp: found network config, applying static IP")
                    val ipConfigClass = Class.forName("android.net.IpConfiguration")
                    val ipAssignmentClass = Class.forName("android.net.IpConfiguration\$IpAssignment")
                    val proxySettingsClass = Class.forName("android.net.IpConfiguration\$ProxySettings")
                    val staticConfigClass = Class.forName("android.net.StaticIpConfiguration")
                    val linkAddrClass = Class.forName("android.net.LinkAddress")

                    val staticConfig = staticConfigClass.newInstance()
                    val ipAddr = InetAddress.getByName("192.168.4.3")
                    val linkAddrCtor = linkAddrClass.getConstructor(InetAddress::class.java, Int::class.java)
                    staticConfigClass.getField("ipAddress").set(staticConfig, linkAddrCtor.newInstance(ipAddr, 24))
                    staticConfigClass.getField("gateway").set(staticConfig, InetAddress.getByName("192.168.4.1"))
                    @Suppress("UNCHECKED_CAST")
                    (staticConfigClass.getField("dnsServers").get(staticConfig) as MutableList<InetAddress>).apply {
                        clear()
                        add(InetAddress.getByName("8.8.8.8"))
                    }

                    val ipConfigCtor = ipConfigClass.getConstructor(
                        ipAssignmentClass, proxySettingsClass, staticConfigClass, Class.forName("android.net.ProxyInfo")
                    )
                    val ipConfig = ipConfigCtor.newInstance(
                        ipAssignmentClass.getField("STATIC").get(null),
                        proxySettingsClass.getField("NONE").get(null),
                        staticConfig, null
                    )

                    WifiConfiguration::class.java.getField("ipConfiguration").set(config, ipConfig)
                    wifi.updateNetwork(config)
                    wifi.saveConfiguration()
                    wifi.reconnect()
                    Log.i(TAG, "Set static IP $SENSOR_IP for $EXPECTED_SSID")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set static IP: ${e.message}", e)
        }
    }
}
