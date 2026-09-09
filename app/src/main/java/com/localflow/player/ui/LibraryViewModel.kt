package com.localflow.player.ui

import androidx.lifecycle.*
import com.localflow.player.AppContainer
import com.localflow.player.data.*
import com.localflow.player.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class LibraryState(val songs: List<LocalMedia> = emptyList(), val videos: List<LocalMedia> = emptyList(), val folders: List<MediaFolder> = emptyList(), val loading: Boolean = true, val error: String? = null, val sort: MediaSort = MediaSort.TITLE) {
    val all: List<LocalMedia> get() = songs + videos
}
data class PlaylistDetail(val playlist: PlaylistEntity, val items: List<LocalMedia>, val missing: Int = 0)
@OptIn(FlowPreview::class)
class LibraryViewModel(private val app: AppContainer) : ViewModel() {
    private val dao = app.database.libraryDao()
    private val content = MutableStateFlow(LibraryState())
    private val sort = MutableStateFlow(MediaSort.TITLE)
    private var refreshJob: Job? = null
    private val notices = Channel<String>(Channel.BUFFERED)
    val messages = notices.receiveAsFlow()
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val state = combine(content,settings,sort) { library,preferences,order ->
        val audio=library.songs.filter { !preferences.hideShort || it.durationMs>=10_000 }.sortedMedia(order)
        val video=library.videos.filter { !preferences.hideShort || it.durationMs>=10_000 }.sortedMedia(order)
        library.copy(songs=audio,videos=video,folders=app.mediaRepository.foldersFrom(audio+video),sort=order)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),LibraryState())
    val favorites=dao.favorites().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val playlists=dao.playlists().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val collections=combine(dao.playlists(),dao.allEntries(),content) { lists,entries,library ->
        val media=library.all.associateBy { it.key }
        val groups=entries.groupBy { it.playlistId }
        lists.map { list ->
            val members=groups[list.id].orEmpty()
            val found=members.mapNotNull { media[it.kind+":"+it.mediaId] }
            PlaylistDetail(list,found,members.size-found.size)
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    val query=MutableStateFlow("")
    val searchResults=combine(query.debounce(220),state) { q,s -> s.all.matching(q) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    init { refresh() }
    fun refresh(sort: MediaSort = this.sort.value) {
        this.sort.value=sort
        if(refreshJob?.isActive==true) return
        refreshJob=viewModelScope.launch {
            content.value=content.value.copy(loading=true,error=null)
            try {
                val a=app.mediaRepository.audio()
                val v=app.mediaRepository.videos()
                content.value=LibraryState(a,v,app.mediaRepository.foldersFrom(a+v),false)
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { content.value=content.value.copy(loading=false,error="Não foi possível ler os arquivos. Confira o acesso à biblioteca.") }
        }
    }
    fun order(value: MediaSort) { sort.value=value }
    fun play(item: LocalMedia, source: List<LocalMedia>) = app.player.play(source,source.indexOf(item).coerceAtLeast(0))
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch(e: CancellationException) { throw e }
        catch(e: Exception) { notices.send("Não foi possível salvar. Tente novamente.") }
    }
    fun toggleFavorite(item: LocalMedia) = action { dao.toggleFavorite(FavoriteEntity(item.id,item.kind.name)) }
    fun createPlaylist(name: String, done: (Long)->Unit = {}) = action {
        if(name.isBlank()) return@action
        val id=dao.createPlaylist(PlaylistEntity(name=name.trim().take(80)))
        done(id)
    }
    fun renamePlaylist(id: Long,name: String) = action { if(name.isNotBlank()) dao.renamePlaylist(id,name.trim().take(80)) }
    fun deletePlaylist(id: Long) = action { dao.deleteCollection(id); notices.send("Playlist excluída. Seus arquivos foram mantidos.") }
    fun addToPlaylist(id: Long,item: LocalMedia) = addToPlaylist(id,listOf(item))
    fun addToPlaylist(id: Long,items: List<LocalMedia>, done: ()->Unit = {}) = action {
        dao.addMedia(id,items.map { PlaylistEntryEntity(id,it.id,it.kind.name,0) })
        notices.send("Mídias adicionadas à playlist.")
        done()
    }
    fun removeFromPlaylist(id: Long,item: LocalMedia) = action { dao.removeEntry(id,item.id,item.kind.name) }
    fun setTheme(value: ThemeMode) = action { app.settings.setTheme(value) }
    fun setVideoAudioBackground(value: Boolean) = action { app.settings.setVideoAudioBackground(value) }
    fun setResumePlayback(value: Boolean) = action { app.settings.setResumePlayback(value) }
    fun setHideShort(value: Boolean) = action { app.settings.setHideShort(value) }
    class Factory(private val app: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass: Class<T>):T=LibraryViewModel(app) as T
    }
}
