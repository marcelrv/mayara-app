# Bugfix Specification: Mayara Android App

**Date:** 2026-04-18  
**Status:** Awaiting implementation  
**Related:** `todo.md` items BF-01 through BF-12

Each section follows the same structure:
- **Root Cause** — what is actually wrong in the code today
- **Fix Specification** — the precise change required
- **Testing** — how the agent will verify the fix

---

## Work Phasing

Given the scope and cross-cutting nature of the bugs, work is split into two phases.

### Phase A — Kotlin-only fixes (single agent session)

| ID | Title |
|----|-------|
| BF-03 | Range fraction formatting |
| BF-06 | System bar inset padding |
| BF-07 | Power state defaults to OFF |
| BF-04 | Connection-type indicator |
| BF-05 | Radar name + multi-radar picker |
| BF-12 | Wire mDNS scanner |

### Phase B — Rendering and JNI (split into two sessions)

**Session B1 — OpenGL ES** (must be done in this order — BF-09 before BF-02):

| ID | Title |
|----|-------|
| BF-09 | Spoke image inverted and rotated |
| BF-08 | Viewport not updated on orientation change |
| BF-02 | No range rings |
| BF-11 | Stale spokes never cleared |

**Session B2 — Data layer + Rust** (requires Rust/NDK toolchain):

| ID | Title |
|----|-------|
| BF-01 | Range not updating from server stream |
| BF-10 | Embedded server logs incomplete |

> **Dependency note:** BF-09 must be fixed before BF-02. With the current wrong `v`/`u`
> coordinates the range rings would appear at the wrong radii, making visual verification
> impossible.

---

## BF-01: Range display does not update from server stream

### Root Cause

`RadarRepository.applyControlUpdate` matches the server-reported range to an index in
`capabilities.ranges` using strict integer equality:

```kotlin
val idx = state.capabilities.ranges.indexOfFirst { it == update.value.toInt() }
```

Two failure modes:

1. **Float truncation**: The server sends the range as a JSON float (e.g. `3703.704`).
   `update.value.toInt()` truncates to `3703`, which never matches the stored integer
   `3704`. `indexOfFirst` returns `-1` and the update is silently discarded.

2. **Spoke data ignored**: Every spoke in the protobuf contains a `rangeMetres` field
   reflecting the hardware's actual live range. `RadarRepository` never reads this field —
   range changes that are signalled only via the spoke stream (not the SignalK delta)
   are therefore invisible to the UI.

### Fix Specification

1. Replace strict integer equality with a nearest-value lookup that tolerates ±2 %:

   ```kotlin
   "range" -> {
       val received = update.value.toInt()
       val idx = state.capabilities.ranges.indexOfFirst {
           Math.abs(it - received) <= maxOf(1, (it * 0.02).toInt())
       }
       if (idx >= 0) _uiState.value = state.copy(currentRangeIndex = idx)
   }
   ```

2. In `launchSpokeStream`, after emitting each spoke to `_spokeFlow`, check whether
   `spokeData.rangeMetres > 0` and the corresponding index differs from
   `currentRangeIndex`. If so, perform the same nearest-value lookup and update the
   state. Throttle to at most once per full revolution (track `lastRangeUpdateSpoke:
   Int` to avoid O(n) state emissions per second).

### Testing

- **Unit test** (`RadarRepositoryTest`): Mock a SignalK stream emitting a range control
  update where `value = 3703.7`. Assert `currentRangeIndex` matches the index of `3704`
  in `capabilities.ranges`.
- **Unit test**: Existing exact-integer cases must still pass unchanged.
- **Unit test**: Mock `SpokeData` with `rangeMetres = 6000` matching
  `capabilities.ranges[2]`. Assert that after spoke emission `currentRangeIndex == 2`.

---

## BF-02: No range rings drawn on radar display

### Root Cause

`RadarGLRenderer`'s fragment shader renders only the polar sweep texture; there is no code
to draw concentric circles. The spec (§3.3) requires range rings as standard marine radar
UI furniture — without them the operator cannot estimate target range visually.

### Fix Specification

Add ring rendering to the GLSL fragment shader. Rings are drawn at fixed fractions of the
outer radius, using a configurable width. Add after the polar coordinate calculation and
before the texture sample:

