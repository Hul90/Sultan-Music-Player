package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStudioProcessor
import com.example.audio.MediaStoreScanner
import com.example.data.repository.MusicRepository
import com.example.data.repository.PreferencesRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.AudioEffectType
import com.example.model.AudioStudioConfig
import com.example.model.CategoryTab
import com.example.model.EqualizerState
import com.example.model.Folder
import com.example.model.PlaybackState
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.model.SortOption
import com.example.model.ThemeOption
import com.example.player.SultanPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SultanMainViewModel(application: Application) : AndroidViewModel(application) {

    val musicRepository = MusicRepository(application)
    val preferencesRepository = PreferencesRepository(application)
    val playerManager = SultanPlayerManager.getInstance(application)

    // Player State & Equalizer State from PlayerManager
    val playbackState: StateFlow<PlaybackState> = playerManager.playbackState
    val equalizerState: StateFlow<EqualizerState> = playerManager.equalizerState

    // Theme & Preferences
    val currentTheme: StateFlow<ThemeOption> = preferencesRepository.theme
    val sortOption: StateFlow<SortOption> = preferencesRepository.sortOption

    // Library State
    val isLoading: StateFlow<Boolean> = musicRepository.isLoading
    val allSongs: StateFlow<List<Song>> = musicRepository.allSongs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val playlists: StateFlow<List<Playlist>> = musicRepository.playlists.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Active Category & Navigation
    private val _currentCategory = MutableStateFlow(CategoryTab.SONGS)
    val currentCategory: StateFlow<CategoryTab> = _currentCategory.asStateFlow()

    // Search & Filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchHistory: StateFlow<List<String>> = preferencesRepository.searchHistory

    // Multi-Select Mode
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds.asStateFlow()

    // Drill-down selections (e.g. selected Album or Artist or Folder)
    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<Artist?>(null)
    val selectedArtist: StateFlow<Artist?> = _selectedArtist.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    // Sultan Audio Studio State
    private val _studioSelectedSong = MutableStateFlow<Song?>(null)
    val studioSelectedSong: StateFlow<Song?> = _studioSelectedSong.asStateFlow()

    private val _studioWaveform = MutableStateFlow<List<Float>>(emptyList())
    val studioWaveform: StateFlow<List<Float>> = _studioWaveform.asStateFlow()

    private val _studioConfig = MutableStateFlow(AudioStudioConfig())
    val studioConfig: StateFlow<AudioStudioConfig> = _studioConfig.asStateFlow()

    private val _studioExportProgress = MutableStateFlow<Float?>(null)
    val studioExportProgress: StateFlow<Float?> = _studioExportProgress.asStateFlow()

    private val _studioExportMessage = MutableStateFlow<String?>(null)
    val studioExportMessage: StateFlow<String?> = _studioExportMessage.asStateFlow()

    // Derived Displayed Songs based on category, search, and sort
    val displayedSongs: StateFlow<List<Song>> = combine(
        allSongs,
        searchQuery,
        sortOption,
        _currentCategory
    ) { songs, query, sort, category ->
        val filteredByCategory = when (category) {
            CategoryTab.SONGS -> songs
            CategoryTab.FAVORITES -> songs.filter { it.isFavorite }
            CategoryTab.RECENTLY_ADDED -> songs.sortedByDescending { it.dateAdded }
            CategoryTab.RECENTLY_PLAYED -> songs.filter { it.lastPlayedTimestamp > 0 }.sortedByDescending { it.lastPlayedTimestamp }
            else -> songs
        }

        val filteredByQuery = if (query.isNotBlank()) {
            musicRepository.filterSongs(filteredByCategory, query)
        } else {
            filteredByCategory
        }

        if (category == CategoryTab.RECENTLY_ADDED || category == CategoryTab.RECENTLY_PLAYED) {
            filteredByQuery
        } else {
            musicRepository.sortSongs(filteredByQuery, sort)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Groupings
    val albums: StateFlow<List<Album>> = allSongs.combine(searchQuery) { songs, query ->
        val list = MediaStoreScanner.groupIntoAlbums(songs)
        if (query.isBlank()) list else list.filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<Artist>> = allSongs.combine(searchQuery) { songs, query ->
        val list = MediaStoreScanner.groupIntoArtists(songs)
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<Folder>> = allSongs.combine(searchQuery) { songs, query ->
        val list = MediaStoreScanner.groupIntoFolders(songs)
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        playerManager.setCallbacks(
            onSongEnded = {
                // Next track handled inside PlayerManager
            },
            onSongPlayed = { songId ->
                viewModelScope.launch {
                    musicRepository.recordSongPlayed(songId)
                }
            }
        )
        refreshMusicLibrary()
    }

    fun refreshMusicLibrary() {
        viewModelScope.launch {
            musicRepository.refreshMusicLibrary()
        }
    }

    fun setCategory(category: CategoryTab) {
        _currentCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            preferencesRepository.addSearchHistory(query)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun clearSearchHistory() {
        preferencesRepository.clearSearchHistory()
    }

    fun setTheme(theme: ThemeOption) {
        preferencesRepository.setTheme(theme)
    }

    fun setSortOption(sort: SortOption) {
        preferencesRepository.setSortOption(sort)
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
        }
    }

    // --- MULTI-SELECT ---
    fun toggleMultiSelectMode() {
        val newMode = !_isMultiSelectMode.value
        _isMultiSelectMode.value = newMode
        if (!newMode) {
            _selectedSongIds.value = emptySet()
        }
    }

    fun toggleSongSelection(songId: Long) {
        val current = _selectedSongIds.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _selectedSongIds.value = current
    }

    fun selectAllDisplayedSongs() {
        _selectedSongIds.value = displayedSongs.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
        _isMultiSelectMode.value = false
    }

    fun playSelectedSongs() {
        val selectedSongs = allSongs.value.filter { _selectedSongIds.value.contains(it.id) }
        if (selectedSongs.isNotEmpty()) {
            playerManager.playQueue(selectedSongs, 0)
            clearSelection()
        }
    }

    fun addSelectedToQueue() {
        val selectedSongs = allSongs.value.filter { _selectedSongIds.value.contains(it.id) }
        selectedSongs.forEach { playerManager.addToQueue(it) }
        clearSelection()
    }

    // --- PLAYBACK ---
    fun playSong(song: Song, contextSongs: List<Song>? = null) {
        val listToPlay = contextSongs ?: displayedSongs.value
        val index = listToPlay.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playerManager.playQueue(listToPlay, index)
        } else {
            playerManager.playSong(song)
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        playerManager.playQueue(songs, startIndex)
    }

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun setPlaybackPitch(pitch: Float) = playerManager.setPlaybackPitch(pitch)
    fun addToQueue(song: Song) = playerManager.addToQueue(song)
    fun playNext(song: Song) = playerManager.playNext(song)
    fun removeFromQueue(index: Int) = playerManager.removeFromQueue(index)
    fun clearQueue() = playerManager.clearQueue()

    // --- EQUALIZER & SLEEP TIMER ---
    fun setEqualizerEnabled(enabled: Boolean) = playerManager.setEqualizerEnabled(enabled)
    fun setBandLevel(bandIndex: Short, level: Short) = playerManager.setBandLevel(bandIndex, level)
    fun setBassBoost(strength: Short) = playerManager.setBassBoost(strength)
    fun setVirtualizer(strength: Short) = playerManager.setVirtualizer(strength)
    fun applyPreset(preset: String) = playerManager.applyPreset(preset)

    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true) {
        playerManager.startSleepTimer(minutes, fadeOut)
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    // --- PLAYLISTS ---
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        viewModelScope.launch {
            musicRepository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // --- SELECTION DRILL-DOWN ---
    fun selectAlbum(album: Album?) {
        _selectedAlbum.value = album
    }

    fun selectArtist(artist: Artist?) {
        _selectedArtist.value = artist
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    // --- METADATA EDITOR ---
    fun saveSongMetadata(
        songId: Long,
        title: String,
        artist: String,
        album: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        composer: String,
        comment: String,
        customArtUri: Uri?
    ) {
        viewModelScope.launch {
            // The picker returns a short-lived content:// Uri (e.g. from the system Photo
            // Picker) whose read permission is revoked once the app process dies. If we
            // save that Uri directly, the cover shows fine until the app is closed and
            // reopened, then it can no longer be loaded. Persist the picture into the
            // app's own storage first so it keeps working across restarts.
            val persistedArtUri = withContext(Dispatchers.IO) {
                persistAlbumArt(songId, customArtUri)
            }

            musicRepository.updateSongMetadata(
                songId = songId,
                title = title,
                artist = artist,
                album = album,
                genre = genre,
                year = year,
                trackNumber = trackNumber,
                composer = composer,
                comment = comment,
                customArtUri = persistedArtUri ?: customArtUri
            )
        }
    }

    /**
     * Copies a picked cover-art image into app-private storage and returns a stable
     * file:// Uri for it. Returns null if [pickedUri] is null, already one of our own
     * persisted files, or the copy fails (in which case the caller falls back to the
     * original Uri rather than losing the selection).
     */
    private fun persistAlbumArt(songId: Long, pickedUri: Uri?): Uri? {
        if (pickedUri == null) return null
        if (pickedUri.scheme == "file") return null // already a persisted local file

        return try {
            val context: android.content.Context = getApplication()
            val artDir = File(context.filesDir, "album_art").apply { mkdirs() }
            val destFile = File(artDir, "art_${songId}_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(pickedUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- SULTAN AUDIO STUDIO ---
    fun loadSongIntoStudio(song: Song) {
        _studioSelectedSong.value = song
        _studioConfig.value = AudioStudioConfig(
            startMs = 0L,
            endMs = song.durationMs.coerceAtLeast(15000L),
            customTitle = "${song.title} (Sultan Mix)",
            customArtist = song.artist,
            exportFilename = "${song.title}_SultanMix.wav"
        )
        viewModelScope.launch {
            val amps = AudioStudioProcessor.extractWaveformAmplitudes(getApplication(), song)
            _studioWaveform.value = amps
        }
    }

    fun updateStudioConfig(config: AudioStudioConfig) {
        _studioConfig.value = config
    }

    fun exportSultanMix() {
        val song = _studioSelectedSong.value ?: return
        val config = _studioConfig.value

        viewModelScope.launch {
            _studioExportProgress.value = 0.05f
            _studioExportMessage.value = "Starting Sultan Studio DSP Rendering..."

            val result = AudioStudioProcessor.processAndExportSultanMix(
                context = getApplication(),
                song = song,
                config = config,
                onProgress = { progress ->
                    _studioExportProgress.value = progress
                    _studioExportMessage.value = when {
                        progress < 0.3f -> "Extracting audio waveform & trimming..."
                        progress < 0.7f -> "Applying Sultan Studio DSP & Equalizer effects..."
                        progress < 0.95f -> "Encoding Master RIFF WAV & tagging..."
                        else -> "Saving to Music/Sultan Music Player/Sultan Audio Studio/..."
                    }
                }
            )

            if (result.isSuccess) {
                val file = result.getOrNull()
                _studioExportProgress.value = null
                _studioExportMessage.value = "Export Succeeded! Saved to: ${file?.name}"
                // Refresh library to show new track
                refreshMusicLibrary()
            } else {
                _studioExportProgress.value = null
                _studioExportMessage.value = "Export failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearStudioExportMessage() {
        _studioExportMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Do not release playerManager here if service is bound for background audio
    }
}
