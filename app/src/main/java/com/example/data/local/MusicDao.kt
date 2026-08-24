package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // --- FAVORITES ---
    @Query("SELECT songId FROM favorites")
    fun getAllFavoriteIdsFlow(): Flow<List<Long>>

    @Query("SELECT songId FROM favorites")
    suspend fun getAllFavoriteIds(): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    fun isFavoriteFlow(songId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    // --- PLAYLIST SONGS ---
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getPlaylistSongIdsFlow(playlistId: Long): Flow<List<Long>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getPlaylistSongIds(playlistId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getPlaylistSongCountFlow(playlistId: Long): Flow<Int>

    data class PlaylistSongCount(
        val playlistId: Long,
        val songCount: Int
    )

    @Query("SELECT playlistId, COUNT(songId) AS songCount FROM playlist_songs GROUP BY playlistId")
    fun getPlaylistSongCountsFlow(): Flow<List<PlaylistSongCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSongEntity)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    // --- PLAY HISTORY ---
    @Query("SELECT * FROM play_history ORDER BY lastPlayedTimestamp DESC")
    fun getPlayHistoryFlow(): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlay(history: PlayHistoryEntity)

    @Query("SELECT * FROM play_history WHERE songId = :songId")
    suspend fun getPlayHistoryForSong(songId: Long): PlayHistoryEntity?

    // --- METADATA OVERRIDES ---
    @Query("SELECT * FROM metadata_overrides")
    fun getAllMetadataOverridesFlow(): Flow<List<SongMetadataOverrideEntity>>

    @Query("SELECT * FROM metadata_overrides WHERE songId = :songId")
    suspend fun getMetadataOverride(songId: Long): SongMetadataOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMetadataOverride(override: SongMetadataOverrideEntity)

    // --- STUDIO PROJECTS ---
    @Query("SELECT * FROM studio_projects ORDER BY createdAt DESC")
    fun getAllStudioProjectsFlow(): Flow<List<StudioProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudioProject(project: StudioProjectEntity): Long

    // --- SONG SCAN CACHE ---
    // Lets the library appear instantly on app start using the last known scan,
    // while a fresh MediaStore scan runs silently in the background.
    @Query("SELECT * FROM song_cache ORDER BY title ASC")
    suspend fun getCachedSongs(): List<SongCacheEntity>

    @Query("SELECT COUNT(*) FROM song_cache")
    suspend fun getCachedSongCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSongs(songs: List<SongCacheEntity>)

    @Query("DELETE FROM song_cache")
    suspend fun clearCachedSongs()

    @Transaction
    suspend fun replaceCachedSongs(songs: List<SongCacheEntity>) {
        clearCachedSongs()
        insertCachedSongs(songs)
    }
}
