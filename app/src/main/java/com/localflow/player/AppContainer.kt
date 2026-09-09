package com.localflow.player

import android.content.Context
import androidx.room.Room
import com.localflow.player.data.AppDatabase
import com.localflow.player.data.SettingsRepository
import com.localflow.player.playback.PlayerConnection
import com.localflow.player.repository.MediaStoreRepository

class AppContainer(context: Context) {
    val mediaRepository = MediaStoreRepository(context)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "localflow.db").fallbackToDestructiveMigration().build()
    val settings = SettingsRepository(context)
    val player = PlayerConnection(context)
}
class LocalFlowApp : android.app.Application() { lateinit var container: AppContainer; override fun onCreate() { super.onCreate(); container = AppContainer(this) } }
