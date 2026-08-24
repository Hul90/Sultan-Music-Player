package com.example.player

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.model.EqualizerBand
import com.example.model.EqualizerState
import com.example.model.PlaybackState
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.service.SultanMediaService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class SultanPlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // High-performance ExoPlayer configuration with broad format extractors and robust seeking for long tracks
    val exoPlayer: ExoPlayer = run {
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 60_000,
                /* bufferForPlaybackMs = */ 1_000,
                /* bufferForPlaybackAfterRebufferMs = */ 2_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .setBackBuffer(30_000, true)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractorsFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
    }

    // The MediaSession is created here (eagerly, alongside the player) rather than inside
    // SultanMediaService.onCreate(). If the session were only built once playback already
    // started, Media3's automatic media-notification updater (which reacts to player events
    // like isPlaying/mediaItem changes on THIS session) would miss the very first events and
    // never build a real notification with play/pause/next/previous controls - it would get
    // stuck showing only a placeholder. Building the session here means it is already
    // listening to the shared player from the moment the app is created, well before any
    // playback begins, so Media3 reliably renders the full lock-screen/notification controls.
    // NOTE: built eagerly (not via `by lazy`) so it exists immediately when the singleton
    // is constructed - typically at app/ViewModel startup - rather than being deferred
    // until the service first asks for it, which would recreate the same timing problem.
    val mediaSession: MediaSession = run {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        MediaSession.Builder(context, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private var sleepTimer: CountDownTimer? = null
    private var positionTrackerJob: Job? = null
    private var onSongEndedCallback: (() -> Unit)? = null
    private var onSongPlayedCallback: ((Long) -> Unit)? = null
    private var fallbackAttemptedMediaId: String? = null

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    init {
        setupPlayerListener()
        registerNoisyReceiver()
        startPositionTracker()
    }

    fun setCallbacks(onSongEnded: () -> Unit, onSongPlayed: (Long) -> Unit) {
        this.onSongEndedCallback = onSongEnded
        this.onSongPlayedCallback = onSongPlayed
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) ensureAudioEffects()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying,
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                )
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) ensureAudioEffects()
                val isBuffering = state == Player.STATE_BUFFERING
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _playbackState.value.durationMs

                _playbackState.value = _playbackState.value.copy(
                    isBuffering = isBuffering,
                    durationMs = duration,
                    errorMessage = if (state == Player.STATE_READY) null else _playbackState.value.errorMessage
                )

                if (state == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentIdx = exoPlayer.currentMediaItemIndex
                val queue = _playbackState.value.queue
                if (currentIdx in queue.indices) {
                    val song = queue[currentIdx]
                    fallbackAttemptedMediaId = null
                    _playbackState.value = _playbackState.value.copy(
                        currentSong = song,
                        queueIndex = currentIdx,
                        durationMs = song.durationMs,
                        errorMessage = null
                    )
                    onSongPlayedCallback?.invoke(song.id)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("SultanPlayerManager", "ExoPlayer Error: ${error.errorCodeName} (${error.errorCode})", error)

                val index = exoPlayer.currentMediaItemIndex
                val queue = _playbackState.value.queue
                val song = queue.getOrNull(index)
                val mediaId = exoPlayer.currentMediaItem?.mediaId
                val localFile = song?.path?.takeIf { it.isNotBlank() }?.let(::File)

                // Some Android/OEM MediaStore implementations expose a valid content URI
                // but fail when the decoder is asked to open it. If the real filesystem path
                // is still readable, retry the same track once using file:// as a fallback.
                if (song != null && localFile?.isFile == true && mediaId != fallbackAttemptedMediaId) {
                    fallbackAttemptedMediaId = mediaId
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val fallbackItem = mediaItemFor(song).buildUpon()
                        .setUri(Uri.fromFile(localFile))
                        .build()
                    exoPlayer.replaceMediaItem(index, fallbackItem)
                    exoPlayer.prepare()
                    exoPlayer.seekTo(index, position)
                    exoPlayer.play()
                    return
                }

                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "Unable to play audio: ${error.message ?: error.errorCodeName}"
                )
            }
        })
    }

    private fun registerNoisyReceiver() {
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.coerceAtLeast(0L)
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playbackState.value.durationMs
                    )
                }
                delay(250)
            }
        }
    }

    private fun ensurePlaybackServiceStarted() {
        try {
            val intent = Intent(context, SultanMediaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.w("SultanPlayerManager", "Could not start media playback service", e)
        }
    }

    private fun mediaItemFor(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        val validIndex = startIndex.coerceIn(0, songs.size - 1)
        val targetSong = songs[validIndex]

        val mediaItems = songs.map(::mediaItemFor)

        try {
            exoPlayer.setMediaItems(mediaItems, validIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            Log.e("SultanPlayerManager", "Failed to start playback", e)
            _playbackState.value = _playbackState.value.copy(
                isPlaying = false,
                isBuffering = false,
                errorMessage = "Playback failed: ${e.message ?: e.javaClass.simpleName}"
            )
            return
        }

        // IMPORTANT: start the MediaSessionService only after playback has actually started.
        // Starting a foreground service before it has an active media session can trigger
        // Android's foreground-service timeout and close the app.
        ensurePlaybackServiceStarted()

        _playbackState.value = _playbackState.value.copy(
            queue = songs,
            queueIndex = validIndex,
            currentSong = targetSong,
            isPlaying = true,
            currentPositionMs = 0L,
            durationMs = targetSong.durationMs
        )

        onSongPlayedCallback?.invoke(targetSong.id)
    }

    fun playSong(song: Song) {
        val currentQueue = _playbackState.value.queue
        val existingIndex = currentQueue.indexOfFirst { it.id == song.id }
        if (existingIndex != -1) {
            exoPlayer.seekTo(existingIndex, 0L)
            exoPlayer.play()
            ensurePlaybackServiceStarted()
            _playbackState.value = _playbackState.value.copy(
                queueIndex = existingIndex,
                currentSong = song,
                isPlaying = true
            )
        } else {
            playQueue(listOf(song), 0)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        ensurePlaybackServiceStarted()
        exoPlayer.play()
        _playbackState.value = _playbackState.value.copy(isPlaying = true)
    }

    fun pause() {
        exoPlayer.pause()
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        } else {
            val queue = _playbackState.value.queue
            if (queue.isNotEmpty() && _playbackState.value.repeatMode == RepeatMode.ALL) {
                exoPlayer.seekTo(0, 0L)
                exoPlayer.play()
            }
        }
    }

    fun previous() {
        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        exoPlayer.shuffleModeEnabled = newShuffle
        _playbackState.value = _playbackState.value.copy(isShuffle = newShuffle)
    }

    fun toggleRepeat() {
        val nextRepeat = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        exoPlayer.repeatMode = when (nextRepeat) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        _playbackState.value = _playbackState.value.copy(repeatMode = nextRepeat)
    }

    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.5f, 2.0f)
        val params = PlaybackParameters(validSpeed, _playbackState.value.playbackPitch)
        exoPlayer.playbackParameters = params
        _playbackState.value = _playbackState.value.copy(playbackSpeed = validSpeed)
    }

    fun setPlaybackPitch(pitch: Float) {
        val validPitch = pitch.coerceIn(0.8f, 1.2f)
        val params = PlaybackParameters(_playbackState.value.playbackSpeed, validPitch)
        exoPlayer.playbackParameters = params
        _playbackState.value = _playbackState.value.copy(playbackPitch = validPitch)
    }

    fun addToQueue(song: Song) {
        val updated = _playbackState.value.queue.toMutableList().apply { add(song) }
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
        exoPlayer.addMediaItem(mediaItem)
        _playbackState.value = _playbackState.value.copy(queue = updated)
    }

    fun playNext(song: Song) {
        val currentIdx = _playbackState.value.queueIndex
        val nextIdx = (currentIdx + 1).coerceAtMost(_playbackState.value.queue.size)
        val updated = _playbackState.value.queue.toMutableList().apply { add(nextIdx, song) }

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .build()
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.addMediaItem(nextIdx, mediaItem)
        _playbackState.value = _playbackState.value.copy(queue = updated)
    }

    fun removeFromQueue(index: Int) {
        val queue = _playbackState.value.queue.toMutableList()
        if (index in queue.indices) {
            queue.removeAt(index)
            exoPlayer.removeMediaItem(index)
            val newIdx = if (index < _playbackState.value.queueIndex) {
                _playbackState.value.queueIndex - 1
            } else {
                _playbackState.value.queueIndex
            }
            _playbackState.value = _playbackState.value.copy(
                queue = queue,
                queueIndex = newIdx.coerceAtLeast(0)
            )
        }
    }

    fun clearQueue() {
        exoPlayer.clearMediaItems()
        _playbackState.value = _playbackState.value.copy(
            queue = emptyList(),
            queueIndex = -1,
            currentSong = null,
            isPlaying = false,
            currentPositionMs = 0L,
            durationMs = 0L
        )
    }

    private fun handleTrackEnded() {
        when (_playbackState.value.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                if (!exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekTo(0, 0L)
                    exoPlayer.play()
                }
            }
            RepeatMode.OFF -> {
                if (!exoPlayer.hasNextMediaItem()) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = false)
                }
            }
        }
        onSongEndedCallback?.invoke()
    }

    // --- SLEEP TIMER ---
    fun startSleepTimer(minutes: Int, fadeOut: Boolean = true) {
        cancelSleepTimer()
        val totalMs = minutes * 60 * 1000L

        sleepTimer = object : CountDownTimer(totalMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = seconds)

                // Optional gradual fade out in the final 30 seconds
                if (fadeOut && millisUntilFinished <= 30000L) {
                    val volume = (millisUntilFinished / 30000f).coerceIn(0.05f, 1.0f)
                    exoPlayer.volume = volume
                }
            }

            override fun onFinish() {
                pause()
                exoPlayer.volume = 1.0f
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = null)
            }
        }.start()
    }

    fun cancelSleepTimer() {
        sleepTimer?.cancel()
        sleepTimer = null
        exoPlayer.volume = 1.0f
        _playbackState.value = _playbackState.value.copy(sleepTimerRemainingSeconds = null)
    }

    // --- AUDIO EFFECTS / EQUALIZER ---
    // Android audio effects are attached to an audio session. The session may be unset during
    // application startup, so effects are created lazily after ExoPlayer has an active session.
    private fun ensureAudioEffects() {
        if (equalizer != null && bassBoost != null && virtualizer != null) return
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = true
                }
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = true
                    setStrength(_equalizerState.value.bassBoostStrength)
                }
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = true
                    setStrength(_equalizerState.value.virtualizerStrength)
                }

                equalizer?.let { eq ->
                    val bands = mutableListOf<EqualizerBand>()
                    val numBands = eq.numberOfBands
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]

                    for (i in 0 until numBands) {
                        val bandIdx = i.toShort()
                        val freqHz = eq.getCenterFreq(bandIdx) / 1000
                        val currentLevel = eq.getBandLevel(bandIdx)
                        bands.add(
                            EqualizerBand(
                                bandIndex = bandIdx,
                                centerFreqHz = freqHz,
                                minLevelMilliBel = minLevel,
                                maxLevelMilliBel = maxLevel,
                                currentLevelMilliBel = currentLevel
                            )
                        )
                    }

                    _equalizerState.value = _equalizerState.value.copy(
                        isEnabled = eq.enabled,
                        bands = bands
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful software fallback: keep default bands without crashing
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        ensureAudioEffects()
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandIndex: Short, levelMilliBel: Short) {
        ensureAudioEffects()
        try {
            equalizer?.setBandLevel(bandIndex, levelMilliBel)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val updatedBands = _equalizerState.value.bands.map {
            if (it.bandIndex == bandIndex) it.copy(currentLevelMilliBel = levelMilliBel) else it
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = "Custom"
        )
    }

    fun setBassBoost(strength: Short) {
        ensureAudioEffects()
        val valid = strength.coerceIn(0, 1000)
        try {
            bassBoost?.setStrength(valid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = valid)
    }

    fun setVirtualizer(strength: Short) {
        ensureAudioEffects()
        val valid = strength.coerceIn(0, 1000)
        try {
            virtualizer?.setStrength(valid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _equalizerState.value = _equalizerState.value.copy(virtualizerStrength = valid)
    }

    fun applyPreset(presetName: String) {
        ensureAudioEffects()
        val targetPreset = when (presetName) {
            "Rock" -> listOf(400, 200, -100, 200, 500)
            "Pop" -> listOf(-100, 200, 500, 200, -100)
            "Jazz" -> listOf(300, 100, -200, 200, 300)
            "Classical" -> listOf(400, 250, -150, 250, 400)
            "Dance" -> listOf(600, 400, 100, 300, 500)
            "Vocal" -> listOf(-200, 400, 600, 400, -100)
            "Bass" -> listOf(800, 500, 100, -100, -200)
            "Flat" -> listOf(0, 0, 0, 0, 0)
            "Sultan Club" -> listOf(700, 400, 150, 350, 600)
            else -> listOf(200, 100, 0, 100, 200) // Normal
        }

        val updatedBands = _equalizerState.value.bands.mapIndexed { index, band ->
            val level = if (index in targetPreset.indices) targetPreset[index].toShort() else 0.toShort()
            try {
                equalizer?.setBandLevel(band.bandIndex, level)
            } catch (e: Exception) {
                // Ignore
            }
            band.copy(currentLevelMilliBel = level)
        }

        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = presetName
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        cancelSleepTimer()
        positionTrackerJob?.cancel()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
        mediaSession.release()
        exoPlayer.release()
        synchronized(SultanPlayerManager::class.java) {
            if (INSTANCE === this) INSTANCE = null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SultanPlayerManager? = null

        fun getInstance(context: Context): SultanPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SultanPlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
