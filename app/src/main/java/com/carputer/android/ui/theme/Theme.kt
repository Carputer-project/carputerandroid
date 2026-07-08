package com.carputer.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private val DarkColorScheme = darkColorScheme()

@Composable
fun CarputerTheme(
    themeState: ThemeState,
    content: @Composable () -> Unit
) {
    val colors by themeState.colors.collectAsState()

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
