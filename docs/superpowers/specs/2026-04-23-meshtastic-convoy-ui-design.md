# Meshtastic Convoy UI — Design Spec

> **UI-only mockup.** No Bluetooth, no Meshtastic SDK, no real data. All values are hardcoded stubs that make the feature look complete for a screenshot/demo.

---

## Goal

Add a Convoy Mode to the Navigator app's idle and navigation screens so the UI looks ready for Meshtastic-based group tracking. Each convoy unit gets a unique color that carries through map dots, unit cards, and callsign labels.

---

## Unit Color Palette

Fixed assignment by slot index. Wine red is always YOU (Unit 01).

| Slot | Callsign | Color | Hex |
|------|----------|-------|-----|
| 0 | UNIT·01 | Wine red (self) | `#8A1226` |
| 1 | UNIT·02 | Cobalt blue | `#4A9EFF` |
| 2 | UNIT·03 | Amber | `#FFB300` |
| 3 | UNIT·04 | Purple | `#A855F7` |
| 4 | UNIT·05 | Teal | `#00D4AA` |

---

## Screen 1 — Idle Overlay Changes

### Relocate STANDBY indicator
Move the "SYS · STANDBY · NO ROUTE" block from `BOTTOM|END` to just below the coordinate readout at `TOP|END`. This frees the bottom-right corner entirely for the mesh button column.

### Fix search slab width
Change the fixed `520dp` width to `MATCH_PARENT` with `rightMargin = (200 * d).toInt()` so it doesn't collide with the button column on any screen size.

### New mesh status tag
Above the button column, a ghost tag: `MESH · OFFLINE` normally. Stub as `MESH · 3 UNITS` in phosphor white for the screenshot.

### New bottom-right button column
Three 52dp × 52dp framed panel buttons (`FramedPanelDrawable`) stacked vertically with 10dp gaps, anchored `BOTTOM|END`, right margin 28dp, bottom margin 28dp. Labels sit to the left of each button (matching MapControlsView style).

| Button label | Icon text | Action |
|---|---|---|
| `PRG` | `⌖` | PAIR·MESH — stub, no-op |
| `CVY` | `◈` | CONVOY — activates convoy mode |
| `CFG` | `⚙` | SETTINGS — stub, no-op |

---

## Screen 2 — Convoy Unit Strip (inside TripSheet)

Added as the topmost child of `TripSheet`'s root column, above the existing progress rail. Slides in/out with TripSheet — no separate animation needed.

### Layout
Horizontal `LinearLayout`, `MATCH_PARENT` height `WRAP_CONTENT`, bottom margin 12dp above the rail row.

Contains:
- One card per stub unit (hardcode 3 units for the screenshot: UNIT·01 self, UNIT·02, UNIT·03)
- A `MI | KM` toggle at the far right end

### Unit card
Each card is a `FramedPanelDrawable` slab whose border color matches the unit's assigned color. Contents left-to-right:

```
[colored dot 8dp]  UNIT·02  ◌ 1.4 mi
```

- Dot: filled `View`, 8dp × 8dp, unit color
- Callsign: 9sp JetBrains Mono, phosphor white
- Distance: 11sp JetBrains Mono, fgDim — stub value hardcoded per unit
- Self card (UNIT·01): distance replaced with `● YOU`

### MI / KM toggle
Two small text slabs side by side: `MI` (active, phosphor white) and `KM` (inactive, ghost). Tapping swaps active state and reformats all distance labels. Purely local UI state — no real conversion needed beyond multiplying stub values by 1.609.

---

## Screen 3 — Navigation While in Convoy Mode

### SpeedBubble: hidden
`speedBubble.visibility = View.GONE` when convoy mode is active. This avoids the overlap with the unit strip and simplifies the nav chrome.

### TripSheet: simplified
Show only:
- Progress rail row (`ROUTE · ACTIVE` + bar + %)
- ETA value
- Distance remaining
- T-REM (time remaining)

Remove: the target destination callout (`TGT·01 EMBARCADERO`) and the ABORT button area from view — replace ABORT with a smaller `END CONVOY` text button at the right edge of the data panel, wine-red, same style.

Actually, keep ABORT for navigation cancellation. Add `END CONVOY` as a separate tag-sized button next to it.

### Unit strip
Visible above the progress rail row, always in view during convoy+navigation.

### Turn card
Unchanged — stays top-left as-is.

---

## Map Dots (Stub)

Add 2–3 hardcoded `SymbolLayer` or `CircleLayer` entries to the Mapbox style at fixed coordinates near the starting camera position. Each dot:

- 14dp circle in the unit's color
- Callsign label above it in JetBrains Mono, white, 10sp
- Positioned plausibly near each other (e.g., 0.002° apart)

These are static map layers — no location updates.

---

## Files Modified / Created

| File | Change |
|---|---|
| `IdleOverlay.kt` | Move STANDBY to top-right; fix search slab width; add mesh status tag + 3-button column |
| `TripSheet.kt` | Add `ConvoyUnitStripView` as top child of column; add `END CONVOY` button; add `showConvoyStrip()`/`hideConvoyStrip()` methods |
| `MainActivity.kt` | Wire CVY button → convoy mode toggle; hide/show SpeedBubble; add stub Mapbox dot layers |
| `ConvoyUnitStripView.kt` | New file — the horizontal unit card row + MI/KM toggle |

---

## Out of Scope

- Bluetooth pairing, Meshtastic SDK, real GPS data
- Settings screen contents
- Voice button
- Actual unit count from hardware
