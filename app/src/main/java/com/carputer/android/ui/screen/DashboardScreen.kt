package com.carputer.android.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carputer.android.ui.PageNavController
import com.carputer.android.ui.components.AnalogGauge
import com.carputer.android.ui.components.SpectrumBar
import com.carputer.android.ui.components.WarningItem
import com.carputer.android.ui.components.WarningPopup
import com.carputer.android.ui.theme.*
import com.carputer.android.viewmodel.DashboardState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    state: DashboardState,
    colors: CarputerColors,
    healthScore: Int,
    worstAlert: String,
    activeWarnings: List<WarningItem>,
    dismissedKey: String,
    onDismissWarnings: () -> Unit,
    centerView: Int,
    onCenterViewToggle: () -> Unit,
    onFanSpeedChange: (Int) -> Unit,
    onHvacToggle: () -> Unit,
    onAcToggle: () -> Unit,
    onAutoModeToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onWindowsUp: () -> Unit,
    onWindowsDown: () -> Unit,
    onRemoteStart: (Boolean) -> Unit,
    onTempChange: (Int) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaNext: () -> Unit,
    // Trip computer data
    tripDistance: Double = 0.0,
    tripAvgSpeed: Double = 0.0,
    tripFuelUsed: Double = 0.0,
    tripInstantMpg: Double = 0.0,
    tripTime: Int = 0,
    pageNavController: PageNavController? = null,
    modifier: Modifier = Modifier,
) {
    var currentTime by remember { mutableStateOf("") }
    // Focus index for joypad navigation (0-14, -1 = uninitialized)
    val focusIndex = remember { mutableStateOf(-1) }

    // Register navigation handlers with the page controller
    SideEffect {
        pageNavController?.let { ctrl ->
            ctrl.navigateLeft = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value > 0) focusIndex.value--
            }
            ctrl.navigateRight = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value < 14) focusIndex.value++
            }
            ctrl.navigateUp = {
                if (focusIndex.value > 0) {
                    focusIndex.value--
                }
                true
            }
            ctrl.navigateDown = {
                if (focusIndex.value < 0) {
                    focusIndex.value = 0
                    true
                } else if (focusIndex.value < 14) {
                    focusIndex.value++
                    true
                } else {
                    false
                }
            }
            ctrl.activateFocus = {
                if (focusIndex.value >= 0) {
                    when (focusIndex.value) {
                        0 -> if (state.configHvacEnabled && state.configTargetTemp > 60) onTempChange(-1)
                        1 -> if (state.configHvacEnabled && state.configTargetTemp < 85) onTempChange(1)
                        2 -> onFanSpeedChange((state.fanSpeed % 5) + 1)
                        3 -> onHvacToggle()
                        4 -> onAcToggle()
                        5 -> onAutoModeToggle()
                        6 -> onLockToggle()
                        7 -> onWindowsUp()
                        8 -> onWindowsDown()
                        9 -> onRemoteStart(!state.remoteStartActive)
                        10 -> onMediaPrev()
                        11 -> onMediaPlayPause()
                        12 -> onMediaNext()
                        13 -> { }  // Trip reset placeholder
                        14 -> onCenterViewToggle()
                    }
                }
            }
            ctrl.handleEscape = {
                focusIndex.value = -1
            }
            ctrl.focusIndex.intValue = focusIndex.value
            ctrl.focusActionName.value = when (focusIndex.value) {
                0 -> "TEMP-"
                1 -> "TEMP+"
                2 -> "FAN"
                3 -> "HVAC"
                4 -> "A/C"
                5 -> "AUTO"
                6 -> "LOCKS"
                7 -> "WIN UP"
                8 -> "WIN DN"
                9 -> "REMOTE"
                10 -> "PREV"
                11 -> "PLAY"
                12 -> "NEXT"
                13 -> "TRIP"
                14 -> "VIEW"
                else -> ""
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
            delay(1000)
        }
    }

    // Gear calculation
    val currentGear = if (state.rpm > 0 && state.speed > 0) {
        val ratio = state.rpm.toFloat() / state.speed.coerceAtLeast(1)
        when {
            ratio > 110 -> 1
            ratio > 65 -> 2
            ratio > 42 -> 3
            ratio > 30 -> 4
            else -> 5
        }
    } else 0

    val shiftAdvice = if (currentGear > 0) {
        when {
            state.rpm > 3500 && currentGear < 5 -> "\u2191 $currentGear\u2192${currentGear + 1}"
            state.rpm < 1300 && currentGear > 1 -> "\u2193 $currentGear\u2192${currentGear - 1}"
            else -> ""
        }
    } else ""

    val shouldShift = shiftAdvice.isNotEmpty()
    val healthColor = when {
        healthScore >= 80 -> StatusGreen
        healthScore >= 50 -> StatusYellow
        else -> StatusRed
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status bar
            StatusBar(
                state = state,
                currentTime = currentTime,
                colors = colors,
                healthScore = healthScore,
                healthColor = healthColor,
                worstAlert = worstAlert,
            )

            // Main gauge area
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Top row: speedo | center panel | rpm
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnalogGauge(
                            value = state.speed.toFloat(),
                            minValue = 0f, maxValue = 120f,
                            label = "SPEED", unitLabel = "mph",
                            fontSize = 18,
                            majorTicks = 6, minorTicks = 4,
                            warnValue = 0.75f, dangerValue = 0.92f, redlineStart = 0.92f,
                            thickness = 0.12f,
                            gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                            tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                            borderColor = colors.gaugeBorderColor,
                            warnColor = WarnColor, dangerColor = DangerColor,
                            modifier = Modifier.size(280.dp)
                        )

                        CenterInfoPanel(
                            state = state, colors = colors,
                            centerView = centerView,
                            healthScore = healthScore, healthColor = healthColor,
                            tripDistance = tripDistance,
                            tripAvgSpeed = tripAvgSpeed,
                            tripFuelUsed = tripFuelUsed,
                            tripInstantMpg = tripInstantMpg,
                            tripTime = tripTime,
                            onTap = onCenterViewToggle,
                            onMediaPlayPause = onMediaPlayPause,
                            onMediaPrev = onMediaPrev, onMediaNext = onMediaNext,
                            modifier = Modifier.fillMaxWidth(0.22f).height(280.dp)
                        )

                        AnalogGauge(
                            value = state.rpm.toFloat(),
                            minValue = 0f, maxValue = 8000f,
                            label = "ENGINE", unitLabel = "rpm",
                            fontSize = 22,
                            majorTicks = 8, minorTicks = 4,
                            warnValue = 0.75f, dangerValue = 0.875f, redlineStart = 0.85f,
                            thickness = 0.12f,
                            gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                            tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                            borderColor = colors.gaugeBorderColor,
                            warnColor = WarnColor, dangerColor = DangerColor,
                            modifier = Modifier.size(280.dp)
                        )
                    }

                    // Shift indicator overlaid
                    if (shouldShift && state.sensorConnected) {
                        val isUpShift = shiftAdvice.contains("\u2191")
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                                .width(80.dp).height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC000000))
                                .border(2.dp, if (isUpShift) StatusGreen else colors.carOrange, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "shift")
                            val opacity by infiniteTransition.animateFloat(
                                initialValue = 1f, targetValue = 0.3f,
                                animationSpec = infiniteRepeatable(animation = tween(400), repeatMode = RepeatMode.Reverse),
                                label = "shiftOpacity"
                            )
                            Text(text = shiftAdvice, color = if (isUpShift) StatusGreen else colors.carOrange,
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alpha(opacity))
                        }
                    }
                }

                // Bottom gauges row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalogGauge(value = state.fuelLevel.toFloat(), minValue = 0f, maxValue = 100f,
                        label = "FUEL", unitLabel = "%", fontSize = 11,
                        majorTicks = 4, minorTicks = 2, showValue = true, showNeedle = true,
                        startAngle = 180f, endAngle = 360f,
                        warnValue = 0.25f, dangerValue = 0.10f, redlineStart = 1f, thickness = 0.22f,
                        gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                        tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                        borderColor = colors.gaugeBorderColor,
                        modifier = Modifier.size(130.dp, 110.dp))

                    AnalogGauge(value = state.throttle.toFloat(), minValue = 0f, maxValue = 100f,
                        label = "TPS", unitLabel = "%", fontSize = 10,
                        majorTicks = 4, minorTicks = 2, showValue = true, showNeedle = true,
                        startAngle = 180f, endAngle = 360f,
                        warnValue = 0.75f, dangerValue = 0.90f, redlineStart = 1f, thickness = 0.22f,
                        gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                        tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                        borderColor = colors.gaugeBorderColor,
                        modifier = Modifier.size(120.dp, 100.dp))

                    AnalogGauge(value = state.map.toFloat(), minValue = 0f, maxValue = 100f,
                        label = "MAP", unitLabel = "%", fontSize = 10,
                        majorTicks = 4, minorTicks = 2, showValue = true, showNeedle = true,
                        startAngle = 180f, endAngle = 360f,
                        warnValue = 0.75f, dangerValue = 0.90f, redlineStart = 1f, thickness = 0.22f,
                        gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                        tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                        borderColor = colors.gaugeBorderColor,
                        modifier = Modifier.size(120.dp, 100.dp))

                    AnalogGauge(value = state.oilPressure.toFloat(), minValue = 0f, maxValue = 100f,
                        label = "OIL P", unitLabel = "%", fontSize = 10,
                        majorTicks = 4, minorTicks = 2, showValue = true, showNeedle = true,
                        startAngle = 180f, endAngle = 360f,
                        warnValue = 0.40f, dangerValue = 0.20f, redlineStart = 1f, thickness = 0.22f,
                        gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                        tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                        borderColor = colors.gaugeBorderColor,
                        modifier = Modifier.size(120.dp, 100.dp))

                    AnalogGauge(value = state.coolantTemp.toFloat(), minValue = 100f, maxValue = 280f,
                        label = "COOLANT", unitLabel = "\u00B0F", fontSize = 11,
                        majorTicks = 4, minorTicks = 2, showValue = true, showNeedle = true,
                        startAngle = 180f, endAngle = 360f,
                        warnValue = 0.67f, dangerValue = 0.78f, redlineStart = 0.67f, thickness = 0.22f,
                        gaugeColor = colors.gaugeColor, needleColor = colors.needleColor,
                        tickColor = colors.tickColor, textColor = colors.gaugeTextColor,
                        borderColor = colors.gaugeBorderColor,
                        modifier = Modifier.size(130.dp, 110.dp))

                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FanRelayDot(1, state.fanRelay >= 1, colors.carBlue)
                        FanRelayDot(2, state.fanRelay >= 2, colors.carBlue)
                    }
                }
            }

            // Quick controls bar
            QuickControlsBar(
                state = state,
                colors = colors,
                onFanSpeedChange = onFanSpeedChange,
                onHvacToggle = onHvacToggle,
                onAcToggle = onAcToggle,
                onAutoModeToggle = onAutoModeToggle,
                onLockToggle = onLockToggle,
                onWindowsUp = onWindowsUp,
                onWindowsDown = onWindowsDown,
                onRemoteStart = onRemoteStart,
                onTempChange = onTempChange,
                modifier = Modifier.fillMaxWidth().height(90.dp)
            )
        }

        // Warning overlay
        WarningPopup(
            worstAlert = worstAlert,
            activeWarnings = activeWarnings,
            dismissedKey = dismissedKey,
            onDismiss = onDismissWarnings,
        )
    }
}

