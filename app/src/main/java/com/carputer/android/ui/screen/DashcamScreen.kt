package com.carputer.android.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.carputer.android.ui.PageNavController
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.StatusRed

@Composable
fun DashcamScreen(
    recording: Boolean,
    recordingSeconds: Int,
    recordings: List<String>,
    playingFile: String,
    playing: Boolean,
    playPosition: Long,
    playDuration: Long,
    cameraSource: String,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayFile: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onTogglePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDeleteFile: (String) -> Unit,
    onScanRecordings: () -> Unit,
    onSetCameraSource: (String) -> Unit,
    formatDuration: (Long) -> String,
    fileLabel: (String) -> String,
    colors: CarputerColors,
    pageNavController: PageNavController? = null,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(0) }
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
                    when (focusIndex.value) {
                        0 -> if (recording) onStopRecording() else onStartRecording()
                        1 -> { activeTab = (activeTab + 1) % 2 }
                        2 -> onScanRecordings()
                        3 -> recordings.firstOrNull()?.let { onPlayFile(it) }
                        4 -> if (playing) onStopPlayback() else onTogglePause()
                        5 -> recordings.firstOrNull()?.let { onDeleteFile(it) }
                    }
                }
            }
            ctrl.handleEscape = {
                focusIndex.value = -1
            }
            ctrl.focusIndex.intValue = focusIndex.value
            ctrl.focusActionName.value = when (focusIndex.value) {
                0 -> "REC"
                1 -> "TAB"
                2 -> "REFRESH"
                3 -> "PLAY"
                4 -> "STOP"
                5 -> "DELETE"
                else -> ""
            }
        }
    }
    var showCameraPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().background(colors.bgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).background(colors.bgPanel)
            ) {
                listOf("\u25CF RECORD", "\uD83D\uDCC2 LIBRARY").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (activeTab == idx) colors.bgCard else Color.Transparent)
                            .clickable { activeTab = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = if (activeTab == idx) colors.carBlue else colors.textSecondary,
                                fontSize = 14.sp, fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Normal)
                            if (activeTab == idx) {
                                Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(colors.carBlue))
                            }
                        }
                    }
                }
            }

            if (activeTab == 0) {
                // Record tab
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Camera preview
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)).background(Color.Black)
                            .border(if (recording) 3.dp else 1.dp,
                                if (recording) StatusRed else colors.bgPanel, RoundedCornerShape(8.dp))
                    ) {
                        Text("Camera preview", color = colors.textSecondary, fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.Center))

                        // REC badge
                        if (recording) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                                    .clip(RoundedCornerShape(4.dp)).background(Color(0xCC000000))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("\u25CF REC ${recordingSeconds}s", color = Color.White,
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Source label
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                .clip(RoundedCornerShape(4.dp)).background(colors.bgPanel)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(cameraSource, color = colors.textSecondary, fontSize = 10.sp)
                        }
                    }

                    // Record button + camera picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(0.65f).height(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (recording) Color(0x33000000) else colors.bgDark)
                                .border(2.dp, if (recording) StatusRed else colors.carBlue, RoundedCornerShape(10.dp))
                                .clickable { if (recording) onStopRecording() else onStartRecording() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (recording) "STOP" else "REC",
                                    fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    color = if (recording) StatusRed else colors.carBlue)
                                Text(if (recording) "STOP RECORDING" else "START RECORDING",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = if (recording) StatusRed else colors.carBlue)
                            }
                        }
                        Box(
                            modifier = Modifier.weight(0.35f).height(60.dp)
                                .clip(RoundedCornerShape(10.dp)).background(colors.bgCard)
                                .border(1.dp, colors.bgPanel, RoundedCornerShape(10.dp))
                                .clickable { showCameraPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("CAMERA", color = colors.textSecondary, fontSize = 9.sp)
                                Text(cameraSource.split("/").lastOrNull() ?: "cam",
                                    color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("tap to change", color = colors.textSecondary, fontSize = 9.sp)
                            }
                        }
                    }

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) { StatBox("FILES", "${recordings.size}", colors.carBlue, colors) }
                        Box(modifier = Modifier.weight(1f)) { StatBox("SAVE TO", cameraSource.split("/").lastOrNull() ?: "dvr", colors.textPrimary, colors) }
                        Box(modifier = Modifier.weight(1f)) {
                            StatBox(
                                if (recording) "RECORDING" else "READY",
                                if (recording) "%d:%02d".format(recordingSeconds / 60, recordingSeconds % 60) else "\u2014",
                                if (recording) colors.textPrimary else colors.textSecondary,
                                colors
                            )
                        }
                    }
                }
            } else {
                // Library tab (same as CameraScreen library)
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (playingFile.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.Black)
                        ) {
                            Text("DVR playback...", color = colors.textSecondary, fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center))
                            Row(
                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                    .background(Color(0x99000000)).align(Alignment.BottomCenter)
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(formatDuration(playPosition), color = Color.White, fontSize = 11.sp)
                                Slider(
                                    value = playPosition.toFloat(),
                                    onValueChange = { onSeekTo(it.toLong()) },
                                    valueRange = 0f..(if (playDuration > 0) playDuration.toFloat() else 100f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(formatDuration(playDuration), color = Color(0xFFAAAAAA), fontSize = 11.sp)
                                Box(
                                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
                                        .background(colors.carBlueDim).clickable { onTogglePause() },
                                    contentAlignment = Alignment.Center
                                ) { Text(if (playing) "\u23F8" else "\u25B6", color = colors.carBlue, fontSize = 14.sp) }
                                Box(
                                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF444444)).clickable { onStopPlayback() },
                                    contentAlignment = Alignment.Center
                                ) { Text("\u2715", color = Color.White, fontSize = 14.sp) }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${recordings.size} recording(s)", color = colors.textSecondary, fontSize = 13.sp)
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                                .clickable { onScanRecordings() }.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("\u21BB Refresh", color = colors.carBlue, fontSize = 12.sp) }
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        if (recordings.isEmpty()) {
                            item {
                                Text("No recordings yet.", color = colors.textSecondary, fontSize = 14.sp,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
                            }
                        }
                        itemsIndexed(recordings) { _, file ->
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (playingFile == file) colors.carBlueDim else colors.bgCard)
                                    .border(if (playingFile == file) 1.dp else 0.dp, colors.carBlue, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileLabel(file), color = colors.textPrimary,
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(file.split("/").lastOrNull() ?: "", color = colors.textSecondary, fontSize = 10.sp)
                                    }
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                            .background(colors.bgPanel).clickable { onPlayFile(file) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("\u25B6", color = colors.carBlue, fontSize = 16.sp) }
                                    Box(
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                                            .background(colors.bgPanel).clickable { deleteTarget = file },
                                        contentAlignment = Alignment.Center
                                    ) { Text("\uD83D\uDDD1", fontSize = 16.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, valueColor: Color, colors: CarputerColors) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp)).background(colors.bgCard),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = colors.textSecondary, fontSize = 9.sp)
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier,
) {
    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier
    )
}
