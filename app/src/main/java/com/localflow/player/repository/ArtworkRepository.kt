package com.localflow.player.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Two decoders at most; no full-resolution bitmaps kept in RAM. */
object ArtworkRepository {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val missing = LruCache<String, Long>(256)
    private val permits = Semaphore(2)
    fun clear() { cache.evictAll(); missing.evictAll() }
    suspend fun load(context: Context, uri: Uri, albumArt: Uri? = null, requested: Int = 128): Bitmap? {
        val size = if(requested <= 160) 128 else 512
        val key = "$uri:$size"
        cache.get(key)?.let { return it }
        if(System.currentTimeMillis() - (missing.get(key) ?: 0L) < 60_000L) return null
        return permits.withPermit {
            withContext(Dispatchers.IO) {
                cache.get(key)?.let { return@withContext it }
                val resolver = context.applicationContext.contentResolver
                var bitmap: Bitmap? = null
                if(Build.VERSION.SDK_INT >= 29) {
                    val signal = CancellationSignal()
                    val handle = currentCoroutineContext().job.invokeOnCompletion { if(it is CancellationException) signal.cancel() }
                    try { bitmap = runCatching { resolver.loadThumbnail(uri, Size(size,size), signal) }.getOrNull() }
                    finally { handle.dispose() }
                }
                ensureActive()
                if(bitmap == null && albumArt != null && albumArt != uri) {
                    bitmap = runCatching {
                        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                        resolver.openInputStream(albumArt)?.use { BitmapFactory.decodeStream(it,null,bounds) }
                        val opts=options(bounds.outWidth,bounds.outHeight,size)
                        resolver.openInputStream(albumArt)?.use { BitmapFactory.decodeStream(it,null,opts) }
                    }.getOrNull()
                }
                if(bitmap == null && uri.toString().contains("/audio/")) {
                    bitmap = runCatching {
                        val retriever=MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context.applicationContext,uri)
                            retriever.embeddedPicture?.takeIf { it.size <= 8 * 1024 * 1024 }?.let { decode(it,size) }
                        } finally { retriever.release() }
                    }.getOrNull()
                }
                if(bitmap == null && Build.VERSION.SDK_INT < 29 && uri.toString().contains("/video/")) {
                    @Suppress("DEPRECATION")
                    bitmap = runCatching { MediaStore.Video.Thumbnails.getThumbnail(resolver,ContentUris.parseId(uri),MediaStore.Video.Thumbnails.MINI_KIND,BitmapFactory.Options()) }.getOrNull()
                }
                ensureActive()
                bitmap?.let {
                    val ratio = size.toFloat() / maxOf(it.width,it.height)
                    val bounded=if(ratio<1) Bitmap.createScaledBitmap(it,(it.width*ratio).toInt().coerceAtLeast(1),(it.height*ratio).toInt().coerceAtLeast(1),true) else it
                    if(bounded !== it) it.recycle()
                    cache.put(key,bounded)
                    bounded
                } ?: run { missing.put(key,System.currentTimeMillis()); null }
            }
        }
    }
    fun decode(bytes: ByteArray, size: Int = 512): Bitmap? {
        if(bytes.size>8*1024*1024) return null
        val b=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        BitmapFactory.decodeByteArray(bytes,0,bytes.size,b)
        return BitmapFactory.decodeByteArray(bytes,0,bytes.size,options(b.outWidth,b.outHeight,size))
    }
    private fun options(width: Int,height: Int,size: Int) = BitmapFactory.Options().apply {
        var sample=1
        while(maxOf(width,height)/sample>size*2) sample*=2
        inSampleSize=sample
    }
}
