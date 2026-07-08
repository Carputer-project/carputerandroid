package com.carputer.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun SpectrumBar(
    spectrumData: List<Float>,
    modifier: Modifier = Modifier,
    barCount: Int = 16,
    height: Dp = 50.dp,
    carBlue: Color = Color(0xFF00A8E8),
    bgDark: Color = Color(0xFF1A1A24),
) {
    val displayData = if (spectrumData.isEmpty()) {
        List(barCount) { -80f }
    } else {
        spectrumData.take(barCount).ifEmpty { List(barCount) { -80f } }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val barWidth = (size.width - (barCount - 1) * 2f) / barCount
        val barGap = 2f

        drawRoundRect(
            color = bgDark,
            topLeft = androidx.compose.ui.geometry.Offset.Zero,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        displayData.forEachIndexed { index, db ->
            val normalized = max(0f, (db + 80f) / 80f)
            val barHeight = max(2f, normalized * size.height)
            val x = index * (barWidth + barGap) + 4f

            val barColor = when {
                normalized < 0.5f -> Color(
                    red = 0f,
                    green = 0.66f + normalized * 0.68f,
                    blue = 0.91f,
                    alpha = 1f
                )
                else -> Color(
                    red = 0f,
                    green = 1.0f,
                    blue = 0.91f - (normalized - 0.5f) * 1.4f,
                    alpha = 1f
                )
            }

            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1f, 1f)
            )
        }

        drawRoundRect(
            color = bgDark.copy(alpha = 0.3f),
            topLeft = androidx.compose.ui.geometry.Offset.Zero,
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
            style = Stroke(width = 1f)
        )
    }
}
