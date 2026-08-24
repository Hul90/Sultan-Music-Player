# Sultan Music Player V8 Fix Notes

## Stability fixes
- Removed the broken template screenshot test references to `Greeting()` and `MyApplicationTheme`.
- Updated the Robolectric app-name assertion to `Sultan Music Player`.
- Replaced destructive Room migration fallback with an explicit data-preserving 2 -> 3 migration.
- Fixed playlist rename so `createdAt` and `coverUri` are not accidentally reset.
- Removed the playlist N+1 song-count query pattern and replaced it with one grouped count query.
- Persisted search history instead of keeping it only in ViewModel memory.

## Audio fixes
- Reworked Audio Studio export to process audio in bounded chunks.
- Added Media3 `SonicAudioProcessor` for streaming speed/pitch processing.
- Kept DSP state across chunks for fades, echo, reverb and tone effects.
- Export now writes temporary PCM to disk and publishes a valid WAV without creating a giant in-memory PCM array.
- Waveform extraction now uses decoded PCM instead of treating compressed file bytes as samples.

## MediaStore fixes
- Scanning no longer uses filesystem `DATA` path expressions to decide which audio files exist.
- Content URIs are the primary playback identity; filesystem paths remain an optional local fallback.
- Folder grouping handles content-URI-only songs safely.
- Removed the unnecessary `READ_MEDIA_IMAGES` permission.

## Playback / notification fixes
- Equalizer, BassBoost and Virtualizer are initialized lazily after ExoPlayer has a valid audio session.
- Media3 `DefaultMediaNotificationProvider` is explicitly configured with the Sultan playback channel and notification icon.
- The system notification therefore exposes MediaSession transport controls for previous, play/pause and next.
- The same notification channel is used by the app and the foreground-service safety notification.

## Artwork
- The supplied 512x512 Sultan Music Player artwork is now the app icon artwork.
- Adaptive and legacy density icon resources were regenerated from the supplied image.
- A dedicated monochrome notification icon was added for the status bar.

## Version
- Android app versionCode: 8
- Android app versionName: 8.0
