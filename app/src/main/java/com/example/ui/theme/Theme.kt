package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ProfessionalPolishColorScheme =
  lightColorScheme(
    primary = PolishBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PolishBlueContainer,
    onPrimaryContainer = PolishBlueOnContainer,
    secondary = PolishPurplePrimary,
    onSecondary = Color.White,
    secondaryContainer = PolishPurpleContainer,
    onSecondaryContainer = PolishPurplePrimary,
    tertiary = PolishSuccess,
    onTertiary = Color.White,
    tertiaryContainer = PolishSuccessContainer,
    onTertiaryContainer = PolishSuccess,
    background = PolishBackground,
    onBackground = PolishTextPrimary,
    surface = PolishSurface,
    onSurface = PolishTextPrimary,
    surfaceVariant = PolishSurfaceVariant,
    onSurfaceVariant = PolishTextSecondary,
    outline = PolishBorder,
    outlineVariant = PolishBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // "Professional Polish" default theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> ProfessionalPolishColorScheme
      else -> ProfessionalPolishColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
