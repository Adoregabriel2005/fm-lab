# Contributing to FM Lab

Help us bring hardware FM radio analysis to more phones, stock and custom ROMs, and Android versions. **You do not need to write code to help.** A reproducible device report or a carefully documented test is valuable.

The only physically verified tuner target is currently the Samsung Galaxy S4 Mini GT-I9195 on LineageOS 14.1 / Android 7.1.2. Version 0.8 declares Android 5.0 / API 21 as its installation minimum, but that does not mean its tuner works on every Android 5+ phone. Other native radio implementations need investigation, adapters, and physical testing.

Read the [device support matrix and technical roadmap](docs/DEVICE_SUPPORT.md) before claiming support for a new target.

## 1. Report a phone with working native FM

[Open a Device support issue](https://github.com/Adoregabriel2005/fm-lab/issues/new?template=device-support.md). Please include:

- Exact model/model number, Android version, ROM name, and build version.
- Whether this is stock firmware or a custom ROM, and whether the device has root or system modifications.
- Native FM radio app package and version, if known.
- Whether that app actually receives over-the-air FM, without internet streaming, and whether wired headphones are required.
- FM Lab version, installation/startup result, exact error, and steps to reproduce.
- Which features work: tuning, sound, mute, RDS, stereo status, reception metrics, direct audio analysis, or scanning. Mark untested features as untested.

If FM Lab opens, use **Dados → Compatibilidade / rádios do aparelho** in the panel settings, then **Copiar relatório**. Review the copied report and paste it into the issue. The interface currently uses Brazilian Portuguese. A detected package is a lead, not confirmation that its tuner can be controlled.

If FM Lab cannot install or start, submit the issue anyway with your Android version and the exact error. You do not need to root, reset, uninstall the native radio, or modify the firmware to contribute a report.

## 2. Test a proposed device adapter or Android version

Use a build identified by its version or commit, and report the exact device/ROM used. If possible, compare with the native app on the same station and antenna position.

| Area | What to record |
| --- | --- |
| Installation / startup | Android version, build/commit, success or exact failure |
| Native app coexistence | Native app still works; behavior when switching between apps |
| Tuning / state | Requested frequency, accepted/rejected commands, actual-frequency event if available |
| Audio / mute | Sound route, mute/unmute, headphone attachment and removal |
| Metadata | RDS fields and stereo/mono events received; unknown values remain unknown |
| RF metrics | Raw readings, missing values, any documented units/encoding |
| Scan | Completion, cancellation, restored tuning/audio, rejected band endpoints |
| Lifecycle | Background/resume, screen off, reconnection, permission denial |
| Audio analysis | Direct FM capture works or exact failure; microphone recordings do not validate this feature |

For Android 5.0/5.1, startup and install-time audio permission behavior are priorities. For Android 6+, include runtime permission behavior. Newer versions also need installation, package visibility, service access, lifecycle, and audio restriction testing. Android below 5.0 is a separate porting task; lowering the manifest minimum alone is insufficient.

Do not describe a whole chipset family or Android release as supported based on one tested model. Report the exact combination and individual capabilities.

## 3. Help investigate a native radio implementation

Useful evidence includes legally available source, official documentation, exported service metadata, public interfaces, dependency names, and small sanitized error excerpts.

For an adapter, we need a verified accessible service/API, protocol and callback signatures, initialization behavior, permission requirements, band limits, metric encoding, and audio capture path. A private or signature-protected service may not be usable by a normal APK, even while the native app works.

Do not assume the CAF operation numbers, signed SINR format, or audio source `1998` apply to another vendor. Do not probe unknown Binder operation numbers or remove version checks simply to make detection pass. Opening the native app and controlling its receiver are separate capabilities.

If you have a Moto g06/g06 Power with a working factory radio, a report identifying its native package/version and service metadata would be particularly helpful. We have not established that the two models share a compatible radio app or protocol.

## 4. Contribute code or documentation

For a new adapter, start with a Device support issue describing the target and evidence. For a focused bug fix or documentation improvement, a pull request can be opened directly.

1. Fork the repository and create a focused branch.
2. Read the build instructions in [README.md](README.md) and the [architecture roadmap](docs/DEVICE_SUPPORT.md).
3. Keep vendor transport details separate from the interface and scanning logic. The current abstract bridge is transitional; typed capability and event interfaces are still needed.
4. Represent unsupported features explicitly. Do not invent metric conversions, infer transmitter power/distance from raw RSSI, or silently substitute microphone audio for FM capture.
5. Add meaningful regression tests for changed protocol selection, data handling, or compatibility behavior. Run the existing tests and explain any physical testing still required.
6. Include the exact target, before/after behavior, validation, and limitations in the pull request. Update the support matrix only for capabilities supported by evidence.

Windows build and tests:

```powershell
.\build.ps1 -Sdk 'C:\Android\Sdk' -Java 'C:\Program Files\Android\Android Studio\jbr'
.\test.ps1 -Sdk 'C:\Android\Sdk' -Java 'C:\Program Files\Android\Android Studio\jbr' -SkipBuild
```

A fresh clone generates its own local test key. Its APK cannot update an installation signed with the distributed key. Prefer a spare test device or emulator for UI-only work; an emulator does not validate FM hardware. Preserve any local measurements before changing installations.

Contributions to accessibility, English UI/localization, low-memory performance, documentation, and clearer error handling are also welcome. None of those requires owning every supported phone.

## Privacy and files

Review everything before posting publicly. Do not upload device serials, IMEIs, account details, locations, private signing keys, or complete personal logs. Use short relevant excerpts.

Do not attach proprietary APKs, firmware images, or decompiled vendor source to issues or pull requests. Describe package/version/dependencies and link to lawful public references instead. Compatibility reports are copied locally on request; FM Lab does not automatically upload them.

## Project licensing status

No redistribution license has been selected yet. Please review this before submitting code; do not import code without the rights to contribute it. Adding a project license is still an open maintainer decision, and public repository access does not itself grant firmware or third-party code rights.
