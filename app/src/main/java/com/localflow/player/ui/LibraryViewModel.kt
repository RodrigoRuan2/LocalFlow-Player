package com.localflow.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.localflow.player.AppContainer
import com.localflow.player.data.FavoriteEntity
import com.localflow.player.data.PlaylistEntity
import com.localflow.player.model.LocalMedia
import com.localflow.player.model.MediaFolder
import com.localflow.player.model.MediaKind
import com.localflow.player.model.MediaSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryState(val songs: List<LocalMedia> = emptyList(), val videos: List<LocalMedia> = emptyList(), val folders: List<MediaFolder> = emptyList(), val loading: Boolean = true, val error: String? = null, val sort: MediaSort = MediaSort.TITLE)
data class PlaylistDetail(val playlist: PlaylistEntity, val items: List<LocalMedia>)
class LibraryViewModel(private val app: AppContainer) : ViewModel() {
    private val content = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = content
    val favorites = app.database.libraryDao().favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists = app.database.libraryDao().playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.localflow.player.data.AppSettings())
    private val openedPlaylist = MutableStateFlow<PlaylistDetail?>(null)
    val playlistDetail: StateFlow<PlaylistDetail?> = openedPlaylist
    init { refresh() }
    fun refresh(sort: MediaSort = content.value.sort) = viewModelScope.launch { content.value = content.value.copy(loading = true, error = null, sort = sort); runCatching { val a = app.mediaRepository.audio(sort); val v = app.mediaRepository.videos(sort); Triple(a, v, app.mediaRepository.foldersFrom(a + v)) }.onSuccess { (a,v,f) -> content.value = LibraryState(a,v,f,false,sort = sort) }.onFailure { content.value = content.value.copy(loading = false, error = "Não foi possível ler a biblioteca. Verifique a permissão.") } }
    fun play(item: LocalMedia, source: List<LocalMedia>) = app.player.play(source, source.indexOf(item).coerceAtLeast(0))
    fun toggleFavorite(item: LocalMedia) = viewModelScope.launch { val value = FavoriteEntity(item.id, item.kind.name); if (favorites.value.any { it.mediaId == item.id && it.kind == item.kind.name }) app.database.libraryDao().removeFavorite(value) else app.database.libraryDao().addFavorite(value) }
    fun createPlaylist(name: String) = viewModelScope.launch { if (name.isNotBlank()) app.database.libraryDao().createPlaylist(PlaylistEntity(name = name.trim())) }
    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch { if (name.isNotBlank()) app.database.libraryDao().renamePlaylist(id, name.trim()) }
    fun deletePlaylist(id: Long) = viewModelScope.launch {
        val dao = app.database.libraryDao()
        dao.deleteEntries(id)
        dao.deletePlaylist(id)
        if (openedPlaylist.value?.playlist?.id == id) openedPlaylist.value = null
    }
    fun openPlaylist(playlist: PlaylistEntity) = viewModelScope.launch { openedPlaylist.value = playlistDetail(playlist) }
    fun closePlaylist() { openedPlaylist.value = null }
    fun addToPlaylist(id: Long, item: LocalMedia) = viewModelScope.launch {
        val dao = app.database.libraryDao()
        val entries = dao.entries(id)
        dao.addEntry(com.localflow.player.data.PlaylistEntryEntity(id, item.id, item.kind.name, entries.size))
        refreshOpenedPlaylist(id)
    }
    fun removeFromPlaylist(playlistId: Long, item: LocalMedia) = viewModelScope.launch {
        app.database.libraryDao().removeEntry(playlistId, item.id, item.kind.name)
        refreshOpenedPlaylist(playlistId)
    }
    fun setVideoAudioBackground(value: Boolean) = viewModelScope.launch { app.settings.setVideoAudioBackground(value) }
    fun setTheme(value: com.localflow.player.data.ThemeMode) = viewModelScope.launch { app.settings.setTheme(value) }
    fun setResumePlayback(value: Boolean) = viewModelScope.launch { app.settings.setResumePlayback(value) }
    fun mediaForFavorites(): List<LocalMedia> { val f = favorites.value.map { it.mediaId to it.kind }.toSet(); return (content.value.songs + content.value.videos).filter { it.id to it.kind.name in f } }
    private suspend fun refreshOpenedPlaylist(id: Long) {
        val current = openedPlaylist.value ?: return
        if (current.playlist.id == id) openedPlaylist.value = playlistDetail(current.playlist)
    }
    private suspend fun playlistDetail(playlist: PlaylistEntity): PlaylistDetail {
        val media = (content.value.songs + content.value.videos).associateBy { it.id to it.kind.name }
        val items = app.database.libraryDao().entries(playlist.id).mapNotNull { media[it.mediaId to it.kind] }
        return PlaylistDetail(playlist, items)
    }
    class Factory(private val app: AppContainer) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(app) as T }
}
