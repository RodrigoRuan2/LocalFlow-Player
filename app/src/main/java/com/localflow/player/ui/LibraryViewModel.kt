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
class LibraryViewModel(private val app: AppContainer) : ViewModel() {
    private val content = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = content
    val favorites = app.database.libraryDao().favorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playlists = app.database.libraryDao().playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = app.settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.localflow.player.data.AppSettings())
    init { refresh() }
    fun refresh(sort: MediaSort = content.value.sort) = viewModelScope.launch { content.value = content.value.copy(loading = true, error = null, sort = sort); runCatching { val a = app.mediaRepository.audio(sort); val v = app.mediaRepository.videos(sort); Triple(a, v, app.mediaRepository.foldersFrom(a + v)) }.onSuccess { (a,v,f) -> content.value = LibraryState(a,v,f,false,sort = sort) }.onFailure { content.value = content.value.copy(loading = false, error = "Não foi possível ler a biblioteca. Verifique a permissão.") } }
    fun play(item: LocalMedia, source: List<LocalMedia>) = app.player.play(source, source.indexOf(item).coerceAtLeast(0))
    fun toggleFavorite(item: LocalMedia) = viewModelScope.launch { val value = FavoriteEntity(item.id, item.kind.name); if (favorites.value.any { it.mediaId == item.id && it.kind == item.kind.name }) app.database.libraryDao().removeFavorite(value) else app.database.libraryDao().addFavorite(value) }
    fun createPlaylist(name: String) = viewModelScope.launch { if (name.isNotBlank()) app.database.libraryDao().createPlaylist(PlaylistEntity(name = name.trim())) }
    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch { if (name.isNotBlank()) app.database.libraryDao().renamePlaylist(id, name.trim()) }
    fun deletePlaylist(id: Long) = viewModelScope.launch { app.database.libraryDao().deleteEntries(id); app.database.libraryDao().deletePlaylist(id) }
    fun addToPlaylist(id: Long, item: LocalMedia) = viewModelScope.launch { val dao = app.database.libraryDao(); val position = dao.entries(id).size; dao.addEntry(com.localflow.player.data.PlaylistEntryEntity(id, item.id, item.kind.name, position)) }
    fun setVideoAudioBackground(value: Boolean) = viewModelScope.launch { app.settings.setVideoAudioBackground(value) }
    fun setTheme(value: com.localflow.player.data.ThemeMode) = viewModelScope.launch { app.settings.setTheme(value) }
    fun setResumePlayback(value: Boolean) = viewModelScope.launch { app.settings.setResumePlayback(value) }
    fun mediaForFavorites(): List<LocalMedia> { val f = favorites.value.map { it.mediaId to it.kind }.toSet(); return (content.value.songs + content.value.videos).filter { it.id to it.kind.name in f } }
    class Factory(private val app: AppContainer) : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryViewModel(app) as T }
}
