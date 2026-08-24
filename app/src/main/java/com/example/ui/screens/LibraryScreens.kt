package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Folder
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.components.SongItemRow
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

// --- ALBUMS SCREEN ---
@Composable
fun AlbumsScreen(
    albums: List<Album>,
    allSongs: List<Song>,
    selectedAlbum: Album?,
    onSelectAlbum: (Album?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    val albumSongs = remember(selectedAlbum, allSongs) {
        if (selectedAlbum != null) {
            allSongs.filter { it.album.equals(selectedAlbum.title, ignoreCase = true) }
        } else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .padding(16.dp)
            .testTag("albums_screen")
    ) {
        if (selectedAlbum != null) {
            // Album Detail View
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = { onSelectAlbum(null) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = palette.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedAlbum.title, style = MaterialTheme.typography.titleLarge, color = TextWhitePrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${selectedAlbum.artist} • ${albumSongs.size} tracks", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                }
                Button(
                    onClick = {
                        if (albumSongs.isNotEmpty()) {
                            onPlaySong(albumSongs.first(), albumSongs)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(albumSongs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentPlayingSongId
                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isMultiSelectMode = false,
                        isSelected = false,
                        onClick = { onPlaySong(song, albumSongs) },
                        onToggleSelect = {},
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = {},
                        onAddToQueue = {},
                        onAddToPlaylist = {},
                        onEditMetadata = {},
                        onOpenInStudio = {},
                        onSongInfo = {}
                    )
                }
            }
        } else {
            // Albums Grid
            Text(
                text = "Albums (${albums.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhitePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(albums, key = { it.id }) { album ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAlbum(album) }
                            .testTag("album_card_${album.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.cardBackground)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(palette.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (album.albumArtResId != null) {
                                    Image(
                                        painter = painterResource(id = album.albumArtResId),
                                        contentDescription = album.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else if (album.albumArtUri != null) {
                                    AsyncImage(
                                        model = album.albumArtUri,
                                        contentDescription = album.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Album, null, tint = palette.primary, modifier = Modifier.size(48.dp))
                                }
                            }

                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = album.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextWhitePrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${album.artist} • ${album.songCount} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGraySecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ARTISTS SCREEN ---
@Composable
fun ArtistsScreen(
    artists: List<Artist>,
    allSongs: List<Song>,
    selectedArtist: Artist?,
    onSelectArtist: (Artist?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    val artistSongs = remember(selectedArtist, allSongs) {
        if (selectedArtist != null) {
            allSongs.filter { it.artist.equals(selectedArtist.name, ignoreCase = true) }
        } else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .padding(16.dp)
            .testTag("artists_screen")
    ) {
        if (selectedArtist != null) {
            // Artist Detail View
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = { onSelectArtist(null) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = palette.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedArtist.name, style = MaterialTheme.typography.titleLarge, color = TextWhitePrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${artistSongs.size} tracks in library", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                }
                Button(
                    onClick = {
                        if (artistSongs.isNotEmpty()) {
                            onPlaySong(artistSongs.first(), artistSongs)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(artistSongs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentPlayingSongId
                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isMultiSelectMode = false,
                        isSelected = false,
                        onClick = { onPlaySong(song, artistSongs) },
                        onToggleSelect = {},
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = {},
                        onAddToQueue = {},
                        onAddToPlaylist = {},
                        onEditMetadata = {},
                        onOpenInStudio = {},
                        onSongInfo = {}
                    )
                }
            }
        } else {
            // Artists List
            Text(
                text = "Artists (${artists.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhitePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(artists, key = { it.id }) { artist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectArtist(artist) }
                            .testTag("artist_card_${artist.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(palette.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = artist.name.take(1).uppercase(),
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(artist.name, style = MaterialTheme.typography.titleMedium, color = TextWhitePrimary, fontWeight = FontWeight.Bold)
                                Text("${artist.songCount} songs", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FOLDERS SCREEN ---
@Composable
fun FoldersScreen(
    folders: List<Folder>,
    allSongs: List<Song>,
    selectedFolder: Folder?,
    onSelectFolder: (Folder?) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    val folderSongs = remember(selectedFolder, allSongs) {
        if (selectedFolder != null) {
            allSongs.filter { it.path.contains(selectedFolder.name) || it.path.contains(selectedFolder.path) }
        } else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .padding(16.dp)
            .testTag("folders_screen")
    ) {
        if (selectedFolder != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = { onSelectFolder(null) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = palette.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedFolder.name, style = MaterialTheme.typography.titleLarge, color = TextWhitePrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${folderSongs.size} tracks • ${selectedFolder.path}", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary, maxLines = 1)
                }
                Button(
                    onClick = {
                        if (folderSongs.isNotEmpty()) {
                            onPlaySong(folderSongs.first(), folderSongs)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(folderSongs, key = { it.id }) { song ->
                    val isCurrent = song.id == currentPlayingSongId
                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isMultiSelectMode = false,
                        isSelected = false,
                        onClick = { onPlaySong(song, folderSongs) },
                        onToggleSelect = {},
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = {},
                        onAddToQueue = {},
                        onAddToPlaylist = {},
                        onEditMetadata = {},
                        onOpenInStudio = {},
                        onSongInfo = {}
                    )
                }
            }
        } else {
            Text(
                text = "Storage Folders (${folders.size})",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhitePrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.path }) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(folder) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = palette.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(folder.name, style = MaterialTheme.typography.titleMedium, color = TextWhitePrimary, fontWeight = FontWeight.Bold)
                                Text("${folder.songCount} files • ${folder.path}", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PLAYLISTS SCREEN ---
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    allSongs: List<Song>,
    selectedPlaylist: Playlist?,
    onSelectPlaylist: (Playlist?) -> Unit,
    onCreatePlaylist: () -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    currentPlayingSongId: Long?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .padding(16.dp)
            .testTag("playlists_screen")
    ) {
        if (selectedPlaylist != null) {
            // In a playlist detail view, for default/favorites it shows favorite tracks, or all songs for custom playlists
            val playlistTracks = remember(selectedPlaylist, allSongs) {
                if (selectedPlaylist.name.contains("Favorite", ignoreCase = true)) {
                    allSongs.filter { it.isFavorite }
                } else {
                    allSongs.take(selectedPlaylist.songCount.coerceAtLeast(1))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                IconButton(onClick = { onSelectPlaylist(null) }) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = palette.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedPlaylist.name, style = MaterialTheme.typography.titleLarge, color = TextWhitePrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${playlistTracks.size} tracks", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                }
                IconButton(onClick = { onDeletePlaylist(selectedPlaylist.id) }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFE53935))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(playlistTracks, key = { it.id }) { song ->
                    val isCurrent = song.id == currentPlayingSongId
                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        isMultiSelectMode = false,
                        isSelected = false,
                        onClick = { onPlaySong(song, playlistTracks) },
                        onToggleSelect = {},
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = {},
                        onAddToQueue = {},
                        onAddToPlaylist = {},
                        onEditMetadata = {},
                        onOpenInStudio = {},
                        onSongInfo = {}
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlists (${playlists.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhitePrimary,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onCreatePlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Playlist", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPlaylist(pl) }
                            .testTag("playlist_card_${pl.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.QueueMusic, null, tint = palette.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(pl.name, style = MaterialTheme.typography.titleMedium, color = TextWhitePrimary, fontWeight = FontWeight.Bold)
                                    Text("${pl.songCount} songs", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                                }
                            }
                            IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                Icon(Icons.Default.Delete, "Delete", tint = TextMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}
