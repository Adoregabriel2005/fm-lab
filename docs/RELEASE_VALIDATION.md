# 1.0 release validation

Release: **1.0.0**, versionCode **14**. Physical target: **Samsung Galaxy S4 Mini GT-I9195, LineageOS 14.1 / Android 7.1.2**, with the native CAF radio `com.caf.fmradio` versionCode 25 / versionName 7.1.2. No root was required. Testing took place on 2026-10-16.

This is a release for that supported configuration, not certification of every FM-capable phone. Native services, firmware routing and synchronous Binder shutdown behavior remain outside the APK's control.

## Build and packaging

- 217 automated checks pass: quality/rate 15, audio/measurement/CSV 52, scanning/SINR 16, extended band 14, adapter selection 8, A/B trials 22, continuous meters/trigger 24, lifecycle/workspace/queue 66.
- APK signature verifies and its certificate matches the previous distributed build. Package remains `br.local.fmlab`; minimum API 21, target API 25. No new permission was added.
- Application and launchable activity labels both resolve to **FM Lab**. Packaging now rejects unexpected labels. The physical app drawer displayed the correct name; an old shortcut-cache issue was not reproduced on the current home pages.
- Version text, diagnostic header and APK filename derive from metadata.

## Physical results

Most long receiver exercises used RC2/RC3. Release 1.0 retains those changes and adds explicit name resources and removal of stale audio text/displays. The final APK was rebuilt, unit-tested, installed in place and checked on the phone. Results are not represented as a complete rerun of every candidate test on the final APK.

| Area | Observation |
| --- | --- |
| Upgrade | The phone's 0.6 installation upgraded through candidates and 1.0 without uninstalling. Existing favorite, named measurement and settings remained available; UID and first-install time were retained. Direct upgrade from 0.10 was not separately exercised, although the signing certificate matches. |
| Native initialization | FM-off state was shown; the native radio initialized after wired headphones were attached. FM Lab reconnected and displayed live receiver data. |
| Tuning and RDS | Manual conventional tuning was confirmed in native logs. RDS station name and RadioText updated. Wide-band scanning exercised extended frequencies; this is not proof of reception across every requested frequency or the entire supported range. |
| Stereo and mute | Real stereo events, forced mono, return to automatic stereo, mute and unmute were observed. Unknown event state is kept distinct from mono. |
| Direct audio | Source 1998 delivered 44.1 kHz mono PCM. Real waveform, RMS/peak meters, spectrum and spectrogram rendered. No microphone fallback was used. PCM mono and RF stereo are separate facts. |
| Capture lifecycle | Three controlled stop/start cycles on the final APK cleared the displays on stop and recovered direct PCM on restart. Home/background and return left analysis stopped, with no stale values. Normal permission denial/re-grant was also exercised. Stopping capture clears the waveform, meters and stale RMS/peak summary. Permanent denial with “do not ask again” was not tested. |
| Headphones | The user removed the P2 cable, waited and reconnected it; the user confirmed audio returned. Native FM off/on transitions and subsequent live samples were observed. |
| Freeze | Freeze changed the spectrum/spectrogram into a frozen state; resume returned to live history. It does not pause playback. |
| Antenna A/B | Two ten-second trials each collected 95 valid samples and displayed mean, variation and difference. The antenna was not physically moved for these trials: this validates workflow, not calibrated improvement detection. References were invalidated after leaving the app. |
| Scanning | A quick wide scan completed 160 coarse channels with 12 refined candidates, eight samples per candidate. A separate cancelled scan displayed partial results. Native logs confirmed restoration and unmuting. Opening/using the native app during another scan interrupted the shared session; that run was not counted as a completed scan. |
| Screen awake | Unchecking the test preference during scanning retained the Android keep-screen-on flag. The preference was restored afterward. |
| Measurements and CSV | A named ten-second measurement saved 96 samples. A real Android DocumentsUI export saved a 294-sample continuous log: 295 CSV rows including header, six columns, 12,913 bytes. Content was parsed locally. On the final APK, cancellation from the real document provider returned “export cancelled, data kept in app”; private reception measurements are not committed. |
| Small display | Fixed navigation, manual-frequency keyboard dismissal, scrolling, long RDS, compact/full settings and audio panels were exercised on the 540×960 screen. |

## Sustained observation and limits

A 30-minute telemetry run covered mixed reception, scanning, audio analysis, navigation, candidate installation and permission tests. Its crash buffer was empty. Process changes from installing RC3 and revoking audio permission were intentional; they are not counted as crashes. This was **not** a continuous 30-minute final-APK audio soak or an isolated performance benchmark. Sampled PSS varied with workload and process lifetime; it does not prove a leak or the absence of one. No exhaustive ANR or vendor-failure certification is claimed.

Further coverage remains desirable: uninterrupted long final-build audio sessions, arbitrary rapid user gestures, low-memory recreation, repeated screen-lock/scan cancellation, missing/broken document providers, permanent permission denial, analog calibration and devices/ROMs beyond the target above. Existing algorithm tests do not establish analog tuner accuracy or calibrated RSSI units. Keep earlier releases available for rollback.

A stalled synchronous vendor Binder call cannot be forcibly timed out by this implementation. Frequency restoration, unmuting and driver shutdown are best effort. Meters report digital PCM levels; FM Lab does not infer transmitter watts or distance.
