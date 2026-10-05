package com.localflow.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localflow.player.data.*
import com.localflow.player.model.*
import com.localflow.player.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable fun HomePage(state: LibraryState,collections: List<PlaylistDetail>,favorites: Set<String>,player: PlayerConnection,open: (String)->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,add: (LocalMedia)->Unit,toggle: (LocalMedia)->Unit) {
    val now by player.state.collectAsStateWithLifecycle()
    val recent=remember(state.songs,state.videos) { state.all.sortedByDescending { it.dateAddedSeconds }.take(6) }
    LazyColumn(contentPadding=PaddingValues(bottom=16.dp)) {
        item { PageHeader("LocalFlow","Seu som, sempre com você",actions={ IconButton({ open("settings") }) { Icon(Icons.Default.Settings,"Configurações") } }) }
        item { Box(Modifier.padding(horizontal=20.dp)) { SearchShortcut { open("search") } } }
        item {
            Card(Modifier.fillMaxWidth().padding(20.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer),shape=MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(22.dp)) {
                    Text(if(now.mediaId!=null) "CONTINUE DE ONDE PAROU" else "SUA BIBLIOTECA, SEU RITMO",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if(now.mediaId!=null) now.title else "Um respiro no seu dia.",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp)); Text(if(now.mediaId!=null) now.artist else "Músicas e vídeos do seu aparelho.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
                        }
                        if(now.uri!=null) { Spacer(Modifier.width(12.dp)); Artwork(now.uri,now.artwork,now.title,now.mediaId?.startsWith("VIDEO:")==true,Modifier.size(80.dp)) }
                    }
                    Spacer(Modifier.height(18.dp))
                    Button({
                        if(now.mediaId!=null) open(if(now.mediaId?.startsWith("VIDEO:")==true) "video" else "audio")
                        else recent.firstOrNull()?.let { play(it,recent) }
                    },enabled=now.mediaId!=null || recent.isNotEmpty()) {
                        Icon(Icons.Default.PlayArrow,null); Spacer(Modifier.width(6.dp)); Text(if(now.mediaId!=null) "Abrir player" else "Começar a ouvir")
                    }
                }
            }
        }
        if(state.error!=null) item { Box(Modifier.padding(horizontal=20.dp)) { Hint(state.error) } }
        item {
            Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Card(onClick={ open("favorites") },Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) { Icon(Icons.Default.Favorite,null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text("Favoritos",fontWeight=FontWeight.SemiBold); Text(favorites.size.toString()+" arquivos",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Card(onClick={ open("playlists") },Modifier.weight(1f),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp)) { Icon(Icons.AutoMirrored.Filled.QueueMusic,null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text("Suas playlists",fontWeight=FontWeight.SemiBold); Text(collections.size.toString()+" coleções",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        if(collections.isNotEmpty()) {
            item { Box(Modifier.padding(horizontal=20.dp)) { SectionTitle("Suas coleções","Ver tudo") { open("playlists") } } }
            item { LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                items(collections.take(8),key={ it.playlist.id }) { c ->
                    Column(Modifier.width(140.dp).clickable { open("playlist/"+c.playlist.id) }) {
                        PlaylistMosaic(c.items,Modifier.size(140.dp)); Spacer(Modifier.height(8.dp)); Text(c.playlist.name,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.Medium)
                        Text(c.items.size.toString()+" arquivos",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } }
        }
        item { Box(Modifier.padding(horizontal=20.dp)) { SectionTitle("Adicionadas recentemente","Ver tudo") { open("songs") } } }
        if(state.loading) item { Box(Modifier.fillMaxWidth().padding(20.dp),Alignment.Center) { CircularProgressIndicator() } }
        else if(recent.isEmpty()) item { EmptyContent("Sua biblioteca está vazia","Adicione arquivos ao aparelho e permita o acesso nas configurações.",action="Configurações",onAction={ open("settings") }) }
        items(recent,key={ it.key }) { m -> Box(Modifier.padding(horizontal=20.dp)) { MediaRow(m,m.key in favorites,{ play(m,recent) },{ add(m) },{ toggle(m) }) } }
    }
}

@Composable fun SongsPage(state: LibraryState,favorites: Set<String>,initialTab: Int,setTab: (Int)->Unit,vm: LibraryViewModel,group: (String,String)->Unit,search: ()->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,shuffle: (List<LocalMedia>)->Unit,add: (LocalMedia)->Unit,addSelected: (List<LocalMedia>)->Unit,delete: (List<LocalMedia>)->Unit,move: (List<LocalMedia>,String)->Unit) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var sortMenu by remember { mutableStateOf(false) }
    var selecting by rememberSaveable(tab) { mutableStateOf(false) }
    var selectedKeys by rememberSaveable(tab) { mutableStateOf(listOf<String>()) }
    val listState=rememberLazyListState()
    val grouped=remember(state.songs,tab) { if(tab==2) state.songs.groupBy(::albumKey) else state.songs.groupBy { it.artist } }
    val selectedSort=if(tab==1) state.whatsAppSort else state.songSort
    val section=if(tab==1) LibrarySection.WHATSAPP_AUDIO else LibrarySection.MUSIC
    val shown=if(tab==1) state.whatsAppAudio else state.songs
    val selected=remember(shown,selectedKeys) { shown.filter { it.key in selectedKeys } }
    LaunchedEffect(initialTab) { tab=initialTab }
    LaunchedEffect(tab,selectedSort) { listState.scrollToItem(0) }
    Column {
        PageHeader("Músicas",(state.songs.size+state.whatsAppAudio.size).toString()+" áudios no aparelho",actions={
            if(tab<2) {
                IconButton({ shuffle(shown) },enabled=shown.isNotEmpty()) { Icon(Icons.Default.Shuffle,"Tocar em aleatório") }
            }
            IconButton(search) { Icon(Icons.Default.Search,"Pesquisar") }
        })
        MediaSelectionBar(selecting,selected,shown,state.folders.map { it.name.substringAfterLast('/') }.distinct(),{ selectedKeys=shown.map { it.key } },{ selecting=false; selectedKeys=emptyList() },delete,move,allowPlaylistAdd=true,addToPlaylist=addSelected)
        LazyRow(contentPadding=PaddingValues(horizontal=12.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                items(listOf("Faixas","WhatsApp","Álbuns","Artistas")) { label ->
                    val index=listOf("Faixas","WhatsApp","Álbuns","Artistas").indexOf(label)
                    FilterChip(tab==index,{ tab=index; setTab(index) },label={ Text(label,maxLines=1) })
                }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(when(tab) { 0->"Todas as músicas"; 1->"Áudios do WhatsApp"; 2->"Seus álbuns"; else->"Seus artistas" },Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                TextButton({ sortMenu=true }) { Text(sortLabel(selectedSort)); Icon(Icons.Default.Sort,null) }
                DropdownMenu(sortMenu,{ sortMenu=false }) { MediaSort.entries.forEach { value ->
                    DropdownMenuItem(text={ Text(sortMenuLabel(value)) },onClick={ sortMenu=false; vm.order(section,value) })
                } }
            }
        }
        if(state.loading) Box(Modifier.fillMaxSize(),Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(state=listState,modifier=if(tab<2) Modifier.dragSelectMedia(listState,shown,0,{ key -> selecting=true; selectedKeys=listOf(key) },{ key -> if(key !in selectedKeys) selectedKeys=selectedKeys+key }) else Modifier,contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp)) {
            if(shown.isEmpty() && tab<2) item { EmptyContent(if(tab==1) "Nenhum áudio do WhatsApp encontrado" else "Nenhuma música encontrada",if(tab==1) "Os áudios em pastas do WhatsApp aparecem separados aqui." else "Confira a permissão para músicas e os arquivos salvos no aparelho.") }
            if(tab==0) {
                items(state.songs,key={ it.key }) { m -> MediaRow(m,m.key in favorites,{ play(m,state.songs) },{ add(m) },{ vm.toggleFavorite(m) },selected=if(selecting) m.key in selectedKeys else null,select={ selectedKeys=if(m.key in selectedKeys) selectedKeys-m.key else selectedKeys+m.key },onLongSelect={ selecting=true; selectedKeys=listOf(m.key) }) }
            } else if(tab==1) {
                items(state.whatsAppAudio,key={ it.key }) { m -> MediaRow(m,m.key in favorites,{ play(m,state.whatsAppAudio) },{ add(m) },{ vm.toggleFavorite(m) },selected=if(selecting) m.key in selectedKeys else null,select={ selectedKeys=if(m.key in selectedKeys) selectedKeys-m.key else selectedKeys+m.key },onLongSelect={ selecting=true; selectedKeys=listOf(m.key) }) }
            } else {
                items(grouped.entries.toList(),key={ it.key }) { (key,media) ->
                    Row(Modifier.fillMaxWidth().clickable { group(if(tab==2) "album" else "artist",key) }.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                        MediaThumbnail(media.first(),Modifier.size(66.dp)); Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if(tab==2) media.first().album ?: "Álbum desconhecido" else key,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            Text(media.size.toString()+" faixas",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight,null)
                    }
                }
            }
        }
    }
}
private fun sortLabel(value: MediaSort): String = when(value) { MediaSort.TITLE->"Nome"; MediaSort.ARTIST->"Artista"; MediaSort.DATE_ADDED->"Recentes"; MediaSort.DURATION->"Duração" }
private fun sortMenuLabel(value: MediaSort): String = when(value) { MediaSort.TITLE->"Nome A–Z"; MediaSort.ARTIST->"Artista"; MediaSort.DATE_ADDED->"Data adicionada"; MediaSort.DURATION->"Duração" }

@Composable fun MediaListPage(title: String,media: List<LocalMedia>,loading: Boolean,favorites: Set<String>,search: ()->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,add: (LocalMedia)->Unit,toggle: (LocalMedia)->Unit,folders: List<MediaFolder>,delete: (List<LocalMedia>)->Unit,move: (List<LocalMedia>,String)->Unit,back: (() -> Unit)?=null,sort: MediaSort?=null,order: (MediaSort)->Unit={}) {
    var kind by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    val listState=rememberLazyListState()
    val items=remember(media,kind) { media.filter { kind==0 || it.kind==if(kind==1) MediaKind.AUDIO else MediaKind.VIDEO } }
    val selected=remember(items,selectedKeys) { items.filter { it.key in selectedKeys } }
    LaunchedEffect(kind,sort) { listState.scrollToItem(0) }
    Column {
        PageHeader(title,media.size.toString()+" arquivos locais",back,actions={
            IconButton(search) { Icon(Icons.Default.Search,"Pesquisar") }
            if(sort!=null) Box { IconButton({ sortMenu=true }) { Icon(Icons.Default.Sort,"Ordenar: "+sortLabel(sort)) }; DropdownMenu(sortMenu,{ sortMenu=false }) { MediaSort.entries.forEach { value -> DropdownMenuItem(text={ Text(sortMenuLabel(value)) },onClick={ sortMenu=false; order(value) }) } } }
        })
        MediaSelectionBar(selecting,selected,items,folders.map { it.name.substringAfterLast('/') }.distinct(),{ selectedKeys=items.map { it.key } },{ selecting=false; selectedKeys=emptyList() },delete,move)
        if(title=="Favoritos") Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Tudo","Músicas","Vídeos").forEachIndexed { i,s -> FilterChip(kind==i,{ kind=i },label={ Text(s) }) } }
        if(loading) Box(Modifier.fillMaxSize(),Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(state=listState,modifier=Modifier.dragSelectMedia(listState,items,0,{ key -> selecting=true; selectedKeys=listOf(key) },{ key -> if(key !in selectedKeys) selectedKeys=selectedKeys+key }),contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp)) {
            if(items.isEmpty()) item { EmptyContent(if(title=="Favoritos") "Seus favoritos aparecem aqui" else "Nenhum vídeo encontrado",if(title=="Favoritos") "Use o menu de uma música ou vídeo para favoritar." else "Confira o acesso a vídeos nas configurações.") }
            items(items,key={ it.key }) { m -> MediaRow(m,m.key in favorites,{ play(m,items) },{ add(m) },{ toggle(m) },selected=if(selecting) m.key in selectedKeys else null,select={ selectedKeys=if(m.key in selectedKeys) selectedKeys-m.key else selectedKeys+m.key },onLongSelect={ selecting=true; selectedKeys=listOf(m.key) }) }
        }
    }
}
@Composable fun FoldersPage(folders: List<MediaFolder>,filter: FolderFilter,sort: FolderSort,setFilter: (FolderFilter)->Unit,setSort: (FolderSort)->Unit,open: (String,Int)->Unit) {
    var sortMenu by remember { mutableStateOf(false) }
    val visible=remember(folders,filter) { folders.filter { folder ->
        when(filter) { FolderFilter.AUDIO -> folder.musicCount+folder.whatsAppCount>0; FolderFilter.VIDEO -> folder.videoCount>0; FolderFilter.ALL -> true }
    } }
    Column {
        PageHeader("Pastas","Organizadas como no seu aparelho",actions={
            Box {
                IconButton({ sortMenu=true }) { Icon(Icons.Default.Sort,"Ordenar por "+folderSortLabel(sort)) }
                DropdownMenu(sortMenu,{ sortMenu=false }) { FolderSort.entries.forEach { value -> DropdownMenuItem(text={ Text(folderSortLabel(value)) },onClick={ sortMenu=false; setSort(value) }) } }
            }
        })
        LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(FolderFilter.entries) { value ->
                FilterChip(filter==value,{ setFilter(value) },label={ Text(folderFilterLabel(value),maxLines=1) })
            }
        }
        LazyColumn(contentPadding=PaddingValues(20.dp)) {
            if(visible.isEmpty()) item { EmptyContent("Nenhuma pasta encontrada",if(filter==FolderFilter.ALL) "As pastas aparecem a partir dos arquivos locais." else "Nenhuma pasta contém esse tipo de mídia.") }
            items(visible,key={ it.name }) { f ->
                Row(Modifier.fillMaxWidth().clickable { open(f.name,filter.ordinal) }.padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Surface(Modifier.size(52.dp),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.secondaryContainer) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.Folder,null,tint=MaterialTheme.colorScheme.primary) } }
                    Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                        Text(f.name.substringAfterLast('/'),fontWeight=FontWeight.SemiBold); Text(f.name,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(f.count.toString()+" arquivos · "+folderBreakdown(f),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }; Icon(Icons.Default.ChevronRight,null)
                }
            }
        }
    }
}
private fun folderFilterLabel(value: FolderFilter) = when(value) { FolderFilter.ALL->"Tudo"; FolderFilter.AUDIO->"Áudios"; FolderFilter.VIDEO->"Vídeos" }
private fun folderSortLabel(value: FolderSort) = when(value) { FolderSort.NAME->"Nome"; FolderSort.MOST_ITEMS->"Mais arquivos" }
private fun folderBreakdown(folder: MediaFolder): String = listOfNotNull(
    folder.musicCount.takeIf { it>0 }?.let { "$it músicas" },
    folder.whatsAppCount.takeIf { it>0 }?.let { "$it WhatsApp" },
    folder.videoCount.takeIf { it>0 }?.let { "$it vídeos" }
).joinToString(" · ").ifBlank { "outros" }

/** A long press starts selection; dragging over visible rows adds each one without reopening menus. */
private fun Modifier.dragSelectMedia(state: LazyListState,media: List<LocalMedia>,firstMediaItemIndex: Int,start: (String)->Unit,extend: (String)->Unit): Modifier = pointerInput(media,firstMediaItemIndex) {
    fun itemAt(y: Float): LocalMedia? = state.layoutInfo.visibleItemsInfo.firstOrNull { info -> y>=info.offset && y<info.offset+info.size }?.let { media.getOrNull(it.index-firstMediaItemIndex) }
    detectDragGesturesAfterLongPress(
        onDragStart={ point -> itemAt(point.y)?.let { start(it.key) } },
        onDrag={ change,_ -> itemAt(change.position.y)?.let { extend(it.key) } }
    )
}

@Composable fun CollectionPage(title: String,media: List<LocalMedia>,subtitle: String,favorites: Set<String>,back: ()->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,add: (LocalMedia)->Unit,toggle: (LocalMedia)->Unit,folders: List<MediaFolder>,delete: (List<LocalMedia>)->Unit,move: (List<LocalMedia>,String)->Unit,isFolder: Boolean=false,initialFilter: Int=0) {
    var filter by rememberSaveable(isFolder,initialFilter) { mutableIntStateOf(initialFilter) }
    var selecting by rememberSaveable(isFolder) { mutableStateOf(false) }
    var selectedKeys by rememberSaveable(isFolder) { mutableStateOf(listOf<String>()) }
    val listState=rememberLazyListState()
    val visible=remember(media,filter,isFolder) { if(!isFolder) media else when(filter) { 1->media.filter { it.kind==MediaKind.AUDIO }; 2->media.filter { it.kind==MediaKind.VIDEO }; else->media } }
    val selected=remember(visible,selectedKeys) { visible.filter { it.key in selectedKeys } }
    LaunchedEffect(filter) { listState.scrollToItem(0) }
    // Header, overview and the selection bar occupy list slots; folders have one extra filter row.
    val firstMediaItemIndex=if(isFolder) 4 else 3
    LazyColumn(state=listState,modifier=Modifier.dragSelectMedia(listState,visible,firstMediaItemIndex,{ key -> selecting=true; selectedKeys=listOf(key) },{ key -> if(key !in selectedKeys) selectedKeys=selectedKeys+key }),contentPadding=PaddingValues(bottom=16.dp)) {
        item { PageHeader(title,subtitle,back) }
        item { Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
            MediaThumbnail(media.firstOrNull(),Modifier.size(116.dp),large=true); Spacer(Modifier.width(18.dp))
            Column { Text(media.size.toString()+" arquivos",style=MaterialTheme.typography.titleMedium); Text(formatTime(media.sumOf { it.durationMs }),color=MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); Button({ visible.firstOrNull()?.let { play(it,visible) } },enabled=visible.isNotEmpty()) { Icon(Icons.Default.PlayArrow,null); Text("Reproduzir") } }
        } }
        if(isFolder) item { LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(listOf("Tudo","Áudios","Vídeos")) { label -> val index=listOf("Tudo","Áudios","Vídeos").indexOf(label); FilterChip(filter==index,{ filter=index },label={ Text(label,maxLines=1) }) } } }
        item { MediaSelectionBar(selecting,selected,visible,folders.map { it.name.substringAfterLast('/') }.distinct(),{ selectedKeys=visible.map { it.key } },{ selecting=false; selectedKeys=emptyList() },delete,move) }
        if(visible.isEmpty()) item { EmptyContent("Coleção indisponível","Os arquivos podem ter sido movidos, excluídos ou não correspondem ao filtro.") }
        items(visible,key={ it.key }) { m -> Box(Modifier.padding(horizontal=20.dp)) { MediaRow(m,m.key in favorites,{ play(m,visible) },{ add(m) },{ toggle(m) },selected=if(selecting) m.key in selectedKeys else null,select={ selectedKeys=if(m.key in selectedKeys) selectedKeys-m.key else selectedKeys+m.key },onLongSelect={ selecting=true; selectedKeys=listOf(m.key) }) } }
    }
}
@Composable fun PlaylistsPage(collections: List<PlaylistDetail>,create: ()->Unit,open: (Long)->Unit) {
    Column {
        PageHeader("Playlists","Coleções feitas por você")
        Button(create,Modifier.fillMaxWidth().padding(horizontal=20.dp)) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(8.dp)); Text("Nova playlist") }
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            if(collections.isEmpty()) item { EmptyContent("Crie sua primeira playlist","Escolha um nome e reúna suas músicas e vídeos favoritos.",action="Criar playlist",onAction=create) }
            items(collections.chunked(2),key={ it.first().playlist.id }) { pair ->
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    pair.forEach { c -> Column(Modifier.weight(1f).clickable { open(c.playlist.id) }) {
                        PlaylistMosaic(c.items,Modifier.fillMaxWidth().aspectRatio(1f))
                        Spacer(Modifier.height(9.dp)); Text(c.playlist.name,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(c.items.size.toString()+" arquivos · "+formatTime(c.items.sumOf { it.durationMs }),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    } }
                    if(pair.size==1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
@Composable fun PlaylistPage(detail: PlaylistDetail?,back: ()->Unit,add: ()->Unit,edit: ()->Unit,delete: ()->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,remove: (LocalMedia)->Unit) {
    var deleting by remember { mutableStateOf(false) }
    if(detail==null) { Column { PageHeader("Playlist",back=back); EmptyContent("Carregando coleção","Se ela foi excluída, volte para suas playlists.") }; return }
    LazyColumn(contentPadding=PaddingValues(bottom=20.dp)) {
        item { PageHeader("Sua playlist","Criada por você",back,actions={ IconButton(edit) { Icon(Icons.Default.Edit,"Editar playlist") }; IconButton({ deleting=true }) { Icon(Icons.Default.DeleteOutline,"Excluir playlist") } }) }
        item { Row(Modifier.padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
            PlaylistMosaic(detail.items,Modifier.size(116.dp)); Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(detail.playlist.name,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold); Text(detail.items.size.toString()+" arquivos · "+formatTime(detail.items.sumOf { it.durationMs }),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        } }
        item { Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Button({ detail.items.firstOrNull()?.let { play(it,detail.items) } },enabled=detail.items.isNotEmpty()) { Icon(Icons.Default.PlayArrow,null); Text("Ouvir") }
            OutlinedButton(add) { Icon(Icons.Default.Add,null); Text("Adicionar músicas") }
        } }
        if(detail.missing>0) item { Box(Modifier.padding(20.dp)) { Hint(detail.missing.toString()+" arquivos indisponíveis. Confira as permissões ou se foram removidos do aparelho.") } }
        if(detail.items.isEmpty()) item { EmptyContent("Esta playlist está vazia","Adicione músicas e vídeos da sua biblioteca.",action="Adicionar músicas",onAction=add) }
        items(detail.items,key={ it.key }) { m -> Box(Modifier.padding(horizontal=20.dp)) { MediaRow(m,play={ play(m,detail.items) },add=add,toggleFavorite={},remove={ remove(m) }) } }
    }
    if(deleting) AlertDialog(onDismissRequest={ deleting=false },title={ Text("Excluir playlist?") },text={ Text("Apenas a playlist será excluída. Os arquivos do celular serão mantidos.") },confirmButton={ TextButton({ deleting=false; delete() }) { Text("Excluir") } },dismissButton={ TextButton({ deleting=false }) { Text("Cancelar") } })
}
@Composable fun PlaylistEditor(playlist: PlaylistEntity?,back: ()->Unit,save: (String,Boolean)->Unit) {
    var name by rememberSaveable(playlist?.id) { mutableStateOf(playlist?.name.orEmpty()) }
    var addNow by rememberSaveable { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeader(if(playlist==null) "Nova playlist" else "Editar playlist","Organize seus momentos",back)
        Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            PlaylistMosaic(emptyList(),Modifier.size(160.dp)); Spacer(Modifier.height(24.dp))
            OutlinedTextField(name,{ name=it.take(80); saving=false },Modifier.fillMaxWidth(),label={ Text("Nome da playlist") },singleLine=true,shape=MaterialTheme.shapes.medium)
            Spacer(Modifier.height(10.dp)); Text("A capa combina os álbuns das músicas escolhidas.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(playlist==null) PreferenceRow("Adicionar músicas agora","Abrir seleção depois de criar",addNow,{ addNow=it })
            Spacer(Modifier.height(20.dp)); Button({ saving=true; save(name,addNow) },Modifier.fillMaxWidth(),enabled=name.isNotBlank() && !saving) { Text(if(playlist==null) "Criar playlist" else "Salvar alterações") }
        }
    }
}
@Composable fun AddMediaPage(name: String,all: List<LocalMedia>,back: ()->Unit,add: (List<LocalMedia>)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    var filtered by remember { mutableStateOf(all) }
    LaunchedEffect(all,query) { delay(200); filtered=withContext(Dispatchers.Default) { all.matching(query) } }
    Column {
        PageHeader("Adicionar músicas","Para: "+name,back)
        OutlinedTextField(query,{ query=it },Modifier.fillMaxWidth().padding(horizontal=20.dp),placeholder={ Text("Buscar na biblioteca") },leadingIcon={ Icon(Icons.Default.Search,null) },singleLine=true,shape=MaterialTheme.shapes.large)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(20.dp)) {
            if(filtered.isEmpty()) item { EmptyContent("Nenhum resultado","Tente outro título, artista ou álbum.") }
            items(filtered,key={ it.key }) { m ->
                MediaRow(m,play={},add={},toggleFavorite={},selected=m.key in selected,select={ selected=if(m.key in selected) selected-m.key else selected+m.key })
            }
        }
        Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(selected.size.toString()+" selecionadas",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
            Button({ add(all.filter { it.key in selected }) },enabled=selected.isNotEmpty()) { Icon(Icons.Default.Check,null); Text("Adicionar") }
        }
    }
}
@Composable fun SearchPage(vm: LibraryViewModel,favorites: Set<String>,back: ()->Unit,group: (String,String)->Unit,play: (LocalMedia,List<LocalMedia>)->Unit,add: (LocalMedia)->Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableIntStateOf(0) }
    val media=remember(results,kind) { results.filter { kind!=1 && kind!=2 || it.kind==if(kind==1) MediaKind.AUDIO else MediaKind.VIDEO } }
    val albums=remember(results) { results.filter { it.kind==MediaKind.AUDIO }.groupBy(::albumKey).entries.toList() }
    val artists=remember(results) { results.filter { it.kind==MediaKind.AUDIO }.groupBy { it.artist }.entries.toList() }
    Column {
        PageHeader("Pesquisa","Encontre na sua biblioteca",back)
        OutlinedTextField(query,{ vm.query.value=it },Modifier.fillMaxWidth().padding(horizontal=20.dp),placeholder={ Text("Música, artista, álbum...") },leadingIcon={ Icon(Icons.Default.Search,null) },trailingIcon={ if(query.isNotEmpty()) IconButton({ vm.query.value="" }) { Icon(Icons.Default.Close,"Limpar") } },singleLine=true,shape=MaterialTheme.shapes.large)
        LazyRow(contentPadding=PaddingValues(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(5) { i -> FilterChip(kind==i,{ kind=i },label={ Text(listOf("Tudo","Músicas","Vídeos","Álbuns","Artistas")[i]) }) } }
        LazyColumn(contentPadding=PaddingValues(horizontal=20.dp,vertical=8.dp)) {
            if(query.isBlank()) item { Hint("Pesquise por título, artista ou álbum. A busca funciona offline.") }
            if(results.isEmpty()) item { EmptyContent("Nenhum resultado","Tente outro nome ou confira sua biblioteca.") }
            if(kind==3 || kind==4) {
                items(if(kind==3) albums else artists,key={ it.key }) { (id,items) -> Row(Modifier.fillMaxWidth().clickable { group(if(kind==3) "album" else "artist",id) }.padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                    MediaThumbnail(items.first()); Spacer(Modifier.width(12.dp)); Column { Text(if(kind==3) items.first().album ?: "Álbum desconhecido" else id); Text(items.size.toString()+" faixas",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                } }
            } else items(media,key={ it.key }) { m -> MediaRow(m,m.key in favorites,{ play(m,media) },{ add(m) },{ vm.toggleFavorite(m) }) }
        }
    }
}

@Composable fun PermissionPage(request: ()->Unit,later: ()->Unit,settings: ()->Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        EmptyContent("Sua biblioteca, no seu ritmo.","Encontre e reproduza músicas e vídeos já salvos no seu celular.",Icons.Default.GraphicEq)
        Hint("O acesso permite organizar e reproduzir seus arquivos. O LocalFlow não baixa conteúdo nem envia sua biblioteca à internet.")
        Spacer(Modifier.height(24.dp)); Button(request,Modifier.fillMaxWidth()) { Icon(Icons.Default.FolderOpen,null); Spacer(Modifier.width(8.dp)); Text("Permitir acesso") }
        TextButton(later) { Text("Agora não") }
        TextButton(settings) { Text("Revisar permissões nas configurações") }
    }
}
@Composable fun SettingsPage(vm: LibraryViewModel,back: ()->Unit,permissions: ()->Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        PageHeader("Configurações","Do seu jeito",back)
        Column(Modifier.padding(horizontal=20.dp)) {
            SectionTitle("Aparência · Midnight")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode ->
                FilterChip(settings.theme==mode,{ vm.setTheme(mode) },label={ Text(when(mode) { ThemeMode.SYSTEM->"Sistema"; ThemeMode.LIGHT->"Claro"; ThemeMode.DARK->"Escuro" }) })
            } }
            SectionTitle("Reprodução")
            PreferenceRow("Áudio de vídeos em segundo plano","Continuar quando a tela não estiver ativa",settings.videoAudioBackground,vm::setVideoAudioBackground)
            PreferenceRow("Retomar última reprodução","Restaurar posição e fila, sem iniciar sozinho",settings.resumePlayback,vm::setResumePlayback)
            SectionTitle("Biblioteca")
            PreferenceRow("Ocultar arquivos muito curtos","Não mostrar arquivos com menos de 10 segundos",settings.hideShort,vm::setHideShort)
            OutlinedButton(permissions,Modifier.fillMaxWidth()) { Text("Revisar acesso a músicas e vídeos") }
            OutlinedButton({ vm.refresh() },Modifier.fillMaxWidth()) { Icon(Icons.Default.Refresh,null); Text("Atualizar biblioteca") }
            Spacer(Modifier.height(20.dp)); Hint("LocalFlow Player "+com.localflow.player.BuildConfig.VERSION_NAME+"\nMidnight · Áudio e vídeo offline\nSem conta, anúncios ou conexão obrigatória.")
            Spacer(Modifier.height(24.dp))
        }
    }
}
