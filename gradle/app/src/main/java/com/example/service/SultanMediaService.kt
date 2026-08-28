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

        val session = SultanPlayerManager.getInstance(this).mediaSession
        mediaSession = session

        // CRITICAL: onGetSession() is only invoked by Media3 when an external MediaController
        // actively connects to this service (e.g. a MediaController.Builder from another
        // process, a legacy MediaBrowser, or a media-button Intent). This app talks to the
        // shared ExoPlayer/MediaSession directly through the SultanPlayerManager singleton and
        // never creates such a controller, so onGetSession() is never triggered and the session
        // is never auto-registered. Without an explicit addSession() call, Media3's internal
        // MediaNotificationManager never attaches its player-event listener to this session, so
        // it never builds or updates the real notification - it stays frozen on whatever
        // placeholder was shown first ("Preparing playback...") forever, with no working
        // transport controls. Registering it explicitly here is what makes the automatic
        // notification (art, title, Play/Pause/Next/Previous) actually appear and update, and
        // is also what allows Bluetooth/lock-screen/notification-bar controls to keep working
        // for the shared player after the app's own Activity/task is closed.
        addSession(session)

        // Keep an immediate foreground notification as a safety net for strict OEM builds.
        // Media3 subsequently replaces/updates this same notification (same NOTIFICATION_ID)
        // with the full media controls generated from the MediaSession now that it is tracked.
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
        // The Activity task being dismissed must NOT terminate active music playback. The
        // MediaSessionService remains the owner of the foreground playback session, so its
        // notification continues to expose Play/Pause/Previous/Next controls.
        val player = SultanPlayerManager.getInstance(this).exoPlayer
        if (player.isPlaying) return

        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        // The singleton player/session is shared with the Activity and must not be released by
        // the service lifecycle. Media3 may recreate the service independently of the Activity.
        // removeSession() only unregisters it from THIS service instance/notification manager -
        // it does not release the underlying MediaSession or ExoPlayer, which stay alive in the
        // SultanPlayerManager singleton for the next time the service is started.
        mediaSession?.let { session ->
            try {
                if (isSessionAdded(session)) removeSession(session)
            } catch (_: Exception) {
                // Ignore - service is tearing down regardless.
            }
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 4321
    }
}
