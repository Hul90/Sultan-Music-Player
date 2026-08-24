# Sultan Music Player - Full Playback Fix

This build updates the original AI Studio project for reliable local Android music playback.

## Playback fixes
- Starts `SultanMediaService` when playback begins so audio continues with screen off/background.
- MediaSession is attached to the same shared Media3/ExoPlayer instance.
- Keeps the playback service alive while an active track is playing and does not release the shared player from the service lifecycle.
- Lock-screen / Bluetooth / headset media controls are provided through Media3 MediaSession.
- Handles `ACTION_AUDIO_BECOMING_NOISY` and pauses when headphones are disconnected.
- Preserves shuffle, repeat-off/all/one, next, previous, seeking, speed and pitch controls.
- Adds a one-time readable-file fallback if a device exposes a MediaStore URI that its decoder cannot open.
- Uses the correct MediaStore volume URI when constructing audio content URIs.
- Keeps Android 13+ audio permission and notification permission support already present in the project.

## Long-track fixes
- Removed the accidental 45-second limit from Sultan Audio Studio export duration selection.
- Studio timeline now uses the selected track's full duration.

## Build wrapper
- Replaced the damaged Gradle wrapper JAR found in the supplied project with a small self-contained bootstrap wrapper that reads `gradle-wrapper.properties` and downloads the configured Gradle distribution on the developer machine.

## Important
The source was inspected and corrected, but this execution environment has no network access and no Android SDK/Gradle distribution cache, so a final APK could not be compiled and device-tested here. The project is prepared for Android Studio/Gradle sync on a normal development machine.

## V3 — Critical crash fix (audio-focus recursion)

Root cause found via code review of `SultanPlayerManager.kt`:

- `play()` called `requestAudioFocus()`.
- `requestAudioFocus()` built a **new** `AudioFocusRequest` + `OnAudioFocusChangeListener` on every call, and that listener called `play()` again whenever `AUDIOFOCUS_GAIN` fired.
- When the app already held audio focus and requested it again, Android could re-fire `AUDIOFOCUS_GAIN` synchronously, causing `play() -> requestAudioFocus() -> AUDIOFOCUS_GAIN -> play() -> ...` to recurse, eventually crashing with a `StackOverflowError` after a few play attempts — matching the reported symptom exactly.
- This manual `AudioManager` focus handling also duplicated logic already provided by ExoPlayer itself, which was built with `handleAudioFocus = true`.

Fix applied:
- Removed the manual `requestAudioFocus()` function, the `AudioManager`/`AudioFocusRequest` fields, and all calls to it from `play()` and `playQueue()`.
- Playback now relies solely on ExoPlayer's built-in audio focus handling (`handleAudioFocus = true`), which already manages ducking, pausing on loss, and resuming on gain, and reports state changes through the existing `Player.Listener.onIsPlayingChanged` callback that the app already observes.
- `play()` now just calls `exoPlayer.play()` directly with no focus-related recursion risk.

## V4 — Crash a few seconds into playback (foreground-service promotion race)

Symptom: audio starts playing correctly, then the app is killed by the system
roughly 2-5 seconds later.

Root cause: `SultanPlayerManager` intentionally starts `SultanMediaService`
only *after* `exoPlayer.play()` has already begun (a deliberate fix from an
earlier round, to avoid a different startup-order crash). Media3's own
automatic media notification / foreground promotion is triggered by a
`Player.EVENT_IS_PLAYING_CHANGED` event observed on the session. Because the
service (and its `MediaSession`) is created *after* playback already started,
that event has already fired once and does not fire again for this session —
so Media3 never calls `startForeground()` on its own. Since the service was
still started with `startForegroundService()`, Android requires it to reach
the foreground state quickly; when it never does, the OS kills the process
with `ForegroundServiceDidNotStartInTimeException`. This is enforced strictly
at `targetSdk 36`, matching the observed multi-second-delay-then-crash
pattern exactly.

Fix applied in `SultanMediaService.onCreate()`:
- Build a low-importance notification channel and call `startForeground()`
  ourselves, synchronously, with a simple placeholder notification, instead
  of waiting on Media3's automatic "isPlaying changed" detection.
