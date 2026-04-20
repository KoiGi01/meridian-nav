# LIDAR — Visual & Experience Rubrics

Use these rubrics to evaluate the first build against the original vision.
Each criterion is scored **Pass / Partial / Fail** with notes.

---

## 1. Map Aesthetic — The LiDAR Scan Test

> Does the map look like a LiDAR scan, or does it look like a dark Google Maps clone?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Background is true black, no grey tint | No ambient light bleed | Slight grey wash | Clearly dark grey, not black |
| Contour lines visibly glow | Soft luminous halo around lines | Lines visible but flat | Lines look like grey strokes |
| Buildings render as wireframe/point-cloud outlines | No solid fills, just edges and points | Partially filled or inconsistent | Solid colored blocks |
| Roads are thin and bright, not thick colored bands | Hair-thin, brightness-based hierarchy | Slightly too thick but readable | Thick colored roads like standard maps |
| 3D terrain pitch creates genuine depth | Clear sense of elevation and distance | Mild depth, could be stronger | Flat, pitch feels fake or unused |
| Grid overlay is present and subtle | Visible without dominating | Too faint or too strong | Missing entirely |

**Threshold to pass this section: 5 of 6 Pass, 0 Fail**

---

## 2. Color & Typography

> Is the palette disciplined and is the type sharp and intentional?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Route line color is exactly `#6b0919` | Confirmed in inspector | Close but slightly off | Clearly wrong color |
| Wine red accents appear only on intentional elements | Used sparingly and purposefully | Slightly overused | Everywhere, lost its impact |
| All UI text is monospaced | Consistent across all panels | Mixed fonts | Default Android sans-serif |
| Text is legible at arm's length (head unit distance) | Readable without squinting | Borderline | Requires leaning in |
| No white elements look grey or washed out | Pure white glows cleanly | Slightly dim | Dull, no luminosity |

**Threshold to pass this section: 4 of 5 Pass, 0 Fail**

---

## 3. Fullscreen & Landscape Enforcement

> Does the app own the screen completely from the first second?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| App launches in landscape orientation | Immediate, no rotation animation | Brief portrait flash then rotates | Launches portrait |
| Android nav buttons hidden on launch | Hidden before first frame is visible | Hidden after ~1 second delay | Visible throughout |
| System status bar hidden | Fully hidden | Partially visible | Fully visible |
| Nav buttons re-hide after accidental reveal | Re-hide within 2 seconds | Re-hide after 3–5 seconds | Stay visible until manual dismiss |
| No letterboxing or black bars | Edge-to-edge rendering | Minor bars on one edge | Obvious bars |

**Threshold to pass this section: 5 of 5 Pass**

---

## 4. Idle State — Immersion Test

> Would someone open this app just to watch it, even without a destination?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Map fills the entire screen with no UI clutter | Only the vehicle marker and LIDAR wordmark visible | 1–2 minor intrusive elements | Buttons or panels visible |
| Particle system is present and moving | Clearly visible, atmospheric | Very faint, barely noticeable | Absent |
| LIDAR wordmark is subtle, not distracting | Low opacity, corner placement | Slightly too prominent | Dominating the screen |
| The map feels alive even with no interaction | Subtle ambient animation present | Mostly static | Completely static |

**Threshold to pass this section: 3 of 4 Pass, 0 Fail**

---

## 5. Music Reactivity

> Does the map breathe with the music, or does it just blink?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Beat hits are visually distinct and timed correctly | Pulse lands on the beat | Slightly off-beat | No correlation to music |
| Ambient mood shifts are gradual, not jarring | Smooth 2–4 second transitions | Abrupt but not instant | Snaps immediately with no easing |
| High energy song produces a noticeably more intense map | Clear visual difference | Subtle, hard to tell | No difference |
| Reactivity never obscures map readability | Always legible underneath | Occasionally hard to read | Frequently obscures content |
| During routing, reactivity dims appropriately | Clearly toned down vs idle | Slightly reduced | Same intensity as idle |
| Wine red pulse originates from vehicle position | Correct origin point | Slightly offset | Random position |

**Threshold to pass this section: 5 of 6 Pass, 0 Fail**

---

## 6. Cinematic Turn Instructions

> Do turn cards feel filmic, or do they feel like a toast notification?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Turn card slides in smoothly ~500m before turn | Fluid, weighted animation | Slightly stiff or linear | Pops in instantly |
| Turn arrow is geometric line-art, not a generic icon | Custom drawn, matches aesthetic | Close but slightly generic | Stock Android icon |
| Distance countdown ticks live on the card | Updates in real time | Updates every few seconds | Static |
| Camera pivots toward the turn at ~50m | Noticeable, cinematic camera move | Very subtle, easy to miss | No camera movement |
| Wine red flash at ~50m is satisfying | Sharp pulse, well-timed | Present but weak | Absent |
| Card exits with horizontal wipe | Clean directional exit | Fades out generically | Snaps away |
| Secondary instruction ghost card is visible behind primary | Clearly readable at reduced opacity | Barely visible | Absent |

**Threshold to pass this section: 6 of 7 Pass, 0 Fail**

---

## 7. Tactical HUD Layout

> Is the HUD readable at a glance without breaking the aesthetic?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| All 5 HUD elements are present (street, compass, speed, ETA, progress bar) | All present | 1 missing | 2+ missing |
| Speed is the largest, most readable number on screen | Dominant, instant read | Readable but not dominant | Hard to find |
| Progress bar is wine red and spans the top | Correct color, correct position | Present but wrong color or position | Absent |
| HUD panels have dark bg + `#6b0919` border | Both present | One present | Neither |
| Compass rotates live with heading | Real-time rotation | Delayed | Static |
| HUD never overlaps the route line critically | Route always visible | Minor overlap | Route obscured by HUD |

**Threshold to pass this section: 5 of 6 Pass, 0 Fail**

---

## 8. Search & Destination Input

> Is the search flow fast, touch-friendly, and on-brand?

| Criterion | Pass | Partial | Fail |
|---|---|---|---|
| Search panel slides up from bottom smoothly | Fluid animation | Stiff but functional | Snaps open |
| Map is dimmed but still visible behind panel | Clearly visible, dimmed | Too dark or too bright | Map hidden entirely |
| Search bar has wine red cursor blink | Correct color, blinking | Wrong color or static | Default cursor |
| Result list items are large enough for touch on a head unit | Min 56dp height per row | Borderline small | Too small, mistouch-prone |
| Route draw animation plays after selection | Animated line draw ~1.5s | Too fast or too slow | No animation, route appears instantly |

**Threshold to pass this section: 4 of 5 Pass, 0 Fail**

---

## Overall Score

| Section | Weight | Result |
|---|---|---|
| 1. Map Aesthetic | Critical | |
| 2. Color & Typography | High | |
| 3. Fullscreen & Landscape | Critical | |
| 4. Idle State | High | |
| 5. Music Reactivity | High | |
| 6. Cinematic Turn Instructions | Critical | |
| 7. Tactical HUD | High | |
| 8. Search & Destination Input | Medium | |

**Critical sections (1, 3, 6) must all Pass. No exceptions.**
If any Critical section fails, the build does not ship — it goes back for rework regardless of other scores.
