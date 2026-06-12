package com.kingfrezz.carputerandroid

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kingfrezz.carputerandroid.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            clockHandler.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val MS_TO_KMH = 3.6f
        private const val MS_TO_MPH = 2.23694f
    }

    private var useKmh = true
    private var maxSpeed = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Full screen immersive mode
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        setupLocationListener()
        setupClickListeners()
        requestLocationPermissions()

        binding.tvSpeed.text = "0"
        binding.tvUnit.text = if (useKmh) "km/h" else "mph"
        binding.tvMaxSpeed.text = getString(R.string.max_speed_format, 0, if (useKmh) "km/h" else "mph")
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
        stopLocationUpdates()
    }

    private fun setupClickListeners() {
        // Toggle speed unit on tap; preserve max speed by converting to new unit
        binding.tvUnit.setOnClickListener {
            if (useKmh) {
                // km/h → mph
                maxSpeed = maxSpeed / MS_TO_KMH * MS_TO_MPH
                useKmh = false
            } else {
                // mph → km/h
                maxSpeed = maxSpeed / MS_TO_MPH * MS_TO_KMH
                useKmh = true
            }
            val unitLabel = if (useKmh) "km/h" else "mph"
            binding.tvUnit.text = unitLabel
            binding.tvMaxSpeed.text = getString(R.string.max_speed_format, maxSpeed.toInt(), unitLabel)
        }

        // Reset max speed on long press
        binding.tvMaxSpeed.setOnLongClickListener {
            maxSpeed = 0f
            binding.tvMaxSpeed.text = getString(R.string.max_speed_format, 0, if (useKmh) "km/h" else "mph")
            true
        }
    }

    private fun setupLocationListener() {
        locationListener = LocationListener { location ->
            updateSpeed(location)
            updateGpsAccuracy(location)
        }
    }

    private fun updateSpeed(location: Location) {
        val speedMs = if (location.hasSpeed()) location.speed else 0f
        val speed = if (useKmh) speedMs * MS_TO_KMH else speedMs * MS_TO_MPH
        val speedInt = speed.toInt()

        if (speed > maxSpeed) {
            maxSpeed = speed
            binding.tvMaxSpeed.text = getString(
                R.string.max_speed_format,
                maxSpeed.toInt(),
                if (useKmh) "km/h" else "mph"
            )
        }

        binding.tvSpeed.text = speedInt.toString()

        // Color code speed using unit-appropriate thresholds:
        //   green  < 60 km/h (37 mph)
        //   yellow < 100 km/h (62 mph)
        //   red   >= 100 km/h (62 mph)
        val lowThreshold  = if (useKmh) 60  else 37
        val highThreshold = if (useKmh) 100 else 62
        val color = when {
            speedInt < lowThreshold  -> ContextCompat.getColor(this, R.color.speed_low)
            speedInt < highThreshold -> ContextCompat.getColor(this, R.color.speed_mid)
            else                     -> ContextCompat.getColor(this, R.color.speed_high)
        }
        binding.tvSpeed.setTextColor(color)
    }

    private fun updateGpsAccuracy(location: Location) {
        val accuracyText = if (location.hasAccuracy()) {
            getString(R.string.gps_accuracy_format, location.accuracy.toInt())
        } else {
            getString(R.string.gps_active)
        }
        binding.tvGpsStatus.text = accuracyText
    }

    private fun updateClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val now = Date()
        binding.tvTime.text = timeFormat.format(now)
        binding.tvDate.text = dateFormat.format(now)
    }

    private fun requestLocationPermissions() {
        val fineLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION

        if (ContextCompat.checkSelfPermission(this, fineLocation) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fineLocation, coarseLocation),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                binding.tvGpsStatus.text = getString(R.string.gps_denied)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,
                0f,
                locationListener
            )
            binding.tvGpsStatus.text = getString(R.string.gps_active)
        } else {
            binding.tvGpsStatus.text = getString(R.string.gps_unavailable)
        }
    }

    private fun stopLocationUpdates() {
        locationManager.removeUpdates(locationListener)
    }
}
