@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.localflow.player.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.*
import com.localflow.player.data.FavoriteEntity
import com.localflow.player.data.PlaylistEntity
import com.localflow.player.model.LocalMedia
import com.localflow.player.playback.PlayerConnection
import com.localflow.player.ui.theme.LocalFlowTheme
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(20.dp)

@Composable fun LocalFlowRoot(vm: LibraryViewModel, player: PlayerConnection, permissions: Array<String>) {
    val nav = rememberNavController(); val context = androidx.compose.ui.platform.LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val launch = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.refresh() }
    val allowed = permissions.filterNot { it.endsWith("POST_NOTIFICATIONS") }.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    LaunchedEffect(allowed) { if (allowed) vm.refresh() }
    LocalFlowTheme(settings.theme) { NavHost(nav, "home") {
        composable("home") { if (allowed) Home(vm, player, { nav.navigate("audio") }, { nav.navigate("video") }, { nav.navigate("settings") }) else PermissionPage { launch.launch(permissions) } }
        composable("audio") { AudioPage(player) { nav.popBackStack() } }
        composable("video") { VideoPage(player) { nav.popBackStack() } }
        composable("settings") { SettingsPage(vm) { nav.popBackStack() } }
    } }
}

@Composable private fun PermissionPage(request: () -> Unit) = Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
    Surface(Modifier.size(76.dp), shape = CardShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LibraryMusic, null, Modifier.size(40.dp)) } }
    Spacer(Modifier.height(28.dp)); Text("Sua mídia. Seu ritmo.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp)); Text("O LocalFlow acessa somente músicas e vídeos já salvos no aparelho.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(28.dp)); Button(request, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Acessar biblioteca") }
}

@Composable private fun Home(vm: LibraryViewModel, player: PlayerConnection, audio: () -> Unit, video: () -> Unit, settings: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle(); val fav by vm.favorites.collectAsStateWithLifecycle(); val playlists by vm.playlists.collectAsStateWithLifecycle(); val now by player.state.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableIntStateOf(0) }; var search by rememberSaveable { mutableStateOf("") }; var newPlaylist by remember { mutableStateOf("") }
    val nav = listOf("Músicas", "Vídeos", "Pastas", "Playlists")
    Scaffold(bottomBar = { Column { MiniPlayer(player, if (now.mediaId?.startsWith("VIDEO:") == true) video else audio); NavigationBar { nav.forEachIndexed { i, label -> NavigationBarItem(i == page, { page = i }, icon = { Icon(listOf(Icons.Default.MusicNote, Icons.Default.VideoLibrary, Icons.Default.Folder, Icons.Default.QueueMusic)[i], null) }, label = { Text(label) }) } } } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("LocalFlow", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Biblioteca local", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(settings) { Icon(Icons.Default.Settings, "Configurações") } }
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Pesquisar na biblioteca") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (search.isNotEmpty()) IconButton({ search = "" }) { Icon(Icons.Default.Close, "Limpar") } }, singleLine = true, shape = RoundedCornerShape(18.dp))
            if (state.loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() } else when (page) {
                0 -> MediaList(state.songs.search(search), fav, playlists, vm, { item, items -> vm.play(item, items); audio() }, "Nenhuma música encontrada")
                1 -> MediaList(state.videos.search(search), fav, playlists, vm, { item, items -> vm.play(item, items); video() }, "Nenhum vídeo encontrado")
                2 -> Folders(state, vm, audio, video)
                else -> Playlists(playlists, newPlaylist, { newPlaylist = it }, vm)
            }
        }
    }
}
private fun List<LocalMedia>.search(term: String) = if (term.isBlank()) this else filter { it.title.contains(term, true) || it.artist.contains(term, true) || (it.album?.contains(term, true) == true) }