```glsl
// Range rings at 25 %, 50 %, 75 % of maximum range.
const float RING_WIDTH = 0.003;
const float R1 = 0.125;  // 25 %
const float R2 = 0.250;  // 50 %
const float R3 = 0.375;  // 75 %
vec4 RING_COLOR = vec4(0.25, 0.25, 0.25, 0.7);

if (abs(dist - R1) < RING_WIDTH ||
    abs(dist - R2) < RING_WIDTH ||
    abs(dist - R3) < RING_WIDTH) {
    gl_FragColor = RING_COLOR;
    return;
}
```

Additional requirements:

- `RING_COLOR` should be adjusted per active palette so it remains visible (e.g. dark
  green tint `vec4(0.0, 0.2, 0.0, 0.7)` for the GREEN palette, dark amber for YELLOW).
- `RING_WIDTH` should scale with `u_Resolution` so rings remain approximately 1–2 px
  wide on both mdpi and xxxhdpi devices.
- Range ring labels (e.g. "0.5 NM" text at the 50 % ring) are **out of scope** for this
  fix — they require a separate Canvas/Compose overlay (Backlog).

### Testing

- Rings cannot be unit-tested via pure JVM tests (no GL context). Use:
- **Paparazzi screenshot test**: Render a frame with a partially filled texture and assert
  pixels at the expected radii match the ring colour, while pixels between rings do not.
- **Manual on-device**: Confirm three evenly spaced concentric rings are visible on the
  radar display in all four palette modes.

---

## BF-03: Range label uses incorrect format for short ranges

### Root Cause

`RangeControls.kt` formats the range with:

```kotlin
"%.1f NM".format(metres / 1852.0)
```

This always outputs one decimal place (e.g. `"0.1 NM"` for 185 m). Marine radar
convention uses fraction notation for sub-1 NM ranges (e.g. `"1/8 NM"`, `"1/4 NM"`).
The `DistanceUnit` enum (`NM`, `KM`, `SM`) is defined in `RadarModels.kt` but is
completely unused — the unit preference set in `UnitsScreen` has no effect on the
range display.

### Fix Specification

1. Create a new file `app/.../ui/radar/overlay/RangeFormatter.kt` containing a pure
   `object RangeFormatter` with:

   ```kotlin
   fun format(metres: Int, unit: DistanceUnit): String
   ```

   Logic for `NM`:
   - Compute `nm = metres / 1852.0`
   - Match against the common Navico/Furuno range fractions with ±5 % tolerance:
     `1/8` (232 m), `1/4` (463 m), `3/8` (694 m), `1/2` (926 m), `3/4` (1389 m)
   - For `nm < 1.0` that doesn't match a fraction: `"%.2f NM".format(nm)`
   - For `1.0 ≤ nm < 10.0`: `"%.1f NM".format(nm)`
   - For `nm ≥ 10.0`: `"%.0f NM".format(nm)`

   Logic for `KM`: convert metres → km; `"%.1f KM"` if < 10, `"%.0f KM"` otherwise.
   Logic for `SM`: convert metres → statute miles; same rounding rules as NM.

2. `RangeControls` obtains the `DistanceUnit` via its existing `Connected.controls`
   state (a `distanceUnit: DistanceUnit` field must be added to `ControlsState` and
   persisted/read from the DataStore key already used in `UnitsScreen`).

3. Replace the inline `formatRange()` call in `RangeControls.kt` with
   `RangeFormatter.format(ranges[currentIndex], distanceUnit)`.

### Testing

- **Unit test** (`RangeFormatterTest`) with parameterised cases:
  - `185 m, NM` → `"1/8 NM"`
  - `370 m, NM` → `"1/4 NM"`
  - `926 m, NM` → `"1/2 NM"`
  - `1852 m, NM` → `"1 NM"`
  - `2778 m, NM` → `"1.5 NM"`
  - `18520 m, NM` → `"10 NM"`
  - `1852 m, KM` → `"1.9 KM"`
  - `1852 m, SM` → `"1.2 SM"`
- **Compose test** (`RangeFormatTest`): Render `RangeControls` with `DistanceUnit.KM`;
  assert the displayed text contains `"KM"`.

---

## BF-04: No connection-type indicator on main screen

### Root Cause

