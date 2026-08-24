package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CategoryTab
import com.example.model.PlaybackState
import com.example.model.Song
import com.example.model.SortOption
import com.example.model.ThemeOption
import com.example.ui.components.SongItemRow
import com.example.ui.components.ThemeSelectorBar
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

@Composable
fun HomeScreen(
    displayedSongs: List<Song>,
    playbackState: PlaybackState,
    currentCategory: CategoryTab,
    currentTheme: ThemeOption,
    currentSort: SortOption,
    searchQuery: String,
    searchHistory: List<String>,
    isMultiSelectMode: Boolean,
    selectedSongIds: Set<Long>,
    isLoading: Boolean,
    onSelectCategory: (CategoryTab) -> Unit,
    onSelectTheme: (ThemeOption) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onOpenSortDialog: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onSelectSong: (Song) -> Unit,
    onToggleSelectSong: (Long) -> Unit,
    onSelectAllSongs: () -> Unit,
    onPlaySelectedSongs: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onEditMetadata: (Song) -> Unit,
    onOpenInStudio: (Song) -> Unit,
    onSongInfo: (Song) -> Unit,
    onRescanMusic: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToArtists: () -> Unit,
    onNavigateToFolders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .testTag("home_screen")
    ) {
        // TOP APP BAR WITH SULTAN BRANDING
        Surface(
            color = palette.surface.copy(alpha = 0.95f),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Sultan Logo and App Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSearchQueryChange("") }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "Sultan Logo",
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "SULTAN",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Music Player",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextWhitePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Action Icons: Search, Sort, Multi-Select, Refresh
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearchExpanded = !isSearchExpanded },
                            modifier = Modifier.testTag("top_search_btn")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded) palette.primary else TextWhitePrimary
                            )
                        }

                        IconButton(
                            onClick = onOpenSortDialog,
                            modifier = Modifier.testTag("top_sort_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort",
                                tint = TextWhitePrimary
                            )
                        }

                        IconButton(
                            onClick = onToggleMultiSelect,
                            modifier = Modifier.testTag("top_multiselect_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Multi Select",
                                tint = if (isMultiSelectMode) palette.primary else TextWhitePrimary
                            )
                        }

                        IconButton(
                            onClick = onRescanMusic,
                            modifier = Modifier.testTag("top_refresh_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan",
                                tint = TextGraySecondary
                            )
                        }
                    }
                }

                // EXPANDABLE SEARCH BAR & HISTORY CHIPS
                AnimatedVisibility(visible = isSearchExpanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search songs, artists, albums, genres...") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = onClearSearch) {
                                        Icon(Icons.Default.Clear, "Clear", tint = TextGraySecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = TextWhitePrimary,
                                unfocusedTextColor = TextWhitePrimary,
                                focusedContainerColor = palette.cardBackground,
                                unfocusedContainerColor = palette.cardBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_text_input")
                        )

                        // Search History Chips
                        if (searchHistory.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                searchHistory.forEach { historyQuery ->
                                    Surface(
                                        color = palette.cardBackground,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { onSearchQueryChange(historyQuery) }
                                    ) {
                                        Text(
                                            text = historyQuery,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextGraySecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // MULTI-SELECT FLOATING ACTION BAR
        AnimatedVisibility(visible = isMultiSelectMode) {
            Surface(
                color = palette.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedSongIds.size} selected",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onSelectAllSongs) {
                            Icon(Icons.Default.SelectAll, "Select All", tint = Color.Black)
                        }
                        IconButton(
                            onClick = onPlaySelectedSongs,
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play Selected", tint = Color.Black)
                        }
                        IconButton(
                            onClick = onAddSelectedToQueue,
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Queue, "Add to Queue", tint = Color.Black)
                        }
                    }
                }
            }
        }

        // QUICK ACTION FEATURE CARDS (Horizontal Scroll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                QuickActionCard(
                    title = "All Songs",
                    subtitle = "${displayedSongs.size} tracks",
                    icon = Icons.Default.MusicNote,
                    gradient = Brush.linearGradient(listOf(Color(0xFF6200EA), Color(0xFF9D46FF))),
                    onClick = { onSelectCategory(CategoryTab.SONGS) }
                )
            }
            item {
                QuickActionCard(
                    title = "Favorites",
                    subtitle = "Loved tracks",
                    icon = Icons.Default.Favorite,
                    gradient = Brush.linearGradient(listOf(Color(0xFFC2185B), Color(0xFFFF4081))),
                    onClick = { onSelectCategory(CategoryTab.FAVORITES) }
                )
            }
            item {
                QuickActionCard(
                    title = "Sultan Studio",
                    subtitle = "Remix & Trim",
                    icon = Icons.Default.ContentCut,
                    gradient = Brush.linearGradient(listOf(Color(0xFFFF6D00), Color(0xFFFFAB40))),
                    onClick = onNavigateToStudio
                )
            }
            item {
                QuickActionCard(
                    title = "Equalizer",
                    subtitle = "5-Band & Bass",
                    icon = Icons.Default.Equalizer,
                    gradient = Brush.linearGradient(listOf(Color(0xFF0091EA), Color(0xFF00E5FF))),
                    onClick = onNavigateToEqualizer
                )
            }
            item {
                QuickActionCard(
                    title = "Albums",
                    subtitle = "By Album",
                    icon = Icons.Default.Album,
                    gradient = Brush.linearGradient(listOf(Color(0xFF2E7D32), Color(0xFF00E676))),
                    onClick = onNavigateToAlbums
                )
            }
            item {
                QuickActionCard(
                    title = "Artists",
                    subtitle = "By Singer",
                    icon = Icons.Default.Person,
                    gradient = Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF7C4DFF))),
                    onClick = onNavigateToArtists
                )
            }
            item {
                QuickActionCard(
                    title = "Folders",
                    subtitle = "Storage Folders",
                    icon = Icons.Default.Folder,
                    gradient = Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF78909C))),
                    onClick = onNavigateToFolders
                )
            }
        }

        // CATEGORY HORIZONTAL TABS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryTab.values().forEach { tab ->
                val isSelected = tab == currentCategory
                Surface(
                    color = if (isSelected) palette.primary else palette.cardBackground,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable { onSelectCategory(tab) }
                        .testTag("category_tab_${tab.name.lowercase()}")
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.Black else TextWhitePrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // MAIN SONG LIST
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = palette.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning Sultan Audio Library...",
                        color = TextWhitePrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else if (displayedSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = palette.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching tracks for \"$searchQuery\"" else "No Music Found in this Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhitePrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rescan to search internal storage or check Sultan Audio Studio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGraySecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRescanMusic,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan Music", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp)
            ) {
                items(displayedSongs, key = { it.id }) { song ->
                    val isCurrent = playbackState.currentSong?.id == song.id
                    val isSelected = selectedSongIds.contains(song.id)

                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isCurrent && playbackState.isPlaying,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = isSelected,
                        onClick = { onSelectSong(song) },
                        onToggleSelect = { onToggleSelectSong(song.id) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onEditMetadata = { onEditMetadata(song) },
                        onOpenInStudio = { onOpenInStudio(song) },
                        onSongInfo = { onSongInfo(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .height(78.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
