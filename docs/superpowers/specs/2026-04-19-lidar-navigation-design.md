# LIDAR — Navigation App Design Spec
**Date:** 2026-04-19
**Platform:** Android (Head Units)
**Language:** Kotlin
**Core SDKs:** Mapbox Maps SDK for Android, Mapbox Navigation SDK for Android

---

## 1. Vision

LIDAR is a car navigation app for Android head units built around one principle: **visual fidelity first**. The map is not a utility — it is the experience. Users should open the app even when they already know the route, because the map is alive, reactive, and stunning.

The aesthetic draws from LiDAR scanning, military tactical HUDs, and sci-fi command centers. Black canvas. Glowing white contour lines. 3D terrain. Wine red accents. A map that breathes with the music playing in the car.

---

## 2. Target Hardware

- **Platform:** Android head units
- **Orientation:** Landscape only — locked via manifest
- **Screen sizes:** 7"–10"
- **Resolutions:** 800×480 to 1280×720 (adaptive layouts)
- **System UI:** Hidden on launch via immersive sticky mode — nav buttons and status bar fully concealed, re-hide within 2 seconds if accidentally revealed

---

## 3. Architecture

### Pattern
Single Activity. No Fragments. One `MainActivity` hosts a fullscreen `MapView` with custom Android Views layered on top for all HUD elements.

### Structure
```
MainActivity
├── MapView                    (Mapbox, fullscreen, always present)
├── IdleOverlay                (custom View — minimal chrome, idle state only)
├── HudOverlay                 (custom View — tactical panels, routing state only)
├── TurnInstructionOverlay     (custom View — cinematic card, per-maneuver)
├── SearchOverlay              (custom View — slides up from bottom on tap)
└── MusicReactivityService     (bound Service — runs independently of UI state)
```

### State Machine
Two states. No intermediate states in v1.

| State | Trigger | UI Active |
|---|---|---|
| `IDLE` | App launch, route cancelled, arrival | `IdleOverlay` + music reactivity at full intensity |
| `ROUTING` | Destination selected, route calculated | `HudOverlay` + `TurnInstructionOverlay` + music reactivity at 70% intensity |

Transitions between states are animated — overlays fade and slide, never snap.

---

## 4. Map Style — "LIDAR Dark"

A custom Mapbox Studio style. Every layer is intentional.

### Palette
| Element | Color | Notes |
|---|---|---|
| Background | `#000000` | True black, no grey |
| Contour lines | `#FFFFFF` | Low opacity, glow shader |
| Roads | `#FFFFFF` | Brightness-based hierarchy |
| Route line | `#6b0919` | Wine red, animated flow |
| Accent / pulse | `#6b0919` | Beat-sync and UI borders |
| Labels | `#FFFFFF` | Monospaced, minimal |
| Grid overlay | `#FFFFFF` | Very low opacity, music-reactive |

### Layers
- **Terrain:** Mapbox terrain-rgb elevation data. 3D terrain enabled. Default camera pitch 55° routing, 45° idle.
- **Contour lines:** Custom Mapbox layer with glow effect. Subtle ambient drift animation in idle.
- **Buildings:** 3D extruded, rendered as wireframe outlines only — no solid fills. Point-cloud style on lower-detail zoom levels.
- **Roads:** Thin strokes. Highways brightest. Arterials medium. Residential barely visible. No colored bands.
- **Route line:** `#6b0919` with outer glow stroke. Animated dashes flow forward along the route direction.
- **Grid overlay:** Custom layer, very low opacity. Flashes briefly on beat hits.
- **Labels:** Monospaced font (JetBrains Mono). White. Only major roads and destination labels rendered. POI labels hidden.
- **Particles:** Custom overlay (not a Mapbox layer) — white particle system floating above terrain. Density and speed driven by music energy.

### Camera Behavior
- Smooth interpolated transitions — no hard cuts anywhere
- Pitch eases between 45° (idle) and 55° (routing)
- On maneuver approach (50m): camera pivots toward turn direction, then recenters after execution
- All camera moves use Mapbox's `CameraAnimationsPlugin` with custom easing curves

---

## 5. Idle State

The map is the UI. No navigation panels visible.

**Visible elements:**
- Fullscreen LiDAR map
- Vehicle position marker (custom — geometric crosshair, not default Mapbox puck)
- `LIDAR` wordmark — bottom-right corner, monospaced, `#FFFFFF` at 15% opacity
- Particle system active at full intensity
- Music reactivity at full intensity

**Interaction:**
- Tap anywhere on the map → `SearchOverlay` slides up from bottom
- No other interactive elements in idle state

---

## 6. Tactical HUD (Routing State)

Activated when routing begins. All panels share a common visual language: dark semi-transparent background (`#000000` at 80% opacity), thin `#6b0919` border, subtle scan-line texture.

