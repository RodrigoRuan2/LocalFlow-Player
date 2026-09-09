package com.localflow.player.model

import android.net.Uri

enum class MediaKind { AUDIO, VIDEO }
enum class MediaSort { TITLE, ARTIST, DATE_ADDED, DURATION }

data class LocalMedia(
    val id: Long,
    val uri: Uri,
    val kind: MediaKind,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val dateAddedSeconds: Long,
    val sizeBytes: Long,
    val folder: String,
    val albumId: Long = 0,
    val artworkUri: Uri? = null
)

val LocalMedia.key: String get() = "${kind.name}:$id"

data class MediaFolder(val name: String, val count: Int, val representative: LocalMedia)
