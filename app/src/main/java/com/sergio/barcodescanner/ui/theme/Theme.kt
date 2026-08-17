package com.sergio.barcodescanner.ui.theme

import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode { LIGHT, DARK, AUTO }

private var currentThemeMode by mutableStateOf(ThemeMode.AUTO)

object ThemePreference {
    private const val PREFS_NAME = "barcode_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_COMPARE_BY_SUFFIX = "compare_by_suffix"
    private const val KEY_COMPARE_SUFFIX_LENGTH = "compare_suffix_length"

    fun getThemeMode(context: android.content.Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return ThemeMode.valueOf(
            prefs.getString(KEY_THEME_MODE, ThemeMode.AUTO.name) ?: ThemeMode.AUTO.name
        )
    }

    fun setThemeMode(context: android.content.Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isCompareBySuffix(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPARE_BY_SUFFIX, false)
    }

    fun setCompareBySuffix(context: android.content.Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPARE_BY_SUFFIX, enabled).apply()
    }

    fun getCompareSuffixLength(context: android.content.Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COMPARE_SUFFIX_LENGTH, 3)
    }

    fun setCompareSuffixLength(context: android.content.Context, length: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COMPARE_SUFFIX_LENGTH, length).apply()
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun BarcodeScannerTheme(
    themeMode: ThemeMode = ThemeMode.AUTO,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

class BarcodeScannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        currentThemeMode = ThemePreference.getThemeMode(this)
    }
}