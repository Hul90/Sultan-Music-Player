package com.example.model

data class PlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1,
    val isBuffering: Boolean = false,
    val sleepTimerRemainingSeconds: Long? = null,
    val errorMessage: String? = null
)

data class EqualizerBand(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val minLevelMilliBel: Short,
    val maxLevelMilliBel: Short,
    val currentLevelMilliBel: Short
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bassBoostStrength: Short = 300, // 0..1000
    val virtualizerStrength: Short = 200, // 0..1000
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 60, -1500, 1500, 300),
        EqualizerBand(1, 230, -1500, 1500, 150),
        EqualizerBand(2, 910, -1500, 1500, 0),
        EqualizerBand(3, 3600, -1500, 1500, 200),
        EqualizerBand(4, 14000, -1500, 1500, 400)
    ),
    val presets: List<String> = listOf(
        "Normal", "Rock", "Pop", "Jazz", "Classical",
        "Dance", "Vocal", "Bass", "Flat", "Sultan Club", "Custom"
    ),
    val currentPreset: String = "Sultan Club"
)

enum class AudioEffectType(val displayName: String, val description: String) {
    NONE("None", "Original sound profile"),
    BASS_BOOST("Bass Boost", "Deep resonant low frequencies (+6dB below 150Hz)"),
    TREBLE_BOOST("Treble Boost", "Crisp highs and airy brightness (+5dB above 4kHz)"),
    VOCAL_BOOST("Vocal Boost", "Enhanced vocal presence and acoustic clarity"),
    SOFT("Soft", "Warm gentle acoustic roll-off for relaxed listening"),
    BRIGHT("Bright", "Sparkling high-end modern pop presence"),
    ECHO("Echo (Delay)", "Rhythmic spacious stereo reflections"),
    REVERB("Reverb", "Concert hall acoustic space simulation")
}

data class AudioStudioConfig(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val fadeInSeconds: Float = 0f,
    val fadeOutSeconds: Float = 0f,
    val volumePercent: Int = 100, // 50 to 200
    val speed: Float = 1.0f, // 0.5f to 2.0f
    val pitch: Float = 1.0f, // 0.8f to 1.2f
    val effect: AudioEffectType = AudioEffectType.NONE,
    val customTitle: String = "",
    val customArtist: String = "Sultan Studio Remix",
    val exportFilename: String = ""
)
