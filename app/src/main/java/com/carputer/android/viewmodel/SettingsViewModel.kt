package com.carputer.android.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.carputer.android.CarputerApplication
import com.carputer.android.data.ConfigRepository
import com.carputer.android.service.*
import com.hoho.android.usbserial.driver.UsbSerialDriver
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*

data class ThemeOption(
    val name: String,
    val accentDefault: Color,
    val accentHexDefault: String,
)

val themeOptions = listOf(
    ThemeOption("Dark", Color(0xFF00A8E8), "#00A8E8"),
    ThemeOption("Light", Color(0xFF0078D4), "#0078D4"),
    ThemeOption("Blue", Color(0xFF00A8E8), "#00A8E8"),
    ThemeOption("Red", Color(0xFFFF2020), "#FF2020"),
    ThemeOption("Green", Color(0xFF00FF88), "#00FF88"),
    ThemeOption("Purple", Color(0xFF9B59B6), "#9B59B6"),
    ThemeOption("Orange", Color(0xFFFF6B35), "#FF6B35"),
)

data class SettingsState(
    // Theme
    val currentTheme: String = "Dark",
    val accentHex: String = "#00A8E8",
    val gaugeBorderMode: String = "gauge",
    // WiFi
    val wifiConnected: Boolean = false,
    val wifiSsid: String = "",
    val wifiIp: String = "",
    val wifiSignal: Int = 0,
    val wifiStatusText: String = "Not connected",
    val wifiNetworks: List<String> = emptyList(),
    // DTC
    val dtcCodes: List<com.carputer.android.data.model.DtcCode> = emptyList(),
    val dtcCount: Int = 0,
    val dtcHistory: List<com.carputer.android.data.model.DtcHistoryEntry> = emptyList(),
    val dtcBusy: Boolean = false,
    val dtcStatusText: String = "Ready",
    val dtcLastScanMode: String = "",
    // System
    val uptime: String = "",
    val systemLoad: String = "",
    val diskUsage: String = "",
    val version: String = "1.3.0",
    // Diagnostics
    val diagRunning: Boolean = false,
    val diagReport: String = "",
    val diagIssueCount: Int = 0,
    // Data logging
    val logging: Boolean = false,
    val logPath: String = "",
    // CarControl
    val carControlHost: String = "192.168.4.1",
    val carControlPort: Int = 5000,
    // ESP32 Flasher
    val flasherState: FlasherState = FlasherState.IDLE,
    val flasherProgress: Float = 0f,
    val flasherStatusText: String = "",
    val flasherDeviceName: String = "",
    val flasherDevices: List<String> = emptyList(),
)

