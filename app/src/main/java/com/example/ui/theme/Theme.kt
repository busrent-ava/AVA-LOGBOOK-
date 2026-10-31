package com.example.ui.theme

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FleetBlueLight,
    onPrimary = Color(0xFF003062),
    primaryContainer = FleetBlueContainerDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = FleetSkyLight,
    onSecondary = Color(0xFF00344F),
    secondaryContainer = Color(0xFF004C70),
    onSecondaryContainer = Color(0xFFCBE6FF),
    tertiary = FleetTealLight,
    background = FleetBgDark,
    onBackground = FleetTextPrimaryDark,
    surface = FleetSurfaceDark,
    onSurface = FleetTextPrimaryDark,
    surfaceVariant = FleetSurfaceVariantDark,
    onSurfaceVariant = FleetTextMutedDark,
    outline = FleetBorderDark,
    error = FleetError
)

private val LightColorScheme = lightColorScheme(
    primary = FleetBluePrimary,
    onPrimary = Color.White,
    primaryContainer = FleetBlueContainerLight,
    onPrimaryContainer = FleetBlueDark,
    secondary = FleetSky,
    onSecondary = Color.White,
    secondaryContainer = FleetSkyContainer,
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = FleetTeal,
    background = FleetBgLight,
    onBackground = FleetTextPrimaryLight,
    surface = FleetSurfaceLight,
    onSurface = FleetTextPrimaryLight,
    surfaceVariant = FleetSurfaceVariantLight,
    onSurfaceVariant = FleetTextMutedLight,
    outline = FleetBorderLight,
    error = FleetError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use FleetOps brand colors by default for consistent fleet identity
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
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