@Composable private fun MediaList(items: List<LocalMedia>, favorites: List<FavoriteEntity>, playlists: List<PlaylistEntity>, vm: LibraryViewModel, play: (LocalMedia, List<LocalMedia>) -> Unit, empty: String) {
    var adding by remember { mutableStateOf<LocalMedia?>(null) }
    if (items.isEmpty()) EmptyState(empty) else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { items(items, key = { "${it.kind}:${it.id}" }) { media -> MediaCard(media, favorites.any { it.mediaId == media.id && it.kind == media.kind.name }, { play(media, items) }, { vm.toggleFavorite(media) }, { adding = media }) } }
    adding?.let { media -> AlertDialog(onDismissRequest = { adding = null }, icon = { Icon(Icons.Default.PlaylistAdd, null) }, title = { Text("Adicionar à playlist") }, text = { if (playlists.isEmpty()) Text("Crie uma playlist primeiro.") else Column { playlists.forEach { p -> TextButton({ vm.addToPlaylist(p.id, media); adding = null }, Modifier.fillMaxWidth()) { Text(p.name) } } } }, confirmButton = { TextButton({ adding = null }) { Text("Fechar") } }) }
}
@Composable private fun MediaCard(item: LocalMedia, favorite: Boolean, play: () -> Unit, toggle: () -> Unit, add: () -> Unit) = Card(onClick = play, shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { MediaThumbnail(item); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold); Text("${item.artist}  •  ${time(item.durationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }; IconButton(add) { Icon(Icons.Default.PlaylistAdd, "Playlist") }; IconButton(toggle) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current) } } }
@Composable private fun EmptyState(text: String) = Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Surface(Modifier.size(64.dp), shape = CardShape, color = MaterialTheme.colorScheme.secondaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.LibraryMusic, null) } }; Spacer(Modifier.height(14.dp)); Text(text, style = MaterialTheme.typography.titleMedium); Text("Adicione arquivos ou use a busca.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun Folders(state: LibraryState, vm: LibraryViewModel, audio: () -> Unit, video: () -> Unit) = LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(state.folders, key = { it.name }) { folder -> Card(onClick = { val list = (state.songs + state.videos).filter { it.folder == folder.name }; vm.play(list.first(), list); if (list.first().kind.name == "VIDEO") video() else audio() }, shape = CardShape) { ListItem(leadingContent = { Surface(Modifier.size(46.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, null) } } }, headlineContent = { Text(folder.name, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text("${folder.count} arquivos") }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }) } } }
@Composable private fun Playlists(items: List<PlaylistEntity>, name: String, nameChanged: (String) -> Unit, vm: LibraryViewModel) = Column(Modifier.padding(horizontal = 16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(name, nameChanged, Modifier.weight(1f), placeholder = { Text("Nova playlist") }, singleLine = true, shape = RoundedCornerShape(16.dp)); Spacer(Modifier.width(8.dp)); FilledIconButton({ vm.createPlaylist(name); nameChanged("") }, Modifier.size(52.dp)) { Icon(Icons.Default.Add, "Criar") } }; Spacer(Modifier.height(12.dp)); if (items.isEmpty()) EmptyState("Crie sua primeira playlist") else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) { items(items, key = { it.id }) { p -> Card(shape = CardShape) { ListItem(leadingContent = { Icon(Icons.Default.QueueMusic, null) }, headlineContent = { Text(p.name, fontWeight = FontWeight.SemiBold) }, trailingContent = { IconButton({ vm.deletePlaylist(p.id) }) { Icon(Icons.Default.DeleteOutline, "Excluir") } }) } } } }

@Composable private fun MiniPlayer(player: PlayerConnection, open: () -> Unit) { val s by player.state.collectAsStateWithLifecycle(); if (s.mediaId != null) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) { Row(Modifier.height(70.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(44.dp), shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primary) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onPrimary) } }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f).clickable(onClick = open)) { Text(s.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(s.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1) }; IconButton(player::toggle) { Icon(if (s.playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, Modifier.size(36.dp)) } } } }