`RadarScreen` provides no visual indication of whether the app is connected via the
embedded JNI server or a remote network server, nor does it display the server address.
The `Connected` state carries `radar.id` and `radar.name` but not the originating URL or
connection mode. `RadarViewModel` knows the mode (it started the JNI server or a network
connection) but does not expose this information to the UI.

### Fix Specification

1. Add `connectionLabel: String` to `RadarUiState.Connected`, populated by
   `RadarViewModel.onConnect()`:
   - Embedded: `"Embedded (127.0.0.1:6502)"`
   - Network: `"Network (${host}:${port})"`
   - PcapDemo: `"Demo (pcap replay)"`

2. In `RadarScreen`, display `connectionLabel` as a small semi-transparent `Text`
   composable below the HUD overlay:
   - Typography: `MaterialTheme.typography.labelSmall`
   - Color: `MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)`
   - Visible only in `Connected` state.

3. In `RadarUiState.Error`, include the attempted URL in the error string (already
   partially done in some cases — make this consistent).

### Testing

- **Compose unit test** (`ConnectionLabelDisplayTest`): Render `RadarScreen` with a
  `Connected` state whose `connectionLabel = "Embedded (127.0.0.1:6502)"`. Assert the
  text is present in the Compose tree.
- **Compose unit test**: Render with Network label; assert correct text is shown.
- **Compose unit test**: Render `Loading` state; assert no connection label is visible.

---

## BF-05: Radar name not shown; no multi-radar switching

### Root Cause

`RadarUiState.Connected.radar.name` is available but `RadarScreen` never renders it.
`RadarRepository.connect()` unconditionally selects `radars.first()` — there is no
mechanism to enumerate multiple radars or switch between them.

### Fix Specification

1. **Display radar name**: In `RadarScreen`, add `Text(connected.radar.name)` to the
   HUD column (below connection label). Style: `MaterialTheme.typography.labelMedium`,
   `primary` color.

2. **Repository change**: Add `val availableRadars: StateFlow<List<RadarInfo>>` to
   `RadarRepository`, updated after the initial `getRadars()` call. Keep the
   `radars.first()` auto-select behaviour; expose the full list for the UI.

3. **ViewModel change**: Expose `val availableRadars: StateFlow<List<RadarInfo>>` from
   `RadarViewModel` (delegated to repository). Add `fun onSwitchRadar(radarId: String)`
   which calls `repository.connect(baseUrl)` after updating `currentRadarId`.

4. **RadarPickerDialog** (new composable): A simple `AlertDialog` with a `LazyColumn`
   of `RadarInfo.name` entries. Visible when `showRadarPicker: StateFlow<Boolean>` is
   `true`. Selecting an entry calls `viewModel.onSwitchRadar(id)` and dismisses.

5. **Tap to switch**: Wrap the radar name `Text` in `Modifier.clickable { ... }` that
   calls `viewModel.onRadarNameTapped()`. The ViewModel sets `showRadarPicker = true`
   only when `availableRadars.size > 1`; with a single radar the tap is a no-op.

### Testing

- **Unit test** (`RadarRepositoryTest`): Mock a server returning 3 radars. Assert
  `availableRadars.value.size == 3` after `connect()`.
- **Compose test**: Render `RadarScreen` with `radar.name = "Navico 4G"`. Assert that
  text is visible.
- **Compose test**: `availableRadars.size == 2`; tap the radar name text; assert
  `RadarPickerDialog` is shown.
- **Compose test**: `availableRadars.size == 1`; tap the radar name text; assert the
  picker dialog is NOT shown.

---

## BF-06: UI controls partially hidden behind Android system bars

### Root Cause

`MainActivity.onCreate()` calls `enableEdgeToEdge()`, which makes the app draw behind
the status bar and navigation bar. However, `RadarScreen` places every overlay composable
inside `Box(Modifier.fillMaxSize())` using only `Alignment` anchors, with no
`WindowInsets` padding. As a result:

- The **Settings gear** and **HUD** (top-left) are partially under the status bar.
- The **Power toggle** (top-right) is partially under the status bar.
- The **Range FABs** and **Control Sheet FAB** (bottom) are partially under the
  navigation bar.

The OpenGL canvas layer (`RadarGLView`) must remain full-screen — padding it would shrink
the radar display area.

### Fix Specification

Apply `Modifier.windowInsetsPadding(...)` to each overlay individually; do **not** apply
insets to the `RadarGLView` or the root `Box`.

