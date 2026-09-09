package com.localflow.player.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.localflow.player.model.LocalMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bounded, decoded-size thumbnail cache. MediaStore performs the downsampling. */
private object ThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    suspend fun get(context: Context, item: LocalMedia): Bitmap? = cache.get(item.uri.toString()) ?: withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        runCatching { context.contentResolver.loadThumbnail(item.uri, Size(96, 96), null) }.getOrNull()?.also { cache.put(item.uri.toString(), it) }
    }
}
@Composable fun MediaThumbnail(item: LocalMedia) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, item.uri) { value = ThumbnailCache.get(context.applicationContext, item) }
    Surface(Modifier.size(48.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), null) else Box(contentAlignment = Alignment.Center) { Text(if (item.kind.name == "AUDIO") "♫" else "▶") }
    }
}
