package com.localflow.player.repository

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.localflow.player.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreRepository(context: Context) {
    private val context = context.applicationContext
    private val resolver = context.contentResolver
    suspend fun audio(sort: MediaSort = MediaSort.TITLE) = query(MediaKind.AUDIO, sort)
    suspend fun videos(sort: MediaSort = MediaSort.TITLE) = query(MediaKind.VIDEO, sort)
    fun foldersFrom(items: List<LocalMedia>, sort: FolderSort = FolderSort.NAME) = items.groupBy { it.folder }.map { (path, media) ->
        MediaFolder(
            name=path,
            count=media.size,
            representative=media.first(),
            musicCount=media.count { it.kind==MediaKind.AUDIO && !it.isWhatsAppAudio() },
            whatsAppCount=media.count { it.isWhatsAppAudio() },
            videoCount=media.count { it.kind==MediaKind.VIDEO }
        )
    }.let { folders -> when(sort) { FolderSort.NAME -> folders.sortedBy { it.name.lowercase() }; FolderSort.MOST_ITEMS -> folders.sortedWith(compareByDescending<MediaFolder> { it.count }.thenBy { it.name.lowercase() }) } }

    /** Called only after the Android write-consent dialog has approved these exact URIs. */
    suspend fun moveToManagedFolder(items: List<LocalMedia>, destinationName: String): Int = withContext(Dispatchers.IO) {
        val safeName=destinationName.trim().replace(Regex("[\\\\/:*?\"<>|]"),"-").take(80).trim('.',' ')
        require(safeName.isNotBlank()) { "Escolha um nome de pasta válido." }
        var moved=0
        items.forEach { item ->
            val relative=if(item.kind==MediaKind.AUDIO) "Music/$safeName/" else "Movies/$safeName/"
            val values=ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH,relative) }
            if(resolver.update(item.uri,values,null,null)>0) moved++
        }
        moved
    }

    private suspend fun query(kind: MediaKind, sort: MediaSort): List<LocalMedia> = withContext(Dispatchers.IO) {
        val permission = if (Build.VERSION.SDK_INT >= 33) { if (kind == MediaKind.AUDIO) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_MEDIA_VIDEO } else Manifest.permission.READ_EXTERNAL_STORAGE
        val selectedVideos = kind==MediaKind.VIDEO && Build.VERSION.SDK_INT>=34 && context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)==PackageManager.PERMISSION_GRANTED
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED && !selectedVideos) return@withContext emptyList()
        val collection = if (kind == MediaKind.AUDIO) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val pathColumn = if (Build.VERSION.SDK_INT >= 29) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val projection = mutableListOf("_id", "_display_name", "title", "duration", "date_added", "_size", pathColumn)
        if (kind == MediaKind.AUDIO) projection.addAll(listOf("artist", "album", "album_id"))
        val order = when(sort) { MediaSort.TITLE -> "title COLLATE NOCASE"; MediaSort.ARTIST -> if(kind == MediaKind.AUDIO) "artist COLLATE NOCASE" else "title COLLATE NOCASE"; MediaSort.DATE_ADDED -> "date_added DESC"; MediaSort.DURATION -> "duration DESC" }
        val result = ArrayList<LocalMedia>()
        resolver.query(collection, projection.toTypedArray(), if (Build.VERSION.SDK_INT >= 29) "is_pending=0" else null, null, order)?.use { c ->
            fun str(name: String): String? { val i=c.getColumnIndex(name); return if(i<0 || c.isNull(i)) null else c.getString(i)?.takeUnless { it.isBlank() || it=="<unknown>" } }
            fun num(name: String): Long { val i=c.getColumnIndex(name); return if(i<0 || c.isNull(i)) 0 else c.getLong(i) }
            while(c.moveToNext()) {
                val id=num("_id"); val duration=num("duration"); if(id<=0 || duration<=0) continue
                val uri=ContentUris.withAppendedId(collection,id)
                val file=str("_display_name") ?: "Arquivo $id"
                val path=str(pathColumn).orEmpty()
                val folder=if(Build.VERSION.SDK_INT>=29) path.trimEnd('/') else path.substringBeforeLast('/', "Armazenamento")
                val albumId=num("album_id")
                result += LocalMedia(id,uri,kind,mediaDisplayTitle(str("title"),file,kind,id),mediaDisplayArtist(str("artist")),str("album"),duration,num("date_added"),num("_size"),folder.ifBlank { "Armazenamento" },albumId,
                    if(kind==MediaKind.VIDEO) uri else if(albumId>0) ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId) else null)
            }
        }
        result
    }
}