```kotlin
// Settings gear + HUD column — inset from status bar
Column(
    modifier = Modifier
        .align(Alignment.TopStart)
        .windowInsetsPadding(WindowInsets.statusBars)
) { ... }

// Power toggle — inset from status bar
PowerToggle(
    modifier = Modifier
        .align(Alignment.TopEnd)
        .windowInsetsPadding(WindowInsets.statusBars)
)

// Range controls — inset from navigation bar
RangeControls(
    modifier = Modifier
        .align(Alignment.BottomEnd)
        .windowInsetsPadding(WindowInsets.navigationBars)
)

// Control sheet FAB — inset from navigation bar
FloatingActionButton(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(bottom = 16.dp)
)
```

Use `WindowInsets.statusBars` and `WindowInsets.navigationBars` from
`androidx.compose.foundation.layout`.

### Testing

- **Compose/Robolectric test** (`SystemBarInsetTest`): Use `setWindowInsets()` to inject
  a mock inset of 48 dp status bar and 56 dp navigation bar. Assert that the
  `PowerToggle` composable's top position is ≥ 48 dp from the screen top. Assert that
  `RangeControls` bottom position is ≥ 56 dp from the screen bottom.
- **Manual test** on a device with gesture navigation and separately with 3-button
  navigation — confirm all controls are fully visible and tappable.

---

## BF-07: Power state shows `OFF` on initial connect when radar is transmitting

### Root Cause

In `RadarRepository.connect()`:

```kotlin
val powerValue = controlValues["power"]?.value?.toInt() ?: 0
```

If the `"power"` control is absent from the controls response (or `value` is not a valid
integer), the expression defaults to `0` = `PowerState.OFF`. For an already-transmitting
radar this causes the UI to display `OFF` immediately after connecting.

`powerValueToState()` also maps any value outside `{0, 1, 2}` to `STANDBY`:

```kotlin
else -> PowerState.STANDBY
```

If a radar model uses a value of `3` or higher for `TRANSMIT`, the state would silently
become `STANDBY` with no diagnostic output.

### Fix Specification

1. **Safer default**: Change the Elvis default from `0` (OFF) to `1` (STANDBY):
   ```kotlin
   val powerValue = controlValues["power"]?.value?.toIntOrNull() ?: 1
   ```
   `toIntOrNull()` safely handles non-numeric strings without throwing.

2. **Log unknown values**: In `powerValueToState()`, add a warning log when the value
   falls through the known cases:
   ```kotlin
   else -> {
       android.util.Log.w("RadarRepository",
           "Unknown power value $value, defaulting to STANDBY")
       PowerState.STANDBY
   }
   ```

3. **Capabilities-driven mapping (future)**: This spec does not require extracting a
   full power-state map from capabilities. A `TODO` comment should be added noting that
   future iterations should derive the mapping from the capabilities schema if radar
   vendors use non-standard numeric codes.

### Testing

- **Unit test** (`RadarRepositoryTest`): `connect()` with no `"power"` key in
  `controlValues`. Assert `powerState == PowerState.STANDBY`.
- **Unit test**: `controlValues["power"].value = "2"`. Assert `powerState == TRANSMIT`.
- **Unit test**: `controlValues["power"].value = "xyz"` (unparseable). Assert
  `powerState == STANDBY`.
- **Unit test**: `applyControlUpdate` with `controlId = "power"`, `value = 99f`. Assert
  state becomes `STANDBY` (not a crash) and a log warning is emitted.

---

## BF-08: Radar circle distorted or over-sized after orientation change

### Root Cause

The fragment shader uses a `u_Resolution` uniform for aspect-ratio correction:

```glsl
float aspect = u_Resolution.x / u_Resolution.y;
vec2 radarPos = vec2(uv.x * aspect, uv.y) - u_Center;
```

Inspection of `RadarGLRenderer.onSurfaceChanged()` confirms that `viewportWidth` and
`viewportHeight` are updated, but `u_Resolution` is only uploaded once inside
`onSurfaceCreated()`. After a rotation (e.g. portrait 1080×2400 → landscape 2400×1080)
the shader continues to use the stale resolution, computing an inverted aspect ratio.
The radar circle is rendered as a horizontally compressed oval in landscape mode.

