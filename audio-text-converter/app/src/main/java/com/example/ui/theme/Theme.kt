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
    primary = SophisticatedPrimary,
    secondary = SophisticatedSecondary,
    tertiary = SophisticatedTertiary,
    background = SophisticatedBackground,
    surface = SophisticatedSurface,
    onPrimary = SophisticatedOnPrimary,
    onSecondary = SophisticatedOnSecondary,
    onBackground = SophisticatedOnBackground,
    onSurface = SophisticatedOnSurface,
    outline = SophisticatedOutline,
    surfaceVariant = SophisticatedSurface,
    onSurfaceVariant = SophisticatedOnSurfaceVariant
  )

private val LightColorScheme = DarkColorScheme // Force sophisticated dark for high-end aesthetic

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for Sophisticated Dark brand consistency
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable to prevent wallpaper from overriding our brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
