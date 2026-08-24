package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R
import com.example.SultanApp
import com.example.player.SultanPlayerManager

@OptIn(UnstableApi::class)
class SultanMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Media3 owns the live notification and maps these actions directly to the shared
        // MediaSession/ExoPlayer. This gives reliable system notification + lock-screen controls:
        // Previous, Play/Pause and Next. Android 13+ System UI also routes media commands to the
        // MediaSession, so the controls work without custom broadcast receivers.
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(SultanApp.CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply { setSmallIcon(R.drawable.ic_notification_music) }
        setMediaNotificationProvider(provider)

        mediaSession = SultanPlayerManager.getInstance(this).mediaSession

        // Keep an immediate foreground notification as a safety net for strict OEM builds.
        // Media3 subsequently replaces/updates this same notification with the full media
        // controls generated from the MediaSession.
        startForegroundWithPlaceholderNotification()
    }

    private fun startForegroundWithPlaceholderNotification() {
        val channelId = SultanApp.CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(channelId) == null) {
                manager?.createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = getString(R.string.notification_channel_description)
                        setShowBadge(false)
                    }
                )
            }
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_preparing))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            // Media3 will retry/update its own media notification. Playback itself does not
            // depend on this placeholder construction succeeding on every OEM.
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playback alive when the user swipes the Activity away. MediaSessionService owns
        // the foreground lifecycle while ExoPlayer is actively playing.
        if (!SultanPlayerManager.getInstance(this).exoPlayer.isPlaying) {
            super.onTaskRemoved(rootIntent)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // The singleton player/session is shared with the Activity and must not be released by
        // the service lifecycle. Media3 may recreate the service independently of the Activity.
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 4321
    }
}