### Fix Specification

Add a `resolutionDirty: AtomicBoolean` flag (initialised to `false`).

In `onSurfaceChanged`:
```kotlin
override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    viewportWidth = width
    viewportHeight = height
    resolutionDirty.set(true)
}
```

In `onDrawFrame`, after binding the program and before drawing:
```kotlin
if (resolutionDirty.getAndSet(false)) {
    GLES20.glUniform2f(
        resolutionUniform,
        viewportWidth.toFloat(),
        viewportHeight.toFloat()
    )
}
```

`onSurfaceCreated` should also set `resolutionDirty = true` so the first draw frame
always uploads the correct dimensions.

### Testing

- **Instrumented test**: Start app in portrait on API 35 emulator. Rotate to landscape
  via `ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE`. Capture a screenshot. Assert that
  the rendered radar shape is circular (aspect ratio of the bounding box ≈ 1.0 ± 0.05).
- **Paparazzi screenshot tests**: Add portrait and landscape device configurations;
  assert the radar circle is circular in both.

---

## BF-09: Radar spoke image is inverted and rotated 180°

This is the most visually critical bug. Two independent shader errors combine to make
the radar image completely unrecognisable.

### Root Cause

**Error A — v-coordinate inversion (range direction reversed)**

The fragment shader contains:

```glsl
// v = 0 at outer edge, 1 at center.  Row 0 in texture = center, so we flip.
float v = 1.0 - (dist / 0.5);
```

The developer comment is incorrect. In OpenGL ES, `glTexImage2D` maps the first byte of
the `ByteBuffer` to texture coordinate `(s=0, t=0)` = bottom-left of the texture.
`radarBuf` is laid out as `[col + row * TEXTURE_SIZE]`, so row 0 (near/center range)
occupies the bottom of the texture (`t=0`). When `dist=0` (screen centre), the shader
computes `v = 1.0`, sampling `t=1.0` = the top of the texture = row 511 = **maximum
range data**. The screen centre therefore shows far-away echoes, and the screen edge
shows close-range echoes — completely inverted.

**Error B — u-coordinate 180° offset (bearing direction rotated)**

```glsl
float u = fract(angle / (2.0 * PI) + 0.5);
```

Marine radar convention: spoke angle 0 = North. In the texture, `computeColumn(0, N)`
maps to column 0 (`u=0`). When the screen points North, `atan(radarPos.x, radarPos.y)
= 0`, and the shader computes `u = fract(0 + 0.5) = 0.5`. But `u=0.5` corresponds to
column 256 = **South spokes**. North echoes appear at the South of the screen, East
echoes at West — a 180° rotation.

### Fix Specification

In `RadarGLRenderer.FRAGMENT_SHADER`, change two lines:

```glsl
// BEFORE (wrong):
float v = 1.0 - (dist / 0.5);
float u = fract(angle / (2.0 * PI) + 0.5);

// AFTER (correct):
float v = dist / 0.5;
float u = fract(angle / (2.0 * PI));
```

Update the comment for `v` to:
```glsl
// v = 0 at screen centre (near range), 1 at screen edge (far range).
// Row 0 in texture = near range = GL t=0 (bottom of texture).
float v = dist / 0.5;
```

No changes to Kotlin code, uniforms, or texture upload logic are required.

### Testing

- **Unit test** (`RadarGLRendererTest`): Verify that `buildPaletteLut(GREEN)` returns
  `(R=0, G=255, B=0, A=255)` at index 255 — confirming the palette is not a contributing
  factor.
- **Instrumented / visual test**: Write a single full-intensity spoke at angle 0 (North)
  into the texture buffer. Render one frame. Capture screenshot. Assert that the bright
  stripe is in the top-centre of the circle, not the bottom.
- **Instrumented / visual test**: Write spokes only for angles 0–(spokesPerRev * 0.1)
  (first 10 % = roughly North sector). Assert the bright region is at the top of the
  circle.
- **Paparazzi screenshot**: Generate a known spoke pattern and compare against a
  reference image (created after the fix is applied and visually verified).

---

## BF-10: Embedded server log screen shows only JNI lifecycle messages

### Root Cause

