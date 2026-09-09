@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.localflow.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.session.*
import com.google.common.util.concurrent.ListenableFuture
import com.localflow.player.model.*
import kotlinx.coroutines.flow.*

const val PLAYBACK_OPTIONS = "com.localflow.player.OPTIONS"
data class PlayerState(val mediaId: String? = null, val title: String = "", val artist: String = "", val duration: Long = 0, val position: Long = 0, val playing: Boolean = false, val repeatMode: Int = Player.REPEAT_MODE_OFF, val shuffle: Boolean = false, val uri: Uri? = null, val artwork: Uri? = null, val connected: Boolean = false)
data class QueueEntry(val id: String,val title: String,val artist: String,val uri: Uri?,val artwork: Uri?,val index: Int)
data class PlaybackOptions(val audioOnly: Boolean=false,val timerEnd: Long=0,val finishTrack: Boolean=false,val eqAvailable: Boolean=false,val eqEnabled: Boolean=false,val bands: List<Int> = emptyList(),val gains: List<Int> = emptyList(),val minGain: Int=-1500,val maxGain: Int=1500,val error: String?=null)

class PlayerConnection(context: Context) : Player.Listener, AutoCloseable {
    private val appContext=context.applicationContext
    private val executor=ContextCompat.getMainExecutor(appContext)
    private val mutableState=MutableStateFlow(PlayerState())
    val state=mutableState.asStateFlow()
    private val mutableQueue=MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue=mutableQueue.asStateFlow()
    private val mutableOptions=MutableStateFlow(PlaybackOptions())
    val options=mutableOptions.asStateFlow()
    private var controller: MediaController?=null
    private var pending: Pair<List<LocalMedia>,Int>?=null
    private var future: ListenableFuture<MediaController>?=null
    init { connect() }
    fun connect() {
        if(controller!=null || future?.isDone==false) return
        val connection=MediaController.Builder(appContext,SessionToken(appContext,ComponentName(appContext,LocalFlowPlaybackService::class.java)))
        .setListener(object:MediaController.Listener {
            override fun onExtrasChanged(controller: MediaController, extras: Bundle) { readOptions(extras) }
            override fun onDisconnected(controller: MediaController) {
                controller.removeListener(this@PlayerConnection)
                this@PlayerConnection.controller=null; future=null
                mutableState.value=mutableState.value.copy(connected=false,playing=false)
            }
        }).buildAsync()
        future=connection
        connection.addListener({
            runCatching { connection.get() }.onSuccess { p ->
                controller=p; p.addListener(this); publish(p); publishQueue(p); readOptions(p.sessionExtras)
                pending?.let { play(it.first,it.second); pending=null }
            }.onFailure { mutableOptions.value=mutableOptions.value.copy(error="Não foi possível conectar ao player. Feche e reabra o aplicativo.") }
        },executor)
    }
    fun play(items: List<LocalMedia>,startIndex: Int=0) {
        if(items.isEmpty()) return
        val p=controller
        if(p==null) { pending=items to startIndex; connect(); return }
        command(Bundle().apply { putBoolean("visible",items[startIndex.coerceIn(items.indices)].kind==MediaKind.VIDEO); putBoolean("audioOnly",false); putBoolean("clearError",true) })
        p.setMediaItems(items.map { item ->
            MediaItem.Builder().setMediaId(item.key).setUri(item.uri).setMediaMetadata(MediaMetadata.Builder()
                .setTitle(item.title).setArtist(item.artist).setAlbumTitle(item.album)
                .setArtworkUri(item.uri).setIsPlayable(true).build()).build()
        },startIndex.coerceIn(items.indices),0)
        p.prepare(); p.play()
    }
    fun toggle() { controller?.let { if(it.isPlaying) it.pause() else { if(it.playbackState==Player.STATE_IDLE) it.prepare(); it.play() } } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPrevious() }
    fun seek(position: Long) { controller?.seekTo(position.coerceAtLeast(0)) }
    fun step(delta: Long) { controller?.let { seek((it.currentPosition+delta).coerceAtMost(it.duration.coerceAtLeast(0))) } }
    fun setShuffle(value: Boolean) { controller?.shuffleModeEnabled=value }
    fun cycleRepeat() { controller?.let { it.repeatMode=when(it.repeatMode) { Player.REPEAT_MODE_OFF->Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL->Player.REPEAT_MODE_ONE; else->Player.REPEAT_MODE_OFF } } }
    fun playQueue(index: Int) { controller?.let { if(index in 0 until it.mediaItemCount) { it.seekTo(index,0); it.prepare(); it.play() } } }
    fun move(from: Int,to: Int) { controller?.let { if(from in 0 until it.mediaItemCount && to in 0 until it.mediaItemCount) it.moveMediaItem(from,to) } }
    fun remove(index: Int) { controller?.let { if(index in 0 until it.mediaItemCount) it.removeMediaItem(index) } }
    fun controllerOrNull(): Player?=controller
    fun refreshPosition() { controller?.let(::publish) }
    fun videoVisible(value: Boolean) = command(Bundle().apply { putBoolean("visible",value) })
    fun checkpoint() = command(Bundle().apply { putBoolean("checkpoint",true) })
    fun audioOnly(value: Boolean) = command(Bundle().apply { putBoolean("audioOnly",value) })
    fun timer(minutes: Int,finishTrack: Boolean=false) = command(Bundle().apply { putInt("timerMinutes",minutes); putBoolean("finishTrack",finishTrack) })
    fun equalizer(enabled: Boolean, gains: List<Int>) = command(Bundle().apply { putBoolean("eqEnabled",enabled); putIntArray("gains",gains.toIntArray()) })
    fun clearError() = command(Bundle().apply { putBoolean("clearError",true) })
    private fun command(args: Bundle) { controller?.sendCustomCommand(SessionCommand(PLAYBACK_OPTIONS,Bundle.EMPTY),args) }
    private fun publish(p: Player) {
        val item=p.currentMediaItem
        mutableState.value=PlayerState(item?.mediaId,item?.mediaMetadata?.title?.toString().orEmpty(),item?.mediaMetadata?.artist?.toString().orEmpty(),p.duration.coerceAtLeast(0),p.currentPosition.coerceAtLeast(0),p.isPlaying,p.repeatMode,p.shuffleModeEnabled,item?.localConfiguration?.uri,item?.mediaMetadata?.artworkUri,true)
    }
    private fun publishQueue(p: Player) {
        mutableQueue.value=(0 until p.mediaItemCount).map { i -> val m=p.getMediaItemAt(i); QueueEntry(m.mediaId,m.mediaMetadata.title.toString(),m.mediaMetadata.artist?.toString().orEmpty(),m.localConfiguration?.uri,m.mediaMetadata.artworkUri,i) }
    }
    private fun readOptions(b: Bundle) {
        mutableOptions.value=PlaybackOptions(b.getBoolean("audioOnly"),b.getLong("timerEnd"),b.getBoolean("finishTrack"),b.getBoolean("eqAvailable"),b.getBoolean("eqEnabled"),b.getIntArray("bands")?.toList().orEmpty(),b.getIntArray("gains")?.toList().orEmpty(),b.getInt("minGain",-1500),b.getInt("maxGain",1500),b.getString("error"))
    }
    override fun onEvents(player: Player,events: Player.Events) {
        publish(player)
        if(events.contains(Player.EVENT_TIMELINE_CHANGED)||events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) publishQueue(player)
    }
    override fun close() { controller?.removeListener(this); future?.let { MediaController.releaseFuture(it) }; future=null; controller=null }
}
