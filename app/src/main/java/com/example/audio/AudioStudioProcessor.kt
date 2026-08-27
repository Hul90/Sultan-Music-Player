package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import com.example.model.AudioEffectType
import com.example.model.AudioStudioConfig
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Audio Studio processor.
 *
 * The important design rule here is: never keep an entire song's PCM in RAM.
 * Decode -> Sonic speed/pitch -> DSP -> temporary PCM file -> final WAV.
 * Processing is chunked, so long tracks use bounded memory instead of allocating
 * hundreds of MB of ShortArray/ByteArray buffers.
 */
@androidx.media3.common.util.UnstableApi
object AudioStudioProcessor {

    private const val IO_BUFFER_BYTES = 64 * 1024
    private const val DSP_CHUNK_BYTES = 64 * 1024

    suspend fun extractWaveformAmplitudes(
        context: Context,
        song: Song,
        pointCount: Int = 120
    ): List<Float> = withContext(Dispatchers.IO) {
        val points = FloatArray(pointCount) { 0.08f }
        var waveformFramesSeen = 0L
        try {
            decodeAudioStream(context, song, 0L, Long.MAX_VALUE) { pcm, sampleRate, channels, _, totalFramesEstimate ->
                val shorts = pcm.asShortBuffer()
                val frameCount = shorts.remaining() / channels.coerceAtLeast(1)
                for (frame in 0 until frameCount) {
                    val absoluteFrame = waveformFramesSeen++
                    val bucket = if (totalFramesEstimate > 0) {
                        ((absoluteFrame.toDouble() / totalFramesEstimate.toDouble()) * pointCount).toInt()
                    } else {
                        ((absoluteFrame / max(1L, sampleRate.toLong() * 30L)).toInt())
                    }.coerceIn(0, pointCount - 1)
                    var peak = 0
                    for (channel in 0 until channels.coerceAtLeast(1)) {
                        peak = max(peak, kotlin.math.abs(shorts.get(frame * channels + channel).toInt()))
                    }
                    points[bucket] = max(points[bucket], (peak / 32767f).coerceIn(0.06f, 1f))
                }
                true
            }
        } catch (_: Exception) {
            for (i in points.indices) {
                val angle = i * 0.18f
                points[i] = (0.15f + 0.65f * kotlin.math.abs(sin(angle)) + 0.2f * kotlin.math.abs(cos(angle * 2.1f)))
                    .coerceIn(0.08f, 1f)
            }
        }
        points.toList()
    }

    suspend fun processAndExportSultanMix(
        context: Context,
        song: Song,
        config: AudioStudioConfig,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, "sultan_audio_studio")
        if (!tempDir.exists()) tempDir.mkdirs()
        val rawOutput = File(tempDir, "render_${System.nanoTime()}.pcm")

