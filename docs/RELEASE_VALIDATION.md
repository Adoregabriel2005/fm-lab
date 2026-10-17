# 1.0 release validation

Candidate: **1.0.0-rc1**, versionCode 11. Scope: Galaxy S4 Mini GT-I9195, LineageOS 14.1 / Android 7.1.2, analyzed CAF native radio profile. Other models and Android versions are not certified by this release.

This candidate is **not yet promoted to stable**. A physical target was not connected during preparation. Earlier hardware observations do not validate the 0.8–0.10 refactors and new analysis UI end to end.

## Completed for RC1

- [x] Build succeeds and 204 automated checks pass.
- [x] APK signature verifies; signing certificate matches 0.10 for in-place upgrade eligibility.
- [x] APK reports `br.local.fmlab`, versionCode 11, versionName `1.0.0-rc1`, minimum API 21, and target API 25.
- [x] App version label and diagnostic header read installed package metadata.
- [x] Build output filename derives from manifest versionName.
- [x] No new permission added; audio permission remains `RECORD_AUDIO`.
- [x] English documentation identifies compatibility and measurement limitations.

## Physical checks required before stable promotion

Run on the exact target and record the candidate/commit, ROM build, native radio version, result, and relevant sanitized errors. Leave untested items unchecked.

- [ ] Upgrade from a distributed 0.10 APK without uninstalling; confirm favorites, saved measurements, and settings remain available.
- [ ] Cold-start with native FM off, then initialize FM through the native app and reconnect.
- [ ] Tune valid conventional and accepted extended-band frequencies; confirm rejected requests have useful feedback.
- [ ] Mute/unmute and automatic stereo/forced mono; verify events and unknown states remain distinct.
- [ ] Confirm received RDS updates and history; verify native app coexistence.
- [ ] Start audio capture, stop, and restart repeatedly. Test permission denial/re-grant and headphone removal/reconnection. Confirm direct FM audio rather than microphone pickup.
- [ ] Check waveform, meters, spectrum, spectrogram, freezing/resuming, and stale-data handling against known audio. Do not claim voltage, analog VU calibration, or intersample true peak.
- [ ] Complete A/B trials; reject missing samples and invalidate references after tuning or leaving the screen.
- [ ] Finish and cancel a quick scan; verify requested-frequency restoration and correct audio state. Repeat a cancellation with screen lock/home, and explicitly open the native app during a scan.
- [ ] Exercise rapid tuning/control taps, reconnects, foreground/background transitions, and activity recreation without crashes or accumulating work.
- [ ] Complete named measurement, continuous logging, CSV save/cancel, and inspect the saved file through a real Android document provider.
- [ ] Check compact/full layout, scrolling, navigation, text size, and long RDS on the S4 Mini screen.
- [ ] Run sustained reception and audio analysis for at least 30 minutes; record crashes/ANRs, UI responsiveness, and observed memory/CPU behavior. A single soak test does not guarantee absence of bugs.

## Promotion criteria

Promote only after the physical checks above have recorded outcomes and any material failures are resolved. Build/signature/unit checks alone are insufficient evidence of receiver stability. Update this document and release notes with actual test results; do not mark untested items as passed.

The current implementation cannot forcibly time out a stalled synchronous vendor Binder call. Driver availability, routing, and vendor shutdown behavior remain firmware-dependent. Keep older releases available while evaluating the candidate.