`EmbeddedServerLogsScreen` polls `RadarJni.getLogs()` every 2 seconds, which calls the
JNI `nativeGetLogs()` function returning the contents of `LOG_BUFFER` in
`mayara-jni/src/lib.rs`. The `append_log()` function that writes to `LOG_BUFFER` is
called **only** at explicit JNI call sites:

- `nativeStart` — "mayara-server starting on port …"
- `nativeStop` — "mayara-server stopped"
- Error paths in those functions

All internal mayara-server log output (`log::info!`, `log::warn!`, `log::error!`
throughout the Rust library) is handled by `android_logger`, which routes to Android
`logcat` only. The in-app log buffer therefore never receives spoke activity, hardware
detection messages, connection events, or errors from within the server library.

### Fix Specification

Replace `android_logger::init_once(...)` in `init_logging()` with a custom composite
`log::Log` implementation that writes to **both** logcat and `LOG_BUFFER`.

```rust
struct CompositeLogger {
    logcat: android_logger::AndroidLogger,
}

impl log::Log for CompositeLogger {
    fn enabled(&self, metadata: &log::Metadata) -> bool {
        metadata.level() <= log::LevelFilter::Debug
    }

    fn log(&self, record: &log::Record) {
        // Route to logcat as before
        self.logcat.log(record);
        // Also route to in-app ring buffer (skip TRACE to avoid noise)
        if record.level() <= log::Level::Debug {
            append_log(format!(
                "[{}] {}: {}",
                record.level(),
                record.target(),
                record.args()
            ));
        }
    }

    fn flush(&self) {}
}
```

The `init_logging()` function becomes:

```rust
fn init_logging() {
    let logcat = android_logger::AndroidLogger::new(
        Config::default()
            .with_max_level(LevelFilter::Debug)
            .with_tag("mayara-radar"),
    );
    let composite = CompositeLogger { logcat };
    // log::set_boxed_logger can only succeed once; ignore the error on repeat calls.
    let _ = log::set_boxed_logger(Box::new(composite))
        .map(|()| log::set_max_level(LevelFilter::Debug));
}
```

**Ring buffer overflow behaviour** is already handled (`drain(0..400)` when `buf.len()
>= 2000`). No change needed there.

**Important:** The `android_logger::AndroidLogger` type must be made available without
calling `init_once`. Check `android_logger` API version — if `AndroidLogger::new()` is
not public, use a `mpsc::channel`-based bridge instead (send log records from the `Log`
impl; a background task drains them into `LOG_BUFFER` and calls `logcat`).

### Testing

- **Rust unit test** in `mayara-jni/src/lib.rs`:
  ```rust
  #[test]
  fn test_composite_logger_routes_to_buffer() {
      init_logging();
      log::info!(target: "test", "hello from test");
      let buf = log_buf().lock().unwrap();
      assert!(buf.iter().any(|l| l.contains("hello from test")));
  }
  ```
- **JVM unit test** (`SettingsViewModelTest`): Mock `RadarJni.getLogs()` to return a
  multi-line string including `"[INFO] mayara: spoke stream active"`. Assert
  `settingsViewModel.logsState.value` exposes all lines.
- **Manual**: Start embedded mode; open Settings → Embedded Server Logs; verify that
  radar detection activity and API request logs appear within 2 seconds.

---

## BF-11: Stale spokes persist indefinitely when transmission stops

### Root Cause

`RadarGLRenderer.writeColumn()` writes spoke bytes into `radarBuf` but there is no
mechanism to:
- Detect that a column has not been refreshed for more than one full rotation period.
- Clear the entire texture when the radar transitions to `STANDBY` or `OFF`.

After the radar stops transmitting, the last complete sweep remains frozen on screen.
From the user's perspective, the app looks stuck or the radar appears to be running when
it is not.

### Fix Specification

**Phase A (MVP — state-driven clear):**

Add a `fun clearAll()` method to `RadarGLRenderer`:
```kotlin
fun clearAll() {
    synchronized(textureLock) {
        radarBuf.fill(0)
    }
    textureDirty.set(true)
}
```

Call `clearAll()` from the GL view whenever `RadarUiState` transitions to `OFF`,
`STANDBY`, `Loading`, or `Error`. Wire this in `RadarGLView` via a `LaunchedEffect`
observing `powerState`.

**Phase B (robust — per-column timeout):**

