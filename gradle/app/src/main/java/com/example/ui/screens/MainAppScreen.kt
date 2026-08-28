package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.CategoryTab
import com.example.model.Playlist
import com.example.model.Song
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.MetadataEditorDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SongInfoDialog
import com.example.ui.components.SortDialog
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.SultanMusicTheme
import com.example.ui.theme.TextGraySecondary
import com.example.ui.viewmodel.SultanMainViewModel

enum class MainNavigationTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    ALBUMS("Albums", Icons.Default.Album),
    ARTISTS("Artists", Icons.Default.Person),
    PLAYLISTS("Playlists", Icons.Default.QueueMusic),
    STUDIO("Studio", Icons.Default.ContentCut),
    EQUALIZER("Equalizer", Icons.Default.Equalizer),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun MainAppScreen(
    viewModel: SultanMainViewModel = viewModel()
) {
    val context = LocalContext.current

    val currentTheme by viewModel.currentTheme.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()

    val allSongs by viewModel.allSongs.collectAsState()
    val displayedSongs by viewModel.displayedSongs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val currentCategory by viewModel.currentCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedSongIds by viewModel.selectedSongIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()

    val studioSelectedSong by viewModel.studioSelectedSong.collectAsState()
    val studioWaveform by viewModel.studioWaveform.collectAsState()
    val studioConfig by viewModel.studioConfig.collectAsState()
    val studioExportProgress by viewModel.studioExportProgress.collectAsState()
    val studioExportMessage by viewModel.studioExportMessage.collectAsState()

    // Navigation & Overlay States
    var currentTab by remember { mutableStateOf(MainNavigationTab.HOME) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    // Dialog Triggers
    var showSortDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var songForMetadataEdit by remember { mutableStateOf<Song?>(null) }
    var songForPlaylistAdd by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songForInfo by remember { mutableStateOf<Song?>(null) }

    // Runtime Permission Request for Audio/Storage
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.refreshMusicLibrary()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            // Needed on API < 29 to save Sultan Audio Studio exports into the public Music folder.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    SultanMusicTheme(selectedTheme = currentTheme) {
        val palette = LocalSultanPalette.current

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Floating Mini Player (visible when not in Fullscreen Now Playing and a song is loaded)
                    AnimatedVisibility(
                        visible = playbackState.currentSong != null && !isNowPlayingExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        MiniPlayer(
                            playbackState = playbackState,
                            onExpandNowPlaying = { isNowPlayingExpanded = true },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.next() },
                            onOpenQueue = { isNowPlayingExpanded = true }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = palette.surface.copy(alpha = 0.96f),
                        tonalElevation = 8.dp
                    ) {
                        MainNavigationTab.values().forEach { tab ->
                            val isSelected = tab == currentTab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = { Text(tab.title) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = palette.primary,
                                    indicatorColor = palette.primary,
                                    unselectedIconColor = TextGraySecondary,
                                    unselectedTextColor = TextGraySecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // PRIMARY SCREEN BASED ON TAB
                when (currentTab) {
                    MainNavigationTab.HOME -> {
                        HomeScreen(
                            displayedSongs = displayedSongs,
                            playbackState = playbackState,
                            currentCategory = currentCategory,
                            currentTheme = currentTheme,
                            currentSort = sortOption,
                            searchQuery = searchQuery,
                            searchHistory = searchHistory,
                            isMultiSelectMode = isMultiSelectMode,
                            selectedSongIds = selectedSongIds,
                            isLoading = isLoading,
                            onSelectCategory = { cat ->
                                viewModel.setCategory(cat)
                                if (cat == CategoryTab.ALBUMS) currentTab = MainNavigationTab.ALBUMS
                                else if (cat == CategoryTab.ARTISTS) currentTab = MainNavigationTab.ARTISTS
                                else if (cat == CategoryTab.PLAYLISTS) currentTab = MainNavigationTab.PLAYLISTS
                            },
                            onSelectTheme = { viewModel.setTheme(it) },
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onClearSearch = { viewModel.clearSearch() },
                            onOpenSortDialog = { showSortDialog = true },
                            onToggleMultiSelect = { viewModel.toggleMultiSelectMode() },
                            onSelectSong = { viewModel.playSong(it, displayedSongs) },
                            onToggleSelectSong = { viewModel.toggleSongSelection(it) },
                            onSelectAllSongs = { viewModel.selectAllDisplayedSongs() },
                            onPlaySelectedSongs = { viewModel.playSelectedSongs() },
                            onAddSelectedToQueue = { viewModel.addSelectedToQueue() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onPlayNext = { viewModel.playNext(it) },
                            onAddToQueue = { viewModel.addToQueue(it) },
                            onAddToPlaylist = { songForPlaylistAdd = it },
                            onEditMetadata = { songForMetadataEdit = it },
                            onOpenInStudio = {
                                viewModel.loadSongIntoStudio(it)
                                currentTab = MainNavigationTab.STUDIO
                            },
                            onSongInfo = { songForInfo = it },
                            onRescanMusic = { viewModel.refreshMusicLibrary() },
                            onNavigateToEqualizer = { currentTab = MainNavigationTab.EQUALIZER },
                            onNavigateToStudio = { currentTab = MainNavigationTab.STUDIO },
                            onNavigateToAlbums = { currentTab = MainNavigationTab.ALBUMS },
                            onNavigateToArtists = { currentTab = MainNavigationTab.ARTISTS },
                            onNavigateToFolders = { viewModel.setCategory(CategoryTab.FOLDERS) }
                        )
                    }
                    MainNavigationTab.ALBUMS -> {
                        AlbumsScreen(
                            albums = albums,
                            allSongs = allSongs,
                            selectedAlbum = selectedAlbum,
                            onSelectAlbum = { viewModel.selectAlbum(it) },
                            onPlaySong = { song, list -> viewModel.playSong(song, list) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            currentPlayingSongId = playbackState.currentSong?.id,
                            isPlaying = playbackState.isPlaying
                        )
                    }
                    MainNavigationTab.ARTISTS -> {
                        ArtistsScreen(
                            artists = artists,
                            allSongs = allSongs,
                            selectedArtist = selectedArtist,
                            onSelectArtist = { viewModel.selectArtist(it) },
                            onPlaySong = { song, list -> viewModel.playSong(song, list) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            currentPlayingSongId = playbackState.currentSong?.id,
                            isPlaying = playbackState.isPlaying
                        )
                    }
                    MainNavigationTab.PLAYLISTS -> {
                        PlaylistsScreen(
                            playlists = playlists,
                            allSongs = allSongs,
                            selectedPlaylist = selectedPlaylist,
                            onSelectPlaylist = { viewModel.selectPlaylist(it) },
                            onCreatePlaylist = { showCreatePlaylistDialog = true },
                            onDeletePlaylist = { viewModel.deletePlaylist(it) },
                            onPlaySong = { song, list -> viewModel.playSong(song, list) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            currentPlayingSongId = playbackState.currentSong?.id,
                            isPlaying = playbackState.isPlaying
                        )
                    }
                    MainNavigationTab.STUDIO -> {
                        SultanAudioStudioScreen(
                            allSongs = allSongs,
                            selectedSong = studioSelectedSong,
                            waveform = studioWaveform,
                            studioConfig = studioConfig,
                            exportProgress = studioExportProgress,
                            exportMessage = studioExportMessage,
                            onSelectSong = { viewModel.loadSongIntoStudio(it) },
                            onUpdateConfig = { viewModel.updateStudioConfig(it) },
                            onExportSultanMix = { viewModel.exportSultanMix() },
                            onClearMessage = { viewModel.clearStudioExportMessage() },
                            onPreviewTrim = { song, startMs, endMs ->
                                viewModel.playSong(song)
                                viewModel.seekTo(startMs)
                            }
                        )
                    }
                    MainNavigationTab.EQUALIZER -> {
                        EqualizerScreen(
                            equalizerState = equalizerState,
                            onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
                            onBandLevelChange = { band, lvl -> viewModel.setBandLevel(band, lvl) },
                            onBassBoostChange = { viewModel.setBassBoost(it) },
                            onVirtualizerChange = { viewModel.setVirtualizer(it) },
                            onApplyPreset = { viewModel.applyPreset(it) }
                        )
                    }
                    MainNavigationTab.SETTINGS -> {
                        SettingsAndAboutScreen(
                            currentTheme = currentTheme,
                            onSelectTheme = { viewModel.setTheme(it) },
                            onRescanLibrary = { viewModel.refreshMusicLibrary() }
                        )
                    }
                }

                // FULLSCREEN NOW PLAYING OVERLAY
                AnimatedVisibility(
                    visible = isNowPlayingExpanded && playbackState.currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NowPlayingScreen(
                        playbackState = playbackState,
                        onCollapse = { isNowPlayingExpanded = false },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onOpenEqualizer = {
                            isNowPlayingExpanded = false
                            currentTab = MainNavigationTab.EQUALIZER
                        },
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onSelectQueueSong = { index -> viewModel.playerManager.playQueue(playbackState.queue, index) },
                        onRemoveQueueSong = { index -> viewModel.removeFromQueue(index) },
                        onClearQueue = { viewModel.clearQueue() },
                        onSetPlaybackSpeed = { viewModel.setPlaybackSpeed(it) }
                    )
                }
            }
        }

        // DIALOGS
        if (showSortDialog) {
            SortDialog(
                currentSort = sortOption,
                onSortSelected = { viewModel.setSortOption(it) },
                onDismiss = { showSortDialog = false }
            )
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                activeRemainingSeconds = playbackState.sleepTimerRemainingSeconds,
                onStartTimer = { mins, fade -> viewModel.startSleepTimer(mins, fade) },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onDismiss = { showSleepTimerDialog = false }
            )
        }

        songForMetadataEdit?.let { editSong ->
            MetadataEditorDialog(
                song = editSong,
                onSave = { title, artist, album, genre, year, track, composer, comment, artUri ->
                    viewModel.saveSongMetadata(
                        editSong.id, title, artist, album, genre, year, track, composer, comment, artUri
                    )
                },
                onDismiss = { songForMetadataEdit = null }
            )
        }

        songForPlaylistAdd?.let { plSong ->
            AddToPlaylistDialog(
                song = plSong,
                playlists = playlists,
                onSelectPlaylist = { pl -> viewModel.addSongToPlaylist(pl.id, plSong.id) },
                onCreateNewPlaylist = { showCreatePlaylistDialog = true },
                onDismiss = { songForPlaylistAdd = null }
            )
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onConfirm = { name -> viewModel.createPlaylist(name) },
                onDismiss = { showCreatePlaylistDialog = false }
            )
        }

        songForInfo?.let { infoSong ->
            SongInfoDialog(
                song = infoSong,
                onDismiss = { songForInfo = null }
            )
        }
    }
}
