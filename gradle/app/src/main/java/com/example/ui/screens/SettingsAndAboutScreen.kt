package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeOption
import com.example.ui.components.ThemeSelectorBar
import com.example.ui.theme.LocalSultanPalette
import com.example.ui.theme.TextGraySecondary
import com.example.ui.theme.TextWhitePrimary

@Composable
fun SettingsAndAboutScreen(
    currentTheme: ThemeOption,
    onSelectTheme: (ThemeOption) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalSultanPalette.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.gradientBrush)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // DEVELOPER / SULTAN BRANDING HERO CARD
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(palette.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "Sultan Logo",
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Sultan Music Player",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Version 1.0.0 Pro Master Edition",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGraySecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Developer Credentials Card
                Surface(
                    color = palette.cardBackground,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Lead Developer & Architect", color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
                                Text("MD SULTAN MAHAMUD", color = TextWhitePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        // Email Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:sultanmahamud5497@gmail.com")
                                        putExtra(Intent.EXTRA_SUBJECT, "Sultan Music Player Feedback")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Email, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Email Support", color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
                                Text("sultanmahamud5497@gmail.com", color = palette.primary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        // Mobile Phone Link
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:01740236384")
                                    }
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Call, null, tint = palette.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Contact Mobile", color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
                                Text("01740-236384", color = palette.primary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // DYNAMIC THEMES SELECTOR
        ThemeSelectorBar(
            currentTheme = currentTheme,
            onSelectTheme = onSelectTheme
        )

        // AUDIO ENGINE & CODEC SPECIFICATIONS
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
                    Icon(Icons.Default.Audiotrack, null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audio Engine & Codec Support", color = TextWhitePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                SettingRow(title = "Media Engine", subtitle = "AndroidX Media3 ExoPlayer 1.3.1 with DSP Pipeline")
                SettingRow(title = "Supported Codecs", subtitle = "MP3, AAC, FLAC (Lossless), WAV (PCM), OGG Vorbis, M4A, OPUS, ALAC")
                SettingRow(title = "Audio Focus & Headset", subtitle = "Automatic pause on disconnect, duck on notification")
                SettingRow(title = "Gapless Playback", subtitle = "Seamless track transitions enabled")
            }
        }

        // LIBRARY SCAN & CACHE
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Library Scanner & Storage", color = TextWhitePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = "Scan on-device storage for newly added audio tracks and update the local database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGraySecondary
                )

                Button(
                    onClick = onRescanLibrary,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Rescan Device Audio Files", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // OPEN SOURCE & ARCHITECTURE INFO
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = palette.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Architecture & Open Source", color = TextWhitePrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Text("• Kotlin 2.0 & Jetpack Compose Modern Reactive Architecture", color = TextGraySecondary, style = MaterialTheme.typography.bodyMedium)
                Text("• AndroidX Room SQLite Local Persistence with Metadata DAO", color = TextGraySecondary, style = MaterialTheme.typography.bodyMedium)
                Text("• MediaSessionService for Background Playback & Lockscreen Controls", color = TextGraySecondary, style = MaterialTheme.typography.bodyMedium)
                Text("• Custom Sultan Audio DSP Synthesizer & WAV Studio Engine", color = TextGraySecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, color = TextWhitePrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        Text(subtitle, color = TextGraySecondary, style = MaterialTheme.typography.labelSmall)
    }
}