@Composable
private fun StatusBar(
    state: DashboardState,
    currentTime: String,
    colors: CarputerColors,
    healthScore: Int,
    healthColor: Color,
    worstAlert: String,
) {
    val lockColor = if (state.doorsLocked) StatusGreen else StatusRed
    val lockText = if (state.doorsLocked) "LOCKED" else "UNLOCKED"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.bgCard)
            .border(
                1.dp, colors.carBlue.copy(alpha = 0.25f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(lockColor))
        Text(lockText, color = lockColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.width(1.dp).height(16.dp).background(colors.bgPanel))
        Text(
            if (state.remoteStartActive) "ENGINE ON" else "ENGINE OFF",
            color = if (state.remoteStartActive) colors.carOrange else colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (state.remoteStartActive) FontWeight.Bold else FontWeight.Normal
        )
        Box(modifier = Modifier.width(1.dp).height(16.dp).background(colors.bgPanel))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(
                "D" to state.driverDoor,
                "P" to state.passengerDoor,
                "T" to state.trunk,
                "H" to state.hood
            ).forEach { (label, open) ->
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (open) colors.carOrange else colors.bgPanel)
                        .border(
                            1.dp,
                            if (open) colors.carOrange else colors.textSecondary,
                            RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, fontSize = 9.sp, fontWeight = if (open) FontWeight.Bold else FontWeight.Normal,
                        color = if (open) Color.Black else colors.textSecondary
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(currentTime, color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        // Warning indicators
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(
                "COOL" to computeAlertLevel(state.coolantTemp, 220, 230, 245),
                "OIL" to computeAlertLevel(state.oilTemp, 220, 240, 260),
                "PRESS" to (if (state.engineRunning) computeAlertLevelInv(state.oilPressure, 40, 20, 10) else "ok"),
                "BAT" to computeAlertLevelInv(state.battery, 50, 25, 10),
            ).forEach { (label, alert) ->
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (alert) {
                                "critical" -> StatusRed
                                "danger" -> StatusOrange
                                "caution" -> StatusYellow
                                else -> colors.bgPanel
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label, fontSize = 8.sp,
                        fontWeight = if (alert != "ok") FontWeight.Bold else FontWeight.Normal,
                        color = if (alert != "ok") Color.Black else colors.textSecondary
                    )
                }
            }
        }
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(healthColor))
        Text(
            if (state.sensorConnected) "HEALTH $healthScore%" else "OFFLINE",
            color = healthColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

private fun computeAlertLevel(value: Int, caution: Int, danger: Int, critical: Int): String {
    if (value <= 0) return "ok"
    return when {
        value > critical -> "critical"
        value > danger -> "danger"
        value > caution -> "caution"
        else -> "ok"
    }
}

private fun computeAlertLevelInv(value: Int, caution: Int, danger: Int, critical: Int): String {
    if (value <= 0) return "ok"
    return when {
        value < critical -> "critical"
        value < danger -> "danger"
        value < caution -> "caution"
        else -> "ok"
    }
}

@Composable
private fun CenterInfoPanel(
    state: DashboardState,
    colors: CarputerColors,
    centerView: Int,
    healthScore: Int,
    healthColor: Color,
    tripDistance: Double = 0.0,
    tripAvgSpeed: Double = 0.0,
    tripFuelUsed: Double = 0.0,
    tripInstantMpg: Double = 0.0,
    tripTime: Int = 0,
    onTap: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrev: () -> Unit,
    onMediaNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bgCard)
            .border(1.dp, colors.carBlue.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(16.dp)
    ) {
        when (centerView) {
            0 -> NowPlayingView(state, colors, onMediaPlayPause, onMediaPrev, onMediaNext)
            1 -> TripComputerView(state, colors, healthScore, healthColor,
                    tripDistance, tripAvgSpeed, tripFuelUsed, tripInstantMpg, tripTime)
            2 -> PerformanceView(state, colors)
        }
    }
}

