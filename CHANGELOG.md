# Changelog

## 0.10

- Serialize scan/control work through one bounded worker and synchronize CAF transport calls.
- Reject stale connection, scan, and command completions; stop offscreen RF animation in compact/paused mode.
- Preserve the first scan cancellation policy, restore on ordinary pause, and handle deadline jumps/interruption cleanup.
- Fix audio frame ownership across capture generations, bound empty reads, preserve split stereo PCM frames, and move vendor stop requests off the UI thread.
- Reuse FFT workspace/window and meter coefficients/scratch buffers.
- Move CSV writes off the UI thread, reject missing pending exports, and skip malformed saved entries.
- Standardize Java formatting and use named mute/stereo operations.
- 53 new regression checks; 204 total pass. Physical validation and device performance measurements remain pending.

## 0.9

- Guided 10-second antenna A/B comparison using mean raw SINR and variation, with freshness/coverage validation and invalidation on state changes.
- Eight-second, 32-band audio spectrogram and waveform/spectrum/spectrogram freezing; meters and alerts stay live.
- Continuous PCM digital meters independent of UI frame delivery: 300 ms exponential RMS, sample peak, 1-second peak hold, 20 dB/s decay, and capture-session full-scale sample counts.
- Measure stereo PCM channels before mono downmix when stereo fallback is available.
- Oscilloscope rising-zero trigger hysteresis, per-pixel extrema, and displayed-window RMS/peak.
- Add 46 reception/metering/trigger checks (151 total). Build and tests pass; physical validation and performance testing remain pending.
- Levels are dBFS, not calibrated analog VU/voltage; sample peaks are not intersample true peaks.

## 0.8

- Separate the CAF Binder transport into `CafRadioBackend` behind an abstract internal contract.
- Add conservative profile selection and eight regression checks against mismatched/unknown protocols (105 total checks).
- Add read-only discovery of known FM packages, service metadata, compatibility report copying, and native app launch.
- Declare Android 5.0 / API 21 minimum; guard API 23 runtime audio permission calls.
- Add a device support matrix, porting requirements, roadmap, and GitHub issue template.
- No new phone's tuner is claimed as supported. Physical regression testing and legacy OS testing remain pending.

## 0.7

- Fixed bottom navigation for the S4 Mini display.
- Persistent compact mode hides connection controls and the RF graph while retaining tuning and signal quality.
- Mute control reads the native service state.
- Persistent, deduplicated history of the last 12 requested frequencies.
- Timestamped history of the last 30 RadioText changes during the session.
- Reception sharing with recent metrics and an explicitly requested frequency.
- Corrected the app name in the launcher.
- Build/signature checks and 97 automated checks passed; the new UI and controls await physical-device validation.

## 0.6

- 76–108 MHz tuning requests and wide, conventional, and extended-only scans.
- Band endpoint and channel alignment tests.
- Device, ROM, and native service compatibility documentation.
- Confirmed a 76.1 MHz tuning event on the target; no useful reception confirmed at that test point.

## 0.5

- Actual stereo/mono receiver events and automatic stereo/forced mono selection.
- Quick/full scans, candidate refinement, and reception ranking.
- Corrected signed 8-bit SINR interpretation before ranking.
- Keep-screen-on during scanning, cancellation, and attempted tuning restoration.
- Repository structure, documentation, and build scripts for GitHub publication.

## 0.4

- Favorites, named location measurements, continuous logging, and CSV export.
- Audio spectrum, low audio detection, and digital peak warnings.
- Navigation shortcuts and keep-screen-on option.

## 0.3

- Card-based interface, adjustable signal colors, and fast RF readings.
- Direct FM audio capture with oscilloscope and RMS/peak levels.

## 0.2

- Initialize the receiver through the native UI to work around a Looper failure in the remote Binder power-on call.

## 0.1

- Initial proof of native service connection, tuning, RDS, RSSI, and SINR access.
