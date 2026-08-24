# Sultan Music Player V8

A feature-rich Android music player built with Kotlin + Jetpack Compose + Media3.

## Included in V8

- Background playback with Media3 `MediaSessionService`
- System notification and lock-screen media controls
- Previous / Play-Pause / Next controls from the notification
- Queue, shuffle, repeat, seek, playback speed and pitch
- Album/artist/folder/library browsing
- Favorites, playlists and play history
- Room-backed library cache for fast startup
- Search history persistence
- Equalizer, bass boost and virtualizer with lazy audio-session initialization
- Audio Studio trim, speed, pitch, volume, fades and DSP effects
- Chunked Audio Studio rendering to keep RAM usage bounded on long tracks
- WAV export to `Music/Sultan Music Player/Sultan Audio Studio`
- Scoped-storage friendly MediaStore scanning using content URIs
- Supplied Sultan Music Player artwork configured as the application icon
- Android 13+ notification/audio permission handling

## Build

1. Open the project in a current Android Studio version that supports the configured Android Gradle Plugin/Kotlin versions.
2. Let Gradle sync and download dependencies.
3. Build the `debug` variant first.
4. For a signed release, provide `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` for the configured `upload` key alias.

## Runtime permissions

- Android 13+: `READ_MEDIA_AUDIO` and `POST_NOTIFICATIONS`
- Android 12 and below: `READ_EXTERNAL_STORAGE`
- Android 9 and below may also need legacy write access for public WAV export

## Notification controls

Playback is exposed through a single Media3 `MediaSession`. The system media notification therefore receives the standard Media3 transport controls directly from the player, including previous, play/pause and next. No separate broadcast receiver is required for these controls.

## Audio Studio memory model

V8 does not decode an entire song into a giant `ShortArray`. Audio is decoded and passed through Media3's streaming `SonicAudioProcessor` and chunked DSP processing, then written to a temporary PCM file before the final WAV is published. This is intended to make long-track exports much safer on phones with limited RAM.
