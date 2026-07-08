package com.carputer.android.viewmodel

import androidx.lifecycle.ViewModel
import com.carputer.android.data.ConfigRepository
import com.carputer.android.data.model.SensorData
import com.carputer.android.service.CarControlClient
import com.carputer.android.service.SensorClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DashboardState(
    val speed: Int = 0,
    val rpm: Int = 0,
    val throttle: Int = 0,
    val map: Int = 0,
    val coolantTemp: Int = 0,
    val oilTemp: Int = 0,
    val ambientTemp: Int = 0,
    val intakeTemp: Int = 0,
    val driverDoor: Boolean = false,
    val passengerDoor: Boolean = false,
    val trunk: Boolean = false,
    val hood: Boolean = false,
    val fuelLevel: Int = 0,
    val oilPressure: Int = 0,
    val brakeFluid: Int = 0,
    val battery: Int = 0,
    val sensorConnected: Boolean = false,
    // CarControl state
    val carConnected: Boolean = false,
    val hvacEnabled: Boolean = false,
    val fanSpeed: Int = 0,
    val acEnabled: Boolean = false,
    val doorsLocked: Boolean = false,
    val remoteStartActive: Boolean = false,
    val fanRelay: Int = 0,
    val configFanSpeed: Int = 0,
    val configHvacEnabled: Boolean = false,
    val configAcEnabled: Boolean = false,
    val configAutoMode: Boolean = true,
    val configRecirculate: Boolean = false,
    val configTargetTemp: Int = 72,
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaPosition: Long = 0,
    val mediaDuration: Long = 0,
    val mediaPlaying: Boolean = false,
    val spectrumData: List<Float> = emptyList(),
    val artworkUrl: String = "",
    val engineRunning: Boolean = false,
)

