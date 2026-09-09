package com.localflow.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.localflow.player.model.LocalMedia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(val mediaId: String? = null, val title: String = "", val artist: String = "", val duration: Long = 0, val position: Long = 0, val playing: Boolean = false, val repeatMode: Int = Player.REPEAT_MODE_OFF, val shuffle: Boolean = false)

class PlayerConnection(context: Context) : Player.Listener, AutoCloseable {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    private var controller: MediaController? = null
    private val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, LocalFlowPlaybackService::class.java))).buildAsync()
    init { future.addListener({ runCatching { future.get() }.onSuccess { controller = it.also { c -> c.addListener(this); publish(c) } } }, MoreExecutors.directExecutor()) }
    fun play(items: List<LocalMedia>, startIndex: Int = 0) { controller?.let { p -> p.setMediaItems(items.map(::asMedia3), startIndex, 0); p.prepare(); p.play() } }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seek(position: Long) = controller?.seekTo(position)
    fun setShuffle(value: Boolean) { controller?.shuffleModeEnabled = value }
    fun cycleRepeat() { controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } } }
    fun controllerOrNull(): Player? = controller
    fun refreshPosition() { controller?.let(::publish) }
    private fun asMedia3(item: LocalMedia) = MediaItem.Builder().setMediaId("${item.kind}:${item.id}").setUri(item.uri).setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist).setAlbumTitle(item.album).setIsPlayable(true).build()).build()
    private fun publish(p: Player) { val m = p.currentMediaItem; _state.value = PlayerState(m?.mediaId, m?.mediaMetadata?.title?.toString().orEmpty(), m?.mediaMetadata?.artist?.toString().orEmpty(), p.duration.coerceAtLeast(0), p.currentPosition.coerceAtLeast(0), p.isPlaying, p.repeatMode, p.shuffleModeEnabled) }
    override fun onEvents(player: Player, events: Player.Events) = publish(player)
    override fun close() { controller?.removeListener(this); controller?.release(); controller = null; if (!future.isDone) future.cancel(false) }
}