1. Add `val columnTimestamps = LongArray(TEXTURE_SIZE) { 0L }` to `RadarGLRenderer`.
2. In `writeColumn(col, data)`, after writing: `columnTimestamps[col] = SystemClock.elapsedRealtimeNanos()`.
3. Add `private fun clearStaleColumns(timeoutMs: Long)` called from `onDrawFrame`:
   - `threshold = SystemClock.elapsedRealtimeNanos() - timeoutMs * 1_000_000L`
   - For each column `c` where `columnTimestamps[c] in 1..<threshold`:
     zero-fill `radarBuf[c, c+TEXTURE_SIZE, c+2*TEXTURE_SIZE, ...]` and reset
     `columnTimestamps[c] = 0L`. Set `textureDirty`.
4. Default timeout: `(spokesPerRevolution.toFloat() / estimatedSpokeRateHz * 2 * 1000).toLong()` ms, minimum 3000 ms. Expose as a settable property for testing.

Phase A is sufficient for the MVP and should be implemented first.

### Testing

**Phase A:**
- **Unit test** (`RadarTextureBufferTest`): Write to several columns. Call `clearAll()`.
  Assert `radarBuf` is entirely zeros and `textureDirty` is `true`.
- **Compose test**: Emit `PowerState.OFF` from the ViewModel; assert `RadarGLRenderer.clearAll()` is called via a mock or spy.

**Phase B:**
- **Unit test**: Write to column 100 at time `T`. Call `clearStaleColumns(3000)` at
  time `T + 4000 ms`. Assert column 100 is all zeros.
- **Unit test**: Call at time `T + 2000 ms`. Assert column 100 is unchanged.

---

## BF-12: mDNS network scanner not wired to connection picker

### Root Cause

`MdnsScanner.kt` is fully implemented: it discovers `_signalk-ws._tcp` and `_mayara._tcp`
services and exposes `StateFlow<List<DiscoveredServer>>`. However:

- `RadarViewModel` never instantiates or starts a `MdnsScanner`.
- `RadarScreen` passes `discoveredServers = emptyList()` to `ConnectionPickerDialog`
  with a `// Phase 5: wire MdnsScanner` TODO comment.
- There is no scan lifecycle management (start when dialog opens, stop when dismissed).

### Fix Specification

1. **Inject `MdnsScanner` into `RadarViewModel`**: Add it as a constructor parameter
   with a default factory. The constructor already uses injectable lambdas for
   `NsdManager` calls, so no changes to `MdnsScanner` itself are needed.

2. **Lifecycle wiring in ViewModel**:
   ```kotlin
   fun onShowConnectionPicker() {
       _showConnectionPicker.value = true
       mdnsScanner.startScanning()  // start on dialog open
   }

   fun onDismissConnectionPicker() {
       _showConnectionPicker.value = false
       mdnsScanner.stopScanning()   // stop on dialog close
   }
   ```

3. **Expose discovered servers from ViewModel**:
   ```kotlin
   val discoveredServers: StateFlow<List<DiscoveredServer>> = mdnsScanner.discovered
   ```

4. **Wire to dialog in `RadarScreen`**:
   ```kotlin
   val discoveredServers by viewModel.discoveredServers.collectAsState()
   // ...
   ConnectionPickerDialog(
       discoveredServers = discoveredServers,  // was emptyList()
       ...
   )
   ```

5. **Scan status indicator**: In `ConnectionPickerDialog`, when the NETWORK option is
   selected and `discoveredServers.isEmpty()`, show a `CircularProgressIndicator` with
   the label "Scanning for servers…". When servers appear, replace the spinner with
   the server list.

6. **Remove the TODO comment** once wired.

### Testing

- **Unit test** (`RadarViewModelTest`): Call `onShowConnectionPicker()`. Assert
  `mdnsScanner.startScanning()` is invoked once.
- **Unit test**: Call `onDismissConnectionPicker()`. Assert `mdnsScanner.stopScanning()`
  is invoked.
- **Compose test** (`ConnectionPickerDialogTest`): Pass `discoveredServers = listOf(
  DiscoveredServer("Test", "192.168.1.100", 6502, false, "http://192.168.1.100:6502"))`.
  Assert the server name `"Test"` is displayed and selectable.
- **Compose test**: Pass `discoveredServers = emptyList()`. Assert the "Scanning…"
  spinner is visible when the NETWORK option is selected.
