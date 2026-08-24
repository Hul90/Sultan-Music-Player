package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary
import java.io.File

@Composable
fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSultanPalette.current
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlaylistAdd, null, tint = palette.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Playlist", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                placeholder = { Text("e.g. Sultan Party Mix") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playlist_name_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = palette.primary,
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_create_playlist")
            ) {
                Text("Create", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGraySecondary)
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onSelectPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSultanPalette.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Add to Playlist", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onCreateNewPlaylist()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("New Playlist...", color = palette.primary, fontWeight = FontWeight.Bold)
                }

                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectPlaylist(pl)
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QueueMusic, null, tint = TextGraySecondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(pl.name, color = TextWhitePrimary, fontWeight = FontWeight.Medium)
                            Text("${pl.songCount} songs", color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextGraySecondary)
            }
        }
    )
}

@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    val palette = LocalSultanPalette.current
    val file = File(song.path)
    val sizeMb = if (song.sizeBytes > 0) String.format("%.2f MB", song.sizeBytes / (1024.0 * 1024.0)) else "Unknown"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(Icons.Default.Info, null, tint = palette.primary, modifier = Modifier.size(28.dp))
        },
        title = {
            Text("Song Details", color = TextWhitePrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoItem("Title", song.title)
                InfoItem("Artist", song.artist)
                InfoItem("Album", song.album)
                InfoItem("Genre", song.genre)
                InfoItem("Duration", song.formattedDuration)
                InfoItem("File Size", sizeMb)
                InfoItem("Year", if (song.year > 0) song.year.toString() else "Unknown")
                InfoItem("Track #", if (song.trackNumber > 0) song.trackNumber.toString() else "N/A")
                InfoItem("Play Count", "${song.playCount} times")
                InfoItem("Location", song.path)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = LocalSultanPalette.current.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(value, color = TextWhitePrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
