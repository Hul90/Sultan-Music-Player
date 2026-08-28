package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun SleepTimerDialog(
    activeRemainingSeconds: Long?,
    onStartTimer: (minutes: Int, fadeOut: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalSultanPalette.current

    var selectedMinutes by remember { mutableFloatStateOf(30f) }
    var fadeOutEnabled by remember { mutableStateOf(true) }

    val presetTimes = listOf(5, 10, 15, 30, 45, 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.testTag("sleep_timer_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhitePrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (activeRemainingSeconds != null && activeRemainingSeconds > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Active Timer Running",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val mins = activeRemainingSeconds / 60
                            val secs = activeRemainingSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", mins, secs),
                                style = MaterialTheme.typography.displayMedium,
                                color = TextWhitePrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    onCancelTimer()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancel Timer", color = Color.White)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Turn off music automatically after:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGraySecondary
                    )

                    // Presets Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetTimes.take(3).forEach { mins ->
                            PresetChip(
                                minutes = mins,
                                isSelected = selectedMinutes.toInt() == mins,
                                onClick = { selectedMinutes = mins.toFloat() }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetTimes.drop(3).forEach { mins ->
                            PresetChip(
                                minutes = mins,
                                isSelected = selectedMinutes.toInt() == mins,
                                onClick = { selectedMinutes = mins.toFloat() }
                            )
                        }
                    }

                    // Custom Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Custom Duration", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                            Text("${selectedMinutes.toInt()} mins", style = MaterialTheme.typography.bodyMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = selectedMinutes,
                            onValueChange = { selectedMinutes = it },
                            valueRange = 1f..120f,
                            steps = 119,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primary,
                                activeTrackColor = palette.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }

                    // Fade Out Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Gradual Fade Out", style = MaterialTheme.typography.bodyMedium, color = TextWhitePrimary)
                            Text("Gently decrease volume before stopping", style = MaterialTheme.typography.labelSmall, color = TextGraySecondary)
                        }
                        Switch(
                            checked = fadeOutEnabled,
                            onCheckedChange = { fadeOutEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.primary,
                                checkedTrackColor = palette.primary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (activeRemainingSeconds == null || activeRemainingSeconds <= 0) {
                Button(
                    onClick = {
                        onStartTimer(selectedMinutes.toInt(), fadeOutEnabled)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("start_timer_btn")
                ) {
                    Text("Start Timer", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextGraySecondary)
            }
        }
    )
}

@Composable
private fun PresetChip(
    minutes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalSultanPalette.current

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                if (isSelected) palette.primary else palette.cardBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$minutes m",
            color = if (isSelected) Color.Black else TextWhitePrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
