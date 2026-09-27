package com.localflow.player.model

import java.text.Normalizer

private val accentMarks="\\p{M}+".toRegex()
fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD).replace(accentMarks, "").lowercase(java.util.Locale.ROOT)
fun List<LocalMedia>.matching(query: String): List<LocalMedia> {
    val terms = normalized(query).split(' ').filter { it.isNotBlank() }
    if(terms.isEmpty()) return this
    return filter { item -> val text=normalized("${item.title} ${item.artist} ${item.album.orEmpty()}"); terms.all { it in text } }
}
fun List<LocalMedia>.sortedMedia(sort: MediaSort): List<LocalMedia> = when(sort) {
    MediaSort.TITLE -> sortedBy { normalized(it.title) }
    MediaSort.ARTIST -> sortedWith(compareBy({normalized(it.artist)},{normalized(it.title)}))
    MediaSort.DATE_ADDED -> sortedByDescending { it.dateAddedSeconds }
    MediaSort.DURATION -> sortedByDescending { it.durationMs }
}
fun formatTime(ms: Long): String { val s=ms.coerceAtLeast(0)/1000; return if(s>=3600) "%d:%02d:%02d".format(s/3600,s/60%60,s%60) else "%d:%02d".format(s/60,s%60) }
fun formatSize(bytes: Long): String = if(bytes>=1_073_741_824) "%.1f GB".format(bytes/1_073_741_824.0) else "%.0f MB".format(bytes/1_048_576.0)

/** Some providers expose damaged tags as only question or replacement characters. */
private fun readableMetadata(value: String?): String? {
    val cleaned=value?.trim()?.takeUnless { it.isBlank() || it.equals("<unknown>",ignoreCase=true) } ?: return null
    return cleaned.takeUnless { text -> text.all { it=='?' || it=='�' || it.isWhitespace() } }
}

fun mediaDisplayTitle(tagTitle: String?, fileName: String, kind: MediaKind, id: Long): String {
    val fileTitle=fileName.substringBeforeLast('.',fileName)
    return readableMetadata(tagTitle)
        ?: readableMetadata(fileTitle)
        ?: if(kind==MediaKind.AUDIO) "Áudio sem título $id" else "Vídeo sem título $id"
}

fun mediaDisplayArtist(tagArtist: String?): String = readableMetadata(tagArtist) ?: "Artista desconhecido"