class SettingsViewModel(
    private val configRepository: ConfigRepository,
    private val themeState: com.carputer.android.ui.theme.ThemeState,
    private val wifiService: WifiService?,
    private val dtcService: DtcService?,
    private val diagnosticsService: DiagnosticsService?,
    private val dataLoggerService: DataLoggerService?,
    private val systemService: SystemService?,
    private val esp32FlasherService: Esp32FlasherService? = null,
    private val appContext: Context? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadThemeConfig()
        observeWifi()
        observeDtc()
        observeDiagnostics()
        observeDataLogger()
        refreshSystemInfo()
        observeFlasher()
    }

    private fun loadThemeConfig() {
        _state.update { it.copy(
            currentTheme = themeState.currentThemeName,
            accentHex = themeState.accentColorHex.value,
            gaugeBorderMode = themeState.gaugeBorderMode,
        ) }
    }

    private fun observeWifi() {
        wifiService?.let { ws ->
            _state.update { it.copy(
                wifiConnected = ws.connected.value,
                wifiSsid = ws.ssid.value,
                wifiIp = ws.ipAddress.value,
                wifiSignal = ws.signalStrength.value,
                wifiStatusText = ws.statusText.value,
                wifiNetworks = ws.networks.value,
            ) }
        }
    }

    private fun observeDtc() {
        dtcService?.let { ds ->
            _state.update { it.copy(
                dtcCodes = ds.dtcCodes.value,
                dtcCount = ds.dtcCount.value,
                dtcHistory = ds.dtcHistory.value,
                dtcBusy = ds.busy.value,
                dtcStatusText = ds.statusText.value,
                dtcLastScanMode = ds.lastScanMode.value,
            ) }
        }
    }

    private fun observeDiagnostics() {
        diagnosticsService?.let { ds ->
            _state.update { it.copy(
                diagRunning = ds.running.value,
                diagReport = ds.lastReport.value,
                diagIssueCount = ds.issueCount.value,
            ) }
        }
    }

    private fun observeDataLogger() {
        dataLoggerService?.let { dl ->
            _state.update { it.copy(
                logging = dl.logging.value,
                logPath = dl.logPath.value,
            ) }
        }
    }

    fun setTheme(name: String) {
        val option = themeOptions.find { it.name == name } ?: return
        themeState.setCurrentTheme(name)
        themeState.setAccentColor(option.accentDefault, option.accentHexDefault)
        _state.update { it.copy(currentTheme = name, accentHex = option.accentHexDefault) }
        CarputerApplication.instance.saveTheme()
    }

    fun setAccentColor(hex: String) {
        themeState.setAccentColor(Color(android.graphics.Color.parseColor(hex)), hex)
        _state.update { it.copy(accentHex = hex) }
        CarputerApplication.instance.saveTheme()
    }

    fun setGaugeBorderMode(mode: String) {
        themeState.updateGaugeBorderMode(mode)
        _state.update { it.copy(gaugeBorderMode = mode) }
        CarputerApplication.instance.saveTheme()
    }

    fun scanWifi() {
        wifiService?.scanNetworks()
    }

    fun connectToWifi(ssid: String) {
        wifiService?.connectToNetwork(ssid)
    }

    fun disconnectWifi() {
        wifiService?.disconnectNetwork()
    }

    fun scanDtc() {
        dtcService?.scanDtc()
        refreshSystemInfo()
    }

    fun scanDtcTestMode() {
        dtcService?.scanDtcTestMode()
    }

    fun clearDtcHistory() {
        dtcService?.clearHistory()
    }

    fun runDiagnostics() {
        diagnosticsService?.runDiagnostics()
    }

    fun saveDiagnosticsReport() {
        diagnosticsService?.saveReport()
    }

    fun refreshSystemInfo() {
        _state.update { it.copy(
            uptime = systemService?.getSystemUptime() ?: "N/A",
            systemLoad = systemService?.getSystemLoad() ?: "N/A",
            diskUsage = systemService?.getDiskUsage() ?: "N/A",
        ) }
    }

    private fun observeFlasher() {
        esp32FlasherService?.let { fs ->
            viewModelScope.launch {
                fs.status.collect { status ->
                    _state.update { it.copy(
                        flasherState = status.state,
                        flasherProgress = status.progress,
                        flasherStatusText = status.statusText,
                        flasherDeviceName = status.deviceName,
                    ) }
                }
            }
        }
    }

    fun scanFlasherDevices() {
        val fs = esp32FlasherService ?: return
        val drivers = fs.findDevice()
        val names = drivers.map { d ->
            val name = d.device.productName ?: "Unknown ESP32"
            val hasPerm = if (fs.hasPermission(d)) "✓" else "⚠"
            "$name (${d.device.vendorId.toString(16)}:${d.device.productId.toString(16)}) $hasPerm"
        }
        _state.update { it.copy(flasherDevices = names) }
    }

    fun flashFirmware(deviceIndex: Int) {
        val fs = esp32FlasherService ?: return
        val drivers = fs.findDevice()
        if (deviceIndex !in drivers.indices) return
        val driver = drivers[deviceIndex]
        if (!fs.hasPermission(driver)) {
            _state.update { it.copy(flasherStatusText = "No USB permission - replug device") }
            return
        }
        appContext?.let { ctx ->
            try {
                fs.flashFromAsset(driver, "firmware/esp32_body_controller.bin")
            } catch (e: Exception) {
                _state.update { it.copy(flasherStatusText = "No bundled firmware. Build from PlatformIO: esp32_body_controller/.pio/build/esp32dev/firmware.bin -> assets/firmware/") }
            }
        }
    }

    fun cancelFlash() {
        esp32FlasherService?.cancel()
    }

    fun setCarControlHost(host: String) {
        _state.update { it.copy(carControlHost = host) }
    }

    fun setCarControlPort(port: Int) {
        _state.update { it.copy(carControlPort = port) }
    }

    fun toggleLogging() {
        if (_state.value.logging) {
            dataLoggerService?.stopLogging()
        } else {
            dataLoggerService?.startLogging()
        }
        observeDataLogger()
    }
}
