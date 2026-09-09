package com.localflow.player.ui.theme

import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.localflow.player.data.ThemeMode
private val Dark = darkColorScheme(primary = Color(0xFFB9C5FF), onPrimary = Color(0xFF08255B), primaryContainer = Color(0xFF153C80), secondary = Color(0xFFC0C7DD), surface = Color(0xFF10131A), surfaceVariant = Color(0xFF292E3A))
private val Light = lightColorScheme(primary = Color(0xFF345FAF), onPrimary = Color.White, primaryContainer = Color(0xFFDCE6FF), secondary = Color(0xFF535F74), surface = Color(0xFFFAF9FF), surfaceVariant = Color(0xFFE0E2EC))
@Composable fun LocalFlowTheme(mode: ThemeMode, content: @Composable () -> Unit) { val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }; MaterialTheme(colorScheme = if (dark) Dark else Light, content = content) }
