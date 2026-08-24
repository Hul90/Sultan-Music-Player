package com.example.model

import android.net.Uri

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: Uri? = null,
    val albumArtResId: Int? = null,
    val year: Int = 0
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int = 1
)

data class Folder(
    val name: String,
    val path: String,
    val songCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val coverUri: String? = null,
    val isSystemDefault: Boolean = false
)

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist"),
    ALBUM_ASC("Album"),
    DURATION_DESC("Longest to Shortest"),
    DURATION_ASC("Shortest to Longest"),
    DATE_ADDED_DESC("Recently Added"),
    MOST_PLAYED("Most Played")
}

enum class CategoryTab(val title: String, val iconName: String) {
    SONGS("Songs", "music_note"),
    PLAYLISTS("Playlists", "queue_music"),
    FOLDERS("Folders", "folder"),
    ALBUMS("Albums", "album"),
    ARTISTS("Artists", "person"),
    GENRES("Genres", "category"),
    RECENTLY_ADDED("Recent", "schedule"),
    RECENTLY_PLAYED("History", "history"),
    FAVORITES("Favorites", "favorite")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ThemeOption(val title: String, val description: String) {
    GRADIENT_GLASS("Gradient Glass", "Modern frosted glass with subtle cyan & violet accents"),
    NEON_DARK("Neon Dark", "High contrast pitch black with electric purple & magenta"),
    NATURE_GREEN("Nature Green", "Lush emerald & mint soothing organic tones"),
    PASTEL_PINK("Pastel Pink", "Soft rose & blush warmth with gentle glowing highlights"),
    OCEAN_BLUE("Ocean Blue", "Deep bioluminescent cyan & azure navy depth"),
    SUNSET_ORANGE("Sunset Orange", "Warm amber, crimson and dusk horizon gradients"),
    ROYAL_PURPLE("Royal Purple", "Deep regal amethyst and indigo royalty"),
    SULTAN_GOLD("Sultan Gold", "Prestigious imperial gold & obsidian luxury")
}