class DashboardViewModel(
    private val sensorClient: SensorClient,
    private val carControlClient: CarControlClient,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeSensorData()
        observeCarControl()
        observeConfig()
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }

    private fun observeSensorData() {
        sensorClient.sensorData.onEach { data ->
            _state.update { it.copy(
                speed = data.speed,
                rpm = data.rpm,
                throttle = data.throttle,
                map = data.map,
                coolantTemp = data.coolant,
                oilTemp = data.oil,
                ambientTemp = data.ambient,
                intakeTemp = data.intake,
                driverDoor = data.driverDoor,
                passengerDoor = data.passengerDoor,
                trunk = data.trunk,
                hood = data.hood,
                fuelLevel = data.fuel,
                oilPressure = data.oilPressure,
                brakeFluid = data.brakeFluid,
                battery = data.battery,
                sensorConnected = true,
                engineRunning = data.rpm > 100,
            ) }
        }.launchIn(scope)

        sensorClient.connected.onEach { connected ->
            _state.update { it.copy(sensorConnected = connected) }
        }.launchIn(scope)
    }

    private fun observeCarControl() {
        carControlClient.connected.onEach { connected ->
            _state.update { it.copy(carConnected = connected) }
        }.launchIn(scope)
        carControlClient.hvacEnabled.onEach { v ->
            _state.update { it.copy(hvacEnabled = v) }
        }.launchIn(scope)
        carControlClient.fanSpeed.onEach { v ->
            _state.update { it.copy(fanSpeed = v) }
        }.launchIn(scope)
        carControlClient.acEnabled.onEach { v ->
            _state.update { it.copy(acEnabled = v) }
        }.launchIn(scope)
        carControlClient.doorsLocked.onEach { v ->
            _state.update { it.copy(doorsLocked = v) }
        }.launchIn(scope)
        carControlClient.remoteStartActive.onEach { v ->
            _state.update { it.copy(remoteStartActive = v) }
        }.launchIn(scope)
        carControlClient.fanRelay.onEach { v ->
            _state.update { it.copy(fanRelay = v) }
        }.launchIn(scope)
    }

    private fun observeConfig() {
        // Read from config repository
        _state.update { it.copy(
            configFanSpeed = configRepository.fanSpeed,
            configHvacEnabled = configRepository.hvacEnabled,
            configAcEnabled = configRepository.acEnabled,
            configAutoMode = configRepository.autoMode,
            configRecirculate = configRepository.recirculate,
            configTargetTemp = configRepository.targetTemp,
        ) }
    }

    fun updateMediaState(
        title: String,
        artist: String,
        position: Long,
        duration: Long,
        playing: Boolean,
        spectrum: List<Float>,
        artwork: String,
    ) {
        _state.update { it.copy(
            mediaTitle = title,
            mediaArtist = artist,
            mediaPosition = position,
            mediaDuration = duration,
            mediaPlaying = playing,
            spectrumData = spectrum,
            artworkUrl = artwork,
        ) }
    }

    // Health score calculation (replicates QML logic)
    fun computeHealthScore(data: DashboardState): Int {
        val coolantScore = if (data.coolantTemp <= 0) 100f
        else if (data.coolantTemp < 220) 100f
        else kotlin.math.max(0f, 100f - (data.coolantTemp - 220) * 100f / 40f)

        val oilScore = if (data.oilTemp <= 0) 100f
        else if (data.oilTemp < 220) 100f
        else kotlin.math.max(0f, 100f - (data.oilTemp - 220) * 100f / 50f)

        val pressureScore = if (!data.engineRunning || data.oilPressure <= 0) 100f
        else if (data.oilPressure > 40) 100f
        else kotlin.math.max(0f, data.oilPressure * 100f / 40f)

        val batteryScore = if (data.battery <= 0) 100f
        else if (data.battery > 50) 100f
        else kotlin.math.max(0f, data.battery * 100f / 50f)

        val driveScore = when {
            data.throttle < 30 -> 100f
            data.throttle < 60 -> 80f
            data.throttle < 80 -> 60f
            else -> 40f
        }

        return (coolantScore * 0.30f + oilScore * 0.20f +
                pressureScore * 0.25f + batteryScore * 0.15f + driveScore * 0.10f).toInt()
    }

    fun computeWorstAlert(data: DashboardState): String {
        val alerts = listOfNotNull(
            computeCoolantAlert(data.coolantTemp),
            computeOilTempAlert(data.oilTemp),
            if (data.engineRunning) computeOilPressureAlert(data.oilPressure) else null,
            computeBatteryAlert(data.battery),
        )
        return when {
            alerts.contains("critical") -> "critical"
            alerts.contains("danger") -> "danger"
            alerts.contains("caution") -> "caution"
            else -> "ok"
        }
    }

    private fun computeCoolantAlert(temp: Int): String? {
        if (temp <= 0) return null
        return when {
            temp > 245 -> "critical"
            temp > 230 -> "danger"
            temp > 220 -> "caution"
            else -> null
        }
    }

    private fun computeOilTempAlert(temp: Int): String? {
        if (temp <= 0) return null
        return when {
            temp > 260 -> "critical"
            temp > 240 -> "danger"
            temp > 220 -> "caution"
            else -> null
        }
    }

    private fun computeOilPressureAlert(pressure: Int): String? {
        if (pressure <= 0) return null
        return when {
            pressure < 10 -> "critical"
            pressure < 20 -> "danger"
            pressure < 40 -> "caution"
            else -> null
        }
    }

    private fun computeBatteryAlert(battery: Int): String? {
        if (battery <= 0) return null
        return when {
            battery < 10 -> "critical"
            battery < 25 -> "danger"
            battery < 50 -> "caution"
            else -> null
        }
    }

    fun getActiveWarnings(data: DashboardState): List<com.carputer.android.ui.components.WarningItem> {
        val list = mutableListOf<com.carputer.android.ui.components.WarningItem>()
        val coolantAlert = computeCoolantAlert(data.coolantTemp)
        if (coolantAlert != null) list.add(
            com.carputer.android.ui.components.WarningItem(
                "COOLANT", coolantAlert, "${data.coolantTemp}°F",
                if (coolantAlert == "critical") "STOP ENGINE - Pull over immediately"
                else if (coolantAlert == "danger") "Reduce load, check coolant level"
                else "Monitor temperature"
            )
        )
        val oilTempAlert = computeOilTempAlert(data.oilTemp)
        if (oilTempAlert != null) list.add(
            com.carputer.android.ui.components.WarningItem(
                "OIL TEMP", oilTempAlert, "${data.oilTemp}°F",
                if (oilTempAlert == "critical") "STOP ENGINE - Oil breakdown risk"
                else if (oilTempAlert == "danger") "Reduce engine load"
                else "Monitor oil temperature"
            )
        )
        val oilPressureAlert = if (data.engineRunning) computeOilPressureAlert(data.oilPressure) else null
        if (oilPressureAlert != null) list.add(
            com.carputer.android.ui.components.WarningItem(
                "OIL PRESS", oilPressureAlert, "${data.oilPressure}%",
                if (oilPressureAlert == "critical") "STOP ENGINE - No oil pressure!"
                else if (oilPressureAlert == "danger") "Check oil level immediately"
                else "Check oil level soon"
            )
        )
        val batteryAlert = computeBatteryAlert(data.battery)
        if (batteryAlert != null) list.add(
            com.carputer.android.ui.components.WarningItem(
                "BATTERY", batteryAlert, "${data.battery}%",
                if (batteryAlert == "critical") "Charge or replace battery"
                else if (batteryAlert == "danger") "Recharge battery soon"
                else "Monitor battery level"
            )
        )
        return list
    }

    fun setConfigHvac(v: Boolean) {
        configRepository.hvacEnabled = v
        _state.update { it.copy(configHvacEnabled = v) }
        carControlClient.setHvacEnabled(v)
    }

    fun setConfigAc(v: Boolean) {
        configRepository.acEnabled = v
        _state.update { it.copy(configAcEnabled = v) }
        carControlClient.setAcEnabled(v)
    }

    fun setConfigAutoMode(v: Boolean) {
        configRepository.autoMode = v
        _state.update { it.copy(configAutoMode = v) }
    }

    fun setConfigTargetTemp(v: Int) {
        configRepository.targetTemp = v
        _state.update { it.copy(configTargetTemp = v) }
    }
}