@Composable
private fun NowPlayingView(
    state: DashboardState,
    colors: CarputerColors,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("NOW PLAYING", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth().height(80.dp)
                .clip(RoundedCornerShape(8.dp)).background(colors.bgDark).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgPanel),
                contentAlignment = Alignment.Center
            ) {
                Text("\u266B", fontSize = 28.sp, color = colors.textSecondary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    state.mediaTitle.ifEmpty { "No track loaded" },
                    color = colors.textPrimary, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(max = 180.dp)
                )
                Text(state.mediaArtist, color = colors.carBlue, fontSize = 11.sp)
                Text(
                    formatTime(state.mediaPosition) + " / " + formatTime(state.mediaDuration),
                    color = colors.textSecondary, fontSize = 10.sp
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("\u25C0\u25C0", fontSize = 14.sp, modifier = Modifier.clickable { onPrev() })
            Text(
                if (state.mediaPlaying) "\u23F8" else "\u25B6",
                fontSize = 20.sp, modifier = Modifier.clickable { onPlayPause() }
            )
            Text("\u25B6\u25B6", fontSize = 14.sp, modifier = Modifier.clickable { onNext() })
        }
        SpectrumBar(
            spectrumData = state.spectrumData,
            barCount = 16,
            height = 40.dp,
            carBlue = colors.carBlue,
            bgDark = colors.bgDark,
        )
        Text("tap for trip \u2192", color = colors.textSecondary, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun TripComputerView(
    state: DashboardState,
    colors: CarputerColors,
    healthScore: Int,
    healthColor: Color,
    tripDistance: Double = 0.0,
    tripAvgSpeed: Double = 0.0,
    tripFuelUsed: Double = 0.0,
    tripInstantMpg: Double = 0.0,
    tripTime: Int = 0,
) {
    val distanceStr = if (tripDistance > 0) "%.1f mi".format(tripDistance) else "-- mi"
    val avgSpeedStr = if (tripAvgSpeed > 0) "%.0f mph".format(tripAvgSpeed) else "-- mph"
    val fuelStr = if (tripFuelUsed > 0) "%.2f gal".format(tripFuelUsed) else "-- gal"
    val mpgStr = if (tripInstantMpg > 0) "%.1f".format(tripInstantMpg) else "--"
    val timeSec = tripTime % 60
    val timeMin = (tripTime / 60) % 60
    val timeHr = tripTime / 3600
    val timeStr = if (timeHr > 0) "%d:%02d:%02d".format(timeHr, timeMin, timeSec) else "%d:%02d".format(timeMin, timeSec)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("TRIP COMPUTER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        // Distance + time
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(50.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE", color = colors.textSecondary, fontSize = 9.sp)
                    Text(distanceStr, color = colors.carBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(50.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TIME", color = colors.textSecondary, fontSize = 9.sp)
                    Text(timeStr, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AVG SPEED", color = colors.textSecondary, fontSize = 9.sp)
                    Text(avgSpeedStr, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FUEL USED", color = colors.textSecondary, fontSize = 9.sp)
                    Text(fuelStr, color = colors.carOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier.weight(1f).height(40.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgDark),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("INST MPG", color = colors.textSecondary, fontSize = 9.sp)
                    Text(mpgStr, color = colors.carBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(28.dp)
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, healthColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(healthColor))
                Text("ENGINE HEALTH: $healthScore%", color = healthColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("\u2190 tap for performance \u2192", color = colors.textSecondary, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun PerformanceView(
    state: DashboardState,
    colors: CarputerColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PERFORMANCE", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("VEHICLE STATUS", color = colors.textSecondary, fontSize = 10.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape)
                    .background(if (state.sensorConnected) StatusGreen else StatusRed)
            )
            Text(
                if (state.sensorConnected) "ALL SYSTEMS NOMINAL" else "SENSOR OFFLINE",
                color = if (state.sensorConnected) StatusGreen else StatusRed,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SensorRow("Ambient", "${state.ambientTemp}\u00B0F", colors)
            SensorRow("Intake", "${state.intakeTemp}\u00B0F", colors)
            SensorRow("Oil Temp", "${state.oilTemp}\u00B0F", colors)
            SensorRow("Brake Fluid", "${state.brakeFluid}%", colors)
        }
        Text("\u2190 tap for now playing", color = colors.textSecondary, fontSize = 9.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun SensorRow(label: String, value: String, colors: CarputerColors) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FanRelayDot(number: Int, active: Boolean, carBlue: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, if (active) carBlue else Color(0xFF8888AA), CircleShape)
            .background(if (active) carBlue else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$number", fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (active) Color.White else Color(0xFF8888AA)
        )
    }
}

@Composable
private fun QuickControlsBar(
    state: DashboardState,
    colors: CarputerColors,
    onFanSpeedChange: (Int) -> Unit,
    onHvacToggle: () -> Unit,
    onAcToggle: () -> Unit,
    onAutoModeToggle: () -> Unit,
    onLockToggle: () -> Unit,
    onWindowsUp: () -> Unit,
    onWindowsDown: () -> Unit,
    onRemoteStart: (Boolean) -> Unit,
    onTempChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.bgCard)
            .border(1.dp, colors.carBlue.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Climate section
        Box(
            modifier = Modifier.width(220.dp).fillMaxHeight()
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, colors.carBlue.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("CLIMATE", color = colors.textSecondary, fontSize = 9.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { if (state.configHvacEnabled && state.configTargetTemp > 60) onTempChange(-1) },
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("-", fontSize = 16.sp) }
                    Text(
                        "${state.configTargetTemp}\u00B0",
                        color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { if (state.configHvacEnabled && state.configTargetTemp < 85) onTempChange(1) },
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("+", fontSize = 16.sp) }
                }
            }
        }

        // Fan speed
        Box(
            modifier = Modifier.width(180.dp).fillMaxHeight()
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, colors.carBlue.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("FAN", color = colors.textSecondary, fontSize = 9.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0 until 5).forEach { idx ->
                        val active = state.fanSpeed > idx
                        Box(
                            modifier = Modifier.size(22.dp).clip(CircleShape)
                                .background(if (active) colors.carBlue else colors.bgPanel)
                                .clickable { onFanSpeedChange(idx + 1) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${idx + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                color = if (active) Color.Black else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // HVAC toggles
        Box(
            modifier = Modifier.width(180.dp).fillMaxHeight()
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, colors.carBlue.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.hvacEnabled) colors.carBlue else colors.bgPanel)
                        .clickable { onHvacToggle() },
                    contentAlignment = Alignment.Center
                ) { Text("HVAC", fontSize = 10.sp) }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.acEnabled) colors.carBlue else colors.bgPanel)
                        .clickable { onAcToggle() },
                    contentAlignment = Alignment.Center
                ) { Text("A/C", fontSize = 10.sp) }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.configAutoMode) colors.carBlue else colors.bgPanel)
                        .clickable { onAutoModeToggle() },
                    contentAlignment = Alignment.Center
                ) { Text(if (state.configAutoMode) "AUTO" else "MAN", fontSize = 10.sp) }
            }
        }

        // Vehicle controls
        Box(
            modifier = Modifier.width(220.dp).fillMaxHeight()
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, colors.carBlue.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.doorsLocked) colors.carOrange else colors.bgPanel)
                        .clickable { onLockToggle() },
                    contentAlignment = Alignment.Center
                ) { Text(if (state.doorsLocked) "UNLOCK" else "LOCK", fontSize = 10.sp) }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp)).background(colors.bgPanel)
                        .clickable { onWindowsUp() },
                    contentAlignment = Alignment.Center
                ) { Text("\u2191WIN", fontSize = 10.sp) }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp)).background(colors.bgPanel)
                        .clickable { onWindowsDown() },
                    contentAlignment = Alignment.Center
                ) { Text("\u2193WIN", fontSize = 10.sp) }
            }
        }

            // Remote start (momentary push button)
        Box(
            modifier = Modifier.width(110.dp).fillMaxHeight()
                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                .border(1.dp, colors.carBlue.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REMOTE\nSTART", color = colors.textSecondary, fontSize = 9.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.size(width = 70.dp, height = 32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (state.remoteStartActive) colors.carOrange else colors.carBlue)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onRemoteStart(true)
                                    try { awaitRelease() } finally { onRemoteStart(false) }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.remoteStartActive) "ON" else "START",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000)
    return "%d:%02d".format(minutes, seconds)
}
