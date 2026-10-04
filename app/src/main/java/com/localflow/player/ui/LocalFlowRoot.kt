package com.localflow.player.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.localflow.player.data.PlaylistEntity
import com.localflow.player.model.*
import com.localflow.player.playback.PlayerConnection

@Composable fun LocalFlowRoot(vm: LibraryViewModel,player: PlayerConnection,permissions: Array<String>) {
    val context=LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val library by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val collections by vm.collections.collectAsStateWithLifecycle()
    val favoriteKeys=remember(favorites) { favorites.map { it.kind+":"+it.mediaId }.toSet() }
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "home"
    val snackbar=remember { SnackbarHostState() }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var skipPermission by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf<LocalMedia?>(null) }
    var addingMany by remember { mutableStateOf<List<LocalMedia>?>(null) }
    var pendingFileOperation by remember { mutableStateOf<MediaFileOperation?>(null) }
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionVersion++; vm.refresh() }
    val fileOperationLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val operation=pendingFileOperation
        pendingFileOperation=null
        if(operation==null) return@rememberLauncherForActivityResult
        if(result.resultCode!=Activity.RESULT_OK) vm.cancelFileOperation()
        else when(operation) {
            is MediaFileOperation.Delete -> vm.completeDelete(operation)
            is MediaFileOperation.Move -> vm.completeMove(operation)
        }
    }
    val granted=remember(permissionVersion) { permissions.filterNot { it=="android.permission.POST_NOTIFICATIONS" }.any { context.checkSelfPermission(it)==PackageManager.PERMISSION_GRANTED } }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event ->
            if(event==Lifecycle.Event.ON_RESUME) { permissionVersion++; vm.refresh(); player.connect() }
            if(event==Lifecycle.Event.ON_STOP) player.checkpoint()
        }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(vm) { vm.fileOperations.collect { operation ->
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.R) { vm.fileOperationUnavailable() }
        else {
            val sender=runCatching {
                val uris=operation.items.map { it.uri }
                when(operation) {
                    is MediaFileOperation.Delete -> MediaStore.createDeleteRequest(context.contentResolver,uris).intentSender
                    is MediaFileOperation.Move -> MediaStore.createWriteRequest(context.contentResolver,uris).intentSender
                }
            }.getOrNull()
            if(sender==null) vm.cancelFileOperation()
            else { pendingFileOperation=operation; fileOperationLauncher.launch(IntentSenderRequest.Builder(sender).build()) }
        }
    } }
    LaunchedEffect(player) { player.options.collect { value -> value.error?.let { snackbar.showSnackbar(it,actionLabel="Fechar"); player.clearError() } } }
    fun open(path: String) { nav.navigate(path) { launchSingleTop=true } }
    fun back() { nav.popBackStack() }
    fun openPlayer() { open(if(player.state.value.mediaId?.startsWith("VIDEO:")==true) "video" else "audio") }
    fun play(item: LocalMedia,items: List<LocalMedia>) { vm.play(item,items); open(if(item.kind==MediaKind.VIDEO) "video" else "audio") }
    fun shuffle(items: List<LocalMedia>) { if(items.isNotEmpty()) { player.playShuffled(items); open("audio") } }
    fun group(type: String,id: String) { open("group/"+type+"/"+Uri.encode(id)) }
    fun folder(name: String,filter: Int) { open("group/folder/"+Uri.encode(name)+"?filter="+filter) }
    val fullPlayer=route in listOf("audio","video","sound")
    com.localflow.player.ui.theme.LocalFlowTheme(settings.theme) {
        val lightBars=MaterialTheme.colorScheme.background.luminance()>0.5f
        SideEffect { (context as? android.app.Activity)?.window?.let { window -> WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=lightBars; isAppearanceLightNavigationBars=lightBars } } }
        Surface(Modifier.fillMaxSize()) {
            if(!granted && !skipPermission) PermissionPage({ launcher.launch(permissions) },{ skipPermission=true },{
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+context.packageName)))
            })
            else Scaffold(snackbarHost={ SnackbarHost(snackbar) },topBar={
                if(!granted) Surface(Modifier.statusBarsPadding().fillMaxWidth(),color=MaterialTheme.colorScheme.secondaryContainer) {
                    TextButton({ launcher.launch(permissions) }) { Text("Permitir acesso à biblioteca") }
                }
            },bottomBar={
                if(!fullPlayer) Column {
                    MiniPlayer(player,::openPlayer)
                    NavigationBar {
                        val tabs=listOf(Triple("home","Início",Icons.Default.Home),Triple("songs","Músicas",Icons.Default.MusicNote),Triple("videos","Vídeos",Icons.Default.VideoLibrary),Triple("folders","Pastas",Icons.Default.Folder),Triple("playlists","Listas",Icons.AutoMirrored.Filled.QueueMusic))
                        tabs.forEach { (id,title,icon) ->
                            NavigationBarItem(selected=route==id,onClick={ nav.navigate(id) { popUpTo("home") { saveState=true }; launchSingleTop=true; restoreState=true } },icon={ Icon(icon,title) },label={ Text(title) })
                        }
                    }
                }
            }) { padding ->
                NavHost(nav,"home",Modifier.padding(padding)) {
                    composable("home") { HomePage(library,collections,favoriteKeys,player,::open,::play,{ adding=it },vm::toggleFavorite) }
                    composable("songs") { SongsPage(library,favoriteKeys,vm,::group,{ open("search") },::play,::shuffle,{ adding=it },{ addingMany=it },vm::requestDelete,vm::requestMove) }
                    composable("videos") { MediaListPage("Vídeos",library.videos,library.loading,favoriteKeys,{ open("search") },::play,{ adding=it },vm::toggleFavorite,library.folders,vm::requestDelete,vm::requestMove,sort=library.videoSort,order={ vm.order(LibrarySection.VIDEO,it) }) }
                    composable("folders") { FoldersPage(library.folders,settings.folderFilter,settings.folderSort,vm::setFolderFilter,vm::setFolderSort,::folder) }
                    composable("group/{type}/{id}?filter={filter}",arguments=listOf(navArgument("filter") { defaultValue=0 })) { e ->
                        val type=e.arguments?.getString("type").orEmpty(); val id=e.arguments?.getString("id").orEmpty()
                        val folderFilter=e.arguments?.getInt("filter") ?: 0
                        val items=remember(library,type,id) { library.all.filter { when(type) { "folder"->it.folder==id; "artist"->it.artist==id; else->albumKey(it)==id } } }
                        val title=when(type) { "folder"->id.substringAfterLast('/'); "artist"->id; else->items.firstOrNull()?.album ?: "Álbum desconhecido" }
                        CollectionPage(title,items,if(type=="folder") id else items.firstOrNull()?.artist.orEmpty(),favoriteKeys,::back,::play,{ adding=it },vm::toggleFavorite,library.folders,vm::requestDelete,vm::requestMove,type=="folder",folderFilter)
                    }
                    composable("playlists") { PlaylistsPage(collections,{ open("edit/0") },{ open("playlist/"+it) }) }
                    composable("playlist/{id}") { e ->
                        val id=e.arguments?.getString("id")?.toLongOrNull() ?: 0
                        PlaylistPage(collections.find { it.playlist.id==id },::back,{ open("add/"+id) },{ open("edit/"+id) },{ vm.deletePlaylist(id); back() },::play,{ vm.removeFromPlaylist(id,it) })
                    }
                    composable("edit/{id}") { e ->
                        val id=e.arguments?.getString("id")?.toLongOrNull() ?: 0
                        PlaylistEditor(playlists.find { it.id==id },::back) { name,addNow ->
                            if(id==0L) vm.createPlaylist(name) { newId -> back(); open(if(addNow) "add/"+newId else "playlist/"+newId) }
                            else { vm.renamePlaylist(id,name); back() }
                        }
                    }
                    composable("add/{id}") { e ->
                        val id=e.arguments?.getString("id")?.toLongOrNull() ?: 0
                        AddMediaPage(playlists.find { it.id==id }?.name.orEmpty(),library.all,::back) { items -> vm.addToPlaylist(id,items) { back() } }
                    }
                    composable("favorites") { MediaListPage("Favoritos",library.all.filter { it.key in favoriteKeys },library.loading,favoriteKeys,{ open("search") },::play,{ adding=it },vm::toggleFavorite,library.folders,vm::requestDelete,vm::requestMove,::back) }
                    composable("search") { SearchPage(vm,favoriteKeys,::back,::group,::play,{ adding=it }) }
                    composable("audio") { AudioPage(player,library.all,favoriteKeys,vm::toggleFavorite,::back,{ open("queue") },{ open("sound") }) }
                    composable("video") { VideoPage(player,settings.videoAudioBackground,vm::setVideoAudioBackground,::back,{ open("sound") },{ open("queue") }) }
                    composable("queue") { QueuePage(player,::back) { id -> open(if(id.startsWith("VIDEO:")) "video" else "audio") } }
                    composable("sound") { SoundPage(player,::back) }
                    composable("settings") { SettingsPage(vm,::back,{ launcher.launch(permissions) }) }
                }
            }
            adding?.let { item -> PlaylistChooser(playlists,{ adding=null },{ id -> vm.addToPlaylist(id,item); adding=null },{ name -> vm.createPlaylist(name) { id -> vm.addToPlaylist(id,item); adding=null } }) }
            addingMany?.let { items -> PlaylistChooser(playlists,{ addingMany=null },{ id -> vm.addToPlaylist(id,items); addingMany=null },{ name -> vm.createPlaylist(name) { id -> vm.addToPlaylist(id,items); addingMany=null } }) }
        }
    }
}
fun albumKey(item: LocalMedia):String = if(item.albumId>0) item.albumId.toString() else (item.album ?: "Álbum desconhecido")+"|"+item.artist

@Composable private fun PlaylistChooser(playlists: List<PlaylistEntity>,dismiss: ()->Unit,choose: (Long)->Unit,create: (String)->Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest=dismiss,title={ Text("Adicionar à playlist") },text={
        Column {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max=260.dp)) {
                items(playlists.size,key={ playlists[it].id }) { i -> TextButton({ choose(playlists[i].id) },Modifier.fillMaxWidth()) { Text(playlists[i].name) } }
            }
            OutlinedTextField(name,{ name=it },label={ Text("Criar nova playlist") },singleLine=true)
        }
    },confirmButton={ TextButton({ create(name) },enabled=name.isNotBlank()) { Text("Criar e adicionar") } },dismissButton={ TextButton(dismiss) { Text("Fechar") } })
}
