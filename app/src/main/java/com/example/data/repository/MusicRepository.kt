package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.audio.MediaStoreScanner
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlayHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistSongEntity
import com.example.data.local.SongCacheEntity
import com.example.data.local.SongMetadataOverrideEntity
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Folder
import com.example.model.Playlist
import com.example.model.Song
import com.example.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) {
    private val musicDao = database.musicDao()

    private val _rawSongs = MutableStateFlow<List<Song>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Exposed Reactive Songs with favorites & metadata overrides applied
    val allSongs: Flow<List<Song>> = combine(
        _rawSongs,
        musicDao.getAllFavoriteIdsFlow(),
        musicDao.getPlayHistoryFlow(),
        musicDao.getAllMetadataOverridesFlow()
    ) { rawList, favoriteIds, historyList, overrides ->
        val favSet = favoriteIds.toSet()
        val historyMap = historyList.associateBy { it.songId }
        val overrideMap = overrides.associateBy { it.songId }

        rawList.map { song ->
            val isFav = favSet.contains(song.id)
            val history = historyMap[song.id]
            val ov = overrideMap[song.id]

            song.copy(
                title = ov?.title ?: song.title,
                artist = ov?.artist ?: song.artist,
                album = ov?.album ?: song.album,
                genre = ov?.genre ?: song.genre,
                year = ov?.year ?: song.year,
                trackNumber = ov?.trackNumber ?: song.trackNumber,
                albumArtUri = ov?.customArtUri?.let { Uri.parse(it) } ?: song.albumArtUri,
                composer = ov?.composer ?: song.composer,
                comment = ov?.comment ?: song.comment,
                isFavorite = isFav,
                playCount = history?.playCount ?: 0,
                lastPlayedTimestamp = history?.lastPlayedTimestamp ?: 0L
            )
        }
    }

    val playlists: Flow<List<Playlist>> = combine(
        musicDao.getAllPlaylistsFlow(),
        musicDao.getPlaylistSongCountsFlow()
    ) { entities, counts ->
        val countMap = counts.associate { it.playlistId to it.songCount }
        entities.map { entity ->
            Playlist(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                songCount = countMap[entity.id] ?: 0,
                coverUri = entity.coverUri
            )
        }
    }

    private var hasShownCachedLibrary = false

    /**
     * Loads the music library.
     *
     * To avoid a visible "scanning" delay every single time the app is opened, the last
     * known scan result is loaded from a local cache and shown immediately (no loading
     * spinner) if one exists. A fresh MediaStore scan then still runs in the background
     * to pick up any new/removed/changed files, silently updating the list and the cache
     * once it completes. The loading indicator is only shown when there is no cache yet
     * (i.e. the very first scan ever).
     */
    suspend fun refreshMusicLibrary() = withContext(Dispatchers.IO) {
        if (!hasShownCachedLibrary) {
            hasShownCachedLibrary = true
            try {
                val cached = musicDao.getCachedSongs()
                if (cached.isNotEmpty()) {
                    _rawSongs.value = cached.map { it.toSong() }
                    _isLoading.value = false
                } else {
                    _isLoading.value = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = true
            }
        }

        try {
            // Scan real device audio files from MediaStore
            val deviceTracks = MediaStoreScanner.scanDeviceAudio(context)
            _rawSongs.value = deviceTracks
            musicDao.replaceCachedSongs(deviceTracks.map { it.toCacheEntity() })
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    private fun SongCacheEntity.toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        durationMs = durationMs,
        path = path,
        contentUri = Uri.parse(contentUri),
        albumArtUri = albumArtUri?.let { Uri.parse(it) },
        genre = genre,
        trackNumber = trackNumber,
        year = year,
        dateAdded = dateAdded,
        sizeBytes = sizeBytes
    )

    private fun Song.toCacheEntity(): SongCacheEntity = SongCacheEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        durationMs = durationMs,
        path = path,
        contentUri = contentUri.toString(),
        albumArtUri = albumArtUri?.toString(),
        genre = genre,
        trackNumber = trackNumber,
        year = year,
        dateAdded = dateAdded,
        sizeBytes = sizeBytes
    )

    suspend fun toggleFavorite(songId: Long) = withContext(Dispatchers.IO) {
        val currentFavs = musicDao.getAllFavoriteIds()
        if (currentFavs.contains(songId)) {
            musicDao.removeFavorite(songId)
        } else {
            musicDao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun recordSongPlayed(songId: Long) = withContext(Dispatchers.IO) {
        val existing = musicDao.getPlayHistoryForSong(songId)
        val newCount = (existing?.playCount ?: 0) + 1
        musicDao.recordPlay(
            PlayHistoryEntity(
                songId = songId,
                playCount = newCount,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        musicDao.createPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        musicDao.renamePlaylist(playlistId, newName.trim())
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.clearPlaylistSongs(playlistId)
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val currentCount = musicDao.getPlaylistSongIds(playlistId).size
        musicDao.addSongToPlaylist(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                orderIndex = currentCount
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        musicDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<Song>> {
        return combine(allSongs, musicDao.getPlaylistSongIdsFlow(playlistId)) { songs, ids ->
            val songMap = songs.associateBy { it.id }
            ids.mapNotNull { songMap[it] }
        }
    }

    suspend fun updateSongMetadata(
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
    ) = withContext(Dispatchers.IO) {
        val override = SongMetadataOverrideEntity(
            songId = songId,
            title = title.trim(),
            artist = artist.trim(),
            album = album.trim(),
            genre = genre.trim(),
            year = year,
            trackNumber = trackNumber,
            customArtUri = customArtUri?.toString(),
            composer = composer.trim(),
            comment = comment.trim()
        )
        musicDao.saveMetadataOverride(override)
    }

    fun sortSongs(songs: List<Song>, sortOption: SortOption): List<Song> {
        return when (sortOption) {
            SortOption.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM_ASC -> songs.sortedBy { it.album.lowercase() }
            SortOption.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
            SortOption.DURATION_ASC -> songs.sortedBy { it.durationMs }
            SortOption.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
            SortOption.MOST_PLAYED -> songs.sortedByDescending { it.playCount }
        }
    }

    fun filterSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        val q = query.lowercase().trim()
        return songs.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.genre.lowercase().contains(q) ||
            it.path.lowercase().contains(q)
        }
    }
}
