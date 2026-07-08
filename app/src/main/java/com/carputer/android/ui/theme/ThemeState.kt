package com.carputer.android.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeState {
    var currentThemeName by mutableStateOf("Dark")
        private set
    var accentColor by mutableStateOf(Color(0xFF00A8E8))
        private set
    var gaugeBorderMode by mutableStateOf("gauge")
        private set

    private val _colors = MutableStateFlow(darkTheme())
    val colors: StateFlow<CarputerColors> = _colors.asStateFlow()

    private val _accentColorHex = MutableStateFlow("#00A8E8")
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    private val _themeName = MutableStateFlow("Dark")
    val themeName: StateFlow<String> = _themeName.asStateFlow()

    private val _gaugeBorderModeState = MutableStateFlow("gauge")
    val gaugeBorderModeState: StateFlow<String> = _gaugeBorderModeState.asStateFlow()

    fun setCurrentTheme(name: String) {
        currentThemeName = name
        _themeName.value = name
        updateColors()
    }

    fun setAccentColor(color: Color, hex: String) {
        accentColor = color
        _accentColorHex.value = hex
        updateColors()
    }

    fun updateGaugeBorderMode(mode: String) {
        gaugeBorderMode = mode
        _gaugeBorderModeState.value = mode
        updateColors()
    }

    private fun updateColors() {
        val base = when (currentThemeName) {
            "Light" -> lightTheme(accentColor)
            "Blue" -> blueTheme(accentColor)
            "Red" -> redTheme(accentColor)
            "Green" -> greenTheme(accentColor)
            "Purple" -> purpleTheme(accentColor)
            "Orange" -> orangeTheme(accentColor)
            else -> darkTheme(accentColor)
        }
        val border = when (gaugeBorderMode) {
            "primary" -> base.carBlue
            "secondary" -> base.carOrange
            else -> base.gaugeColor
        }
        _colors.value = base.copy(gaugeBorderColor = border)
    }
}