### Panel Layout (Landscape)
```
┌─────────────────────────────────────────────────────────────────┐
│ [Street Name]                              [Compass]            │
│━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ (progress)│
│                                                                 │
│              MAP                                                │
│                                                                 │
│ [Speed]                                    [ETA / Distance]    │
│ [Speed Limit]                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Elements
| Element | Position | Content |
|---|---|---|
| Street name | Top-left | Current street, monospaced small caps |
| Compass | Top-right | Geometric line-art, rotates live with heading |
| Progress bar | Full width below top panels | Wine red `#6b0919`, fills left-to-right as trip progresses |
| Speed | Bottom-left, large | Current speed, largest text on screen |
| Speed limit | Below speed | Smaller, secondary |
| ETA | Bottom-right | Arrival time |
| Distance remaining | Below ETA | Kilometers/miles remaining — follows device locale default |

### Typography
- Font: JetBrains Mono (or equivalent monospaced)
- Speed: 48sp
- Primary labels: 16sp
- Secondary labels: 12sp
- All text: `#FFFFFF`

---

## 7. Cinematic Turn Instructions

The most visually expressive part of the routing experience. Treated as a filmic moment, not a notification.

### Sequence
1. **500m before turn** — turn card slides in from the top of the screen with a weighted, decelerating animation (cubic ease-out). The card partially overlaps the map.
2. **Card contents:**
   - Geometric line-art turn arrow (custom drawn, matches LiDAR aesthetic)
   - Destination street name (monospaced)
   - Live distance countdown (updates every second)
   - Ghost card behind it showing the secondary instruction at 40% opacity
3. **50m before turn** — card pulses once with a wine red flash. Camera pivots toward the turn direction. Distance countdown accelerates.
4. **At turn** — card exits with a horizontal wipe in the turn direction. Camera recenters. Map resumes normal pitch.

### Animation Specs
- Slide in: 400ms, cubic ease-out
- Wine red pulse: 200ms flash, 300ms fade
- Camera pivot: 600ms, smooth ease-in-out
- Card exit wipe: 350ms, linear

---

## 8. Music Reactivity System

### Audio Source
- **Primary:** Bluetooth audio — `MediaSessionManager` to detect active playback session + `AudioPlaybackCaptureConfiguration` (Android 10+) to capture audio for FFT analysis
- **Stretch goal (v2):** System-wide audio capture via `AudioRecord` for built-in head unit audio apps

### Signal Processing
- Raw PCM audio → lightweight FFT on a dedicated background thread
- Output per frame:
  - `beatIntensity: Float` (0.0–1.0) — instantaneous beat strength
  - `energyLevel: Float` (0.0–1.0) — rolling 4-second average = "mood"

### Ambient Mood Layer (energy-driven, continuous)
| Energy Level | Visual Response |
|---|---|
| High (0.7–1.0) | Particles fast, contour glow bright, grid opacity high |
| Medium (0.4–0.7) | Particles moderate, standard glow |
| Low (0.0–0.4) | Particles slow, map dims slightly, atmosphere still |

Transitions use 2–4 second easing — never abrupt.

### Beat-Sync Layer (intensity-driven, instantaneous)
On beat hit:
- Grid lines flash briefly (80ms)
- Wine red pulse expands outward from vehicle position, radius proportional to `beatIntensity`
- Particle burst at vehicle position

### Routing Modifier
When `ROUTING` state is active, all reactivity values are multiplied by `0.7`. The map still breathes — it never shouts over the HUD data.

---

## 9. Search & Destination Input

### Flow
1. User taps map in idle state
2. `SearchOverlay` slides up from bottom (400ms, cubic ease-out)
3. Map dims to 40% brightness behind a `#000000` 60% opacity overlay — map still visible
4. Music reactivity continues at 30% intensity behind overlay

### Search Panel Contents
- Search bar at top: large touch target (min 56dp height), monospaced font, wine red blinking cursor
- Results list below: each row min 56dp height, name + address + distance from current location
- Scroll if more than 5 results

### On Selection
1. Panel slides down and dismisses (300ms)
2. Map brightens back to full
3. Route calculated via Mapbox Navigation SDK
4. **Route draw animation:** route line draws itself from vehicle position to destination over ~1.5 seconds (animated stroke, wine red)
5. HUD activates — state transitions to `ROUTING`

### Input Methods
- Touch keyboard (Android system keyboard)
- No voice input in v1

---

## 10. Out of Scope — v1

The following are explicitly excluded from the first build:

- Voice input / voice search
- Settings screen
- Built-in head unit audio source reactivity (stretch goal, v2)
- Offline maps
- Traffic layer
- Points of interest beyond destination labels
- Day mode / light theme

---

## 11. Quality Bar

This app is held to the references provided during design: tactical military HUD aesthetics, LiDAR scan visuals, sci-fi command center data displays. Every element must pass that test before it ships.

The rubrics document at `docs/RUBRICS.md` defines Pass/Partial/Fail criteria for every major system. **Critical sections (Map Aesthetic, Fullscreen Enforcement, Cinematic Turn Instructions) must all Pass before any build is considered shippable.**

The implementation author is expected to self-critique against those rubrics at each milestone. "It works" is not the bar. "It looks like the references" is the bar.