        try {
            onProgress(0.02f)
            val startMs = config.startMs.coerceAtLeast(0L)
            val endMs = if (config.endMs > startMs) {
                config.endMs
            } else {
                song.durationMs.coerceAtLeast(startMs + 1000L)
            }
            val selectedDurationMs = (endMs - startMs).coerceAtLeast(1L)

            var sourceSampleRate = 0
            var sourceChannels = 0
            var totalInputFrames = 0L
            var totalOutputFrames = 0L
            var outputSampleRate = 0

            val speed = config.speed.coerceIn(0.5f, 2.0f)
            val pitch = config.pitch.coerceIn(0.8f, 1.2f)
            var expectedOutputFrames = 1L

            BufferedOutputStream(FileOutputStream(rawOutput), IO_BUFFER_BYTES).use { rawOut ->
                val sonic = SonicAudioProcessor().apply {
                    setSpeed(speed)
                    setPitch(pitch)
                }
                var configured = false
                var sonicActive = false
                var dspState = DspState()

                decodeAudioStream(context, song, startMs, endMs) { pcm, sampleRate, channels, decodedFrames, durationEstimate ->
                    if (!configured) {
                        expectedOutputFrames = max(1L, ((selectedDurationMs.toDouble() / 1000.0) * sampleRate.toDouble() / speed).toLong())
                        sourceSampleRate = sampleRate
                        sourceChannels = channels
                        val stereoFormat = AudioProcessor.AudioFormat(sampleRate, 2, C.ENCODING_PCM_16BIT)
                        sonic.configure(stereoFormat)
                        sonic.flush(AudioProcessor.StreamMetadata.DEFAULT)
                        sonicActive = sonic.isActive()
                        outputSampleRate = sampleRate
                        configured = true
                    }

                    totalInputFrames += decodedFrames
                    val stereoBytes = pcm16ToStereo(pcm, channels)
                    if (sonicActive) {
                        val direct = ByteBuffer.allocateDirect(stereoBytes.size).order(ByteOrder.nativeOrder())
                        direct.put(stereoBytes).flip()
                        sonic.queueInput(direct)
                    } else {
                        val processed = processDspChunk(
                            bytes = stereoBytes,
                            sampleRate = outputSampleRate,
                            config = config,
                            state = dspState,
                            expectedOutputFrames = expectedOutputFrames
                        )
                        rawOut.write(processed)
                        val frames = processed.size / 4L
                        totalOutputFrames += frames
                        dspState.processedFrames += frames
                        onProgress(0.05f + (dspState.processedFrames.toDouble() / expectedOutputFrames.coerceAtLeast(1L).toDouble()).toFloat().coerceIn(0f, 1f) * 0.82f)
                    }
                    drainSonicOutput(
                        sonic = sonic,
                        rawOut = rawOut,
                        sampleRate = outputSampleRate,
                        config = config,
                        state = dspState,
                        expectedOutputFrames = expectedOutputFrames,
                        totalOutputFrames = { totalOutputFrames },
                        onChunkFrames = { totalOutputFrames += it },
                        onProgress = { p -> onProgress(0.05f + p * 0.82f) }
                    )
                    true
                }

                if (!configured) {
                    throw IOException("Could not decode the selected audio track")
                }

                if (sonicActive) sonic.queueEndOfStream()
                while (sonicActive && !sonic.isEnded()) {
                    val before = totalOutputFrames
                    drainSonicOutput(
                        sonic = sonic,
                        rawOut = rawOut,
                        sampleRate = outputSampleRate,
                        config = config,
                        state = dspState,
                        expectedOutputFrames = expectedOutputFrames,
                        totalOutputFrames = { totalOutputFrames },
                        onChunkFrames = { totalOutputFrames += it },
                        onProgress = { p -> onProgress(0.05f + p * 0.82f) }
                    )
                    if (before == totalOutputFrames && sonic.isEnded()) break
                }

                // Flush the final fade-out tail if the exact Sonic output was slightly longer
                // than the duration estimate. The fade is based on the expected duration, and
                // any tiny tail difference is harmless because the output is still valid PCM.
                rawOut.flush()
            }

            onProgress(0.90f)
            val sanitizedTitle = song.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val fileName = sanitizeExportFileName(
                if (config.exportFilename.isNotBlank()) config.exportFilename
                else "${sanitizedTitle}_SultanMix_${System.currentTimeMillis() % 10000}.wav"
            )

            val finalFile = saveRawPcmAsWav(
                context = context,
                rawPcm = rawOutput,
                fileName = fileName,
                sampleRate = outputSampleRate.coerceAtLeast(8_000),
                channels = 2
            )

            onProgress(1f)
            Result.success(finalFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            rawOutput.delete()
        }
    }

    private data class DspState(
        var lpL: Double = 0.0,
        var lpR: Double = 0.0,
        var vocalLpL: Double = 0.0,
        var vocalLpR: Double = 0.0,
        var hpPrevInL: Double = 0.0,
        var hpPrevInR: Double = 0.0,
        var hpPrevOutL: Double = 0.0,
        var hpPrevOutR: Double = 0.0,
        var brightPrevL: Double = 0.0,
        var brightPrevR: Double = 0.0,
        var reverbL: Double = 0.0,
        var reverbR: Double = 0.0,
        var echoIndex: Int = 0,
        var processedFrames: Long = 0L,
        var echoBufferL: ShortArray? = null,
        var echoBufferR: ShortArray? = null
    )

