package com.carputer.android.service

import android.content.Context
import android.util.Log
import com.carputer.android.data.model.SensorData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TripComputerService(private val context: Context? = null) {

    companion object {
        private const val TAG = "TripComputer"
        private const val TICK_MS = 1000L
        private const val PREFS_NAME = "trip_computer"
        private const val KEY_DISTANCE = "distance"
        private const val KEY_TIME = "total_seconds"
        private const val KEY_SPEED_SUM = "speed_sum"
        private const val KEY_SPEED_SAMPLES = "speed_samples"
    }

    private var scope: CoroutineScope? = null

    private val _distance = MutableStateFlow(0.0)
    val distance: StateFlow<Double> = _distance.asStateFlow()

    private val _avgSpeed = MutableStateFlow(0.0)
    val avgSpeed: StateFlow<Double> = _avgSpeed.asStateFlow()

    private val _fuelUsed = MutableStateFlow(0.0)
    val fuelUsed: StateFlow<Double> = _fuelUsed.asStateFlow()

    private val _instantMpg = MutableStateFlow(0.0)
    val instantMpg: StateFlow<Double> = _instantMpg.asStateFlow()

    private val _tripTime = MutableStateFlow(0)
    val tripTime: StateFlow<Int> = _tripTime.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private var lastSpeed = 0
    private var lastRpm = 0
    private var lastThrottle = 0
    private var totalSeconds = 0
    private var speedSum = 0.0
    private var speedSamples = 0
    private var lastMaf = 0.0

    fun start() {
        load()
        if (_running.value) return
        _running.value = true
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope?.launch {
            while (isActive && _running.value) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    fun stop() {
        _running.value = false
        scope?.cancel()
        scope = null
        save()
    }

    fun reset() {
        _distance.value = 0.0
        _avgSpeed.value = 0.0
        _fuelUsed.value = 0.0
        _instantMpg.value = 0.0
        _tripTime.value = 0
        totalSeconds = 0
        speedSum = 0.0
        speedSamples = 0
        lastSpeed = 0
        lastRpm = 0
        lastThrottle = 0
        lastMaf = 0.0
        save()
    }

    fun onSensorData(data: SensorData) {
        if (!_running.value) return
        lastSpeed = data.speed
        lastRpm = data.rpm
        lastThrottle = data.throttle
        updateInstantMpg()
    }

    private fun tick() {
        totalSeconds++
        _tripTime.value = totalSeconds

        // Distance: speed(mph) * (1/3600) hours = miles per second
        val speedMph = lastSpeed.toDouble()
        _distance.value = _distance.value + speedMph / 3600.0

        // Average speed
        speedSum += speedMph
        speedSamples++
        _avgSpeed.value = if (speedSamples > 0) speedSum / speedSamples else 0.0

        updateInstantMpg()

        // Fuel consumption: MAF(g/s) / AFR(14.7) / 3600 = gal/s (approx)
        _fuelUsed.value = _fuelUsed.value + (lastMaf / 14.7) / 3600.0
    }

    private fun updateInstantMpg() {
        if (lastSpeed > 0 && lastRpm > 0) {
            val maf = (lastRpm / 60.0) * (lastThrottle / 100.0) * 0.0012
            lastMaf = maf
            _instantMpg.value = if (maf > 0) lastSpeed / maf else 0.0
        } else {
            lastMaf = 0.0
            _instantMpg.value = 0.0
        }
    }

    private fun load() {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        _distance.value = prefs.getFloat(KEY_DISTANCE, 0f).toDouble()
        totalSeconds = prefs.getInt(KEY_TIME, 0)
        _tripTime.value = totalSeconds
        speedSum = prefs.getFloat(KEY_SPEED_SUM, 0f).toDouble()
        speedSamples = prefs.getInt(KEY_SPEED_SAMPLES, 0)
        if (speedSamples > 0) _avgSpeed.value = speedSum / speedSamples
        Log.d(TAG, "Trip loaded: ${_distance.value} mi, ${_tripTime.value}s")
    }

    private fun save() {
        val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        prefs.edit().apply {
            putFloat(KEY_DISTANCE, _distance.value.toFloat())
            putInt(KEY_TIME, totalSeconds)
            putFloat(KEY_SPEED_SUM, speedSum.toFloat())
            putInt(KEY_SPEED_SAMPLES, speedSamples)
            apply()
        }
        Log.d(TAG, "Trip saved: ${_distance.value} mi, ${_fuelUsed.value} gal")
    }
}
