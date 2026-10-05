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

private val LightColorScheme = lightColorScheme(
  primary = NavyPrimary,
  onPrimary = Color.White,
  primaryContainer = BlueAccentLight,
  onPrimaryContainer = NavyDark,
  secondary = BlueAccent,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFF1F5F9),
  onSecondaryContainer = Color(0xFF0F172A),
  tertiary = ColorReport,
  onTertiary = Color.White,
  background = BgLight,
  onBackground = TextPrimary,
  surface = CardBg,
  onSurface = TextPrimary,
  surfaceVariant = Color(0xFFF1F5F9),
  onSurfaceVariant = TextSecondary,
  outline = SurfaceBorder,
  error = ColorLate,
  onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
  primary = BlueAccent,
  onPrimary = NavyDark,
  primaryContainer = NavyLight,
  onPrimaryContainer = Color.White,
  secondary = BlueAccentLight,
  onSecondary = NavyDark,
  background = Color(0xFF0B1320),
  onBackground = Color(0xFFF8FAFC),
  surface = Color(0xFF131D2E),
  onSurface = Color(0xFFF8FAFC),
  surfaceVariant = Color(0xFF1E293B),
  onSurfaceVariant = Color(0xFFCBD5E1),
  outline = Color(0xFF334155),
  error = Color(0xFFF87171),
  onError = Color.Black
)

@Composable
fun TheKuzanTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Preserve dedicated brand identity by default
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
