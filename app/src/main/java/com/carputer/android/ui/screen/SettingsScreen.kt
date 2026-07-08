package com.carputer.android.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carputer.android.service.FlasherState
import com.carputer.android.ui.PageNavController
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.StatusGreen
import com.carputer.android.ui.theme.StatusOrange
import com.carputer.android.ui.theme.StatusRed
import com.carputer.android.ui.theme.StatusYellow
import com.carputer.android.viewmodel.SettingsState
import com.carputer.android.viewmodel.ThemeOption
import com.carputer.android.viewmodel.themeOptions

@Composable
fun SettingsScreen(
    state: SettingsState,
    colors: CarputerColors,
    onSetTheme: (String) -> Unit,
    onSetAccentColor: (String) -> Unit,
    onSetGaugeBorderMode: (String) -> Unit,
    onScanWifi: () -> Unit,
    onConnectToWifi: (String) -> Unit,
    onDisconnectWifi: () -> Unit,
    onScanDtc: () -> Unit,
    onScanDtcTestMode: () -> Unit,
    onClearDtcHistory: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onSaveDiagnosticsReport: () -> Unit,
    onRefreshSystemInfo: () -> Unit,
    onToggleLogging: () -> Unit,
    onScanFlasherDevices: () -> Unit = {},
    onFlashFirmware: (Int) -> Unit = {},
    onCancelFlash: () -> Unit = {},
    pageNavController: PageNavController? = null,
    modifier: Modifier = Modifier,
) {
    var activeSection by remember { mutableStateOf(0) }
    val focusIndex = remember { mutableStateOf(-1) }

    SideEffect {
        pageNavController?.let { ctrl ->
            ctrl.navigateLeft = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value > 0) focusIndex.value--
            }
            ctrl.navigateRight = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value < 5) focusIndex.value++
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
                } else if (focusIndex.value < 5) {
                    focusIndex.value++
                    true
                } else {
                    false
                }
            }
            ctrl.activateFocus = {
                if (focusIndex.value >= 0) {
                    activeSection = focusIndex.value
                }
            }
            ctrl.handleEscape = {
                focusIndex.value = -1
            }
            ctrl.focusIndex.intValue = focusIndex.value
            ctrl.focusActionName.value = when (focusIndex.value) {
                0 -> "THEME"
                1 -> "WIFI"
                2 -> "DTC"
                3 -> "SYSTEM"
                4 -> "LOGGING"
                5 -> "FLASH"
                else -> ""
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Section tabs
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).background(colors.bgPanel)
            ) {
                val sections = listOf("THEME", "WIFI", "DTC", "SYSTEM", "LOGGING", "FLASH")
                sections.forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (activeSection == idx) colors.bgCard else Color.Transparent)
                            .clickable { activeSection = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = if (activeSection == idx) colors.carBlue else colors.textSecondary,
                                fontSize = 11.sp, fontWeight = if (activeSection == idx) FontWeight.Bold else FontWeight.Normal)
                            if (activeSection == idx) {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(colors.carBlue))
                            }
                        }
                    }
                }
            }

            when (activeSection) {
                0 -> ThemeSection(state, colors, onSetTheme, onSetAccentColor, onSetGaugeBorderMode)
                1 -> WifiSection(state, colors, onScanWifi, onConnectToWifi, onDisconnectWifi)
                2 -> DtcSection(state, colors, onScanDtc, onScanDtcTestMode, onClearDtcHistory)
                3 -> SystemSection(state, colors, onRunDiagnostics, onSaveDiagnosticsReport, onRefreshSystemInfo)
                4 -> LoggingSection(state, colors, onToggleLogging)
                5 -> FlasherSection(state, colors, onScanFlasherDevices, onFlashFirmware, onCancelFlash)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, colors: CarputerColors) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = colors.carBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemeSection(
    state: SettingsState,
    colors: CarputerColors,
    onSetTheme: (String) -> Unit,
    onSetAccentColor: (String) -> Unit,
    onSetGaugeBorderMode: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader("Theme", colors)
        }

        // Theme grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themeOptions.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { option ->
                            val isSelected = state.currentTheme == option.name
                            Box(
                                modifier = Modifier.weight(1f).height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) option.accentDefault.copy(alpha = 0.25f)
                                        else colors.bgCard
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) option.accentDefault else colors.bgPanel,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSetTheme(option.name) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier.size(16.dp).clip(CircleShape)
                                            .background(option.accentDefault)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(option.name, color = if (isSelected) option.accentDefault else colors.textPrimary,
                                        fontSize = 9.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Current theme display
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(44.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current: ${state.currentTheme}", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(state.accentHex))))
                }
            }
        }

        // Gauge border mode
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Gauge Border", color = colors.textSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("gauge", "primary", "secondary").forEach { mode ->
                        val isSelected = state.gaugeBorderMode == mode
                        Box(
                            modifier = Modifier.height(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.carBlue else colors.bgCard)
                                .clickable { onSetGaugeBorderMode(mode) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mode.uppercase(), color = if (isSelected) Color.White else colors.textPrimary,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiSection(
    state: SettingsState,
    colors: CarputerColors,
    onScanWifi: () -> Unit,
    onConnectToWifi: (String) -> Unit,
    onDisconnectWifi: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("WiFi", colors)
        }

        // Connection status
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(if (state.wifiConnected) StatusGreen else StatusRed))
                        Text(
                            if (state.wifiConnected) "Connected to ${state.wifiSsid}" else "Not connected",
                            color = if (state.wifiConnected) StatusGreen else StatusRed,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold
                        )
                    }
                    if (state.wifiConnected) {
                        Text("IP: ${state.wifiIp}  |  Signal: ${state.wifiSignal}dBm",
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Text(state.wifiStatusText, color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }

        // Actions
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(36.dp)
                        .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                        .clickable { onScanWifi() },
                    contentAlignment = Alignment.Center
                ) { Text("SCAN", color = colors.carBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                if (state.wifiConnected) {
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp)
                            .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                            .clickable { onDisconnectWifi() },
                        contentAlignment = Alignment.Center
                    ) { Text("DISCONNECT", color = StatusOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Network list
        item {
            Text("Available Networks", color = colors.textSecondary, fontSize = 12.sp)
        }
        if (state.wifiNetworks.isEmpty()) {
            item {
                Text("No networks found. Tap SCAN to search.", color = colors.textSecondary, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
        }
        itemsIndexed(state.wifiNetworks) { _, network ->
            Box(
                modifier = Modifier.fillMaxWidth().height(36.dp)
                    .clip(RoundedCornerShape(4.dp)).background(colors.bgCard)
                    .clickable { onConnectToWifi(network.substringBefore(" (")) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(network, color = colors.textPrimary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DtcSection(
    state: SettingsState,
    colors: CarputerColors,
    onScanDtc: () -> Unit,
    onScanDtcTestMode: () -> Unit,
    onClearDtcHistory: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("DTC Diagnostic Scan", colors)
        }

        // Status
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(44.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(state.dtcStatusText, color = colors.textPrimary, fontSize = 13.sp)
                    if (state.dtcLastScanMode.isNotEmpty()) {
                        Text("Mode: ${state.dtcLastScanMode}", color = colors.textSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Scan buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(40.dp)
                        .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                        .clickable(enabled = !state.dtcBusy) { onScanDtc() },
                    contentAlignment = Alignment.Center
                ) { Text("SCAN", color = colors.carBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                Box(
                    modifier = Modifier.weight(1f).height(40.dp)
                        .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                        .clickable(enabled = !state.dtcBusy) { onScanDtcTestMode() },
                    contentAlignment = Alignment.Center
                ) { Text("TEST MODE", color = colors.carOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
        }

        // Current codes
        item {
            Text("Current Codes (${state.dtcCount})", color = colors.textSecondary, fontSize = 12.sp)
        }
        if (state.dtcCodes.isEmpty()) {
            item {
                Text("No DTC codes found.", color = colors.textSecondary, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
        }
        itemsIndexed(state.dtcCodes) { _, code ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A0000))
                    .border(1.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("P${code.code}", color = StatusRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(code.description, color = colors.textSecondary, fontSize = 11.sp)
                }
            }
        }

        // History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("History (${state.dtcHistory.size})", color = colors.textSecondary, fontSize = 12.sp)
                if (state.dtcHistory.isNotEmpty()) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(colors.bgCard)
                            .clickable { onClearDtcHistory() }.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("CLEAR", color = StatusOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
        itemsIndexed(state.dtcHistory) { _, entry ->
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(3.dp)).background(colors.bgCard)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("P${entry.code}", color = colors.carOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(entry.timestamp, color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SystemSection(
    state: SettingsState,
    colors: CarputerColors,
    onRunDiagnostics: () -> Unit,
    onSaveReport: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("System Info", colors)
        }

        // Info cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Version", state.version, colors)
                InfoRow("Uptime", state.uptime, colors)
                InfoRow("System Load", state.systemLoad, colors)
                InfoRow("Disk Usage", state.diskUsage, colors)
            }
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(36.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) { Text("REFRESH", color = colors.carBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }

        // Diagnostics
        item {
            SectionHeader("Diagnostics", colors)
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .clickable(enabled = !state.diagRunning) { onRunDiagnostics() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.diagRunning) "RUNNING..." else "RUN DIAGNOSTICS",
                    color = colors.carBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        if (state.diagIssueCount > 0) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp)).background(Color(0xFF1A0000))
                        .border(1.dp, StatusRed.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Issues: ${state.diagIssueCount}", color = StatusRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(state.diagReport, color = colors.textSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        if (state.diagReport.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                        .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                        .clickable { onSaveReport() },
                    contentAlignment = Alignment.Center
                ) { Text("SAVE REPORT", color = colors.carBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun LoggingSection(
    state: SettingsState,
    colors: CarputerColors,
    onToggleLogging: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader("Data Logging", colors)
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sensor Data Logging", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (state.logging) "ACTIVE" else "INACTIVE",
                                color = if (state.logging) StatusGreen else colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier.size(width = 80.dp, height = 36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (state.logging) StatusRed else colors.bgDark)
                                .clickable { onToggleLogging() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (state.logging) "STOP" else "START",
                                color = if (state.logging) Color.White else colors.carBlue,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (state.logging && state.logPath.isNotEmpty()) {
                        Text("Saving to:", color = colors.textSecondary, fontSize = 10.sp)
                        Text(state.logPath, color = colors.carBlue, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Log Format", color = colors.textSecondary, fontSize = 11.sp)
                    Text("CSV: timestamp,speed,rpm,throttle,map,coolant,oil,...",
                        color = colors.textPrimary, fontSize = 10.sp)
                    Text("Location: /storage/emulated/0/carputer_logs/",
                        color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun FlasherSection(
    state: SettingsState,
    colors: CarputerColors,
    onScanDevices: () -> Unit,
    onFlash: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionHeader("ESP32 Flasher", colors) }

        item {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(
                            when (state.flasherState) {
                                FlasherState.IDLE -> colors.textSecondary
                                FlasherState.ERROR -> StatusRed
                                FlasherState.DONE -> StatusGreen
                                else -> StatusYellow
                            }
                        ))
                        Text(when (state.flasherState) {
                            FlasherState.IDLE -> "Ready"
                            FlasherState.DETECTING -> "Detecting..."
                            FlasherState.CONNECTING -> "Connecting..."
                            FlasherState.SYNCING -> "Syncing..."
                            FlasherState.FLASHING -> "Flashing"
                            FlasherState.VERIFYING -> "Verifying..."
                            FlasherState.DONE -> "Complete"
                            FlasherState.ERROR -> "Error"
                        }, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (state.flasherStatusText.isNotEmpty()) {
                        Text(state.flasherStatusText, color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Progress bar during flash
        if (state.flasherState == FlasherState.FLASHING ||
            state.flasherState == FlasherState.SYNCING) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(12.dp)
                            .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(state.flasherProgress).fillMaxSize()
                                .clip(RoundedCornerShape(6.dp)).background(colors.carBlue)
                        )
                    }
                    Text("${(state.flasherProgress * 100).toInt()}%",
                        color = colors.textSecondary, fontSize = 10.sp)
                }
            }
        }

        // Device list
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.weight(1f).height(36.dp)
                        .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                        .clickable(enabled = state.flasherState != FlasherState.FLASHING) { onScanDevices() },
                    contentAlignment = Alignment.Center
                ) { Text("SCAN USB", color = colors.carBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                if (state.flasherState == FlasherState.FLASHING ||
                    state.flasherState == FlasherState.SYNCING) {
                    Box(
                        modifier = Modifier.weight(1f).height(36.dp)
                            .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) { Text("CANCEL", color = StatusOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        if (state.flasherDevices.isEmpty()) {
            item {
                Text("No USB serial devices found. Plug in ESP32 (GPIO0 to GND, then power).",
                    color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
            }
        }

        itemsIndexed(state.flasherDevices) { idx, device ->
            val canFlash = state.flasherState == FlasherState.IDLE
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .clip(RoundedCornerShape(4.dp)).background(colors.bgCard)
                    .clickable(enabled = canFlash) { onFlash(idx) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(device, color = if (canFlash) colors.textPrimary else colors.textSecondary, fontSize = 12.sp)
                    if (canFlash) {
                        Text("FLASH", color = colors.carBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, colors: CarputerColors) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(4.dp)).background(colors.bgCard)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textSecondary, fontSize = 12.sp)
        Text(value, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
