package com.carputer.android.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.carputer.android.CarputerApplication
import com.carputer.android.service.CarControlClient
import com.carputer.android.service.DtcService
import com.carputer.android.service.DvrService
import com.carputer.android.service.JoypadEvent
import com.carputer.android.service.MediaPlayerService
import com.carputer.android.service.SensorClient
import com.carputer.android.service.WifiService
import com.carputer.android.service.SystemService
import com.carputer.android.service.DiagnosticsService
import com.carputer.android.service.DataLoggerService
import com.carputer.android.service.TripComputerService
import com.carputer.android.service.CameraService
import com.carputer.android.service.Esp32FlasherService
import com.carputer.android.ui.screen.*
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.ThemeState
import com.carputer.android.viewmodel.*
import kotlinx.coroutines.delay

private fun getRequiredPermissions(): Array<String> {
    val perms = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    return perms.toTypedArray()
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    themeState: ThemeState,
    colors: CarputerColors,
) {
    val context = LocalContext.current
    val app = context.applicationContext as CarputerApplication
    val configRepo = app.configRepository

    // Permission state
    val allGranted = remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        allGranted.value = granted.values.all { it }
    }

    // Request permissions on first composition
    LaunchedEffect(Unit) {
        val perms = getRequiredPermissions()
        val missing = perms.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            permissionLauncher.launch(perms)
        } else {
            allGranted.value = true
        }
    }

    // Services
    val sensorClient = remember { SensorClient(context) }
    val carControlClient = remember { CarControlClient() }
    val mediaPlayerService = remember { MediaPlayerService(context) }
    val wifiService = remember { WifiService(context) }
    val dtcService = remember { DtcService() }
    val systemService = remember { SystemService(context.applicationContext as Application) }
    val diagnosticsService = remember { DiagnosticsService() }.also {
        it.sensorClient = sensorClient
        it.carControlClient = carControlClient
        it.wifiService = wifiService
        it.mediaPlayerService = mediaPlayerService
    }
    val dataLoggerService = remember { DataLoggerService() }
    val tripComputerService = remember { TripComputerService(context) }

    // ESP32 flasher service
    val esp32FlasherService = remember { Esp32FlasherService(context) }

    // Camera & DVR services
    val cameraManager = remember {
        context.getSystemService("camera") as CameraManager
    }
    val cameraService = remember { CameraService() }.also {
        it.initialize(cameraManager)
    }
    val dvrService = remember { DvrService() }

    // ViewModels
    val dashboardViewModel = remember { DashboardViewModel(sensorClient, carControlClient, configRepo) }
    val mediaViewModel = remember { MediaViewModel(mediaPlayerService, configRepo) }
    val settingsViewModel = remember {
        SettingsViewModel(
            configRepo, themeState, wifiService, dtcService,
            diagnosticsService, dataLoggerService, systemService,
            esp32FlasherService, context
        )
    }

    // Start services on composition
    DisposableEffect(Unit) {
        sensorClient.start()
        carControlClient.connect()
        mediaPlayerService.initPlayer()
        dvrService.ensureDvrDir()
        dvrService.scanRecordings()

        onDispose {
            sensorClient.stop()
            carControlClient.disconnect()
            mediaPlayerService.release()
            wifiService.release()
            cameraService.release()
            dvrService.release()
        }
    }

    // Trip computer sync
    LaunchedEffect(Unit) {
        tripComputerService.start()
        while (true) {
            sensorClient.sensorData.value.let { data ->
                tripComputerService.onSensorData(data)
                dataLoggerService.onSensorData(data)
            }
            delay(1000)
        }
    }

    // Page routing
    var currentPage by remember { mutableStateOf(0) }
    var dismissedKey by remember { mutableStateOf("") }
    var centerView by remember { mutableStateOf(0) }

    // Nav bar focus (like desktop app's navBarFocused + focusedNavIndex)
    var navBarFocused by remember { mutableStateOf(false) }
    var focusedNavIndex by remember { mutableStateOf(0) }

    // Page navigation controller (each page registers its handlers here)
    val pageNavController = remember { PageNavController() }

    // Derived focus state for display
    val focusLabel = remember {
        derivedStateOf {
            if (navBarFocused) {
                val names = listOf("DASH", "MEDIA", "REAR", "DVR", "SETUP")
                "NAV: ${names.getOrElse(focusedNavIndex) { "?" }}"
            } else if (pageNavController.focusIndex.intValue >= 0) {
                "${pageNavController.focusActionName.value} [${pageNavController.focusIndex.intValue}]"
            } else {
                ""
            }
        }
    }

    // Joypad navigation (matches desktop app model exactly)
    LaunchedEffect(Unit) {
        carControlClient.joypadEvents.collect { event ->
            when (event) {
                JoypadEvent.LEFT -> {
                    if (navBarFocused) {
                        focusedNavIndex = if (focusedNavIndex > 0) focusedNavIndex - 1 else 4
                    } else {
                        pageNavController.navigateLeft()
                    }
                }
                JoypadEvent.RIGHT -> {
                    if (navBarFocused) {
                        focusedNavIndex = if (focusedNavIndex < 4) focusedNavIndex + 1 else 0
                    } else {
                        pageNavController.navigateRight()
                    }
                }
                JoypadEvent.UP -> {
                    if (navBarFocused) {
                        navBarFocused = false
                    } else {
                        pageNavController.navigateUp()
                    }
                }
                JoypadEvent.DOWN -> {
                    if (navBarFocused) {
                        // stay in nav bar
                    } else {
                        val consumed = pageNavController.navigateDown()
                        if (!consumed) {
                            navBarFocused = true
                            focusedNavIndex = currentPage
                        }
                    }
                }
                JoypadEvent.SELECT -> {
                    if (navBarFocused) {
                        currentPage = focusedNavIndex
                        navBarFocused = false
                    } else {
                        pageNavController.activateFocus()
                    }
                }
                JoypadEvent.EXIT -> {
                    if (navBarFocused) {
                        navBarFocused = false
                    } else {
                        pageNavController.handleEscape()
                    }
                }
            }
        }
    }

    // Collect dashboard state
    val dashState by dashboardViewModel.state.collectAsState()
    val healthScore = remember(dashState) { dashboardViewModel.computeHealthScore(dashState) }
    val worstAlert = remember(dashState) { dashboardViewModel.computeWorstAlert(dashState) }
    val activeWarnings = remember(dashState) { dashboardViewModel.getActiveWarnings(dashState) }

    // Collect media state
    val playlist by mediaViewModel.playlist.collectAsState()
    val currentIndex by mediaViewModel.currentIndex.collectAsState()
    val playing by mediaViewModel.playing.collectAsState()
    val position by mediaViewModel.position.collectAsState()
    val duration by mediaViewModel.duration.collectAsState()
    val currentTrack by mediaViewModel.currentTrack.collectAsState()
    val volume by mediaViewModel.volume.collectAsState()
    val repeatMode by mediaViewModel.repeatMode.collectAsState()
    val shuffleOn by mediaViewModel.shuffleOn.collectAsState()
    val spectrumData by mediaViewModel.spectrumData.collectAsState()

    // Collect settings state
    val settingsState by settingsViewModel.state.collectAsState()

    // Collect trip computer state
    val tripDistance by tripComputerService.distance.collectAsState()
    val tripAvgSpeed by tripComputerService.avgSpeed.collectAsState()
    val tripFuelUsed by tripComputerService.fuelUsed.collectAsState()
    val tripInstantMpg by tripComputerService.instantMpg.collectAsState()
    val tripTime by tripComputerService.tripTime.collectAsState()

    // Collect camera state
    val cameraStreaming by cameraService.streaming.collectAsState()
    val cameraDevice by cameraService.device.collectAsState()
    val availableCameras by cameraService.availableDevices.collectAsState()

    // Collect DVR state
    val dvrRecording by dvrService.recording.collectAsState()
    val dvrRecordingSeconds by dvrService.recordingSeconds.collectAsState()
    val dvrRecordings by dvrService.recordings.collectAsState()
    val dvrPlaying by dvrService.playing.collectAsState()
    val dvrPlayingFile by dvrService.playingFile.collectAsState()
    val dvrPlayPosition by dvrService.playPosition.collectAsState()
    val dvrPlayDuration by dvrService.playDuration.collectAsState()
    val dvrCameraSource by dvrService.cameraSource.collectAsState()

    // Sync media state to dashboard viewmodel
    LaunchedEffect(currentTrack, playing, position, duration, spectrumData) {
        dashboardViewModel.updateMediaState(
            title = currentTrack?.title ?: "",
            artist = currentTrack?.artist ?: "",
            position = position,
            duration = duration,
            playing = playing,
            spectrum = spectrumData,
            artwork = "",
        )
    }

    if (!allGranted.value) {
        // Show permission request overlay
        Box(
            modifier = modifier.fillMaxSize().background(colors.bgDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Carputer", color = colors.carBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Grant permissions to continue", color = colors.textSecondary, fontSize = 16.sp)
                Box(
                    modifier = Modifier.size(width = 200.dp, height = 44.dp)
                        .clip(RoundedCornerShape(8.dp)).background(colors.carBlue)
                        .clickable { permissionLauncher.launch(getRequiredPermissions()) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("GRANT PERMISSIONS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Page content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (currentPage) {
                    0 -> DashboardScreen(
                        state = dashState,
                        colors = colors,
                        pageNavController = pageNavController,
                        healthScore = healthScore,
                        worstAlert = worstAlert,
                        activeWarnings = activeWarnings,
                        dismissedKey = dismissedKey,
                        onDismissWarnings = { dismissedKey = "${worstAlert}_${activeWarnings.size}" },
                        centerView = centerView,
                        onCenterViewToggle = { centerView = (centerView + 1) % 3 },
                        tripDistance = tripDistance,
                        tripAvgSpeed = tripAvgSpeed,
                        tripFuelUsed = tripFuelUsed,
                        tripInstantMpg = tripInstantMpg,
                        tripTime = tripTime,
                        onFanSpeedChange = { carControlClient.setFanSpeed(it) },
                        onHvacToggle = { dashboardViewModel.setConfigHvac(!dashState.configHvacEnabled) },
                        onAcToggle = { dashState.configAcEnabled.let { dashboardViewModel.setConfigAc(!it) } },
                        onAutoModeToggle = { dashboardViewModel.setConfigAutoMode(!dashState.configAutoMode) },
                        onLockToggle = { if (dashState.doorsLocked) carControlClient.unlockDoors() else carControlClient.lockDoors() },
                        onWindowsUp = { carControlClient.windowsUp() },
                        onWindowsDown = { carControlClient.windowsDown() },
                        onRemoteStart = { if (it) carControlClient.startRemote() else carControlClient.stopRemote() },
                        onTempChange = { dashboardViewModel.setConfigTargetTemp(dashState.configTargetTemp + it) },
                        onMediaPlayPause = { if (playing) mediaViewModel.pause() else mediaViewModel.play() },
                        onMediaPrev = { mediaViewModel.previous() },
                        onMediaNext = { mediaViewModel.next() },
                    )

                    1 -> MediaScreen(
                        playlist = playlist,
                        pageNavController = pageNavController,
                        currentIndex = currentIndex,
                        playing = playing,
                        position = position,
                        duration = duration,
                        currentTrack = currentTrack,
                        volume = volume,
                        repeatMode = repeatMode,
                        shuffleOn = shuffleOn,
                        spectrumData = spectrumData,
                        onScanMedia = { mediaViewModel.scanMedia(it) },
                        onPlay = { mediaViewModel.play() },
                        onPause = { mediaViewModel.pause() },
                        onNext = { mediaViewModel.next() },
                        onPrevious = { mediaViewModel.previous() },
                        onSeek = { mediaViewModel.seek(it) },
                        onPlayTrack = { mediaViewModel.playTrack(it) },
                        onSetVolume = { mediaViewModel.setVolume(it) },
                        onSetRepeatMode = { mediaViewModel.setRepeatMode(it) },
                        onSetShuffle = { mediaViewModel.setShuffleOn(it) },
                        colors = colors,
                    )

                    2 -> CarPlayScreen(
                        connected = cameraStreaming,
                        deviceName = cameraDevice.ifEmpty { "No camera" },
                        audioSource = "N/A",
                        pageNavController = pageNavController,
                        onConnect = {
                            val camId = availableCameras.firstOrNull()
                            if (camId != null) cameraService.startStream(camId)
                        },
                        onDisconnect = { cameraService.stopStream() },
                        onTap = {
                            if (cameraDevice.isNotEmpty()) cameraService.startStream()
                        },
                        colors = colors,
                        cameraService = cameraService,
                    )

                    3 -> DashcamScreen(
                        recording = dvrRecording,
                        pageNavController = pageNavController,
                        recordingSeconds = dvrRecordingSeconds,
                        recordings = dvrRecordings,
                        playingFile = dvrPlayingFile,
                        playing = dvrPlaying,
                        playPosition = dvrPlayPosition,
                        playDuration = dvrPlayDuration,
                        cameraSource = dvrCameraSource,
                        onStartRecording = { dvrService.startRecording() },
                        onStopRecording = { dvrService.stopRecording() },
                        onPlayFile = { dvrService.playFile(it) },
                        onStopPlayback = { dvrService.stopPlayback() },
                        onTogglePause = { dvrService.togglePause() },
                        onSeekTo = { dvrService.seekTo(it) },
                        onDeleteFile = { dvrService.deleteFile(it) },
                        onScanRecordings = { dvrService.scanRecordings() },
                        onSetCameraSource = { dvrService.setCameraSource(it) },
                        formatDuration = { dvrService.formatDuration(it) },
                        fileLabel = { dvrService.fileLabel(it) },
                        colors = colors,
                    )

                    4 -> SettingsScreen(
                        state = settingsState,
                        colors = colors,
                        pageNavController = pageNavController,
                        onSetTheme = { settingsViewModel.setTheme(it) },
                        onSetAccentColor = { settingsViewModel.setAccentColor(it) },
                        onSetGaugeBorderMode = { settingsViewModel.setGaugeBorderMode(it) },
                        onScanWifi = { settingsViewModel.scanWifi() },
                        onConnectToWifi = { settingsViewModel.connectToWifi(it) },
                        onDisconnectWifi = { settingsViewModel.disconnectWifi() },
                        onScanDtc = { settingsViewModel.scanDtc() },
                        onScanDtcTestMode = { settingsViewModel.scanDtcTestMode() },
                        onClearDtcHistory = { settingsViewModel.clearDtcHistory() },
                        onRunDiagnostics = { settingsViewModel.runDiagnostics() },
                        onSaveDiagnosticsReport = { settingsViewModel.saveDiagnosticsReport() },
                        onRefreshSystemInfo = { settingsViewModel.refreshSystemInfo() },
                        onToggleLogging = { settingsViewModel.toggleLogging() },
                    onScanFlasherDevices = { settingsViewModel.scanFlasherDevices() },
                    onFlashFirmware = { settingsViewModel.flashFirmware(it) },
                    onCancelFlash = { settingsViewModel.cancelFlash() },
                    )
                }
            }

            // Focus indicator bar
            if (focusLabel.value.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(20.dp).background(colors.bgDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        focusLabel.value,
                        color = colors.carBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom navigation bar with focus indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.bgPanel)
                    .border(1.dp, colors.bgDark, RoundedCornerShape(0.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    "DASH" to 0,
                    "MEDIA" to 1,
                    "REAR" to 2,
                    "DVR" to 3,
                    "SETUP" to 4,
                ).forEach { (label, page) ->
                    val isActive = currentPage == page
                    val isFocused = navBarFocused && focusedNavIndex == page
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isActive) colors.carBlue.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .border(
                                if (isFocused) 2.dp else 0.dp,
                                colors.carBlue.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { currentPage = page },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                label,
                                color = if (isActive || isFocused) colors.carBlue else colors.textSecondary,
                                fontSize = if (isActive || isFocused) 13.sp else 11.sp,
                                fontWeight = if (isActive || isFocused) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (isActive || isFocused) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(3.dp)
                                        .background(colors.carBlue, RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
