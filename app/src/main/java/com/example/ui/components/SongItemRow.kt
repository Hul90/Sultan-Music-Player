package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.Song
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

@Composable
fun SongItemRow(
    song: Song,
    isCurrentPlaying: Boolean,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
    onOpenInStudio: () -> Unit,
    onSongInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    var menuExpanded by remember { mutableStateOf(false) }

    val rowBgColor by animateColorAsState(
        if (isSelected) palette.primary.copy(alpha = 0.2f)
        else if (isCurrentPlaying) palette.cardBackground.copy(alpha = 0.95f)
        else Color.Transparent,
        label = "rowBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isMultiSelectMode) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }
            .testTag("song_item_${song.id}"),
        color = rowBgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-select Checkbox
            AnimatedVisibility(visible = isMultiSelectMode) {
                Row {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select",
                        tint = if (isSelected) palette.primary else TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onToggleSelect() }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }

            // Album Artwork / Thumbnail
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.cardBackground),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtResId != null) {
                    Image(
                        painter = painterResource(id = song.albumArtResId),
                        contentDescription = song.title,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } else if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = palette.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isCurrentPlaying) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrentPlaying) palette.primary else TextWhitePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Medium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGraySecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = " • ${song.formattedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("fav_btn_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Color(0xFFFF4081) else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("more_btn_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = TextGraySecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(palette.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Play Now", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Play Next", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.SkipNext, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.Queue, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open in Sultan Audio Studio", color = palette.primary, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.ContentCut, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onOpenInStudio()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Metadata (ID3)", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onEditMetadata()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Song Information", color = TextWhitePrimary) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = palette.primary) },
                        onClick = {
                            menuExpanded = false
                            onSongInfo()
                        }
                    )
                }
            }
        }
    }
}
