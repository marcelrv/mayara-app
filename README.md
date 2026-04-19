# Mayara — Marine Radar Display for Android

Mayara is a native Android application that displays marine radar data from
supported radar hardware. It connects to a **mayara-server** backend (running
either as an embedded library or on a remote host) and renders live spoke data
through OpenGL ES.

## Supported Radar Brands

| Brand | Models |
|-------|--------|
| **Navico** | Broadband (BR24, 3G, 4G), Halo (20+, 24) |
| **Garmin** | xHD, xHD2, Fantom |
| **Raymarine** | Quantum (Wi-Fi) |
| **Furuno** | DRS series |

## Features

- **Real-time radar rendering** — OpenGL ES polar sweep display at full
  spoke rate (typically 2 048 spokes / revolution).
- **Dual connection mode** — Run the server embedded on the phone (via JNI)
  or connect to a remote server on the local network.
- **Range control** — Step through nautical, metric, or statute ranges
  using the +/− buttons. The available range list comes from the radar itself.
- **Gain / Sea / Rain / Interference** — All controls exposed through a
  swipe-up bottom sheet with sliders and auto toggles.
- **Color palettes** — Green, Yellow, Red / Green / Blue, and more.
- **Compass rose & range rings** — Overlay showing heading and distance.
- **Portrait & landscape** — Layout automatically adapts to device orientation.
- **Multi-radar** — When the server exposes more than one radar, tap the
  radar name pill to switch.

## Building

### Prerequisites

- Android Studio Hedgehog (2023.1) or later
- JDK 17
- Android SDK 34 (compile), API 26+ (min)
- Rust toolchain (for building the embedded server `.so`)

### Build the .so (embedded mode)

```bash
rustup target add aarch64-linux-android
cargo install cargo-ndk
# Set ANDROID_NDK_HOME to NDK r26b
bash scripts/build_jni.sh
```

### Build the APK

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Run tests

```bash
./gradlew test                    # JVM unit tests
./gradlew connectedAndroidTest    # Compose + integration tests (emulator/device)
```

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                 Jetpack Compose UI                    │
│  RadarScreen ─ HudOverlay ─ RangeControls ─ Sheet    │
├──────────────────────────────────────────────────────┤
│                RadarViewModel                        │
├──────────────────────────────────────────────────────┤
│  RadarRepository (single source of truth)            │
│  ├─ ApiClient (REST)                                 │
│  ├─ SpokeClient (WS binary protobuf)                │
│  └─ SignalKStreamClient (WS JSON delta)              │
├──────────────────────────────────────────────────────┤
│  RadarGLRenderer (OpenGL ES 2.0)                     │
├──────────────────────────────────────────────────────┤
│  mayara-jni → libradar.so (Rust, axum on 127.0.0.1) │
└──────────────────────────────────────────────────────┘
```

## License

This application is licensed under the **GNU General Public License v2.0**
(GPL-2.0). See [LICENSE](LICENSE) for details.

The embedded **mayara-server** library is also GPL-2.0 licensed.