    private fun drainSonicOutput(
        sonic: SonicAudioProcessor,
        rawOut: OutputStream,
        sampleRate: Int,
        config: AudioStudioConfig,
        state: DspState,
        expectedOutputFrames: Long,
        totalOutputFrames: () -> Long,
        onChunkFrames: (Long) -> Unit,
        onProgress: (Float) -> Unit
    ) {
        while (true) {
            val output = sonic.output
            if (!output.hasRemaining()) break
            val bytes = ByteArray(min(output.remaining(), DSP_CHUNK_BYTES))
            output.get(bytes)
            val processed = processDspChunk(
                bytes = bytes,
                sampleRate = sampleRate,
                config = config,
                state = state,
                expectedOutputFrames = expectedOutputFrames
            )
            rawOut.write(processed)
            val frames = processed.size / 4L
            onChunkFrames(frames)
            state.processedFrames += frames
            onProgress((state.processedFrames.toDouble() / expectedOutputFrames.coerceAtLeast(1L).toDouble()).toFloat().coerceIn(0f, 1f))
        }
    }

    private fun processDspChunk(
        bytes: ByteArray,
        sampleRate: Int,
        config: AudioStudioConfig,
        state: DspState,
        expectedOutputFrames: Long
    ): ByteArray {
        val shortBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val out = ByteArray(bytes.size)
        val outBuffer = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val volumeMultiplier = (config.volumePercent / 100.0).coerceIn(0.1, 2.5)
        val fadeInFrames = (config.fadeInSeconds.coerceAtLeast(0f) * sampleRate).toLong()
        val fadeOutFrames = (config.fadeOutSeconds.coerceAtLeast(0f) * sampleRate).toLong()
        val echoDelayFrames = (sampleRate * 0.25).toInt().coerceAtLeast(1)
        if (config.effect == AudioEffectType.ECHO && state.echoBufferL == null) {
            state.echoBufferL = ShortArray(echoDelayFrames)
            state.echoBufferR = ShortArray(echoDelayFrames)
        }

        val frameCount = shortBuffer.remaining() / 2
        for (frame in 0 until frameCount) {
            val globalFrame = state.processedFrames + frame
            var l = shortBuffer.get(frame * 2).toDouble()
            var r = shortBuffer.get(frame * 2 + 1).toDouble()

            when (config.effect) {
                AudioEffectType.BASS_BOOST -> {
                    state.lpL += 0.15 * (l - state.lpL)
                    state.lpR += 0.15 * (r - state.lpR)
                    l += state.lpL * 0.6
                    r += state.lpR * 0.6
                }
                AudioEffectType.TREBLE_BOOST -> {
                    val hpL = l - state.hpPrevInL + 0.7 * state.hpPrevOutL
                    val hpR = r - state.hpPrevInR + 0.7 * state.hpPrevOutR
                    state.hpPrevInL = l; state.hpPrevInR = r
                    state.hpPrevOutL = hpL; state.hpPrevOutR = hpR
                    l += hpL * 0.5; r += hpR * 0.5
                }
                AudioEffectType.VOCAL_BOOST -> {
                    state.vocalLpL += 0.05 * (l - state.vocalLpL)
                    state.vocalLpR += 0.05 * (r - state.vocalLpR)
                    l += (l - state.vocalLpL) * 0.4
                    r += (r - state.vocalLpR) * 0.4
                }
                AudioEffectType.SOFT -> {
                    state.lpL += 0.25 * (l - state.lpL)
                    state.lpR += 0.25 * (r - state.lpR)
                    l = state.lpL; r = state.lpR
                }
                AudioEffectType.BRIGHT -> {
                    val diffL = l - state.brightPrevL
                    val diffR = r - state.brightPrevR
                    state.brightPrevL = l; state.brightPrevR = r
                    l += diffL * 0.6; r += diffR * 0.6
                }
                AudioEffectType.ECHO -> {
                    val left = state.echoBufferL!!
                    val right = state.echoBufferR!!
                    l += left[state.echoIndex] * 0.45
                    r += right[state.echoIndex] * 0.45
                }
                AudioEffectType.REVERB -> {
                    state.reverbL = state.reverbL * 0.6 + l * 0.4
                    state.reverbR = state.reverbR * 0.6 + r * 0.4
                    l += state.reverbL * 0.35
                    r += state.reverbR * 0.35
                }
                AudioEffectType.NONE -> Unit
            }

            l *= volumeMultiplier
            r *= volumeMultiplier

            if (fadeInFrames > 0 && globalFrame < fadeInFrames) {
                val ratio = globalFrame.toDouble() / fadeInFrames.toDouble()
                l *= ratio; r *= ratio
            }
            if (fadeOutFrames > 0 && globalFrame >= expectedOutputFrames - fadeOutFrames) {
                val remaining = (expectedOutputFrames - globalFrame).coerceAtLeast(0L)
                val ratio = remaining.toDouble() / fadeOutFrames.toDouble()
                l *= ratio; r *= ratio
            }

            val ls = l.coerceIn(-32767.0, 32767.0).toInt().toShort()
            val rs = r.coerceIn(-32767.0, 32767.0).toInt().toShort()
            outBuffer.put(ls).put(rs)

            if (config.effect == AudioEffectType.ECHO) {
                state.echoBufferL!![state.echoIndex] = ls
                state.echoBufferR!![state.echoIndex] = rs
                state.echoIndex = (state.echoIndex + 1) % echoDelayFrames
            }
        }
        return out
    }

