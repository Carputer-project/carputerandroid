package com.carputer.android.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carputer.android.ui.theme.CarputerColors

@Composable
fun TransportControls(
    playing: Boolean,
    position: Long,
    duration: Long,
    volume: Int,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetVolume: (Int) -> Unit,
    colors: CarputerColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                    .background(colors.bgDark).clickable { onPrev() },
                contentAlignment = Alignment.Center
            ) { Text("PREV", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp))
                    .background(colors.bgDark).clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) { Text(if (playing) "PAUSE" else "PLAY", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                    .background(colors.bgDark).clickable { onNext() },
                contentAlignment = Alignment.Center
            ) { Text("NEXT", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(formatTime(position), color = colors.textSecondary, fontSize = 10.sp)
            Slider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(if (duration > 0) duration.toFloat() else 100f),
                modifier = Modifier.weight(1f)
            )
            Text(formatTime(duration), color = colors.textSecondary, fontSize = 10.sp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("VOL", color = colors.textSecondary, fontSize = 11.sp)
            Slider(
                value = volume.toFloat(),
                onValueChange = { onSetVolume(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier.width(100.dp)
            )
            Text("$volume%", color = colors.textSecondary, fontSize = 10.sp)
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 60000)
    return "%d:%02d".format(minutes, seconds)
}
