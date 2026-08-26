package com.loantrack.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = SecondaryDark,
    background = SurfaceLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surface = CardLight,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    error = StatusOverdue,
    onError = androidx.compose.ui.graphics.Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = Primary,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
    secondary = SecondaryLight,
    onSecondary = SecondaryDark,
    secondaryContainer = Secondary,
    onSecondaryContainer = androidx.compose.ui.graphics.Color.White,
    background = SurfaceDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surface = CardDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    error = StatusOverdue,
    onError = androidx.compose.ui.graphics.Color.White,
)

@Composable
fun LoanTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
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
