package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.model.RepeatMode
import com.example.model.SortOption
import com.example.model.ThemeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("sultan_music_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<ThemeOption> = _theme.asStateFlow()

    private val _sortOption = MutableStateFlow(loadSortOption())
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _isShuffle = MutableStateFlow(prefs.getBoolean(KEY_SHUFFLE, false))
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(loadRepeatMode())
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean(KEY_GAPLESS, true))
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt(KEY_CROSSFADE, 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _resumePlayback = MutableStateFlow(prefs.getBoolean(KEY_RESUME, true))
    val resumePlayback: StateFlow<Boolean> = _resumePlayback.asStateFlow()

    private val _fadeSleepTimer = MutableStateFlow(prefs.getBoolean(KEY_FADE_SLEEP, true))
    val fadeSleepTimer: StateFlow<Boolean> = _fadeSleepTimer.asStateFlow()

    private val _searchHistory = MutableStateFlow(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    fun setTheme(theme: ThemeOption) {
        _theme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    fun setSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
        prefs.edit().putString(KEY_SORT, sortOption.name).apply()
    }

    fun setShuffle(shuffle: Boolean) {
        _isShuffle.value = shuffle
        prefs.edit().putBoolean(KEY_SHUFFLE, shuffle).apply()
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        _repeatMode.value = repeatMode
        prefs.edit().putString(KEY_REPEAT, repeatMode.name).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        prefs.edit().putBoolean(KEY_GAPLESS, enabled).apply()
    }

    fun setCrossfadeSeconds(seconds: Int) {
        _crossfadeSeconds.value = seconds
        prefs.edit().putInt(KEY_CROSSFADE, seconds).apply()
    }

    fun setResumePlayback(resume: Boolean) {
        _resumePlayback.value = resume
        prefs.edit().putBoolean(KEY_RESUME, resume).apply()
    }

    fun setFadeSleepTimer(fade: Boolean) {
        _fadeSleepTimer.value = fade
        prefs.edit().putBoolean(KEY_FADE_SLEEP, fade).apply()
    }

    fun addSearchHistory(query: String) {
        val value = query.trim()
        if (value.isBlank()) return
        val updated = (listOf(value) + _searchHistory.value.filterNot { it.equals(value, ignoreCase = true) }).take(8)
        _searchHistory.value = updated
        prefs.edit().putStringSet(KEY_SEARCH_HISTORY, updated.toSet()).apply()
        // StringSet does not preserve ordering, so keep an explicit ordered representation too.
        prefs.edit().putString(KEY_SEARCH_HISTORY_ORDERED, updated.joinToString("\u001F")).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove(KEY_SEARCH_HISTORY).remove(KEY_SEARCH_HISTORY_ORDERED).apply()
    }

    private fun loadTheme(): ThemeOption {
        val name = prefs.getString(KEY_THEME, ThemeOption.SULTAN_GOLD.name)
        return try {
            ThemeOption.valueOf(name ?: ThemeOption.SULTAN_GOLD.name)
        } catch (e: Exception) {
            ThemeOption.SULTAN_GOLD
        }
    }

    private fun loadSortOption(): SortOption {
        val name = prefs.getString(KEY_SORT, SortOption.TITLE_ASC.name)
        return try {
            SortOption.valueOf(name ?: SortOption.TITLE_ASC.name)
        } catch (e: Exception) {
            SortOption.TITLE_ASC
        }
    }

    private fun loadSearchHistory(): List<String> {
        val ordered = prefs.getString(KEY_SEARCH_HISTORY_ORDERED, null)
            ?.split("\u001F")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
        if (!ordered.isNullOrEmpty()) return ordered.take(8)
        return prefs.getStringSet(KEY_SEARCH_HISTORY, emptySet())?.toList()?.take(8) ?: emptyList()
    }

    private fun loadRepeatMode(): RepeatMode {
        val name = prefs.getString(KEY_REPEAT, RepeatMode.OFF.name)
        return try {
            RepeatMode.valueOf(name ?: RepeatMode.OFF.name)
        } catch (e: Exception) {
            RepeatMode.OFF
        }
    }

    companion object {
        private const val KEY_THEME = "key_theme"
        private const val KEY_SORT = "key_sort"
        private const val KEY_SHUFFLE = "key_shuffle"
        private const val KEY_REPEAT = "key_repeat"
        private const val KEY_GAPLESS = "key_gapless"
        private const val KEY_CROSSFADE = "key_crossfade"
        private const val KEY_RESUME = "key_resume"
        private const val KEY_FADE_SLEEP = "key_fade_sleep"
        private const val KEY_SEARCH_HISTORY = "key_search_history"
        private const val KEY_SEARCH_HISTORY_ORDERED = "key_search_history_ordered"
    }
}
