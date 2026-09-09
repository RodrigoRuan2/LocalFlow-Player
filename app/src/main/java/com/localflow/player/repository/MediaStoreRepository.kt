package com.localflow.player.repository

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.localflow.player.model.LocalMedia
import com.localflow.player.model.MediaFolder
import com.localflow.player.model.MediaKind
import com.localflow.player.model.MediaSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(context: Context) {
    private val resolver: ContentResolver = context.contentResolver
    suspend fun audio(sort: MediaSort = MediaSort.TITLE): List<LocalMedia> = query(MediaKind.AUDIO, sort)
    suspend fun videos(sort: MediaSort = MediaSort.TITLE): List<LocalMedia> = query(MediaKind.VIDEO, sort)
    fun foldersFrom(items: List<LocalMedia>): List<MediaFolder> = items.groupBy { it.folder }.map { (name, files) -> MediaFolder(name, files.size, files.first()) }.sortedBy { it.name.lowercase() }
    private suspend fun query(kind: MediaKind, sort: MediaSort): List<LocalMedia> = withContext(Dispatchers.IO) {
        val collection = if (kind == MediaKind.AUDIO) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val projection = if (kind == MediaKind.AUDIO) arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, pathColumn, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM) else arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.SIZE, pathColumn)
        val order = when (sort) { MediaSort.TITLE -> "${MediaStore.MediaColumns.DISPLAY_NAME} COLLATE NOCASE"; MediaSort.ARTIST -> "${MediaStore.Audio.AudioColumns.ARTIST} COLLATE NOCASE"; MediaSort.DATE_ADDED -> "${MediaStore.MediaColumns.DATE_ADDED} DESC"; MediaSort.DURATION -> "${MediaStore.MediaColumns.DURATION} DESC" }
        val result = ArrayList<LocalMedia>()
        resolver.query(collection, projection, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "${MediaStore.MediaColumns.IS_PENDING}=0" else null, null, order)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID); val name = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME); val duration = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION); val added = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED); val size = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE); val path = c.getColumnIndexOrThrow(pathColumn); val artist = c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST); val album = c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM)
            while (c.moveToNext()) { val fileName = c.getString(name).orEmpty(); if (c.getLong(duration) > 0) result += LocalMedia(c.getLong(id), android.content.ContentUris.withAppendedId(collection, c.getLong(id)), kind, fileName.substringBeforeLast('.', fileName), c.getStringOrNull(artist) ?: "Artista desconhecido", c.getStringOrNull(album), c.getLong(duration), c.getLong(added), c.getLong(size), c.getString(path)?.trimEnd('/')?.substringAfterLast('/')?.ifBlank { "Armazenamento" } ?: "Armazenamento") }
        }
        result
    }
    private fun android.database.Cursor.getStringOrNull(index: Int): String? = if (index >= 0 && !isNull(index)) getString(index)?.takeUnless { it == "<unknown>" } else null
}
