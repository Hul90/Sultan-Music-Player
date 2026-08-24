package com.example.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val durationMs: Long,
    val path: String,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val albumArtResId: Int? = null,
    val genre: String = "Unknown",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val dateAdded: Long = 0,
    val sizeBytes: Long = 0,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val composer: String = "",
    val comment: String = ""
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                val remainingMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
