package com.carputer.android

import android.app.Application
import androidx.compose.ui.graphics.Color
import com.carputer.android.data.ConfigRepository
import com.carputer.android.ui.theme.ThemeState

class CarputerApplication : Application() {

    lateinit var configRepository: ConfigRepository
        private set
    lateinit var themeState: ThemeState
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configRepository = ConfigRepository(this)
        themeState = ThemeState()

        configRepository.loadTheme { name, accentHex, borderMode ->
            themeState.setCurrentTheme(name)
            themeState.setAccentColor(Color(android.graphics.Color.parseColor(accentHex)), accentHex)
            themeState.updateGaugeBorderMode(borderMode)
        }
    }

    fun saveTheme() {
        configRepository.saveTheme(
            themeState.currentThemeName,
            themeState.accentColorHex.value,
            themeState.gaugeBorderMode
        )
    }

    companion object {
        lateinit var instance: CarputerApplication
            private set
    }
}