@Composable private fun AudioPage(player: PlayerConnection, back: () -> Unit) { val s by player.state.collectAsStateWithLifecycle(); Tick(player); Scaffold(topBar = { TopAppBar(title = { Text("Tocando agora") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { pad -> Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Spacer(Modifier.height(20.dp)); Surface(Modifier.fillMaxWidth().aspectRatio(1f), color = MaterialTheme.colorScheme.primaryContainer, shape = CardShape) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, Modifier.size(116.dp), tint = MaterialTheme.colorScheme.primary) } }; Spacer(Modifier.height(26.dp)); Text(s.title.ifBlank { "Nenhuma mídia selecionada" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(s.artist, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(16.dp)); Slider((s.position.toFloat() / s.duration.coerceAtLeast(1)).coerceIn(0f, 1f), { player.seek((it * s.duration).toLong()) }); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(time(s.position)); Text(time(s.duration)) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { IconButton(player::cycleRepeat) { Icon(Icons.Default.Repeat, null, tint = if (s.repeatMode == Player.REPEAT_MODE_OFF) LocalContentColor.current else MaterialTheme.colorScheme.primary) }; IconButton(player::previous) { Icon(Icons.Default.SkipPrevious, null, Modifier.size(32.dp)) }; FilledIconButton(player::toggle, Modifier.size(68.dp)) { Icon(if (s.playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, Modifier.size(36.dp)) }; IconButton(player::next) { Icon(Icons.Default.SkipNext, null, Modifier.size(32.dp)) }; IconButton({ player.setShuffle(!s.shuffle) }) { Icon(Icons.Default.Shuffle, null, tint = if (s.shuffle) MaterialTheme.colorScheme.primary else LocalContentColor.current) } } } } }
@Composable private fun VideoPage(player: PlayerConnection, back: () -> Unit) { val p = player.controllerOrNull(); DisposableEffect(p) { onDispose { p?.clearVideoSurface() } }; Scaffold(topBar = { TopAppBar(title = { Text("Vídeo") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { pad -> Column(Modifier.fillMaxSize().padding(pad)) { if (p != null) AndroidView(factory = { PlayerView(it).apply { this.player = p; useController = true } }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), update = { it.player = p }) else Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), Alignment.Center) { CircularProgressIndicator() }; Card(Modifier.padding(16.dp), shape = CardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Headphones, null); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Modo somente áudio", fontWeight = FontWeight.SemiBold); Text("Desliga a imagem para economizar bateria.", style = MaterialTheme.typography.bodySmall) }; IconButton({ p?.clearVideoSurface() }) { Icon(Icons.Default.VisibilityOff, "Somente áudio") } } } } } }
@Composable private fun SettingsPage(vm: LibraryViewModel, back: () -> Unit) { val s by vm.settings.collectAsStateWithLifecycle(); Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }, navigationIcon = { IconButton(back) { Icon(Icons.Default.ArrowBack, "Voltar") } }) }) { pad -> Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { Text("Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Row { com.localflow.player.data.ThemeMode.entries.forEach { mode -> FilterChip(s.theme == mode, { vm.setTheme(mode) }, label = { Text(when (mode) { com.localflow.player.data.ThemeMode.SYSTEM -> "Sistema"; com.localflow.player.data.ThemeMode.LIGHT -> "Claro"; else -> "Escuro" }) }, modifier = Modifier.padding(end = 6.dp)) } }; Divider(); Setting("Áudio de vídeos em segundo plano", "Continue ouvindo ao fechar o vídeo", s.videoAudioBackground, vm::setVideoAudioBackground); Setting("Retomar reprodução", "Volta à última mídia quando disponível", s.resumePlayback, vm::setResumePlayback) } } }
@Composable private fun Setting(title: String, text: String, checked: Boolean, change: (Boolean) -> Unit) = Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked, change) }
@Composable private fun Tick(player: PlayerConnection) { LaunchedEffect(player) { while (true) { delay(750); player.refreshPosition() } } }
private fun time(ms: Long): String { val s = (ms / 1000).coerceAtLeast(0); return "%d:%02d".format(s / 60, s % 60) }
