package com.carputer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.carputer.android.ui.theme.CarputerTheme
import com.carputer.android.ui.MainScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as CarputerApplication

        setContent {
            val colors by app.themeState.colors.collectAsState()

            CarputerTheme(themeState = app.themeState) {
                MainScreen(
                    modifier = Modifier.fillMaxSize(),
                    themeState = app.themeState,
                    colors = colors
                )
            }
        }
    }
}
