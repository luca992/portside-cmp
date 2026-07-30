# Portside

A live **ferry tracker** for the Baltic Sea, built with **Compose Multiplatform** and targeting
**Android**, **iOS**, **desktop (JVM)**, and the **web (Kotlin/Wasm)** from a single codebase.
All data is mocked — a frozen snapshot of "Sun, Jul 19" with one overnight crossing under way.
The ports are real; every operator and vessel is fictional.

**Try it live:** [luca992.github.io/portside-cmp](https://luca992.github.io/portside-cmp/) — the
web (Kotlin/Wasm) build, served straight from this repo's [`docs/`](docs) folder via GitHub Pages
(`scripts/publish-web.sh` regenerates it).

## What's in the app

- **Night-sea globe backdrop** (every screen) — an orthographic earth drawn from real Natural
  Earth coastlines and NASA Blue Marble imagery, with the selected sailing's great-circle route
  arced over it in amber, port chips at the endpoints, and a vessel marker at the sailing's live
  progress. Drawn entirely with a common-code `Canvas` (GPU runtime shader where available).
- **My Sailings** — one flat run of compact rows: status badge, line badge, port-to-port times
  (green = on time, red = late, struck-through original schedule on delays), and a live
  "Docking in…" countdown for the crossing under way.
- **Sailing detail** — sheet with line + sailing number header, "Docking in 2h 31min" status
  banner, departure/arrival blocks with berth/terminal chips, speed-and-heading line for the
  live crossing, booking-code & cabin tiles, arrival forecast, and a "Where's My Ship?" card
  tracking the inbound vessel.
- **Friends** — friends' crossings with live statuses.
- **Logbook** — deep-teal "All-Time Sea Logbook" gradient card (sailings, nautical miles, time
  at sea, ports, lines) and an amber monthly share card.

## Architecture

Two-layer KMP with unidirectional data flow, all in common source sets. The
platform modules are 3-line entry stubs.

```
core/                    kmp/lib — Compose-free domain layer
  src/portside/
    model/               Domain types (Sailing, Port, Line, Friend, …) plus the
                         shared palette / presentation rules / UI strings that
                         keep every renderer pixel-parallel
    data/                SailingRepository interface + MockSailingRepository + dataset;
                         AppGraph is the (deliberately tiny) composition root
    vm/                  One ViewModel per screen exposing a single immutable
                         UiState via StateFlow; events are plain functions
  test/                  Mock-data invariants + ViewModel unit tests
shared/                  kmp/lib — Compose UI, depends on core
  src/portside/
    Nav.kt               Navigation 3 back-stack keys
    App.kt               Root: backdrop + sheet + NavDisplay + tab bar;
                         ViewModels created at nav-entry level
    ui/                  Screens (state down, events up — they never see a
                         ViewModel), theme, components
android-app/             android/app — MainActivity calling App()
ios-app/                 ios/app — ComposeUIViewController + SwiftUI host (Xcode project included)
ios-app-glass/           ios/app — native Liquid Glass chrome around the shared Compose flows
ios-app-swiftui/         ios/app — the same app rebuilt 100% in SwiftUI against core's
                         shared palette/presentation, for a side-by-side comparison
jvm-app/                 jvm/app — desktop window, handy for quick iteration
web-app/                 wasm-js/app — ComposeViewport entry + index.html
```

Rules of the shape: UI depends on `core`, never the reverse; `core` has no
Compose dependency; ViewModels depend on `SailingRepository` (the interface), so
a real backend replaces `MockSailingRepository` without touching presentation;
navigation is a Navigation 3 back stack (`NavDisplay` + `entryProvider`).

Cross-platform behavior parity: system back on Android pops the detail sheet via the new
`NavigationBackHandler` (the non-deprecated replacement for `BackHandler`), matching the X button;
safe-area insets handled with `statusBarsPadding`/`navigationBarsPadding`; no emoji glyphs (vector
icons only) so rendering is identical on all platforms.

Renderer parity: every color, metric, and UI string lives in `core`
(`PortsidePalette`, `PortsideMetrics`, `PortsideStrings`, `SailingPresentation`,
`PortsideMenus`), and both the Compose UI and the all-SwiftUI app read those
shared values — neither side re-derives anything, so they cannot drift apart.

## Toolchain

Set up and built entirely with the new **[Kotlin Toolchain](https://kotlin-toolchain.org)**
(the evolution of Amper announced at KotlinConf'26) — no Gradle project files. Modules
are declared in `module.yaml`; the `./kotlin` wrapper provisions everything (JRE, Android SDK,
Kotlin/Native, xcodebuild glue).

```sh
./kotlin build                                       # build all platforms
./kotlin test -p jvm                                 # run tests
./kotlin run --module jvm-app                        # desktop app
./kotlin run --module android-app                    # Android emulator/device
./kotlin run --module ios-app \
  --platform=iosSimulatorArm64 --device-id=<UDID>    # iOS simulator
scripts/serve-web.sh                                 # web (wasm) at localhost:8080
```

You can also open `ios-app/module.xcodeproj` in Xcode.

### Versions

| Thing | Version | Note |
| --- | --- | --- |
| Kotlin Toolchain (build tool) | 0.12.0-dev | bundles AGP 9.2.1 (Compose 1.12 Android needs AGP 9.1+) |
| Kotlin compiler | 2.4.0 | pinned via `settings.kotlin.version` (latest stable) |
| Compose Multiplatform | 1.12.0-beta02 | pinned via `settings.compose.version` |
| navigation3-ui | 1.1.1 | new predictive-back API |
| androidx.activity:activity-compose | 1.13.0 | latest |
| MapLibre Compose / MapLibre Native iOS | 0.13.0 / 6.25.1 | see "Maps" below |

Notes:
- Compose 1.11+ dropped the Intel iOS simulator (`iosX64`), so targets are `iosArm64` +
  `iosSimulatorArm64`.
- The generated Xcode project needed `IPHONEOS_DEPLOYMENT_TARGET = 16.0` — without it, Xcode 26
  defaults the minimum OS to the SDK version and finds no iOS 18.x simulator destinations.

## Maps: real MapLibre + a drawn globe

Two backdrops:

- **Sailing detail** (Android + iOS): a real
  [MapLibre Compose](https://maplibre.org/maplibre-compose/) vector map
  (`org.maplibre.compose:maplibre-compose:0.13.0`, OpenFreeMap `dark` style, no API key) with the
  great-circle route, endpoints, and vessel position as GeoJSON layers.
- **Tabs, desktop & web**: an orthographic **globe** drawn in commonMain — real Natural Earth
  coastlines projected onto a sphere with an aqua horizon glow, graticule, and the live route
  arcing over it. (MapLibre Compose's desktop backend is ~15% complete, hence the fallback;
  mobile MapLibre has no globe projection yet, hence the canvas globe for the hero screens.)

### How MapLibre works here without Gradle (the interesting part)

MapLibre Compose on iOS normally requires the Kotlin CocoaPods/SPM **Gradle** plugins to link the
native `MapLibre.framework` — which the Kotlin Toolchain doesn't support yet. This project wires
it manually:

1. `scripts/fetch-maplibre-ios.sh` downloads the official `MapLibre.dynamic.xcframework` (6.25.1)
   into `ios-app/Frameworks/` (gitignored) and rewrites the absolute `-F` linker paths in the
   module.yaml files to your checkout's location — run it once after cloning (and again if the
   checkout moves).
2. `shared/module.yaml` and `ios-app/module.yaml` pass `-linker-option -F<slice> -framework
   MapLibre` to the Kotlin/Native link via `settings@iosArm64` / `settings@iosSimulatorArm64`
   `freeCompilerArgs` (absolute paths required — relative paths resolve against varying link-task
   working directories).
3. `ios-app/module.xcodeproj` has a hand-added **Embed Frameworks** phase that copies and signs
   the xcframework into the app bundle.

On Android, MapLibre is just a Maven dependency (plus `INTERNET` permission).

### Running on Android from an IDE

Android Studio's "Android App" run configurations don't work here (no Gradle/AGP project model —
that's the "Cannot obtain the package" error). Use `./kotlin run --module android-app
--device-id=<serial>`, or IntelliJ IDEA's Amper/Kotlin Toolchain support.
