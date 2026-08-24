package com.example.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.model.Album
import com.example.model.Artist
import com.example.model.Folder
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object MediaStoreScanner {

    private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")

    suspend fun scanDeviceAudio(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.IS_MUSIC
        )

        // Prefer MediaStore metadata over filesystem-path filtering. DATA is still read as a
        // legacy/local-file fallback, but it is not used to decide which audio files exist.
        // This keeps scanning compatible with scoped storage and content-URI based providers.
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%') AND " +
                "(${MediaStore.Audio.Media.DURATION} >= 1000 OR ${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} = 0)"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val path = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                    val mimeType = if (mimeTypeColumn >= 0) cursor.getString(mimeTypeColumn) ?: "" else ""
                    val albumId = cursor.getLong(albumIdColumn)
                    val track = if (trackColumn != -1) cursor.getInt(trackColumn) else 0
                    val year = if (yearColumn != -1) cursor.getInt(yearColumn) else 0
                    val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                    val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
                    } else {
                        null
                    }

                    val cleanTitle = if (title.isBlank() || title == "<unknown>") {
                        if (path.isNotBlank()) File(path).nameWithoutExtension else "Audio Track $id"
                    } else {
                        title
                    }

                    val cleanArtist = if (artist.isBlank() || artist == "<unknown>") "Unknown Artist" else artist
                    val cleanAlbum = if (album.isBlank() || album == "<unknown>") "Unknown Album" else album

                    // Guess genre based on path or default
                    val genre = guessGenreFromPath(path, mimeType)

                    songList.add(
                        Song(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            albumArtist = cleanArtist,
                            durationMs = duration.coerceAtLeast(0L),
                            path = path,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            genre = genre,
                            trackNumber = track,
                            year = year,
                            dateAdded = dateAdded,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songList
    }

    private fun guessGenreFromPath(path: String, mimeType: String = ""): String {
        val lower = path.lowercase()
        val mime = mimeType.lowercase()
        return when {
            lower.contains("rock") -> "Rock"
            lower.contains("pop") -> "Pop"
            lower.contains("jazz") -> "Jazz"
            lower.contains("hiphop") || lower.contains("rap") -> "Hip-Hop"
            lower.contains("electronic") || lower.contains("edm") || lower.contains("dance") -> "Electronic"
            lower.contains("classical") -> "Classical"
            lower.contains("metal") -> "Metal"
            lower.contains("acoustic") || lower.contains("folk") -> "Acoustic"
            lower.contains("ambient") || lower.contains("lofi") || lower.contains("lo-fi") -> "Lo-Fi / Ambient"
            lower.contains("sultan") -> "Sultan Signature"
            mime.contains("audio") && path.isBlank() -> "General Audio"
            else -> "General Audio"
        }
    }

    fun groupIntoAlbums(songs: List<Song>): List<Album> {
        return songs.groupBy { it.album }
            .map { (albumTitle, albumSongs) ->
                val firstSong = albumSongs.first()
                Album(
                    id = firstSong.id,
                    title = albumTitle,
                    artist = firstSong.artist,
                    songCount = albumSongs.size,
                    albumArtUri = firstSong.albumArtUri,
                    albumArtResId = firstSong.albumArtResId,
                    year = albumSongs.maxOfOrNull { it.year } ?: 0
                )
            }
            .sortedBy { it.title.lowercase() }
    }

    fun groupIntoArtists(songs: List<Song>): List<Artist> {
        return songs.groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                val firstSong = artistSongs.first()
                val albumCount = artistSongs.map { it.album }.distinct().size
                Artist(
                    id = firstSong.id,
                    name = artistName,
                    songCount = artistSongs.size,
                    albumCount = albumCount
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun groupIntoFolders(songs: List<Song>): List<Folder> {
        return songs.groupBy { song ->
            val path = song.path
            if (path.isBlank()) "Media Library" else File(path).parentFile?.name ?: "Root"
        }.map { (folderName, folderSongs) ->
            val firstPath = folderSongs.first().path
            val parentPath = if (firstPath.isBlank()) "media://library" else File(firstPath).parent ?: "/"
            Folder(
                name = folderName,
                path = parentPath,
                songCount = folderSongs.size
            )
        }.sortedBy { it.name.lowercase() }
    }
}
