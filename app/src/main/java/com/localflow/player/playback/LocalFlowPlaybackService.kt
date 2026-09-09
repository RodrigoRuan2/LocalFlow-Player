@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.localflow.player.playback

import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.*
import androidx.core.content.ContextCompat
import androidx.media3.common.*
import androidx.media3.common.util.BitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.google.common.util.concurrent.*
import com.localflow.player.MainActivity
import com.localflow.player.data.AppSettings
import com.localflow.player.data.SettingsRepository
import com.localflow.player.repository.ArtworkRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** The session owns playback, effects, background-video policy and the sleep timer. */
class LocalFlowPlaybackService : MediaSessionService() {
    private var session: MediaSession?=null
    private lateinit var player: ExoPlayer
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val handler=Handler(Looper.getMainLooper())
    private var preferences=AppSettings()
    private var videoVisible=false
    private var audioOnly=false
    private var timerEnd=0L
    private var finishTrack=false
    private var eq: Equalizer?=null
    private var eqEnabled=false
    private var desiredGains=emptyList<Int>()
    private var error: String?=null
    private var snapshotJob: Job?=null
    private var failureCount=0
    private var screenOn=true
    private val timerAction=Runnable { player.pause(); timerEnd=0; finishTrack=false; publishExtras(); saveSnapshot() }
    private val receiver=object:BroadcastReceiver() {
        override fun onReceive(context: Context,intent: Intent) {
            screenOn=intent.action!=Intent.ACTION_SCREEN_OFF
            applyVideoPolicy()
            if(!screenOn) saveSnapshot()
        }
    }
    override fun onCreate() {
        super.onCreate()
        screenOn=(getSystemService(POWER_SERVICE) as PowerManager).isInteractive
        player=ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true)
            setHandleAudioBecomingNoisy(true)
            setWakeMode(C.WAKE_MODE_LOCAL)
        }
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session=MediaSession.Builder(this,player).setSessionActivity(open).setBitmapLoader(object:BitmapLoader {
            override fun supportsMimeType(mimeType: String)=mimeType.startsWith("image/")
            override fun decodeBitmap(data: ByteArray):ListenableFuture<Bitmap> = bitmapFuture { ArtworkRepository.decode(data) }
            override fun loadBitmap(uri: Uri):ListenableFuture<Bitmap> = bitmapFuture { ArtworkRepository.load(applicationContext,uri,requested=512) }
        }).setCallback(object:MediaSession.Callback {
            override fun onConnect(session: MediaSession,controller: MediaSession.ControllerInfo):MediaSession.ConnectionResult {
                val result=super.onConnect(session,controller)
                if(controller.packageName!=packageName) return result
                return MediaSession.ConnectionResult.accept(result.availableSessionCommands.buildUpon().add(SessionCommand(PLAYBACK_OPTIONS,Bundle.EMPTY)).build(),result.availablePlayerCommands)
            }
            override fun onCustomCommand(session: MediaSession,controller: MediaSession.ControllerInfo,customCommand: SessionCommand,args: Bundle):ListenableFuture<SessionResult> {
                if(controller.packageName!=packageName || customCommand.customAction!=PLAYBACK_OPTIONS) return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
                if(args.containsKey("visible")) { videoVisible=args.getBoolean("visible"); if(!videoVisible) saveSnapshot() }
                if(args.getBoolean("checkpoint")) saveSnapshot()
                if(args.containsKey("audioOnly")) audioOnly=args.getBoolean("audioOnly")
                if(args.getBoolean("clearError")) { error=null; failureCount=0 }
                if(args.containsKey("timerMinutes")) {
                    handler.removeCallbacks(timerAction)
                    finishTrack=args.getBoolean("finishTrack")
                    player.pauseAtEndOfMediaItems=finishTrack
                    val minutes=args.getInt("timerMinutes").coerceIn(0,180)
                    timerEnd=if(minutes>0) SystemClock.elapsedRealtime()+minutes*60_000L else 0L
                    if(minutes>0) handler.postDelayed(timerAction,minutes*60_000L)
                }
                if(args.containsKey("eqEnabled")) {
                    eqEnabled=args.getBoolean("eqEnabled")
                    desiredGains=args.getIntArray("gains")?.toList().orEmpty()
                    applyEqualizer()
                }
                applyVideoPolicy(); publishExtras()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }).build()
        ContextCompat.registerReceiver(this,receiver,IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) },ContextCompat.RECEIVER_NOT_EXPORTED)
        player.addListener(object:Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                eq?.release(); eq=null
                if(audioSessionId!=C.AUDIO_SESSION_ID_UNSET) eq=runCatching { Equalizer(0,audioSessionId) }.getOrNull()
                applyEqualizer(); publishExtras()
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?,reason: Int) { applyVideoPolicy(); saveSnapshot() }
            override fun onPlaybackStateChanged(playbackState: Int) { if(playbackState==Player.STATE_READY) failureCount=0 }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean,reason: Int) {
                if(!playWhenReady) saveSnapshot()
                if(!playWhenReady && reason==Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && finishTrack) {
                    finishTrack=false; player.pauseAtEndOfMediaItems=false; publishExtras()
                }
                if(playWhenReady) applyVideoPolicy()
            }
            override fun onEvents(player: Player,events: Player.Events) {
                if(events.contains(Player.EVENT_TIMELINE_CHANGED)||events.contains(Player.EVENT_REPEAT_MODE_CHANGED)||events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) saveSnapshot()
            }
            override fun onPlayerError(exception: PlaybackException) {
                error="Não foi possível reproduzir este arquivo. Ele pode estar indisponível ou em um formato incompatível."
                publishExtras()
                failureCount++
                if(failureCount<minOf(player.mediaItemCount,5) && player.hasNextMediaItem()) {
                    handler.post { player.seekToNextMediaItem(); player.prepare(); player.play() }
                } else player.pause()
            }
        })
        scope.launch {
            val repo=SettingsRepository(applicationContext)
            preferences=repo.settings.first()
            restoreSnapshot()
            repo.settings.collect { settings ->
                preferences=settings; applyVideoPolicy()
                if(!settings.resumePlayback) withContext(Dispatchers.IO) { getSharedPreferences("playback",MODE_PRIVATE).edit().clear().commit() }
            }
        }
        publishExtras()
    }
    private fun bitmapFuture(load: suspend ()->Bitmap?):ListenableFuture<Bitmap> {
        val future=SettableFuture.create<Bitmap>()
        val job=scope.launch(Dispatchers.IO) {
            try { val value=load(); if(value==null) future.setException(IllegalArgumentException("Sem capa disponível")) else future.set(value) }
            catch(e: Exception) { future.setException(e) }
        }
        future.addListener({ if(future.isCancelled) job.cancel() },MoreExecutors.directExecutor())
        return future
    }
    private fun applyVideoPolicy() {
        if(!::player.isInitialized) return
        val video=player.currentMediaItem?.mediaId?.startsWith("VIDEO:")==true
        val visible=videoVisible && screenOn
        val policy=videoPolicy(video,visible,audioOnly,preferences.videoAudioBackground)
        val disabled=policy.disableVideo
        if(player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)!=disabled)
            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_VIDEO,disabled).build()
        if(policy.pause && player.playWhenReady) player.pause()
    }
    private fun applyEqualizer() {
        val effect=eq ?: return
        try {
            effect.enabled=eqEnabled
            val range=effect.bandLevelRange
            desiredGains.take(effect.numberOfBands.toInt()).forEachIndexed { i,g -> effect.setBandLevel(i.toShort(),g.coerceIn(range[0].toInt(),range[1].toInt()).toShort()) }
        } catch(e: Exception) { error="O equalizador não está disponível para esta saída de áudio."; eq?.release(); eq=null; eqEnabled=false }
    }
    private fun publishExtras() {
        val b=Bundle().apply {
            putBoolean("audioOnly",audioOnly); putLong("timerEnd",timerEnd); putBoolean("finishTrack",finishTrack)
            putString("error",error); putBoolean("eqAvailable",eq!=null); putBoolean("eqEnabled",eqEnabled)
            eq?.let { effect -> runCatching {
                val n=effect.numberOfBands.toInt()
                putIntArray("bands",IntArray(n) { effect.getCenterFreq(it.toShort())/1000 })
                putIntArray("gains",IntArray(n) { effect.getBandLevel(it.toShort()).toInt() })
                putInt("minGain",effect.bandLevelRange[0].toInt()); putInt("maxGain",effect.bandLevelRange[1].toInt())
            } }
        }
        session?.setSessionExtras(b)
    }
    private fun saveSnapshot() {
        if(!preferences.resumePlayback || !::player.isInitialized) return
        val count=player.mediaItemCount
        if(count==0) return
        val start=(player.currentMediaItemIndex-100).coerceAtLeast(0)
        val end=minOf(count,start+500)
        val items=(start until end).map { player.getMediaItemAt(it) }
        val index=(player.currentMediaItemIndex-start).coerceAtLeast(0)
        val position=player.currentPosition
        val repeat=player.repeatMode
        val shuffle=player.shuffleModeEnabled
        snapshotJob?.cancel()
        snapshotJob=scope.launch(Dispatchers.IO) {
            val a=JSONArray()
            items.forEach { m -> a.put(JSONObject().put("id",m.mediaId).put("uri",m.localConfiguration?.uri?.toString()).put("title",m.mediaMetadata.title).put("artist",m.mediaMetadata.artist)) }
            val json=JSONObject().put("items",a).put("index",index).put("position",position).put("repeat",repeat).put("shuffle",shuffle)
            ensureActive()
            runCatching { getSharedPreferences("playback",MODE_PRIVATE).edit().putString("snapshot",json.toString()).commit() }
        }
    }
    private suspend fun restoreSnapshot() {
        if(!preferences.resumePlayback) return
        val saved=withContext(Dispatchers.IO) { getSharedPreferences("playback",MODE_PRIVATE).getString("snapshot",null) } ?: return
        if(player.mediaItemCount>0) return
        runCatching {
            val json=JSONObject(saved); val a=json.getJSONArray("items")
            val list=(0 until a.length()).map { i ->
                val v=a.getJSONObject(i); val uri=Uri.parse(v.getString("uri"))
                MediaItem.Builder().setMediaId(v.getString("id")).setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(v.optString("title")).setArtist(v.optString("artist")).setArtworkUri(uri).build()).build()
            }
            if(list.isNotEmpty()) { player.setMediaItems(list,json.optInt("index").coerceIn(list.indices),json.optLong("position").coerceAtLeast(0)); player.repeatMode=json.optInt("repeat").coerceIn(0,2); player.shuffleModeEnabled=json.optBoolean("shuffle") }
        }
    }
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo):MediaSession?=session
    override fun onTaskRemoved(rootIntent: Intent?) { saveSnapshot(); if(!player.playWhenReady) stopSelf() }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(receiver) }
        eq?.release(); eq=null
        session?.release(); session=null
        if(::player.isInitialized) player.release()
        scope.cancel()
        super.onDestroy()
    }
}
