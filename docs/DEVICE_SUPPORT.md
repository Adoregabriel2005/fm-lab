# Device support and porting roadmap

## Goal and present status

The goal is to reuse the native FM implementation on more phones and ROMs, including stock firmware and older Android releases. **This is an ongoing porting effort, not universal FM support.**

Version 0.8 introduces adapter separation, bounded native-radio discovery, and a copyable compatibility report. The app now declares Android 5.0 / API 21 as its minimum, with runtime audio permission calls guarded for API 23+. Installation eligibility is separate from tuner support. No API 21/22 device has been physically validated yet. Android versions below 5.0 are not currently supported.

Only the CAF profile from `com.caf.fmradio`, versionCode 25 / versionName 7.1.2, is implemented. Physical FM testing remains limited to GT-I9195 on LineageOS 14.1 / Android 7.1.2. Matching a package/version on another phone is a candidate match, not a verified device claim.

## Why the native app is not a universal bridge

A native app may access private vendor libraries, a non-exported service, signature-only permissions, or privileged device nodes. Launching it does not grant FM Lab those rights. Android services define exported access and permission requirements; see the [official service manifest documentation](https://developer.android.com/guide/topics/manifest/service-element).

The same brand or chipset does not establish the Binder interface, operation numbers, callbacks, measurement units, audio source, or supported frequency range. FM Lab must not send CAF transactions to a Motorola, Samsung, MediaTek, Sony, or other unknown service.

Some phones may support only opening the native app from FM Lab. If there is no accessible external interface, full integration may require vendor cooperation, a ROM-side helper, or a separately designed privileged installation. The ordinary APK cannot promise no-root control on such devices. We do not replace protected system apps with unsigned modified copies.

## Current matrix

| Integration | Discovery / native app launch | Tuner control | RF / RDS / stereo / PCM |
| --- | --- | --- | --- |
| GT-I9195 / LineageOS 14.1 / analyzed CAF profile | Implemented | Verified in 1.0 candidate regression | RF/RDS/stereo/direct mono PCM exercised on target |
| Same CAF package and exact version on another ROM/device | Candidate detection | Profile can be selected; device unverified | Must be tested separately |
| Motorola `com.motorola.fmplayer` | Candidate discovery / launch | No adapter | Unknown; Motorola package alone is insufficient |
| MediaTek / AOSP FM packages | Candidate discovery / launch | No adapter | Unknown |
| Samsung stock FM | Candidate discovery / launch | No adapter | Unknown; separate from the S4's CAF integration |
| Sony / Xiaomi / Huawei candidates | Candidate discovery / launch | No adapter | Unknown |
| Other package names | Not yet in candidate list | No adapter | Unknown |

The list is bounded and is not an inventory of all FM applications. A missing package does not prove missing hardware. Discovery does not power on the tuner or test a service.

## What is needed for each new target

1. **Exact environment:** phone model, ROM/build, Android version, and native radio package/version. Confirm the original app actually receives over-the-air stations, with wired headphones if required.
2. **Accessible control path:** service/component, exported status, effective permission requirements, and legal access from an ordinary installed app. The 0.8 report lists declared service metadata but cannot prove that binding or transactions will succeed.
3. **Verified protocol:** interface descriptor, method signatures, transaction mapping, callbacks, threading, initialization, and cleanup. Review legally available source or inspect a locally owned app; do not publish proprietary APKs or firmware in this repository.
4. **Capability evidence:** independently verify power/state, tuning, current frequency if exposed, mute, antenna, RDS, stereo events, RSSI/SINR encoding and units, frequency limits, and direct FM PCM availability.
5. **Physical tests:** known strong/weak stations, headphones attached/detached, mute and restore behavior, background/resume, disconnection, scan cancellation, band endpoints, and permission denial.
6. **Recorded support level:** discovery only, control verified, metadata verified, or full analyzer verified. Unavailable features must remain unavailable; do not substitute microphone audio or fabricated measurements.

An APK alone may be insufficient because the native app can depend on framework JARs, HALs, privileged permissions, and vendor libraries supplied by its original firmware.

## Architecture work

Implemented in 0.8:

- `RadioBridge` is now an abstract internal operation contract.
- `CafRadioBackend` contains the previously verified CAF Binder transport.
- `BackendRegistry` selects only the exact implemented CAF profile; package-only and mismatched-version selections are rejected by tests.
- `DeviceSupport` inspects a known list of installed FM packages, lists service metadata, copies a report on user action, and offers native app launch.
- API 21 minimum and API 23 runtime-permission guard. See [Android runtime permissions](https://developer.android.com/training/permissions/requesting).

Still needed before adding another adapter:

- Replace the transitional CAF-shaped operation IDs and callback registration with typed, vendor-neutral control and event interfaces. Future adapters must map operations, not reuse unverified wire numbers.
- Add per-adapter capabilities, measurement encodings, audio source configuration, and band plans. The current SINR normalization and source 1998 are CAF-target assumptions, not universal settings.
- Make UI controls and scanning depend on supported capabilities; a basic tuner might expose neither SINR nor direct PCM.
- Add transport tests with fakes and physical tests for each actual protocol. The selection-policy tests do not validate hardware behavior.
- Audit modern Android package visibility, service lifecycle, target SDK migration, installation requirements, and audio restrictions when extending OS coverage.
- Validate API 21/22 startup and permissions on actual legacy devices. Supporting Android 4.x would additionally require UI/API fallbacks and a separate compatibility audit.

## Priorities

1. Extend regression coverage beyond the S4 Mini tests recorded in [release validation](RELEASE_VALIDATION.md).
2. Gather reports and locate a working native app for the Moto g06; determine its actual service/library path before promising a backend.
3. Select one additional accessible CAF or MediaTek device for a verified port.
4. Implement the typed capability layer, then the first non-CAF adapter.
5. Expand the tested matrix one model/ROM at a time, including stock ROMs and legacy Android.

## How to submit a report

Install the latest FM Lab APK and open **Dados → Compatibilidade / rádios do aparelho** in the panel settings. Tap **Copiar relatório**, review it, and paste it into a Device support issue. It contains model/build information and known FM package/service metadata, not serial numbers, accounts, locations, or unrelated applications.

Add whether the original radio works, the exact missing feature/error, and whether the phone uses stock firmware or a custom ROM. Do not include private device identifiers or complete logs containing personal information. Do not attach proprietary APKs. Reports are reviewed manually; no data is uploaded automatically.
