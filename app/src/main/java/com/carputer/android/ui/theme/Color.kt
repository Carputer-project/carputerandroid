package com.carputer.android.ui.theme

import androidx.compose.ui.graphics.Color

// Status colors (shared across all themes)
val StatusGreen = Color(0xFF00FF88)
val StatusRed = Color(0xFFFF4444)
val StatusYellow = Color(0xFFFFAA00)
val StatusOrange = Color(0xFFFF6600)
val WarnColor = Color(0xFFFF9900)
val DangerColor = Color(0xFFFF4444)

// Common
val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)
val Transparent = Color(0x00000000)

data class CarputerColors(
    val carBlue: Color,
    val carBlueDim: Color,
    val carOrange: Color,
    val bgDark: Color,
    val bgPanel: Color,
    val bgCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val gaugeColor: Color,
    val tickColor: Color,
    val gaugeTextColor: Color,
    val needleColor: Color,
    val gaugeBorderColor: Color,
)

fun darkTheme(accent: Color = Color(0xFF00A8E8)) = CarputerColors(
    carBlue = accent,
    carBlueDim = Color(0xFF003A5C),
    carOrange = Color(0xFFFF6B35),
    bgDark = Color(0xFF0A0A0F),
    bgPanel = Color(0xFF12121A),
    bgCard = Color(0xFF1A1A24),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8888AA),
    gaugeColor = accent,
    tickColor = Color(0xFF666666),
    gaugeTextColor = Color(0xFFFFFFFF),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun lightTheme(accent: Color = Color(0xFF0078D4)) = CarputerColors(
    carBlue = accent,
    carBlueDim = Color(0xFF004E8C),
    carOrange = Color(0xFFE65C2E),
    bgDark = Color(0xFFF5F5F5),
    bgPanel = Color(0xFFFFFFFF),
    bgCard = Color(0xFFE8E8E8),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF666666),
    gaugeColor = accent,
    tickColor = Color(0xFF999999),
    gaugeTextColor = Color(0xFF1A1A1A),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun blueTheme(accent: Color = Color(0xFF00A8E8)) = CarputerColors(
    carBlue = Color(0xFF00D4FF),
    carBlueDim = Color(0xFF005A6E),
    carOrange = Color(0xFFFF8C42),
    bgDark = Color(0xFF0A0F1A),
    bgPanel = Color(0xFF0D1520),
    bgCard = Color(0xFF142030),
    textPrimary = Color(0xFFE0E8FF),
    textSecondary = Color(0xFF7A8AAA),
    gaugeColor = accent,
    tickColor = Color(0xFF4A5A7A),
    gaugeTextColor = Color(0xFFE0E8FF),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun redTheme(accent: Color = Color(0xFFFF2020)) = CarputerColors(
    carBlue = Color(0xFFFF2020),
    carBlueDim = Color(0xFF6E0000),
    carOrange = Color(0xFFFF8C00),
    bgDark = Color(0xFF0F0A0A),
    bgPanel = Color(0xFF1A0D0D),
    bgCard = Color(0xFF240F0F),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFAA8888),
    gaugeColor = accent,
    tickColor = Color(0xFF664444),
    gaugeTextColor = Color(0xFFFFFFFF),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun greenTheme(accent: Color = Color(0xFF00FF88)) = CarputerColors(
    carBlue = Color(0xFF00FF88),
    carBlueDim = Color(0xFF006633),
    carOrange = Color(0xFFFFAA00),
    bgDark = Color(0xFF0A0F0A),
    bgPanel = Color(0xFF0D1A0D),
    bgCard = Color(0xFF142414),
    textPrimary = Color(0xFFE0FFE0),
    textSecondary = Color(0xFF7AAA7A),
    gaugeColor = accent,
    tickColor = Color(0xFF336633),
    gaugeTextColor = Color(0xFFE0FFE0),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun purpleTheme(accent: Color = Color(0xFF9B59B6)) = CarputerColors(
    carBlue = Color(0xFF9B59B6),
    carBlueDim = Color(0xFF4A235A),
    carOrange = Color(0xFFFF8C42),
    bgDark = Color(0xFF0F0A14),
    bgPanel = Color(0xFF1A0D24),
    bgCard = Color(0xFF241430),
    textPrimary = Color(0xFFF0E0FF),
    textSecondary = Color(0xFF8A7AAA),
    gaugeColor = accent,
    tickColor = Color(0xFF4A2A5A),
    gaugeTextColor = Color(0xFFF0E0FF),
    needleColor = accent,
    gaugeBorderColor = accent,
)

fun orangeTheme(accent: Color = Color(0xFFFF6B35)) = CarputerColors(
    carBlue = Color(0xFFFF6B35),
    carBlueDim = Color(0xFF8C3A1A),
    carOrange = Color(0xFFFFAA00),
    bgDark = Color(0xFF0F0A07),
    bgPanel = Color(0xFF1A0D09),
    bgCard = Color(0xFF241A10),
    textPrimary = Color(0xFFFFF5E0),
    textSecondary = Color(0xFFAA8A78),
    gaugeColor = accent,
    tickColor = Color(0xFF5A3A28),
    gaugeTextColor = Color(0xFFFFF5E0),
    needleColor = accent,
    gaugeBorderColor = accent,
)
