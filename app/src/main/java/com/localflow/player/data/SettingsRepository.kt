package com.localflow.player.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore("settings")
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class AppSettings(val theme: ThemeMode = ThemeMode.DARK, val videoAudioBackground: Boolean = true, val resumePlayback: Boolean = true, val hideShort: Boolean = false)
class SettingsRepository(private val context: Context) {
    private object Keys { val theme = stringPreferencesKey("theme"); val videoAudio = booleanPreferencesKey("video_audio"); val resume = booleanPreferencesKey("resume"); val short = booleanPreferencesKey("hide_short") }
    val settings: Flow<AppSettings> = context.settingsDataStore.data.catch { if(it is IOException) emit(emptyPreferences()) else throw it }.map { p -> AppSettings(ThemeMode.entries.find { it.name == p[Keys.theme] } ?: ThemeMode.DARK, p[Keys.videoAudio] ?: true, p[Keys.resume] ?: true, p[Keys.short] ?: false) }
    suspend fun setTheme(value: ThemeMode) = context.settingsDataStore.edit { it[Keys.theme] = value.name }
    suspend fun setVideoAudioBackground(value: Boolean) = context.settingsDataStore.edit { it[Keys.videoAudio] = value }
    suspend fun setResumePlayback(value: Boolean) = context.settingsDataStore.edit { it[Keys.resume] = value }
    suspend fun setHideShort(value: Boolean) = context.settingsDataStore.edit { it[Keys.short] = value }
}
