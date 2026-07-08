package com.carputer.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AnalogGauge(
    value: Float,
    minValue: Float = 0f,
    maxValue: Float = 100f,
    label: String = "",
    unitLabel: String = "",
    fontSize: Int = 14,
    majorTicks: Int = 5,
    minorTicks: Int = 4,
    warnValue: Float = 0.75f,
    dangerValue: Float = 0.90f,
    redlineStart: Float = 0.85f,
    showValue: Boolean = true,
    showNeedle: Boolean = true,
    startAngle: Float = 135f,
    endAngle: Float = 405f,
    thickness: Float = 0.13f,
    gaugeColor: Color = Color(0xFF00A8E8),
    warnColor: Color = Color(0xFFFF9900),
    dangerColor: Color = Color(0xFFFF4444),
    needleColor: Color = Color(0xFF00A8E8),
    tickColor: Color = Color(0xFF666666),
    textColor: Color = Color(0xFFFFFFFF),
    borderColor: Color = Color(0xFF00A8E8),
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 200.dp,
) {
    val animValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(durationMillis = 350, easing = androidx.compose.animation.core.EaseOutQuart),
        label = "gaugeValue"
    )
    val range = maxValue - minValue
    val frac = if (range > 0f) ((animValue - minValue) / range).coerceIn(0f, 1f) else 0f
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = modifier.size(gaugeSize)) {
        Canvas(modifier = Modifier.size(gaugeSize)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = min(this.size.width, this.size.height) / 2f - 6f
            val innerR = radius * (1f - thickness)
            val labelR = innerR - 18f
            val sa = startAngle * PI.toFloat() / 180f
            val ea = endAngle * PI.toFloat() / 180f
            val arcSpan = endAngle - startAngle

            // Outer bezel
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF252530), Color(0xFF050508)),
                    center = Offset(cx, cy),
                    radius = radius + 5f
                ),
                radius = radius + 5f,
                center = Offset(cx, cy)
            )

            // Glowing accent ring
            drawCircle(
                color = borderColor,
                radius = radius + 5f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )

            // Dark background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1E28), Color(0xFF131318), Color(0xFF080810)),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )

            // Full arc track baseline
            drawArc(
                color = Color(0xFF18181F),
                startAngle = startAngle,
                sweepAngle = arcSpan,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = (radius - innerR))
            )

            // Permanent redline zone
            if (redlineStart < 1f) {
                val redSweep = arcSpan * (1f - redlineStart)
                drawArc(
                    color = Color(0xFF3D0A0A),
                    startAngle = startAngle + arcSpan * redlineStart,
                    sweepAngle = redSweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius, cy - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = (radius - innerR))
                )
                drawArc(
                    color = Color(0xFFDD1111),
                    startAngle = startAngle + arcSpan * redlineStart,
                    sweepAngle = redSweep,
                    useCenter = false,
                    topLeft = Offset(cx - radius + 1.5f, cy - radius + 1.5f),
                    size = Size((radius - 1.5f) * 2, (radius - 1.5f) * 2),
                    style = Stroke(width = 3f)
                )
            }

            // Active colored arc
            if (frac > 0f) {
                val warnAngle = startAngle + warnValue * arcSpan
                val dangerAngle = startAngle + dangerValue * arcSpan
                val curAngle = startAngle + frac * arcSpan

                // Green zone
                val safeEnd = minOf(curAngle, warnAngle)
                if (safeEnd > startAngle) {
                    drawArc(
                        color = gaugeColor,
                        startAngle = startAngle,
                        sweepAngle = safeEnd - startAngle,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = (radius - innerR))
                    )
                }
                // Yellow zone
                if (curAngle > warnAngle) {
                    val wEnd = minOf(curAngle, dangerAngle)
                    drawArc(
                        color = warnColor,
                        startAngle = warnAngle,
                        sweepAngle = wEnd - warnAngle,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = (radius - innerR))
                    )
                }
                // Red zone
                if (curAngle > dangerAngle) {
                    drawArc(
                        color = dangerColor,
                        startAngle = dangerAngle,
                        sweepAngle = curAngle - dangerAngle,
                        useCenter = false,
                        topLeft = Offset(cx - radius, cy - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = (radius - innerR))
                    )
                }
            }

            // Tick marks
            val totalTicks = majorTicks * minorTicks
            for (i in 0..totalTicks) {
                val tf = i.toFloat() / totalTicks.toFloat()
                val tAngle = startAngle + tf * arcSpan
                val tRad = tAngle * PI.toFloat() / 180f
                val isMajor = i % minorTicks == 0
                val outerT = innerR - 1f
                val innerT = if (isMajor) innerR - 16f else innerR - 8f

                val tc = when {
                    tf >= dangerValue -> dangerColor
                    tf >= warnValue -> warnColor
                    else -> tickColor
                }

                drawLine(
                    color = tc,
                    start = Offset(cx + outerT * cos(tRad), cy + outerT * sin(tRad)),
                    end = Offset(cx + innerT * cos(tRad), cy + innerT * sin(tRad)),
                    strokeWidth = if (isMajor) 2.5f else 1.2f
                )

                // Major tick labels
                if (isMajor) {
                    val lr = labelR - 2f
                    val tVal = (minValue + tf * range).toInt()
                    val tStr = if (maxValue >= 1000f && tVal >= 1000) {
                        (tVal / 1000).toString()
                    } else tVal.toString()

                    val tickLabelResult = textMeasurer.measure(
                        text = tStr,
                        style = TextStyle(
                            color = if (tf >= dangerValue) dangerColor else textColor,
                            fontSize = (fontSize - 3).sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    drawText(
                        textLayoutResult = tickLabelResult,
                        topLeft = Offset(
                            cx + lr * cos(tRad) - tickLabelResult.size.width / 2f,
                            cy + lr * sin(tRad) - tickLabelResult.size.height / 2f
                        )
                    )
                }
            }

            // Center hub
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF383848), Color(0xFF0C0C18)),
                    center = Offset(cx, cy - 1f),
                    radius = innerR * 0.25f
                ),
                radius = innerR * 0.25f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF2A2A3A),
                radius = innerR * 0.25f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f)
            )

            // Needle
            if (showNeedle) {
                val nAngle = (startAngle + frac * arcSpan + 90f) * PI.toFloat() / 180f
                val nLen = innerR - 4f
                val nTail = innerR * 0.20f
                val nW = 2.5f

                val nColor = when {
                    frac >= dangerValue -> dangerColor
                    frac >= warnValue -> warnColor
                    else -> needleColor
                }

                val cosA = cos(nAngle)
                val sinA = sin(nAngle)

                // Needle body
                val needlePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, -nLen)
                    lineTo(-nW, 0f)
                    lineTo(-nW * 0.5f, nTail)
                    lineTo(nW * 0.5f, nTail)
                    lineTo(nW, 0f)
                    close()
                }

                translate(
                    left = cx,
                    top = cy
                ) {
                    rotate(nAngle * 180f / PI.toFloat() - 90f, Offset.Zero) {
                        drawPath(
                            path = needlePath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    nColor,
                                    Color(0xFF1A1A28)
                                ),
                                start = Offset(0f, -nLen),
                                end = Offset(0f, nTail)
                            )
                        )
                    }
                }

                // Pivot cap
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF505060), Color(0xFF0E0E1C)),
                        center = Offset(cx, cy - 1.5f),
                        radius = nW + 3f
                    ),
                    radius = nW + 3f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF404050),
                    radius = nW + 3f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1f)
                )
            }

            // Value display
            if (showValue) {
                val vColor = when {
                    frac >= dangerValue -> dangerColor
                    frac >= warnValue -> warnColor
                    else -> gaugeColor
                }
                val hasUnit = unitLabel.isNotEmpty()
                val valY = cy + if (hasUnit) -(fontSize * 0.7f) else 0f

                val valueResult = textMeasurer.measure(
                    text = value.toInt().toString(),
                    style = TextStyle(
                        color = vColor,
                        fontSize = (fontSize + 6).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = valueResult,
                    topLeft = Offset(cx - valueResult.size.width / 2f, valY - valueResult.size.height / 2f)
                )

                if (hasUnit) {
                    val unitResult = textMeasurer.measure(
                        text = unitLabel,
                        style = TextStyle(
                            color = textColor,
                            fontSize = (fontSize - 1).sp
                        )
                    )
                    drawText(
                        textLayoutResult = unitResult,
                        topLeft = Offset(
                            cx - unitResult.size.width / 2f,
                            valY + fontSize + 4f - unitResult.size.height / 2f
                        )
                    )
                }
            }

            // Gauge name label
            if (label.isNotEmpty()) {
                val labelResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        color = textColor,
                        fontSize = (fontSize - 2).sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                drawText(
                    textLayoutResult = labelResult,
                    topLeft = Offset(
                        cx - labelResult.size.width / 2f,
                        cy + radius * 0.60f - labelResult.size.height / 2f
                    )
                )
            }
        }
    }
}
