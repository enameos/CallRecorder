# Call Recorder (mic + speakerphone)

A Kotlin/Android Studio project for a manual, consent-based call recorder.
Built for Android 10+ (tested target: Galaxy Z Fold6/7/8, Android 14/15).

## How it works — and its real limitation

Android does not allow any app to silently tap into another app's call
audio (phone, WhatsApp, Telegram, Messenger, etc.) — that channel is
blocked at the OS level. This app instead:

1. You start a call as normal, in any app.
2. You tap **Start Recording** in this app and put the call on
   **speakerphone**.
3. The app records via the **microphone**, using
   `MediaRecorder.AudioSource.VOICE_COMMUNICATION` by default, which
   applies the device's built‑in echo cancellation/noise suppression —
   this is what lets it pick up both your voice and the other party's
   voice coming out of the speaker without it sounding like a muddy room
   recording.
4. Tap **Stop Recording** (in-app or from the persistent notification)
   when done.

This works with *any* calling app, because it doesn't depend on what's
"inside" the call — but it only picks up audio that's actually audible
through the speaker, and quality depends on room noise and speaker
volume. There is no way to make this automatic or silent without
violating Android's platform protections (and likely the law, depending
on your jurisdiction's consent requirements) — see `speaker_reminder`
and `consent_reminder` strings, shown to the user before every
recording starts.

## Project structure

- `RecordingService` — foreground service; owns the `MediaRecorder`
  instance, notification, and start/stop lifecycle.
- `MainActivity` — record button, permission requests, recordings list.
- `SettingsActivity` + `root_preferences.xml` — file format, audio
  source, quality, and storage location settings.
- `RecordingsRepository` — lists/deletes files from either app-private
  storage or a user-chosen folder (Storage Access Framework).
- `RecordingsAdapter` — in-app playback, share, delete per recording.

## Settings implemented

| Setting | Options |
|---|---|
| File format | AAC/M4A (recommended) or AMR-NB/3GP (smaller, lower quality) |
| Audio source | Voice Communication (noise/echo suppression) or raw Mic |
| Quality | 32 / 64 / 128 kbps (AAC only) |
| Storage location | App-private folder, or any folder you pick via the system folder picker |

**Note on formats:** `MediaRecorder` natively supports AAC (.m4a) and
AMR-NB (.3gp) only. True MP3 or WAV encoding requires either bundling a
third-party encoder library (e.g. LAME for MP3) or writing raw PCM via
`AudioRecord` and boxing it into a WAV header yourself — both are
straightforward additions if you want them, just flag it and I'll add
one.

## Setup

1. Open the `CallRecorder` folder in Android Studio (Koala/2024.1+).
2. Let it sync — Android Studio will generate the Gradle wrapper
   automatically on first open if it's missing.
3. Run on a device or emulator with API 29+.
4. Grant microphone (and, on Android 13+, notification) permission when
   prompted.

## Permissions used

`RECORD_AUDIO`, `MODIFY_AUDIO_SETTINGS`, `FOREGROUND_SERVICE`,
`FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`. No
`SYSTEM_ALERT_WINDOW`, accessibility service, or root access is used —
this keeps the app Play Store-policy-compatible, since it only ever
records with the user actively starting each session.

## Legal

Call-recording consent laws vary by country/state (one-party vs
all-party consent). This app surfaces a reminder dialog before each
recording, but you're responsible for complying with local law —
this isn't legal advice.
