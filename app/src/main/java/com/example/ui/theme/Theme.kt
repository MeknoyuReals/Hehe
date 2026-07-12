package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonViolet,
    secondary = NeonCyan,
    tertiary = NeonPink,
    background = DarkBg,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = TextPrimary,
    onSecondary = DarkBg
  )

private val LightColorScheme = DarkColorScheme // Force dark theme for Roblox loader aesthetic


@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic colors to keep cyberpunk aesthetics
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
