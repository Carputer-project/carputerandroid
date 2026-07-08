package com.carputer.android.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carputer.android.ui.theme.DangerColor
import com.carputer.android.ui.theme.StatusOrange
import com.carputer.android.ui.theme.StatusRed
import com.carputer.android.ui.theme.StatusYellow

data class WarningItem(
    val sensor: String,
    val level: String,
    val value: String,
    val action: String,
)

@Composable
fun WarningPopup(
    worstAlert: String,
    activeWarnings: List<WarningItem>,
    dismissedKey: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = worstAlert != "ok" && activeWarnings.isNotEmpty() && dismissedKey != "${worstAlert}_${activeWarnings.size}",
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            val borderColor = when (worstAlert) {
                "critical" -> StatusRed
                "danger" -> StatusOrange
                else -> StatusYellow
            }
            val bgColor = when (worstAlert) {
                "critical" -> Color(0xFF1A0000)
                "danger" -> Color(0xFF1A0F00)
                else -> Color(0xFF00001A)
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when (worstAlert) {
                        "critical" -> "⚠ CRITICAL WARNING"
                        "danger" -> "⚠ WARNING"
                        else -> "⚠ CAUTION"
                    },
                    color = borderColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor)
                )

                activeWarnings.forEach { warning ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x22000000))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val dotColor = when (warning.level) {
                            "critical" -> StatusRed
                            "danger" -> StatusOrange
                            else -> StatusYellow
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(dotColor, RoundedCornerShape(4.dp))
                        )
                        Column {
                            Text(
                                text = "${warning.sensor}: ${warning.value}",
                                color = when (warning.level) {
                                    "critical" -> StatusRed
                                    "danger" -> StatusOrange
                                    else -> Color.White
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = warning.action,
                                color = Color(0xFF8888AA),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally).width(200.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF12121A)
                    )
                ) {
                    Text("DISMISS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
