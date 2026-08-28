package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerState
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhitePrimary

@Composable
fun EqualizerScreen(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChange: (bandIndex: Short, level: Short) -> Unit,
    onBassBoostChange: (strength: Short) -> Unit,
    onVirtualizerChange: (strength: Short) -> Unit,
    onApplyPreset: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("equalizer_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // EQUALIZER HEADER & MASTER TOGGLE
        Surface(
            color = palette.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sultan Equalizer Pro",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextWhitePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (equalizerState.isEnabled) "Active • High Fidelity DSP" else "Disabled (Bypass)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (equalizerState.isEnabled) palette.primary else TextMuted
                        )
                    }
                }

                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.primary,
                        checkedTrackColor = palette.primary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("eq_master_switch")
                )
            }
        }

        // PRESETS SELECTOR BAR
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Acoustic Presets",
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
                    equalizerState.presets.forEach { preset ->
                        val isSelected = preset == equalizerState.currentPreset
                        Surface(
                            color = if (isSelected) palette.primary else palette.surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .clickable { onApplyPreset(preset) }
                                .testTag("eq_preset_${preset.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = preset,
                                color = if (isSelected) Color.Black else TextWhitePrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5-BAND GRAPHIC EQUALIZER
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "5-Band Graphic Equalizer",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )

                // Band sliders
                equalizerState.bands.forEachIndexed { index, band ->
                    val label = bandLabels.getOrElse(index) { "${band.centerFreqHz} Hz" }
                    val dbValue = (band.currentLevelMilliBel / 100).toInt()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhitePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (dbValue >= 0) "+$dbValue dB" else "$dbValue dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = band.currentLevelMilliBel.toFloat(),
                            onValueChange = { onBandLevelChange(band.bandIndex, it.toInt().toShort()) },
                            valueRange = band.minLevelMilliBel.toFloat()..band.maxLevelMilliBel.toFloat(),
                            enabled = equalizerState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primary,
                                activeTrackColor = palette.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("eq_band_slider_$index")
                        )
                    }
                }
            }
        }

        // BASS BOOST & 3D VIRTUALIZER
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Sound Enhancement & Spatial Effects",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )

                // Bass Boost
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sultan Bass Boost", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                        }
                        Text("${(equalizerState.bassBoostStrength / 10)}%", style = MaterialTheme.typography.bodyMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = equalizerState.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = equalizerState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )
                }

                // 3D Virtualizer
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SurroundSound, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("3D Spatial Virtualizer", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                        }
                        Text("${(equalizerState.virtualizerStrength / 10)}%", style = MaterialTheme.typography.bodyMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = equalizerState.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = equalizerState.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primary,
                            activeTrackColor = palette.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("virtualizer_slider")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
