package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioEffectType
import com.example.model.AudioStudioConfig
import com.example.model.Song
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SultanAudioStudioScreen(
    allSongs: List<Song>,
    selectedSong: Song?,
    waveform: List<Float>,
    studioConfig: AudioStudioConfig,
    exportProgress: Float?,
    exportMessage: String?,
    onSelectSong: (Song) -> Unit,
    onUpdateConfig: (AudioStudioConfig) -> Unit,
    onExportSultanMix: () -> Unit,
    onClearMessage: () -> Unit,
    onPreviewTrim: (Song, Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current

    var showSongPicker by remember { mutableStateOf(false) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val activeSong = selectedSong ?: allSongs.firstOrNull()
    val totalDurationMs = (activeSong?.durationMs ?: 0L).coerceAtLeast(10000L)

    val currentStartMs = studioConfig.startMs.coerceIn(0L, totalDurationMs - 1000L)
    val currentEndMs = if (studioConfig.endMs > currentStartMs) {
        studioConfig.endMs.coerceIn(currentStartMs + 1000L, totalDurationMs)
    } else {
        totalDurationMs
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("sultan_audio_studio_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // STUDIO HERO HEADER
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.headerBrush.let { Brush.horizontalGradient(listOf(Color(0xFF3E2723), Color(0xFF1B0000))) })
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SULTAN AUDIO STUDIO",
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.primary,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Professional Non-Destructive Audio Editor & Sultan Mix Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Original File Protection Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B5E20))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Safe Mode", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. SELECT AUDIO TRACK
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "1. Active Audio Track",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .clickable { showSongPicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = activeSong?.title ?: "Select a song...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhitePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${activeSong?.artist ?: "Unknown"} • ${activeSong?.formattedDuration ?: "0:00"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGraySecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showSongPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Change", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. INTERACTIVE WAVEFORM & TRIM TIMELINE
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Waveform & Trim Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selection: ${formatMs(currentEndMs - currentStartMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhitePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Waveform Canvas Visualizer
                val waveData = if (waveform.isNotEmpty()) waveform else List(60) { 0.3f }
                val startFrac = (currentStartMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                val endFrac = (currentEndMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width / waveData.size
                        val centerY = size.height / 2

                        // Draw background amplitude bars
                        waveData.forEachIndexed { index, amp ->
                            val x = index * barWidth + barWidth / 2
                            val barFrac = index.toFloat() / waveData.size
                            val isInSelection = barFrac in startFrac..endFrac

                            val barHeight = (amp * size.height * 0.8f).coerceAtLeast(4f)
                            val barColor = if (isInSelection) palette.primary else Color.White.copy(alpha = 0.2f)

                            drawLine(
                                color = barColor,
                                start = Offset(x, centerY - barHeight / 2),
                                end = Offset(x, centerY + barHeight / 2),
                                strokeWidth = (barWidth * 0.7f).coerceIn(2f, 6f),
                                cap = StrokeCap.Round
                            )
                        }

                        // Draw Trim Selection Shading
                        val startX = startFrac * size.width
                        val endX = endFrac * size.width
                        drawRect(
                            color = palette.primary.copy(alpha = 0.18f),
                            topLeft = Offset(startX, 0f),
                            size = Size(endX - startX, size.height)
                        )

                        // Draw Start & End Handle Lines
                        drawLine(
                            color = palette.primary,
                            start = Offset(startX, 0f),
                            end = Offset(startX, size.height),
                            strokeWidth = 4f
                        )
                        drawLine(
                            color = palette.primary,
                            start = Offset(endX, 0f),
                            end = Offset(endX, size.height),
                            strokeWidth = 4f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Trimming Sliders
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start: ${formatMs(currentStartMs)}", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                        Text("End: ${formatMs(currentEndMs)}", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start handle slider
                        Slider(
                            value = startFrac,
                            onValueChange = { frac ->
                                val newStart = (frac * totalDurationMs).toLong()
                                if (newStart < currentEndMs - 1000L) {
                                    onUpdateConfig(studioConfig.copy(startMs = newStart))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                        )
                        // End handle slider
                        Slider(
                            value = endFrac,
                            onValueChange = { frac ->
                                val newEnd = (frac * totalDurationMs).toLong()
                                if (newEnd > currentStartMs + 1000L) {
                                    onUpdateConfig(studioConfig.copy(endMs = newEnd))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                        )
                    }

                    // Preview Trim Button
                    Button(
                        onClick = {
                            if (activeSong != null) {
                                onPreviewTrim(activeSong, currentStartMs, currentEndMs)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = palette.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview Selected Section", color = TextWhitePrimary)
                    }
                }
            }
        }

        // 3. FADE IN & FADE OUT ENVELOPES
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "3. Fade In & Fade Out Envelopes",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Fade In Chips
                Text("Fade In Duration", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f, 1f, 2f, 3f, 5f).forEach { sec ->
                        FadeChip(
                            label = if (sec == 0f) "None" else "${sec.toInt()}s",
                            isSelected = studioConfig.fadeInSeconds == sec,
                            onClick = { onUpdateConfig(studioConfig.copy(fadeInSeconds = sec)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fade Out Chips
                Text("Fade Out Duration", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0f, 1f, 2f, 3f, 5f).forEach { sec ->
                        FadeChip(
                            label = if (sec == 0f) "None" else "${sec.toInt()}s",
                            isSelected = studioConfig.fadeOutSeconds == sec,
                            onClick = { onUpdateConfig(studioConfig.copy(fadeOutSeconds = sec)) }
                        )
                    }
                }
            }
        }

        // 4. VOLUME, SPEED & PITCH CONTROLS
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "4. Gain, Speed & Pitch Tuning",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )

                // Volume Gain
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, null, tint = palette.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Volume Gain", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                        }
                        Text("${studioConfig.volumePercent}%", style = MaterialTheme.typography.bodyMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = studioConfig.volumePercent.toFloat(),
                        onValueChange = { onUpdateConfig(studioConfig.copy(volumePercent = it.toInt())) },
                        valueRange = 50f..200f,
                        colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                    )
                }

                // Speed Multiplier
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, null, tint = palette.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Playback / Export Speed", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                        }
                        Text("${studioConfig.speed}x", style = MaterialTheme.typography.bodyMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            FadeChip(
                                label = "${spd}x",
                                isSelected = studioConfig.speed == spd,
                                onClick = { onUpdateConfig(studioConfig.copy(speed = spd)) }
                            )
                        }
                    }
                }

                // Pitch Tuning
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = palette.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pitch Adjustment", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                        }
                        Text(
                            text = when {
                                studioConfig.pitch < 0.95f -> "Deep Pitch (${studioConfig.pitch}x)"
                                studioConfig.pitch > 1.05f -> "High Pitch (${studioConfig.pitch}x)"
                                else -> "Normal"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = studioConfig.pitch,
                        onValueChange = { onUpdateConfig(studioConfig.copy(pitch = it)) },
                        valueRange = 0.8f..1.2f,
                        colors = SliderDefaults.colors(thumbColor = palette.primary, activeTrackColor = palette.primary)
                    )
                }
            }
        }

        // 5. AUDIO EFFECTS PRESETS
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "5. Studio Audio Effects",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioEffectType.values().forEach { eff ->
                        val isSelected = eff == studioConfig.effect
                        Surface(
                            color = if (isSelected) palette.primary else palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { onUpdateConfig(studioConfig.copy(effect = eff)) }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = eff.displayName,
                                    color = if (isSelected) Color.Black else TextWhitePrimary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = eff.description.take(22) + "...",
                                    color = if (isSelected) Color.Black.copy(alpha = 0.7f) else TextGraySecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. SULTAN MIX EXPORT SECTION
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, null, tint = palette.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "6. Export Master Sultan Mix",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhitePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = studioConfig.customTitle,
                    onValueChange = { onUpdateConfig(studioConfig.copy(customTitle = it)) },
                    label = { Text("Output Track Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        focusedLabelColor = palette.primary,
                        focusedTextColor = TextWhitePrimary,
                        unfocusedTextColor = TextWhitePrimary
                    )
                )

                OutlinedTextField(
                    value = studioConfig.exportFilename,
                    onValueChange = { onUpdateConfig(studioConfig.copy(exportFilename = it)) },
                    label = { Text("Export File Name (.wav)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        focusedLabelColor = palette.primary,
                        focusedTextColor = TextWhitePrimary,
                        unfocusedTextColor = TextWhitePrimary
                    )
                )

                Text(
                    text = "Export Destination: Music/Sultan Music Player/Sultan Audio Studio/",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary
                )

                // Export Progress bar if active
                if (exportProgress != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { exportProgress },
                            color = palette.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = exportMessage ?: "Rendering Sultan Mix...",
                            color = TextWhitePrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Export Button
                Button(
                    onClick = onExportSultanMix,
                    enabled = exportProgress == null && activeSong != null,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_sultan_mix_btn")
                ) {
                    if (exportProgress != null) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Download, null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Render & Save Sultan Mix", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // SONG PICKER DIALOG
    if (showSongPicker) {
        AlertDialog(
            onDismissRequest = { showSongPicker = false },
            containerColor = palette.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Select Song for Studio", color = TextWhitePrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allSongs.forEach { song ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onSelectSong(song)
                                    showSongPicker = false
                                }
                                .background(if (song.id == activeSong?.id) palette.cardBackground else Color.Transparent)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Audiotrack, null, tint = palette.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(song.title, color = TextWhitePrimary, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(song.artist, color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSongPicker = false }) {
                    Text("Close", color = palette.primary)
                }
            }
        )
    }

    // EXPORT RESULT NOTIFICATION DIALOG
    if (exportMessage != null && exportProgress == null) {
        AlertDialog(
            onDismissRequest = onClearMessage,
            containerColor = palette.surface,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Sultan Audio Studio", color = palette.primary, fontWeight = FontWeight.Bold) },
            text = { Text(exportMessage, color = TextWhitePrimary) },
            confirmButton = {
                Button(
                    onClick = onClearMessage,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun FadeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalSultanPalette.current
    Surface(
        color = if (isSelected) palette.primary else palette.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else TextWhitePrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%02d:%02d", m, s)
}