    private fun decodeAudioStream(
        context: Context,
        song: Song,
        startMs: Long,
        endMs: Long,
        onPcmChunk: (pcm: ByteBuffer, sampleRate: Int, channels: Int, decodedFrames: Long, durationEstimateFrames: Long) -> Boolean
    ) {
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            val localFile = song.path.takeIf { it.isNotBlank() }?.let(::File)
            if (localFile?.isFile == true) {
                extractor.setDataSource(localFile.absolutePath)
            } else {
                extractor.setDataSource(context, song.contentUri, null)
            }

            var trackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val candidate = extractor.getTrackFormat(i)
                val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    trackIndex = i
                    format = candidate
                    break
                }
            }
            if (trackIndex < 0 || format == null) throw IOException("No decodable audio track found")

            extractor.selectTrack(trackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: throw IOException("Missing audio MIME type")
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            val totalDurationMs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                (format.getLong(MediaFormat.KEY_DURATION) / 1000L).coerceAtLeast(1L)
            } else {
                song.durationMs.coerceAtLeast(1L)
            }
            val durationEstimateFrames = ((totalDurationMs.toDouble() / 1000.0) * sampleRate).toLong().coerceAtLeast(1L)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val startUs = startMs.coerceAtLeast(0L) * 1000L
            val endUs = if (endMs == Long.MAX_VALUE) Long.MAX_VALUE else endMs.coerceAtLeast(startMs + 1L) * 1000L
            if (startUs > 0) extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val info = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            var stopped = false

            while (!outputEos && !stopped) {
                if (!inputEos) {
                    val inputIndex = codec.dequeueInputBuffer(10_000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        val size = inputBuffer?.let { extractor.readSampleData(it, 0) } ?: -1
                        val sampleTime = extractor.sampleTime
                        if (size < 0 || sampleTime < 0 || sampleTime > endUs) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEos = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outputIndex = codec.dequeueOutputBuffer(info, 10_000L)
                while (outputIndex >= 0) {
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEos = true
                    if (info.size > 0 && info.presentationTimeUs >= startUs && info.presentationTimeUs <= endUs) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null) {
                            outputBuffer.position(info.offset)
                            outputBuffer.limit(info.offset + info.size)
                            val direct = ByteBuffer.allocateDirect(info.size).order(ByteOrder.nativeOrder())
                            direct.put(outputBuffer).flip()
                            val frames = info.size / (channels * 2L)
                            val shouldContinue = onPcmChunk(direct, sampleRate, channels, frames, durationEstimateFrames)
                            if (!shouldContinue) stopped = true
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (outputEos || stopped) break
                    outputIndex = codec.dequeueOutputBuffer(info, 0L)
                }
            }
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
        }
    }

    private fun pcm16ToStereo(input: ByteBuffer, channels: Int): ByteArray {
        val source = input.duplicate().order(ByteOrder.nativeOrder()).asShortBuffer()
        val frameCount = source.remaining() / channels.coerceAtLeast(1)
        val out = ByteArray(frameCount * 4)
        val outShorts = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        when (channels) {
            1 -> repeat(frameCount) {
                val sample = source.get(it)
                outShorts.put(sample).put(sample)
            }
            2 -> {
                source.rewind()
                val shorts = source.remaining()
                val temp = ShortArray(min(shorts, 32_768))
                var copied = 0
                while (copied < shorts) {
                    val count = min(temp.size, shorts - copied)
                    source.get(temp, 0, count)
                    outShorts.put(temp, 0, count)
                    copied += count
                }
            }
            else -> {
                for (frame in 0 until frameCount) {
                    var left = 0
                    var right = 0
                    var leftCount = 0
                    var rightCount = 0
                    for (channel in 0 until channels) {
                        val value = source.get(frame * channels + channel).toInt()
                        if (channel % 2 == 0) { left += value; leftCount++ } else { right += value; rightCount++ }
                    }
                    if (rightCount == 0) { right = left; rightCount = leftCount }
                    outShorts.put((left / leftCount.coerceAtLeast(1)).toShort())
                    outShorts.put((right / rightCount.coerceAtLeast(1)).toShort())
                }
            }
        }
        return out
    }

    private fun sanitizeExportFileName(name: String): String {
        val base = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "SultanMix" }
        return if (base.lowercase().endsWith(".wav")) base else "$base.wav"
    }

    private fun saveRawPcmAsWav(
        context: Context,
        rawPcm: File,
        fileName: String,
        sampleRate: Int,
        channels: Int
    ): File {
        val relativePath = "Music/Sultan Music Player/Sultan Audio Studio"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-wav")
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val itemUri = resolver.insert(collection, values)
                ?: throw IOException("Failed to create export entry in MediaStore")
            try {
                resolver.openOutputStream(itemUri)?.use { out ->
                    writeWavHeader(out, rawPcm.length(), sampleRate, channels)
                    BufferedInputStream(FileInputStream(rawPcm), IO_BUFFER_BYTES).use { input ->
                        input.copyTo(out, IO_BUFFER_BYTES)
                    }
                } ?: throw IOException("Failed to open export output stream")
                resolver.update(itemUri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(itemUri, null, null)
                throw e
            }
            // A MediaStore Uri is not a java.io.File. Return a lightweight marker File with
            // the display name for compatibility with the existing ViewModel/UI contract.
            return File(relativePath, fileName)
        }

        @Suppress("DEPRECATION")
        val publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val outputDir = File(publicMusicDir, "Sultan Music Player/Sultan Audio Studio")
        if (!outputDir.exists() && !outputDir.mkdirs()) throw IOException("Could not create export directory")
        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { out ->
            writeWavHeader(out, rawPcm.length(), sampleRate, channels)
            BufferedInputStream(FileInputStream(rawPcm), IO_BUFFER_BYTES).use { input ->
                input.copyTo(out, IO_BUFFER_BYTES)
            }
        }
        MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), arrayOf("audio/x-wav"), null)
        return outputFile
    }

    private fun writeWavHeader(out: OutputStream, pcmBytes: Long, sampleRate: Int, channels: Int) {
        val totalDataLen = pcmBytes + 36L
        val byteRate = sampleRate.toLong() * channels * 2L
        val blockAlign = channels * 2
        val header = ByteArray(44)
        fun putAscii(offset: Int, text: String) {
            text.forEachIndexed { i, c -> header[offset + i] = c.code.toByte() }
        }
        fun putIntLE(offset: Int, value: Long) {
            for (i in 0..3) header[offset + i] = ((value shr (8 * i)) and 0xFF).toByte()
        }
        fun putShortLE(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }
        putAscii(0, "RIFF")
        putIntLE(4, totalDataLen)
        putAscii(8, "WAVE")
        putAscii(12, "fmt ")
        putIntLE(16, 16)
        putShortLE(20, 1)
        putShortLE(22, channels)
        putIntLE(24, sampleRate.toLong())
        putIntLE(28, byteRate)
        putShortLE(32, blockAlign)
        putShortLE(34, 16)
        putAscii(36, "data")
        putIntLE(40, pcmBytes)
        out.write(header)
    }
}
