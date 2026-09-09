package com.localflow.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localflow.player.model.*

@Composable fun PageHeader(title: String, subtitle: String = "", back: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        if(back!=null) IconButton(back) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Voltar") }
        else Surface(Modifier.size(38.dp),shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.primary) {
            Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.GraphicEq,null,tint=MaterialTheme.colorScheme.onPrimary) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
            if(subtitle.isNotBlank()) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        actions()
    }
}
@Composable fun SectionTitle(title: String,action: String?=null,onClick: ()->Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        if(action!=null) TextButton(onClick) { Text(action) }
    }
}
@Composable fun SearchShortcut(open: ()->Unit) {
    Surface(onClick=open,shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surfaceContainer,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Search,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp)); Text("Músicas, artistas, álbuns...",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable fun EmptyContent(title: String,description: String,icon: ImageVector=Icons.Default.LibraryMusic,action: String?=null,onAction: ()->Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Surface(Modifier.size(80.dp),shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.secondaryContainer) {
            Box(contentAlignment=Alignment.Center) { Icon(icon,null,Modifier.size(38.dp),tint=MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(18.dp)); Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp)); Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium)
        if(action!=null) { Spacer(Modifier.height(20.dp)); Button(onAction) { Text(action) } }
    }
}
@Composable fun Hint(text: String) {
    Surface(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainer) {
        Text(text,Modifier.padding(14.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun MediaRow(item: LocalMedia,favorite: Boolean=false,play: ()->Unit,add: ()->Unit,toggleFavorite: ()->Unit,selected: Boolean?=null,select: ()->Unit = {},remove: (() -> Unit)?=null) {
    var menu by remember(item.key) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable(onClick=if(selected==null) play else select).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
        if(selected!=null) Checkbox(selected,{ select() })
        MediaThumbnail(item,if(item.kind==MediaKind.VIDEO) Modifier.width(88.dp).height(58.dp) else Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(if(item.kind==MediaKind.VIDEO) formatTime(item.durationMs)+" · "+formatSize(item.sizeBytes) else item.artist,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            if(item.kind==MediaKind.AUDIO) Text((item.album ?: "Álbum desconhecido")+" · "+formatTime(item.durationMs),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(favorite && selected==null) Icon(Icons.Default.Favorite,null,Modifier.size(14.dp),tint=MaterialTheme.colorScheme.primary)
        if(remove!=null) IconButton(remove) { Icon(Icons.Default.RemoveCircleOutline,"Remover da playlist") }
        else if(selected==null) Box {
            IconButton({ menu=true }) { Icon(Icons.Default.MoreVert,"Opções de "+item.title) }
            DropdownMenu(menu,{ menu=false }) {
                DropdownMenuItem(text={ Text("Adicionar à playlist") },leadingIcon={ Icon(Icons.AutoMirrored.Filled.PlaylistAdd,null) },onClick={ menu=false; add() })
                DropdownMenuItem(text={ Text(if(favorite) "Remover dos favoritos" else "Favoritar") },leadingIcon={ Icon(Icons.Default.FavoriteBorder,null) },onClick={ menu=false; toggleFavorite() })
            }
        }
    }
}
@Composable fun PreferenceRow(title: String,description: String,checked: Boolean,change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title,style=MaterialTheme.typography.bodyLarge); Text(description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.width(8.dp)); Switch(checked,change)
    }
}
