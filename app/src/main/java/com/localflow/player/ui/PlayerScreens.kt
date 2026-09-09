@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.localflow.player.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.localflow.player.model.*
import com.localflow.player.playback.*
import kotlinx.coroutines.delay

@Composable fun MiniPlayer(player: PlayerConnection,open: ()->Unit) {
    val s by player.state.collectAsStateWithLifecycle()
    if(s.mediaId==null) return
    Surface(Modifier.fillMaxWidth().padding(horizontal=10.dp).clickable(onClick=open),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.secondaryContainer) {
        Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
            Artwork(s.uri,s.artwork,s.title,s.mediaId?.startsWith("VIDEO:")==true,Modifier.size(42.dp))
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                Text(s.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyMedium)
                Text(s.artist,maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(player::toggle) { Icon(if(s.playing) Icons.Default.Pause else Icons.Default.PlayArrow,if(s.playing) "Pausar" else "Reproduzir") }
            IconButton(player::next) { Icon(Icons.Default.SkipNext,"Próxima") }
        }
    }
}
@Composable private fun PlaybackProgress(player: PlayerConnection,s: PlayerState) {
    var drag by remember(s.mediaId) { mutableStateOf<Float?>(null) }
    Slider(value=drag ?: s.position.toFloat().coerceIn(0f,s.duration.coerceAtLeast(1).toFloat()),onValueChange={ drag=it },onValueChangeFinished={ drag?.let { player.seek(it.toLong()) }; drag=null },valueRange=0f..s.duration.coerceAtLeast(1).toFloat(),enabled=s.duration>0)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(formatTime(drag?.toLong() ?: s.position),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatTime(s.duration),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable private fun Transport(player: PlayerConnection,s: PlayerState) {
    Row(Modifier.fillMaxWidth().padding(vertical=18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
        IconButton({ player.setShuffle(!s.shuffle) }) { Icon(Icons.Default.Shuffle,if(s.shuffle) "Desativar aleatório" else "Ativar aleatório",tint=if(s.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        IconButton(player::previous) { Icon(Icons.Default.SkipPrevious,"Anterior",Modifier.size(32.dp)) }
        FilledIconButton(player::toggle,Modifier.size(68.dp),enabled=s.mediaId!=null) { Icon(if(s.playing) Icons.Default.Pause else Icons.Default.PlayArrow,if(s.playing) "Pausar" else "Reproduzir",Modifier.size(36.dp)) }
        IconButton(player::next) { Icon(Icons.Default.SkipNext,"Próxima",Modifier.size(32.dp)) }
        IconButton(player::cycleRepeat) {
            Icon(if(s.repeatMode==Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,"Repetir: "+when(s.repeatMode) { Player.REPEAT_MODE_OFF->"desligado"; Player.REPEAT_MODE_ONE->"uma faixa"; else->"todas" },tint=if(s.repeatMode==Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable fun AudioPage(player: PlayerConnection,library: List<LocalMedia>,favorites: Set<String>,toggle: (LocalMedia)->Unit,back: ()->Unit,queue: ()->Unit,sound: ()->Unit) {
    val s by player.state.collectAsStateWithLifecycle()
    val q by player.queue.collectAsStateWithLifecycle()
    val item=remember(s.mediaId,library) { library.find { it.key==s.mediaId } }
    Tick(player)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Tocando agora",if(q.isEmpty()) "Selecione uma mídia" else "Sua fila · "+q.size+" arquivos",back)
        Column(Modifier.padding(start=24.dp,end=24.dp,bottom=20.dp)) {
            Artwork(s.uri,item?.artworkUri ?: s.artwork,s.title,s.mediaId?.startsWith("VIDEO:")==true,Modifier.fillMaxWidth().aspectRatio(1f),large=true)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.title.ifBlank { "Sua próxima música" },style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                    Text(s.artist.ifBlank { "Escolha na biblioteca" },color=MaterialTheme.colorScheme.onSurfaceVariant)
                    item?.album?.let { Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                IconButton({ item?.let(toggle) },enabled=item!=null) { Icon(if(s.mediaId in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,"Favoritar",tint=MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(12.dp)); PlaybackProgress(player,s); Transport(player,s)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                TextButton(queue) { Icon(Icons.AutoMirrored.Filled.QueueMusic,null); Spacer(Modifier.width(5.dp)); Text("Fila") }
                TextButton(sound) { Icon(Icons.Default.Tune,null); Spacer(Modifier.width(5.dp)); Text("Áudio / timer") }
            }
        }
    }
}
private fun Context.activity(): Activity? = when(this) { is Activity->this; is ContextWrapper->baseContext.activity(); else->null }

@Composable fun VideoPage(player: PlayerConnection,background: Boolean,setBackground: (Boolean)->Unit,back: ()->Unit,sound: ()->Unit) {
    val s by player.state.collectAsStateWithLifecycle()
    val options by player.options.collectAsStateWithLifecycle()
    val p=if(s.connected) player.controllerOrNull() else null
    val context=LocalContext.current
    val activity=context.activity()
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    var view by remember { mutableStateOf<PlayerView?>(null) }
    val onlyAudio by rememberUpdatedState(options.audioOnly)
    var visible by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var landscape by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(p,lifecycle) {
        if(p!=null) player.videoVisible(true)
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_START) { visible=true; player.videoVisible(true); if(!onlyAudio) view?.player=p }
            if(event==Lifecycle.Event.ON_STOP) {
                visible=false; view?.player=null
                if(activity?.isChangingConfigurations!=true) player.videoVisible(false)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer); view?.player=null
            if(activity?.isChangingConfigurations!=true) player.videoVisible(false)
        }
    }
    DisposableEffect(activity) {
        onDispose { if(activity?.isChangingConfigurations!=true) activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Tick(player)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Vídeo",s.title,back,actions={ IconButton({ landscape=!landscape; activity?.requestedOrientation=if(landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }) { Icon(Icons.Default.Fullscreen,"Alternar orientação") } })
        if(p!=null && !options.audioOnly) AndroidView(
            factory={ PlayerView(it).apply { this.player=p; useController=false; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); view=this } },
            modifier=Modifier.fillMaxWidth().aspectRatio(16f/9f),
            onRelease={ it.player=null; if(view===it) view=null },
            update={ it.player=if(visible) p else null; it.keepScreenOn=visible && s.playing }
        ) else Box(Modifier.fillMaxWidth().aspectRatio(16f/9f),Alignment.Center) {
            if(p==null) CircularProgressIndicator() else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.Headphones,null,Modifier.size(64.dp),tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp)); Text("Somente áudio",style=MaterialTheme.typography.titleLarge)
            }
        }
        Column(Modifier.padding(20.dp)) {
            Text(s.title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
            PlaybackProgress(player,s)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                IconButton({ player.step(-10_000) }) { Icon(Icons.Default.Replay10,"Voltar 10 segundos") }
                FilledIconButton(player::toggle,Modifier.size(58.dp),enabled=p!=null) { Icon(if(s.playing) Icons.Default.Pause else Icons.Default.PlayArrow,if(s.playing) "Pausar" else "Reproduzir") }
                IconButton({ player.step(10_000) }) { Icon(Icons.Default.Forward10,"Avançar 10 segundos") }
            }
            Spacer(Modifier.height(14.dp))
            PreferenceRow("Somente áudio","Desligue a imagem; volte ao vídeo quando quiser.",options.audioOnly,player::audioOnly)
            PreferenceRow("Continuar em segundo plano","Ao sair do vídeo ou bloquear a tela.",background,setBackground)
            Hint(if(options.audioOnly) "Imagem desligada. A faixa de áudio continua sendo a do vídeo original." else "Vídeo ativo. O modo somente áudio depende da sua escolha.")
            TextButton(sound,Modifier.fillMaxWidth()) { Icon(Icons.Default.Tune,null); Text("Equalizador e temporizador") }
        }
    }
}
@Composable fun QueuePage(player: PlayerConnection,back: ()->Unit,open: (String)->Unit) {
    val queue by player.queue.collectAsStateWithLifecycle()
    val now by player.state.collectAsStateWithLifecycle()
    Column {
        PageHeader("Fila de reprodução",queue.size.toString()+" arquivos",back)
        LazyColumn(contentPadding=PaddingValues(20.dp)) {
            if(queue.isEmpty()) item { EmptyContent("A fila está vazia","Escolha uma música, vídeo ou playlist para começar.") }
            items(queue,key={ it.id+":"+it.index }) { m ->
                Card(Modifier.fillMaxWidth().padding(bottom=8.dp),colors=CardDefaults.cardColors(containerColor=if(m.id==now.mediaId) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(Modifier.fillMaxWidth().clickable { player.playQueue(m.index); open(m.id) },verticalAlignment=Alignment.CenterVertically) {
                            Artwork(m.uri,m.artwork,m.title,m.id.startsWith("VIDEO:"),Modifier.size(44.dp)); Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium)
                                Text(if(m.id==now.mediaId) "Tocando agora" else m.artist,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton({ player.remove(m.index) }) { Icon(Icons.Default.Close,"Remover da fila") }
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                            IconButton({ player.move(m.index,m.index-1) },enabled=m.index>0) { Icon(Icons.Default.KeyboardArrowUp,"Mover para cima") }
                            IconButton({ player.move(m.index,m.index+1) },enabled=m.index<queue.lastIndex) { Icon(Icons.Default.KeyboardArrowDown,"Mover para baixo") }
                        }
                    }
                }
            }
        }
    }
}
@Composable fun SoundPage(player: PlayerConnection,back: ()->Unit) {
    val options by player.options.collectAsStateWithLifecycle()
    var gains by remember(options.gains) { mutableStateOf(options.gains) }
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    var time by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(options.timerEnd,lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(options.timerEnd>0) { time=SystemClock.elapsedRealtime(); delay(1000) }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader("Ajustes de escuta","Equalizador e timer",back)
        Column(Modifier.padding(horizontal=20.dp)) {
            SectionTitle("Equalizador")
            if(!options.eqAvailable) Hint("Inicie uma reprodução para ativar o equalizador. Se esta saída de áudio não oferecer suporte, ele ficará indisponível.")
            else {
                PreferenceRow("Ativar equalizador","Ajusta apenas o áudio do LocalFlow.",options.eqEnabled,{ player.equalizer(it,gains) })
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Normal","Graves","Voz").forEach { preset ->
                        OutlinedButton({
                            gains=options.bands.map { hz -> when(preset) { "Graves"->if(hz<300) 400 else 0; "Voz"->if(hz in 600..4000) 300 else -100; else->0 } }
                            player.equalizer(true,gains)
                        }) { Text(preset) }
                    }
                }
                options.bands.forEachIndexed { i,hz ->
                    val gain=gains.getOrElse(i) { 0 }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(hz>=1000) "%.1f kHz".format(hz/1000f) else hz.toString()+" Hz",Modifier.width(65.dp),style=MaterialTheme.typography.labelMedium)
                        Slider(gain.toFloat(),{ value -> gains=gains.toMutableList().also { if(i in it.indices) it[i]=value.toInt() } },Modifier.weight(1f),enabled=options.eqEnabled,valueRange=options.minGain.toFloat()..options.maxGain.toFloat(),onValueChangeFinished={ player.equalizer(options.eqEnabled,gains) })
                        Text("%.1f dB".format(gain/100f),Modifier.width(56.dp),style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
            SectionTitle("Temporizador de sono")
            Text(if(options.finishTrack) "Fim da faixa" else if(options.timerEnd>0) formatTime(options.timerEnd-time) else minutes.toString()+" min",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) { listOf(15,30,60).forEach { n -> FilterChip(minutes==n,{ minutes=n },label={ Text(n.toString()+" min") }) } }
            Button({ player.timer(minutes) },Modifier.fillMaxWidth()) { Icon(Icons.Default.Bedtime,null); Spacer(Modifier.width(8.dp)); Text("Ativar temporizador") }
            OutlinedButton({ player.timer(0,true) },Modifier.fillMaxWidth()) { Text("Pausar ao terminar a faixa") }
            if(options.timerEnd>0 || options.finishTrack) TextButton({ player.timer(0) },Modifier.fillMaxWidth()) { Text("Cancelar temporizador") }
            Spacer(Modifier.height(12.dp)); Hint("A reprodução pausa ao terminar o tempo, inclusive com a tela bloqueada.")
            Spacer(Modifier.height(24.dp))
        }
    }
}
@Composable private fun Tick(player: PlayerConnection) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(player,lifecycle) { lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while(true) { player.refreshPosition(); delay(750) } } }
}
