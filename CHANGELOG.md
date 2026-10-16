# Changelog

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
