package com.carputer.android.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carputer.android.data.model.TrackInfo
import com.carputer.android.ui.PageNavController
import com.carputer.android.ui.components.SpectrumBar
import com.carputer.android.ui.theme.CarputerColors
import com.carputer.android.ui.theme.StatusGreen

@Composable
fun MediaScreen(
    playlist: List<TrackInfo>,
    currentIndex: Int,
    playing: Boolean,
    position: Long,
    duration: Long,
    currentTrack: TrackInfo?,
    volume: Int,
    repeatMode: Int,
    shuffleOn: Boolean,
    spectrumData: List<Float>,
    onScanMedia: (String) -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onPlayTrack: (Int) -> Unit,
    onSetVolume: (Int) -> Unit,
    onSetRepeatMode: (Int) -> Unit,
    onSetShuffle: (Boolean) -> Unit,
    colors: CarputerColors,
    pageNavController: PageNavController? = null,
    modifier: Modifier = Modifier,
) {
    var browseMode by remember { mutableStateOf(0) }
    val focusIndex = remember { mutableStateOf(-1) }

    SideEffect {
        pageNavController?.let { ctrl ->
            ctrl.navigateLeft = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value > 0) focusIndex.value--
            }
            ctrl.navigateRight = {
                if (focusIndex.value < 0) focusIndex.value = 0
                else if (focusIndex.value < 4) focusIndex.value++
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
                } else if (focusIndex.value < 4) {
                    focusIndex.value++
                    true
                } else {
                    false
                }
            }
            ctrl.activateFocus = {
                if (focusIndex.value >= 0) {
                    when (focusIndex.value) {
                        0 -> if (playing) onPause() else onPlay()
                        1 -> onPrevious()
                        2 -> onNext()
                        3 -> onSetShuffle(!shuffleOn)
                        4 -> onSetRepeatMode((repeatMode + 1) % 3)
                    }
                }
            }
            ctrl.handleEscape = {
                focusIndex.value = -1
            }
            ctrl.focusIndex.intValue = focusIndex.value
            ctrl.focusActionName.value = when (focusIndex.value) {
                0 -> "PLAY"
                1 -> "PREV"
                2 -> "NEXT"
                3 -> "SHUFFLE"
                4 -> "REPEAT"
                else -> ""
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.bgDark)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title bar
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp)
                    .clip(RoundedCornerShape(8.dp)).background(colors.bgCard)
                    .border(1.dp, colors.carBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("MEDIA PLAYER", color = colors.carBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            // Source buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("/storage/emulated/0/Music" to "MUSIC",
                       "/storage/emulated/0/Download" to "DOWNLOADS",
                       "/storage/emulated/0" to "INTERNAL").forEach { (path, label) ->
                    Box(
                        modifier = Modifier
                            .width(100.dp).height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.bgCard)
                            .clickable { onScanMedia(path) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Now playing + transport
            Box(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)).background(colors.bgCard)
                    .border(1.dp, colors.carBlue.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    .padding(15.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Now Playing", color = colors.textSecondary, fontSize = 14.sp)

                    // Artwork + track info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(70.dp)
                                .clip(RoundedCornerShape(6.dp)).background(colors.bgDark)
                                .border(1.dp, colors.carBlue.copy(alpha = 0.25f), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\u266B", fontSize = 28.sp, color = colors.textSecondary)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                currentTrack?.title?.ifEmpty { currentTrack?.fileName ?: "No track loaded" } ?: "No track loaded",
                                color = colors.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(currentTrack?.artist ?: "", color = colors.carBlue, fontSize = 12.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(currentTrack?.album ?: "", color = colors.textSecondary, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    // Spectrum
                    SpectrumBar(
                        spectrumData = spectrumData,
                        barCount = 32,
                        height = 60.dp,
                        carBlue = colors.carBlue,
                        bgDark = colors.bgDark,
                    )

                    // Transport + repeat/shuffle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Repeat
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (repeatMode > 0) colors.carBlue else colors.bgDark)
                                .border(1.dp, colors.textSecondary, RoundedCornerShape(4.dp))
                                .clickable { onSetRepeatMode((repeatMode + 1) % 3) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (repeatMode == 2) "1" else "\u21BB",
                                color = if (repeatMode > 0) Color.White else colors.textSecondary,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                                .background(colors.bgDark).clickable { onPrevious() },
                            contentAlignment = Alignment.Center
                        ) { Text("PREV", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Box(
                            modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp))
                                .background(colors.bgDark).clickable { if (playing) onPause() else onPlay() },
                            contentAlignment = Alignment.Center
                        ) { Text(if (playing) "PAUSE" else "PLAY", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                                .background(colors.bgDark).clickable { onNext() },
                            contentAlignment = Alignment.Center
                        ) { Text("NEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        // Shuffle
                        Box(
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
                                .background(if (shuffleOn) colors.carBlue else colors.bgDark)
                                .border(1.dp, colors.textSecondary, RoundedCornerShape(4.dp))
                                .clickable { onSetShuffle(!shuffleOn) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\u21C4", color = if (shuffleOn) Color.White else colors.textSecondary,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Seek slider
                    Slider(
                        value = position.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        formatTime(position) + " / " + formatTime(duration),
                        color = colors.textSecondary, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Volume
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("VOL", color = colors.textSecondary, fontSize = 12.sp)
                        Slider(
                            value = volume.toFloat(),
                            onValueChange = { onSetVolume(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.width(100.dp)
                        )
                        Text("$volume%", color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Browse tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf("Playlist", "Albums").forEachIndexed { idx, label ->
                    Box(
                        modifier = Modifier.weight(1f).height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (browseMode == idx) colors.carBlue else colors.bgCard)
                            .clickable { browseMode = idx },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (browseMode == idx) Color.White else colors.textSecondary,
                            fontSize = 12.sp, fontWeight = if (browseMode == idx) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            // Playlist
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (playlist.isEmpty()) {
                    item {
                        Text("No tracks loaded", color = colors.textSecondary, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
                    }
                }

                itemsIndexed(playlist) { index, track ->
                    Box(
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                            .background(if (index == currentIndex) colors.carBlueDim else Color.Transparent)
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { onPlayTrack(index) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${index + 1}", color = colors.textSecondary, fontSize = 11.sp,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                track.fileName, color = colors.textPrimary, fontSize = 11.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
