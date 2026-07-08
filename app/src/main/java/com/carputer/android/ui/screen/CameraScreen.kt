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
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.StatusRed

@Composable
fun CameraScreen(
    streaming: Boolean,
    device: String,
    recordings: List<String>,
    playingFile: String,
    playing: Boolean,
    playPosition: Long,
    playDuration: Long,
    onStartStream: () -> Unit,
    onStopStream: () -> Unit,
    onSetDevice: (String) -> Unit,
    onPlayFile: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onTogglePause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDeleteFile: (String) -> Unit,
    onScanRecordings: () -> Unit,
    formatDuration: (Long) -> String,
    fileLabel: (String) -> String,
    colors: CarputerColors,
    modifier: Modifier = Modifier,
) {
    var activeTab by remember { mutableStateOf(0) }
    var showCameraPicker by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().background(colors.bgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab bar
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).background(colors.bgPanel)
            ) {
                listOf("\uD83D\uDCF7 CAMERA", "\uD83D\uDCC2 LIBRARY").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (activeTab == idx) colors.bgCard else Color.Transparent)
                            .clickable {
                                activeTab = idx
                                if (idx == 0 && !streaming) onStartStream()
                            },
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
                // Camera tab
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)).background(Color.Black)
                            .clickable { if (streaming) onStopStream() else onStartStream() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!streaming) {
                            Text("TAP TO START CAMERA", color = colors.textSecondary, fontSize = 24.sp)
                        } else {
                            Text(if (streaming) "Camera feed" else "", color = colors.textSecondary, fontSize = 14.sp)
                        }
                        Box(
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (streaming) colors.carOrange else Color(0xFF444444))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (streaming) device else "Offline",
                                color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Control bar
                    Row(
                        modifier = Modifier.fillMaxWidth().height(60.dp)
                            .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(100.dp).fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (streaming) Color(0x33000000) else colors.bgDark)
                                .border(2.dp, if (streaming) StatusRed else colors.carBlue, RoundedCornerShape(8.dp))
                                .clickable { if (streaming) onStopStream() else onStartStream() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (streaming) "STOP" else "START",
                                color = if (streaming) StatusRed else colors.carBlue,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.width(80.dp).fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(8.dp)).background(colors.bgDark)
                                .border(1.dp, colors.bgPanel, RoundedCornerShape(8.dp))
                                .clickable { showCameraPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(device.split("/").lastOrNull() ?: "cam",
                                color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier.width(80.dp).fillMaxHeight(0.8f)
                                .clip(RoundedCornerShape(8.dp)).background(colors.bgDark)
                                .border(1.dp, colors.bgPanel, RoundedCornerShape(8.dp))
                                .clickable { activeTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("LIBRARY", color = colors.carBlue, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                // Library tab
                Column(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Video player
                    if (playingFile.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                                .clip(RoundedCornerShape(8.dp)).background(Color.Black)
                        ) {
                            Text("Video playing...", color = colors.textSecondary, fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center))
                            // Playback controls
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

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${recordings.size} recording(s)", color = colors.textSecondary, fontSize = 13.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp)).background(colors.bgCard)
                                .clickable { onScanRecordings() }.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) { Text("\u21BB Refresh", color = colors.carBlue, fontSize = 12.sp) }
                    }

                    // File list
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
                                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                            maxLines = 1)
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
