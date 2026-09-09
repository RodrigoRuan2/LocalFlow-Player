package com.localflow.player.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.localflow.player.model.LocalMedia
import com.localflow.player.repository.ArtworkRepository

@Composable fun MediaThumbnail(item: LocalMedia?, modifier: Modifier = Modifier.size(52.dp), large: Boolean = false) {
    Artwork(item?.uri,item?.artworkUri,item?.title.orEmpty(),item?.kind?.name=="VIDEO",modifier,large)
}
@Composable fun Artwork(uri: Uri?, albumArt: Uri?, title: String, video: Boolean, modifier: Modifier, large: Boolean = false) {
    val context=LocalContext.current.applicationContext
    var bitmap by remember(uri,albumArt,large) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uri,albumArt,large) {
        if(uri != null) bitmap=ArtworkRepository.load(context,uri,albumArt,if(large)512 else 128)
    }
    Surface(modifier,shape=RoundedCornerShape(if(large)22.dp else 12.dp),color=MaterialTheme.colorScheme.secondaryContainer) {
        val image=bitmap
        if(image!=null) Image(image.asImageBitmap(),title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        else {
            val palettes=listOf(0xFF44365B to 0xFF302B46,0xFF293F50 to 0xFF282C45,0xFF5A3C50 to 0xFF342C48,0xFF384455 to 0xFF322A46)
            val pair=palettes[(title.hashCode() and Int.MAX_VALUE)%palettes.size]
            Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.linearGradient(listOf(Color(pair.first),Color(pair.second))))
                    val radius=size.minDimension*.36f
                    val center=Offset(size.width*.72f,size.height*.28f)
                    drawCircle(Color.White.copy(alpha=.06f),radius*1.8f,center)
                    for(i in 1..4) drawCircle(Color.White.copy(alpha=.10f),radius*i/4,center,style=Stroke(1.dp.toPx()))
                }
                Icon(if(video) Icons.Default.PlayArrow else Icons.Default.MusicNote,null,Modifier.size(if(large)76.dp else 25.dp),tint=Color(0xFFE0D6FF))
                if(large) Text(if(video)"VÍDEO LOCAL" else title.ifBlank { "LOCALFLOW" }.take(30),Modifier.align(Alignment.BottomStart).padding(20.dp),color=Color(0xFFE0D6FF),style=MaterialTheme.typography.labelMedium)
            }
        }
    }
}
@Composable fun PlaylistMosaic(items: List<LocalMedia>, modifier: Modifier = Modifier.size(104.dp)) {
    val covers=items.take(4)
    Surface(modifier,shape=MaterialTheme.shapes.large) {
        Column {
            repeat(2) { row ->
                Row(Modifier.weight(1f)) { repeat(2) { col -> MediaThumbnail(covers.getOrNull(row*2+col),Modifier.weight(1f).fillMaxHeight().padding(1.dp)) } }
            }
        }
    }
}
