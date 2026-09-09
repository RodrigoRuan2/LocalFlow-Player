package com.localflow.player.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.localflow.player.data.ThemeMode
private val Midnight = darkColorScheme(
    primary = Color(0xFFBCABFF), onPrimary = Color(0xFF261550),
    primaryContainer = Color(0xFF302747), onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFC8BFE0), onSecondary = Color(0xFF292137),
    secondaryContainer = Color(0xFF292A3B), onSecondaryContainer = Color(0xFFF4F2FF),
    background = Color(0xFF111219), onBackground = Color(0xFFF4F2FF),
    surface = Color(0xFF111219), onSurface = Color(0xFFF4F2FF),
    surfaceVariant = Color(0xFF1D1E29), onSurfaceVariant = Color(0xFFB7B4C9),
    surfaceContainer = Color(0xFF1D1E29), surfaceContainerLow = Color(0xFF171820),
    surfaceContainerHigh = Color(0xFF292A3B), outline = Color(0xFF858096), outlineVariant = Color(0xFF343443)
)
private val Daylight = lightColorScheme(
    primary = Color(0xFF665098), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADFFF), onPrimaryContainer = Color(0xFF261550),
    secondaryContainer = Color(0xFFECE6F6), onSecondaryContainer = Color(0xFF292137),
    background = Color(0xFFFAF8FF), surface = Color(0xFFFAF8FF), onSurface = Color(0xFF221E2C),
    surfaceVariant = Color(0xFFF0ECF7), onSurfaceVariant = Color(0xFF615A70),
    surfaceContainer = Color(0xFFF0ECF7), surfaceContainerHigh = Color(0xFFE8E1F0), outlineVariant = Color(0xFFD9D1E3)
)
@Composable fun LocalFlowTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    MaterialTheme(colorScheme = if (dark) Midnight else Daylight,
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp)), content = content)
}
