package com.carputer.android.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiService(private val context: Context) {

    companion object {
        private const val TAG = "WifiService"
        const val CARPUTER_ECU_SSID = "Carputer_ECU"
        const val STATIC_IP = "192.168.4.3"
    }

    private val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _ssid = MutableStateFlow("")
    val ssid: StateFlow<String> = _ssid.asStateFlow()

    private val _ipAddress = MutableStateFlow("")
    val ipAddress: StateFlow<String> = _ipAddress.asStateFlow()

    private val _signalStrength = MutableStateFlow(0)
    val signalStrength: StateFlow<Int> = _signalStrength.asStateFlow()

    private val _statusText = MutableStateFlow("Not connected")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _networks = MutableStateFlow<List<String>>(emptyList())
    val networks: StateFlow<List<String>> = _networks.asStateFlow()

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val results = wifiManager.scanResults
                _networks.value = results
                    .sortedByDescending { it.level }
                    .map { "${it.SSID} (${it.level}dBm)" }
                Log.d(TAG, "Networks found: ${results.size}")
            }
        }
    }

    init {
        context.registerReceiver(scanReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
    }

    fun checkConnectionStatus() {
        val network = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        _connected.value = isWifi

        if (isWifi) {
            val info = wifiManager.connectionInfo
            _ssid.value = info.ssid?.removeSurrounding("\"") ?: ""
            _signalStrength.value = info.rssi
            val ipInt = info.ipAddress
            _ipAddress.value = if (ipInt != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    (ipInt shr 8) and 0xff,
                    (ipInt shr 16) and 0xff,
                    (ipInt shr 24) and 0xff
                )
            } else ""
            _statusText.value = if (isWifi) "Connected to ${_ssid.value}" else "Not connected"
        }
    }

    fun scanNetworks() {
        wifiManager.startScan()
        _statusText.value = "Scanning..."
    }

    fun connectToNetwork(ssid: String, password: String? = null) {
        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            if (password != null) {
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                preSharedKey = "\"$password\""
            } else {
                allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
        }
        val netId = wifiManager.addNetwork(config)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
        _statusText.value = "Connecting to $ssid..."
    }

    fun disconnectNetwork() {
        wifiManager.disconnect()
        _connected.value = false
        _ssid.value = ""
        _statusText.value = "Disconnected"
    }

    fun connectToCarputerECU() {
        connectToNetwork(CARPUTER_ECU_SSID)
        _statusText.value = "Connecting to Carputer_ECU..."
    }

    fun release() {
        try {
            context.unregisterReceiver(scanReceiver)
        } catch (_: Exception) { }
    }
}
