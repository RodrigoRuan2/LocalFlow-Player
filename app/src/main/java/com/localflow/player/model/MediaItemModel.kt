package com.localflow.player.model

import android.net.Uri

enum class MediaKind { AUDIO, VIDEO }
enum class MediaSort { TITLE, ARTIST, DATE_ADDED, DURATION }
enum class LibrarySection { MUSIC, WHATSAPP_AUDIO, VIDEO }

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

/** WhatsApp locations vary by Android version and by the Business edition. */
fun isWhatsAppAudioFolder(folder: String): Boolean = normalized(folder)
    .replace('\\', '/')
    .contains("whatsapp")

fun LocalMedia.isWhatsAppAudio(): Boolean = kind == MediaKind.AUDIO && isWhatsAppAudioFolder(folder)

data class MediaFolder(
    val name: String,
    val count: Int,
    val representative: LocalMedia,
    val musicCount: Int = 0,
    val whatsAppCount: Int = 0,
    val videoCount: Int = 0
)

sealed interface MediaFileOperation {
    val items: List<LocalMedia>
    data class Delete(override val items: List<LocalMedia>) : MediaFileOperation
    data class Move(override val items: List<LocalMedia>, val destinationName: String) : MediaFileOperation
}