- This satisfies Android's foreground-service timing requirement
  unconditionally, regardless of when the service is created relative to
  when playback started.
- Media3 will still take over and replace this placeholder with its own
  richer media notification (album art, title, transport controls) as it
  normally does.
- Wrapped in a try/catch so a notification failure (e.g. a denied
  `POST_NOTIFICATIONS` permission on some OEM builds) cannot crash the
  service — playback itself does not depend on the notification being
  visible.

## V5 — Notification controls, real Audio Studio trim/export, persistent cover art, instant library load

### 1. Notification stuck on "Preparing playback…", no play/pause/next/previous controls
Root cause: `SultanMediaService` built its `MediaSession` only in `onCreate()`, which only
runs once the service is first started — i.e. *after* `exoPlayer.play()` had already begun.
Media3's automatic media-notification builder reacts to player events (`isPlaying` changes,
track changes, etc.) observed on that session; since the session didn't exist yet when those
first events fired, it never had anything to build a real notification from and stayed on the
placeholder text indefinitely.

Fix: the `MediaSession` is now created once, eagerly, inside `SultanPlayerManager` alongside
the shared `ExoPlayer` — i.e. as soon as the app starts, well before any playback begins.
`SultanMediaService.onGetSession()` simply returns that shared session instead of building
its own. Because the session is already attached and listening from app startup, Media3
correctly builds and updates its full notification (art, title, play/pause/next/previous,
plus lock-screen and Bluetooth controls) as soon as playback starts.

### 2. Sultan Audio Studio: trimming/exporting a song produced a file that wasn't the real audio
Root cause: `AudioStudioProcessor.processAndExportSultanMix()` never actually read the
selected song's audio at all. It only used the song's *duration* and then generated a
completely synthetic sine-wave tone (`sin(2*PI*baseFreq*t)` etc.) as the "export" — so the
waveform/trim UI looked correct, but the saved .wav file had no relationship to the real
song content, which is why the trimmed section could never be found in it.

Additionally, exports were written to `getExternalFilesDir()` (the app's private storage
folder), which scoped storage generally excludes from the shared Music library — so even a
correct export would not have been found by the song scanner.

Fix:
- The source track is now actually decoded (via `MediaExtractor` + `MediaCodec`) and trimmed
  to the selected start/end range using real PCM samples, not a placeholder tone.
- Speed, pitch, volume, fade in/out and the studio effect (Bass Boost, Treble Boost, Vocal
  Boost, Soft, Bright, Echo, Reverb) are all applied on top of that real decoded audio.
- The rendered .wav is saved into the public `Music/Sultan Music Player/Sultan Audio Studio/`
  collection via `MediaStore` (Android 10+) or the public Music directory + MediaScanner
  (older versions), so it is properly indexed and shows up in the app's song list.

### 3. Changed album cover reverts after closing and reopening the app
Root cause: the cover picker (`ActivityResultContracts.PickVisualMedia`, the system Photo
Picker) returns a temporary `content://` Uri. Its read grant is not persistable and is
revoked once the app process dies, so the previously "changed" cover simply becomes
unreadable the next time the app is opened — the edit itself was saved, only the underlying
image had become inaccessible.

Fix: when metadata is saved, if a newly picked cover was selected, its bytes are copied
into the app's own storage (`filesDir/album_art/…`) and a stable `file://` Uri pointing at
that private copy is stored instead of the original temporary picker Uri. This keeps working
across app restarts.

### 4. App feels like it "re-scans" the whole library every time it's opened
Root cause: every app start unconditionally ran a fresh `MediaStore` query with a loading
state shown while it completed, with no cache in between.

Fix: the last successful scan is now cached in the local Room database. On startup, the
cached list is shown immediately (no loading spinner) if one exists, while a fresh
`MediaStore` scan still runs silently in the background to catch any new/removed/changed
files and update both the visible list and the cache once it finishes. Only the very first
scan ever (empty cache) still shows a loading state.
