package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val coverUri: String? = null
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey
    val songId: Long,
    val playCount: Int = 1,
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "metadata_overrides")
data class SongMetadataOverrideEntity(
    @PrimaryKey
    val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val customArtUri: String? = null,
    val composer: String? = null,
    val comment: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "studio_projects")
data class StudioProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val sourceSongId: Long,
    val outputFilePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)

// Cached snapshot of the last successful MediaStore scan so the library can be
// shown instantly on app start without waiting for (or visibly re-running) a
// fresh scan every single time the app is opened.
@Entity(tableName = "song_cache")
data class SongCacheEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val durationMs: Long,
    val path: String,
    val contentUri: String,
    val albumArtUri: String?,
    val genre: String,
    val trackNumber: Int,
    val year: Int,
    val dateAdded: Long,
    val sizeBytes: Long
)
