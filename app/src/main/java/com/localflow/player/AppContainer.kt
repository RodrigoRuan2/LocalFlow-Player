package com.localflow.player

import android.content.Context
import androidx.room.Room
import com.localflow.player.data.AppDatabase
import com.localflow.player.data.SettingsRepository
import com.localflow.player.playback.PlayerConnection
import com.localflow.player.repository.MediaStoreRepository

class AppContainer(context: Context) {
    val mediaRepository = MediaStoreRepository(context)
    val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "localflow.db").build()
    val settings = SettingsRepository(context)
    val player = PlayerConnection(context)
}
class LocalFlowApp : android.app.Application() {
    lateinit var container: AppContainer
    override fun onCreate() { super.onCreate(); container = AppContainer(this) }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if(level>=TRIM_MEMORY_RUNNING_LOW) com.localflow.player.repository.ArtworkRepository.clear()
    }
}
