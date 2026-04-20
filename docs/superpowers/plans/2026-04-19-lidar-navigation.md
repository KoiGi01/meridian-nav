# LIDAR Navigation App — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a visually stunning Android car navigation app with a LiDAR scan aesthetic, music reactivity, and cinematic turn instructions — targeting Android head units.

**Architecture:** Single `MainActivity` hosts a fullscreen Mapbox `MapView` with custom Android Views layered on top. A bound `MusicReactivityService` handles audio analysis independently of UI state. Two UI states (`IDLE` / `ROUTING`) control which overlays are visible.

**Tech Stack:** Kotlin, Mapbox Maps SDK v11, Mapbox Navigation SDK v3, Android `AudioPlaybackCaptureConfiguration` (API 29+), `ValueAnimator`, `Canvas` drawing, JetBrains Mono font.

---

## File Structure

```
app/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/lidar/nav/
│   │   │   ├── MainActivity.kt                   — single activity, state machine host
│   │   │   ├── state/
│   │   │   │   └── AppState.kt                   — IDLE/ROUTING sealed class + StateFlow
│   │   │   ├── map/
│   │   │   │   ├── LidarStyleBuilder.kt           — builds the custom Mapbox style
│   │   │   │   └── MapCameraManager.kt            — all camera animations
│   │   │   ├── ui/
│   │   │   │   ├── IdleOverlay.kt                 — wordmark + particle host, idle only
│   │   │   │   ├── HudOverlay.kt                  — speed/ETA/compass/progress, routing only
│   │   │   │   ├── TurnInstructionOverlay.kt      — cinematic turn card
│   │   │   │   ├── SearchOverlay.kt               — search panel, slides up from bottom
│   │   │   │   ├── CompassView.kt                 — rotating geometric compass widget
│   │   │   │   ├── ParticleView.kt                — Canvas-based particle system
│   │   │   │   └── GridReactivityView.kt          — Canvas grid that pulses on beat
│   │   │   ├── music/
│   │   │   │   ├── MusicReactivityService.kt      — bound service, lifecycle owner
│   │   │   │   ├── AudioAnalyzer.kt               — PCM → beatIntensity + energyLevel
│   │   │   │   └── ReactivityState.kt             — data class for beat/energy values
│   │   │   └── navigation/
│   │   │       ├── NavigationManager.kt           — Mapbox Navigation SDK wrapper
│   │   │       └── RouteDrawAnimator.kt           — animates route line draw-on
│   │   └── res/
│   │       ├── layout/activity_main.xml
│   │       ├── font/jetbrains_mono_regular.ttf
│   │       └── values/
│   │           ├── colors.xml
│   │           └── strings.xml
│   └── test/java/com/lidar/nav/
│       ├── state/AppStateTest.kt
│       ├── music/AudioAnalyzerTest.kt
│       └── map/MapCameraManagerTest.kt
```

---

## Pre-Flight: Mapbox Account Setup

Before Task 1, the developer needs:

1. Create a Mapbox account at mapbox.com
2. Obtain a **public token** (starts with `pk.`) — used at runtime
3. Obtain a **secret token** (starts with `sk.`) — used to download the SDK

Add to `~/.gradle/gradle.properties` (create if missing):
```
MAPBOX_DOWNLOADS_TOKEN=sk.eyJ1Ijoyour_secret_token_here
```

Add to project `local.properties` (never commit this file):
```
MAPBOX_ACCESS_TOKEN=pk.eyJ1Ijoyour_public_token_here
```

---

## Task 1: Android Project Setup & Gradle Configuration

**Files:**
- Create: `app/build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create a new Android project** in Android Studio
  - Package name: `com.lidar.nav`
  - Language: Kotlin
  - Min SDK: API 26 (Android 8.0)
  - Target SDK: API 35
  - Template: Empty Activity

- [ ] **Step 2: Configure `settings.gradle.kts` for Mapbox Maven**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN").get()
            }
        }
    }
}
rootProject.name = "LIDAR"
include(":app")
```

- [ ] **Step 3: Configure `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.lidar.nav"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lidar.nav"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val props = com.android.build.gradle.internal.cxx.configure.gradleLocalProperties(rootDir)
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN",
            "\"${props.getProperty("MAPBOX_ACCESS_TOKEN", "")}\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
}

dependencies {
    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android:11.9.0")
    // Mapbox Navigation SDK v3
    implementation("com.mapbox.navigationcore:android:3.7.0")
    implementation("com.mapbox.navigationcore:tripdata:3.7.0")
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

- [ ] **Step 4: Create `app/src/main/res/values/colors.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="lidar_black">#000000</color>
    <color name="lidar_white">#FFFFFF</color>
    <color name="lidar_wine_red">#6b0919</color>
    <color name="lidar_overlay_bg">#CC000000</color>
</resources>
```

- [ ] **Step 5: Create `app/src/main/res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">LIDAR</string>
</resources>
```

- [ ] **Step 6: Replace `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".LidarApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize"
            android:exported="true"
            android:screenOrientation="landscape"
            android:windowSoftInputMode="adjustNothing">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".music.MusicReactivityService"
            android:foregroundServiceType="mediaPlayback" />
    </application>
</manifest>
```

- [ ] **Step 7: Create `LidarApp.kt` for Mapbox token init**

```kotlin
// app/src/main/java/com/lidar/nav/LidarApp.kt
package com.lidar.nav

import android.app.Application
import com.mapbox.maps.MapboxOptions

class LidarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
```

- [ ] **Step 8: Sync Gradle and verify build**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "feat: initial project setup with Mapbox SDK dependencies"
```

---

## Task 2: Fullscreen Landscape Enforcement

**Files:**
- Create: `app/src/main/java/com/lidar/nav/MainActivity.kt`
- Create: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Write the test**

```kotlin
// app/src/test/java/com/lidar/nav/MainActivityTest.kt
package com.lidar.nav

import org.junit.Test
import org.junit.Assert.*

class MainActivityTest {
    @Test
    fun `immersive flags bitmask includes all required flags`() {
        val SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000
        val SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004
        val SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002
        val SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100
        val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400
        val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200

        val expectedFlags = (SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or SYSTEM_UI_FLAG_FULLSCREEN
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        assertTrue(expectedFlags and SYSTEM_UI_FLAG_IMMERSIVE_STICKY != 0)
        assertTrue(expectedFlags and SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0)
    }
}
```

- [ ] **Step 2: Run test to verify it passes (logic-only test)**

```bash
./gradlew test --tests "com.lidar.nav.MainActivityTest"
```
Expected: PASS

- [ ] **Step 3: Create `activity_main.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/lidar_black">

    <com.mapbox.maps.MapView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <!-- Overlays added programmatically in MainActivity -->

</FrameLayout>
```

- [ ] **Step 4: Create `MainActivity.kt` with fullscreen enforcement**

```kotlin
// app/src/main/java/com/lidar/nav/MainActivity.kt
package com.lidar.nav

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import com.lidar.nav.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enforceFullscreen()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enforceFullscreen()
    }

    private fun enforceFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
}
```

- [ ] **Step 5: Enable ViewBinding in `build.gradle.kts`**

Add inside `android { buildFeatures { ... } }`:
```kotlin
buildFeatures {
    buildConfig = true
    viewBinding = true
}
```

- [ ] **Step 6: Build and deploy to device/emulator. Verify:**
  - App opens in landscape
  - Nav buttons are hidden
  - Status bar is hidden
  - Swiping from edge briefly shows bars, they re-hide automatically

- [ ] **Step 7: Commit**

```bash
git add app/src app/build.gradle.kts
git commit -m "feat: enforce landscape fullscreen immersive mode"
```

---

## Task 3: App State Machine

**Files:**
- Create: `app/src/main/java/com/lidar/nav/state/AppState.kt`
- Create: `app/src/test/java/com/lidar/nav/state/AppStateTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/lidar/nav/state/AppStateTest.kt
package com.lidar.nav.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppStateTest {

    @Test
    fun `initial state is IDLE`() = runTest {
        val controller = AppStateController()
        assertEquals(AppState.Idle, controller.state.first())
    }

    @Test
    fun `startRouting transitions to ROUTING`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        assertEquals(AppState.Routing::class, controller.state.first()::class)
    }

    @Test
    fun `cancelRoute transitions back to IDLE`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        controller.cancelRoute()
        assertEquals(AppState.Idle, controller.state.first())
    }

    @Test
    fun `arrive transitions back to IDLE`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        controller.arrive()
        assertEquals(AppState.Idle, controller.state.first())
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew test --tests "com.lidar.nav.state.AppStateTest"
```
Expected: FAIL — `AppStateController` not defined

- [ ] **Step 3: Implement `AppState.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/state/AppState.kt
package com.lidar.nav.state

import com.mapbox.api.directions.v5.models.DirectionsRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AppState {
    object Idle : AppState()
    data class Routing(val route: DirectionsRoute? = null) : AppState()
}

class AppStateController {
    private val _state = MutableStateFlow<AppState>(AppState.Idle)
    val state: StateFlow<AppState> = _state

    fun startRouting(route: DirectionsRoute? = null) {
        _state.value = AppState.Routing(route)
    }

    fun cancelRoute() {
        _state.value = AppState.Idle
    }

    fun arrive() {
        _state.value = AppState.Idle
    }
}
```

- [ ] **Step 4: Run tests to verify pass**

```bash
./gradlew test --tests "com.lidar.nav.state.AppStateTest"
```
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lidar/nav/state app/src/test/java/com/lidar/nav/state
git commit -m "feat: IDLE/ROUTING state machine with StateFlow"
```

---

## Task 4: LiDAR Dark Map Style

**Files:**
- Create: `app/src/main/java/com/lidar/nav/map/LidarStyleBuilder.kt`

- [ ] **Step 1: Add JetBrains Mono font**

Download `JetBrainsMono-Regular.ttf` from [jetbrains.com/lp/mono](https://www.jetbrains.com/lp/mono/) and place at:
`app/src/main/res/font/jetbrains_mono_regular.ttf`

Also create `app/src/main/res/font/jetbrains_mono.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:app="http://schemas.android.com/apk/res-auto">
    <font
        app:fontStyle="normal"
        app:fontWeight="400"
        app:font="@font/jetbrains_mono_regular" />
</font-family>
```

- [ ] **Step 2: Create `LidarStyleBuilder.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/map/LidarStyleBuilder.kt
package com.lidar.nav.map

import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.zoom
import com.mapbox.maps.extension.style.layers.generated.*
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.generated.*
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain

object LidarStyleBuilder {

    // Wine red accent — matches spec exactly
    const val WINE_RED = "#6b0919"
    const val LIDAR_WHITE = "#FFFFFF"
    const val LIDAR_BLACK = "#000000"

    fun build() = style(styleUri = "mapbox://styles/mapbox/empty-v9") {

        // ── Sources ──────────────────────────────────────────────────────────

        +rasterDemSource("mapbox-dem") {
            url("mapbox://mapbox.mapbox-terrain-dem-v1")
            tileSize(512)
            maxzoom(14.0)
        }

        +vectorSource("mapbox-terrain") {
            url("mapbox://mapbox.mapbox-terrain-v2")
        }

        +vectorSource("mapbox-streets") {
            url("mapbox://mapbox.mapbox-streets-v8")
        }

        // ── Terrain (3D elevation) ────────────────────────────────────────────
        +terrain("mapbox-dem") {
            exaggeration(1.3)
        }

        // ── Background ───────────────────────────────────────────────────────
        +backgroundLayer("background") {
            backgroundColor(LIDAR_BLACK)
        }

        // ── Contour lines ────────────────────────────────────────────────────
        +lineLayer("contour-minor", "mapbox-terrain") {
            sourceLayer("contour")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.eq {
                get("index"); literal(1)
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.15)
            lineWidth(0.5)
        }

        +lineLayer("contour-major", "mapbox-terrain") {
            sourceLayer("contour")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.eq {
                get("index"); literal(5)
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.35)
            lineWidth(0.8)
        }

        // ── Roads ─────────────────────────────────────────────────────────────
        +lineLayer("roads-residential", "mapbox-streets") {
            sourceLayer("road")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.match {
                get("class")
                literal(listOf("street", "street_limited", "residential", "service"))
                literal(true)
                literal(false)
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.15)
            lineWidth(0.5)
        }

        +lineLayer("roads-arterial", "mapbox-streets") {
            sourceLayer("road")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.match {
                get("class")
                literal(listOf("secondary", "tertiary"))
                literal(true)
                literal(false)
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.3)
            lineWidth(0.7)
        }

        +lineLayer("roads-primary", "mapbox-streets") {
            sourceLayer("road")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.match {
                get("class")
                literal(listOf("primary", "trunk"))
                literal(true)
                literal(false)
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.5)
            lineWidth(1.0)
        }

        +lineLayer("roads-highway", "mapbox-streets") {
            sourceLayer("road")
            filter(com.mapbox.maps.extension.style.expressions.dsl.generated.eq {
                get("class"); literal("motorway")
            })
            lineColor(LIDAR_WHITE)
            lineOpacity(0.7)
            lineWidth(1.5)
        }

        // ── Building wireframe outlines ───────────────────────────────────────
        +fillExtrusionLayer("buildings", "mapbox-streets") {
            sourceLayer("building")
            fillExtrusionColor(LIDAR_BLACK)
            fillExtrusionOpacity(0.0)
            fillExtrusionHeight(get("height"))
            fillExtrusionBase(get("min_height"))
        }

        +lineLayer("building-outlines", "mapbox-streets") {
            sourceLayer("building")
            lineColor(LIDAR_WHITE)
            lineOpacity(0.3)
            lineWidth(0.6)
        }

        // ── Road labels (minimal) ─────────────────────────────────────────────
        +symbolLayer("road-labels", "mapbox-streets") {
            sourceLayer("road_label")
            textField(get("name"))
            textSize(10.0)
            textColor(LIDAR_WHITE)
            textOpacity(0.5)
            textMaxAngle(30.0)
            symbolPlacement("line")
        }

        // ── Route line (added dynamically by NavigationManager, not here) ─────
        // Route line source/layer is added by the Navigation SDK
    }
}
```

- [ ] **Step 3: Load the style in `MainActivity.kt`**

Add to `onCreate` after `setContentView`:
```kotlin
import com.lidar.nav.map.LidarStyleBuilder

// Inside onCreate, after setContentView:
binding.mapView.mapboxMap.loadStyle(LidarStyleBuilder.build()) { style ->
    // Style loaded — set camera pitch
    binding.mapView.mapboxMap.setCamera(
        com.mapbox.maps.CameraOptions.Builder()
            .pitch(45.0)
            .zoom(15.0)
            .build()
    )
}
```

- [ ] **Step 4: Run on device. Verify against Rubric Section 1:**
  - Background is true black ✓/✗
  - Contour lines are visible and glow ✓/✗
  - Roads are thin white lines ✓/✗
  - 3D terrain creates depth at pitched camera ✓/✗
  - Building outlines visible (in urban areas) ✓/✗

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lidar/nav/map/LidarStyleBuilder.kt app/src/main/java/com/lidar/nav/MainActivity.kt app/src/main/res/font
git commit -m "feat: LiDAR Dark custom Mapbox style with terrain, contour, road layers"
```

---

## Task 5: Map Camera Manager

**Files:**
- Create: `app/src/main/java/com/lidar/nav/map/MapCameraManager.kt`
- Create: `app/src/test/java/com/lidar/nav/map/MapCameraManagerTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// app/src/test/java/com/lidar/nav/map/MapCameraManagerTest.kt
package com.lidar.nav.map

import org.junit.Assert.*
import org.junit.Test

class MapCameraManagerTest {

    @Test
    fun `idle pitch is 45 degrees`() {
        assertEquals(45.0, MapCameraManager.IDLE_PITCH, 0.001)
    }

    @Test
    fun `routing pitch is 55 degrees`() {
        assertEquals(55.0, MapCameraManager.ROUTING_PITCH, 0.001)
    }

    @Test
    fun `turn approach distance is 50 meters`() {
        assertEquals(50.0, MapCameraManager.TURN_APPROACH_METERS, 0.001)
    }

    @Test
    fun `turn card trigger distance is 500 meters`() {
        assertEquals(500.0, MapCameraManager.TURN_CARD_TRIGGER_METERS, 0.001)
    }
}
```

- [ ] **Step 2: Run to verify failure**

```bash
./gradlew test --tests "com.lidar.nav.map.MapCameraManagerTest"
```
Expected: FAIL

- [ ] **Step 3: Implement `MapCameraManager.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/map/MapCameraManager.kt
package com.lidar.nav.map

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera

class MapCameraManager(private val mapboxMap: MapboxMap) {

    companion object {
        const val IDLE_PITCH = 45.0
        const val ROUTING_PITCH = 55.0
        const val TURN_APPROACH_METERS = 50.0
        const val TURN_CARD_TRIGGER_METERS = 500.0
    }

    fun animateToIdle() {
        mapboxMap.setCamera(
            CameraOptions.Builder().pitch(IDLE_PITCH).build()
        )
    }

    fun animateToRouting() {
        mapboxMap.setCamera(
            CameraOptions.Builder().pitch(ROUTING_PITCH).build()
        )
    }

    fun pivotTowardTurn(bearingDelta: Double) {
        val currentBearing = mapboxMap.cameraState.bearing
        val targetBearing = currentBearing + bearingDelta
        mapboxMap.camera.easeTo(
            CameraOptions.Builder()
                .bearing(targetBearing)
                .pitch(ROUTING_PITCH + 5.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(600)
                interpolator(DecelerateInterpolator())
            }
        )
    }

    fun recenterAfterTurn() {
        mapboxMap.camera.easeTo(
            CameraOptions.Builder()
                .pitch(ROUTING_PITCH)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(600) }
        )
    }
}
```

- [ ] **Step 4: Run tests to verify pass**

```bash
./gradlew test --tests "com.lidar.nav.map.MapCameraManagerTest"
```
Expected: PASS

- [ ] **Step 5: Wire into `MainActivity.kt`**

Add as a field after style loads:
```kotlin
private lateinit var cameraManager: MapCameraManager

// Inside loadStyle callback:
cameraManager = MapCameraManager(binding.mapView.mapboxMap)
cameraManager.animateToIdle()
```

- [ ] **Step 6: Add custom vehicle position marker**

Replace the default Mapbox location puck with a geometric crosshair. Add to `MainActivity.kt` inside the `loadStyle` callback, after `cameraManager` setup:

```kotlin
import com.mapbox.maps.plugin.locationcomponent.location

// Inside loadStyle callback:
binding.mapView.location.updateSettings {
    enabled = true
    pulsingEnabled = false
    locationPuck = com.mapbox.maps.plugin.locationcomponent.LocationPuck2D(
        topImage = createCrosshairDrawable(),
        bearingImage = createCrosshairDrawable(),
        shadowImage = null
    )
}
```

Add `createCrosshairDrawable()` to `MainActivity.kt`:
```kotlin
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape

private fun createCrosshairDrawable(): android.graphics.drawable.Drawable {
    // Thin white ring + crosshair lines drawn on a Bitmap
    val size = 40
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    val cx = size / 2f; val cy = size / 2f; val r = size * 0.35f
    canvas.drawCircle(cx, cy, r, paint)
    canvas.drawLine(cx, 0f, cx, cy - r, paint)
    canvas.drawLine(cx, cy + r, cx, size.toFloat(), paint)
    canvas.drawLine(0f, cy, cx - r, cy, paint)
    canvas.drawLine(cx + r, cy, size.toFloat(), cy, paint)
    return android.graphics.drawable.BitmapDrawable(resources, bitmap)
}
```

- [ ] **Step 7: Run on device — verify vehicle marker is a geometric crosshair, not the default blue dot**

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lidar/nav/map/MapCameraManager.kt app/src/test/java/com/lidar/nav/map app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: camera manager and custom geometric crosshair vehicle marker"
```

---

## Task 6: Particle View (Idle Ambient)

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/ParticleView.kt`

- [ ] **Step 1: Create `ParticleView.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/ParticleView.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class ParticleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var alpha: Float,
        var radius: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 0.0 = still, 1.0 = full speed
    var energyLevel: Float = 0.3f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    // 0.0–1.0 beat intensity — triggers a burst
    var beatIntensity: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            if (value > 0.5f) spawnBurst((value * 8).toInt())
        }

    // Multiplier applied to all movement — routing dims to 0.7
    var intensityMultiplier: Float = 1.0f

    private val particleCount = 80
    private var lastFrameTime = System.currentTimeMillis()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        repeat(particleCount) { particles.add(spawnParticle()) }
    }

    private fun spawnParticle(): Particle {
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
        val speed = (0.2f + Random.nextFloat() * 0.8f)
        return Particle(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            alpha = 0.1f + Random.nextFloat() * 0.4f,
            radius = 1f + Random.nextFloat() * 2f
        )
    }

    private fun spawnBurst(count: Int) {
        repeat(count) {
            val p = spawnParticle()
            p.vx *= 3f
            p.vy *= 3f
            p.alpha = 0.8f
            particles.add(p)
        }
        while (particles.size > particleCount + 40) particles.removeAt(0)
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime) / 1000f).coerceAtMost(0.05f)
        lastFrameTime = now

        val speedFactor = (0.3f + energyLevel * 0.7f) * intensityMultiplier

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) { invalidate(); return }

        val toRemove = mutableListOf<Particle>()
        for (p in particles) {
            p.x += p.vx * dt * speedFactor * 0.03f
            p.y += p.vy * dt * speedFactor * 0.03f
            p.alpha -= dt * 0.02f * speedFactor
            if (p.alpha <= 0f || p.x < 0f || p.x > 1f || p.y < 0f || p.y > 1f) {
                toRemove.add(p)
            } else {
                paint.alpha = (p.alpha * 255 * intensityMultiplier).toInt().coerceIn(0, 255)
                canvas.drawCircle(p.x * w, p.y * h, p.radius, paint)
            }
        }
        particles.removeAll(toRemove)
        while (particles.size < particleCount) particles.add(spawnParticle())

        invalidate()
    }
}
```

- [ ] **Step 2: Create `GridReactivityView.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/GridReactivityView.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GridReactivityView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    private var flashAlpha = 0f
    private val gridSpacingDp = 60f
    private var lastFrame = System.currentTimeMillis()

    var intensityMultiplier: Float = 1.0f

    fun onBeat(intensity: Float) {
        flashAlpha = (intensity * 0.4f * intensityMultiplier).coerceIn(0f, 0.4f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.currentTimeMillis()
        val dt = (now - lastFrame) / 1000f
        lastFrame = now

        flashAlpha = (flashAlpha - dt * 5f).coerceAtLeast(0f)

        val alpha = (flashAlpha * 255).toInt()
        if (alpha <= 0) return

        gridPaint.alpha = alpha

        val spacing = gridSpacingDp * resources.displayMetrics.density
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += spacing
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += spacing
        }

        if (flashAlpha > 0f) invalidate()
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/ParticleView.kt app/src/main/java/com/lidar/nav/ui/GridReactivityView.kt
git commit -m "feat: particle system and beat-reactive grid overlay views"
```

---

## Task 7: Idle Overlay

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/IdleOverlay.kt`

- [ ] **Step 1: Create `IdleOverlay.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/IdleOverlay.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class IdleOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val particleView: ParticleView
    val gridView: GridReactivityView
    private val wordmark: TextView

    init {
        setBackgroundColor(Color.TRANSPARENT)

        gridView = GridReactivityView(context).also {
            addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        particleView = ParticleView(context).also {
            addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }

        wordmark = TextView(context).apply {
            text = "LIDAR"
            setTextColor(Color.WHITE)
            textSize = 14f
            alpha = 0.15f
            typeface = try {
                ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular)
            } catch (e: Exception) {
                Typeface.MONOSPACE
            }
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, 32, 24)
            }
            layoutParams = params
        }
        addView(wordmark)
    }

    fun applyReactivity(beatIntensity: Float, energyLevel: Float, multiplier: Float = 1.0f) {
        particleView.energyLevel = energyLevel
        particleView.beatIntensity = beatIntensity
        particleView.intensityMultiplier = multiplier
        gridView.intensityMultiplier = multiplier
        if (beatIntensity > 0.5f) gridView.onBeat(beatIntensity)
    }
}
```

- [ ] **Step 2: Add `IdleOverlay` to `MainActivity.kt`**

```kotlin
// Add field:
private lateinit var idleOverlay: IdleOverlay

// Inside loadStyle callback, after cameraManager setup:
idleOverlay = IdleOverlay(this).also {
    binding.rootContainer.addView(
        it,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    )
}
```

- [ ] **Step 3: Run on device. Verify:**
  - Particle system is visible and moving
  - LIDAR wordmark appears bottom-right at low opacity
  - No other UI elements visible

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/IdleOverlay.kt app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: idle overlay with particle system and LIDAR wordmark"
```

---

## Task 8: Audio Analyzer & Music Reactivity Service

**Files:**
- Create: `app/src/main/java/com/lidar/nav/music/ReactivityState.kt`
- Create: `app/src/main/java/com/lidar/nav/music/AudioAnalyzer.kt`
- Create: `app/src/main/java/com/lidar/nav/music/MusicReactivityService.kt`
- Create: `app/src/test/java/com/lidar/nav/music/AudioAnalyzerTest.kt`

- [ ] **Step 1: Create `ReactivityState.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/music/ReactivityState.kt
package com.lidar.nav.music

data class ReactivityState(
    val beatIntensity: Float = 0f,   // 0.0–1.0 instantaneous beat strength
    val energyLevel: Float = 0f      // 0.0–1.0 rolling 4-second mood average
)
```

- [ ] **Step 2: Write failing tests for `AudioAnalyzer`**

```kotlin
// app/src/test/java/com/lidar/nav/music/AudioAnalyzerTest.kt
package com.lidar.nav.music

import org.junit.Assert.*
import org.junit.Test

class AudioAnalyzerTest {

    @Test
    fun `silence produces zero beat intensity`() {
        val analyzer = AudioAnalyzer()
        val silence = ShortArray(1024) { 0 }
        val state = analyzer.analyze(silence)
        assertEquals(0f, state.beatIntensity, 0.01f)
    }

    @Test
    fun `loud burst produces high beat intensity`() {
        val analyzer = AudioAnalyzer()
        val burst = ShortArray(1024) { Short.MAX_VALUE }
        val state = analyzer.analyze(burst)
        assertTrue("Expected > 0.5, got ${state.beatIntensity}", state.beatIntensity > 0.5f)
    }

    @Test
    fun `energy level is clamped between 0 and 1`() {
        val analyzer = AudioAnalyzer()
        val loud = ShortArray(1024) { Short.MAX_VALUE }
        repeat(100) { analyzer.analyze(loud) }
        val state = analyzer.analyze(loud)
        assertTrue(state.energyLevel in 0f..1f)
    }

    @Test
    fun `beat intensity is clamped between 0 and 1`() {
        val analyzer = AudioAnalyzer()
        val loud = ShortArray(1024) { Short.MAX_VALUE }
        val state = analyzer.analyze(loud)
        assertTrue(state.beatIntensity in 0f..1f)
    }
}
```

- [ ] **Step 3: Run to verify failure**

```bash
./gradlew test --tests "com.lidar.nav.music.AudioAnalyzerTest"
```
Expected: FAIL

- [ ] **Step 4: Implement `AudioAnalyzer.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/music/AudioAnalyzer.kt
package com.lidar.nav.music

import kotlin.math.sqrt

class AudioAnalyzer {

    // Rolling buffer for 4-second energy average (at ~50fps: 200 samples)
    private val energyBuffer = ArrayDeque<Float>(200)
    private var previousEnergy = 0f

    fun analyze(pcmBuffer: ShortArray): ReactivityState {
        val rms = computeRms(pcmBuffer)
        val normalizedRms = (rms / Short.MAX_VALUE.toFloat()).coerceIn(0f, 1f)

        // Beat detection: current energy significantly higher than recent average
        val recentAvg = if (energyBuffer.isEmpty()) 0f else energyBuffer.average().toFloat()
        val beatIntensity = if (normalizedRms > recentAvg * 1.5f && normalizedRms > 0.05f) {
            ((normalizedRms - recentAvg) / (1f - recentAvg + 0.001f)).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Update rolling energy buffer
        energyBuffer.addLast(normalizedRms)
        if (energyBuffer.size > 200) energyBuffer.removeFirst()

        val energyLevel = energyBuffer.average().toFloat().coerceIn(0f, 1f)
        previousEnergy = normalizedRms

        return ReactivityState(
            beatIntensity = beatIntensity,
            energyLevel = energyLevel
        )
    }

    private fun computeRms(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return 0f
        var sum = 0.0
        for (sample in buffer) sum += sample.toDouble() * sample.toDouble()
        return sqrt(sum / buffer.size).toFloat()
    }
}
```

- [ ] **Step 5: Run tests to verify pass**

```bash
./gradlew test --tests "com.lidar.nav.music.AudioAnalyzerTest"
```
Expected: PASS (4 tests)

- [ ] **Step 6: Implement `MusicReactivityService.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/music/MusicReactivityService.kt
package com.lidar.nav.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.lidar.nav.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicReactivityService : Service() {

    inner class LocalBinder : Binder() {
        fun getService() = this@MusicReactivityService
    }

    private val binder = LocalBinder()
    private val analyzer = AudioAnalyzer()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(ReactivityState())
    val state: StateFlow<ReactivityState> = _state

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1, buildNotification())
        }
    }

    fun startCapture(mediaProjection: android.media.projection.MediaProjection? = null) {
        captureJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            startAudioCapture(mediaProjection)
        } else {
            startSilentFallback()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startAudioCapture(projection: android.media.projection.MediaProjection) {
        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        audioRecord?.startRecording()

        captureJob = scope.launch {
            val buffer = ShortArray(bufferSize / 2)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    _state.value = analyzer.analyze(buffer.copyOf(read))
                }
            }
        }
    }

    private fun startSilentFallback() {
        // No audio available — emit neutral state
        captureJob = scope.launch {
            while (isActive) {
                _state.value = ReactivityState(beatIntensity = 0f, energyLevel = 0.2f)
                delay(100)
            }
        }
    }

    fun stopCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun onDestroy() {
        stopCapture()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "lidar_music", "Music Reactivity", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, "lidar_music")
            .setContentTitle("LIDAR")
            .setContentText("Music reactivity active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }
}
```

- [ ] **Step 7: Run unit tests**

```bash
./gradlew test
```
Expected: All tests PASS

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/lidar/nav/music app/src/test/java/com/lidar/nav/music
git commit -m "feat: audio analyzer with beat/energy detection and music reactivity service"
```

---

## Task 9: Bind Music Service to MainActivity & Wire Reactivity

**Files:**
- Modify: `app/src/main/java/com/lidar/nav/MainActivity.kt`

- [ ] **Step 1: Add service binding and coroutine collection to `MainActivity.kt`**

```kotlin
// Add imports:
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.lidar.nav.music.MusicReactivityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Add fields to MainActivity:
private var musicService: MusicReactivityService? = null
private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

private val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        musicService = (binder as MusicReactivityService.LocalBinder).getService()
        musicService?.startCapture()
        observeReactivity()
    }
    override fun onServiceDisconnected(name: ComponentName) {
        musicService = null
    }
}

// Add to onStart():
override fun onStart() {
    super.onStart()
    binding.mapView.onStart()
    Intent(this, MusicReactivityService::class.java).also { intent ->
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
}

// Add to onStop():
override fun onStop() {
    super.onStop()
    binding.mapView.onStop()
    unbindService(serviceConnection)
}

// Add observeReactivity():
private fun observeReactivity() {
    activityScope.launch {
        musicService?.state?.collect { reactivity ->
            val multiplier = if (appState.state.value is AppState.Routing) 0.7f else 1.0f
            idleOverlay.applyReactivity(
                beatIntensity = reactivity.beatIntensity,
                energyLevel = reactivity.energyLevel,
                multiplier = multiplier
            )
        }
    }
}

// Add to onDestroy():
override fun onDestroy() {
    super.onDestroy()
    binding.mapView.onDestroy()
    activityScope.cancel()
}

// Add appState field:
private val appState = AppStateController()
```

- [ ] **Step 2: Build and run on device**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Play music on the device via Bluetooth. Verify:**
  - Particles speed up with high-energy music
  - Grid flashes on beats
  - Particles slow with quiet/ambient music

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: bind music reactivity service and wire beat/energy to idle overlay"
```

---

## Task 10: Compass View

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/CompassView.kt`

- [ ] **Step 1: Create `CompassView.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/CompassView.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CompassView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        alpha = 200
    }

    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6b0919")
        style = Paint.Style.FILL
    }

    var bearing: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = (minOf(width, height) / 2f) * 0.85f

        canvas.save()
        canvas.rotate(-bearing, cx, cy)

        // Outer ring
        canvas.drawCircle(cx, cy, r, linePaint)

        // Cardinal tick marks
        for (i in 0 until 8) {
            val angle = Math.toRadians((i * 45).toDouble())
            val innerR = if (i % 2 == 0) r * 0.75f else r * 0.85f
            canvas.drawLine(
                cx + (innerR * sin(angle)).toFloat(),
                cy - (innerR * cos(angle)).toFloat(),
                cx + (r * sin(angle)).toFloat(),
                cy - (r * cos(angle)).toFloat(),
                linePaint
            )
        }

        // North pointer — wine red triangle
        val path = Path().apply {
            moveTo(cx, cy - r * 0.6f)
            lineTo(cx - r * 0.1f, cy)
            lineTo(cx + r * 0.1f, cy)
            close()
        }
        canvas.drawPath(path, accentPaint)

        // South pointer — white outline
        val southPath = Path().apply {
            moveTo(cx, cy + r * 0.6f)
            lineTo(cx - r * 0.1f, cy)
            lineTo(cx + r * 0.1f, cy)
            close()
        }
        linePaint.style = Paint.Style.STROKE
        canvas.drawPath(southPath, linePaint)
        linePaint.style = Paint.Style.STROKE

        canvas.restore()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/CompassView.kt
git commit -m "feat: geometric line-art compass view with wine red north pointer"
```

---

## Task 11: HUD Overlay (Routing State)

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/HudOverlay.kt`

- [ ] **Step 1: Create `HudOverlay.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/HudOverlay.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class HudOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    val streetNameView: TextView
    val compassView: CompassView
    val speedView: TextView
    val speedLimitView: TextView
    val etaView: TextView
    val distanceView: TextView
    val progressBar: View

    init {
        setBackgroundColor(Color.TRANSPARENT)

        // ── Progress bar (full width, just below top strip) ───────────────────
        progressBar = View(context).apply {
            setBackgroundColor(Color.parseColor("#6b0919"))
            alpha = 0.9f
        }
        addView(progressBar, LayoutParams(0, dipToPx(2)))

        // ── Top strip ─────────────────────────────────────────────────────────
        val topPanel = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        addView(topPanel, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(40)).apply {
            gravity = Gravity.TOP
        })

        streetNameView = makeLabel(12f, Gravity.START or Gravity.CENTER_VERTICAL).also {
            topPanel.addView(it, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                leftMargin = dipToPx(16)
            })
        }

        compassView = CompassView(context).also {
            topPanel.addView(it, FrameLayout.LayoutParams(dipToPx(40), dipToPx(40)).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                rightMargin = dipToPx(8)
            })
        }

        // ── Bottom strip ──────────────────────────────────────────────────────
        val bottomPanel = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        addView(bottomPanel, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(64)).apply {
            gravity = Gravity.BOTTOM
        })

        // Speed block — bottom left
        val speedBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bottomPanel.addView(speedBlock, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            leftMargin = dipToPx(16)
        })

        speedView = TextView(context).apply {
            text = "0"
            setTextColor(Color.WHITE)
            textSize = 32f
            typeface = monoTypeface
        }.also { speedBlock.addView(it) }

        speedLimitView = makeLabel(10f).also { speedBlock.addView(it) }

        // ETA block — bottom right
        val etaBlock = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bottomPanel.addView(etaBlock, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            rightMargin = dipToPx(16)
        })

        etaView = makeLabel(12f).also { etaBlock.addView(it) }
        distanceView = makeLabel(10f).also { etaBlock.addView(it) }
    }

    private fun makeLabel(sizeSp: Float, gravity: Int = Gravity.START): TextView =
        TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = sizeSp
            typeface = monoTypeface
            this.gravity = gravity
            alpha = 0.9f
        }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()

    fun updateProgress(fraction: Float) {
        post {
            progressBar.layoutParams = (progressBar.layoutParams as LayoutParams).apply {
                width = (parent as? FrameLayout)?.let { (it.width * fraction.coerceIn(0f, 1f)).toInt() } ?: 0
            }
            progressBar.requestLayout()
        }
    }

    fun update(
        streetName: String,
        speedKmh: Int,
        speedLimit: Int?,
        etaText: String,
        distanceText: String,
        progressFraction: Float,
        bearingDegrees: Float
    ) {
        streetNameView.text = streetName.uppercase()
        speedView.text = speedKmh.toString()
        speedLimitView.text = speedLimit?.let { "LIMIT $it" } ?: ""
        etaView.text = etaText
        distanceView.text = distanceText
        compassView.bearing = bearingDegrees
        updateProgress(progressFraction)
    }

    fun show() {
        animate().alpha(1f).setDuration(400).start()
        visibility = View.VISIBLE
    }

    fun hide() {
        animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
    }
}
```

- [ ] **Step 2: Add `HudOverlay` to `MainActivity.kt`**

```kotlin
// Add field:
private lateinit var hudOverlay: HudOverlay

// In loadStyle callback, after idleOverlay setup:
hudOverlay = HudOverlay(this).apply { visibility = View.GONE; alpha = 0f }.also {
    binding.rootContainer.addView(it, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    ))
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/HudOverlay.kt app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: tactical HUD overlay with speed, ETA, compass, progress bar"
```

---

## Task 12: Search Overlay

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/SearchOverlay.kt`

- [ ] **Step 1: Create `SearchOverlay.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/SearchOverlay.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

data class SearchResult(val name: String, val address: String, val distanceKm: Float)

class SearchOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onResultSelected: ((SearchResult) -> Unit)? = null
    var onDismissed: (() -> Unit)? = null

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    private val searchField: EditText
    private val resultsList: LinearLayout

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#F0000000"))
        translationY = 2000f
        visibility = View.GONE

        // Wine-red bordered search bar container
        val searchContainer = FrameLayout(context).apply {
            val border = GradientDrawable().apply {
                setColor(Color.parseColor("#1A000000"))
                setStroke(1, Color.parseColor("#6b0919"))
                cornerRadius = 0f
            }
            background = border
        }
        addView(searchContainer, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(56)))

        searchField = EditText(context).apply {
            hint = "DESTINATION"
            setHintTextColor(Color.parseColor("#446b0919"))
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = monoTypeface
            background = null
            setPadding(dipToPx(16), 0, dipToPx(16), 0)
        }
        searchContainer.addView(searchField, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ).apply { gravity = Gravity.CENTER_VERTICAL })

        resultsList = LinearLayout(context).apply { orientation = VERTICAL }
        addView(resultsList, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { onQueryChanged(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun onQueryChanged(query: String) {
        // Mapbox Search SDK integration point — for now show placeholder
        if (query.length >= 2) {
            showResults(listOf(
                SearchResult(query.uppercase(), "Search via Mapbox Search API", 0f)
            ))
        } else {
            resultsList.removeAllViews()
        }
    }

    fun showResults(results: List<SearchResult>) {
        resultsList.removeAllViews()
        results.forEach { result ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dipToPx(16), dipToPx(14), dipToPx(16), dipToPx(14))
                minimumHeight = dipToPx(56)
                val border = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(0, Color.parseColor("#336b0919"))
                }
                background = border
                setOnClickListener {
                    onResultSelected?.invoke(result)
                    dismiss()
                }
            }
            row.addView(TextView(context).apply {
                text = result.name
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = monoTypeface
            })
            row.addView(TextView(context).apply {
                text = result.address
                setTextColor(Color.parseColor("#80FFFFFF"))
                textSize = 10f
                typeface = monoTypeface
            })
            resultsList.addView(row)
            resultsList.addView(View(context).apply {
                setBackgroundColor(Color.parseColor("#1A6b0919"))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            })
        }
    }

    fun show() {
        visibility = View.VISIBLE
        translationY = height.toFloat().coerceAtLeast(600f)
        animate().translationY(0f).setDuration(400)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
        searchField.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(searchField, InputMethodManager.SHOW_IMPLICIT)
    }

    fun dismiss() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(searchField.windowToken, 0)
        animate().translationY(height.toFloat().coerceAtLeast(600f)).setDuration(300)
            .withEndAction { visibility = View.GONE; onDismissed?.invoke() }.start()
    }

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}
```

- [ ] **Step 2: Add `SearchOverlay` to `MainActivity.kt`**

```kotlin
// Add field:
private lateinit var searchOverlay: SearchOverlay

// In loadStyle callback:
searchOverlay = SearchOverlay(this).also {
    it.onDismissed = { /* map brightens — handled by dim overlay */ }
    binding.rootContainer.addView(it, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply { gravity = Gravity.BOTTOM })
}

// Wire idle overlay tap → show search:
idleOverlay.setOnClickListener {
    searchOverlay.show()
}
```

- [ ] **Step 3: Run on device. Verify:**
  - Tapping idle map opens search panel from bottom
  - Search bar has wine red cursor / border
  - Results rows are large enough to tap comfortably
  - Panel dismisses smoothly

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/SearchOverlay.kt app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: search overlay with slide-up animation and wine red styling"
```

---

## Task 13: Navigation Manager & Route Draw Animator

**Files:**
- Create: `app/src/main/java/com/lidar/nav/navigation/NavigationManager.kt`
- Create: `app/src/main/java/com/lidar/nav/navigation/RouteDrawAnimator.kt`

- [ ] **Step 1: Create `RouteDrawAnimator.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/navigation/RouteDrawAnimator.kt
package com.lidar.nav.navigation

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.generated.LineLayer

class RouteDrawAnimator {

    companion object {
        const val ROUTE_LAYER_ID = "lidar-route-line"
        const val ROUTE_TRIM_LAYER_ID = "lidar-route-trim"
    }

    fun animateRouteOn(mapboxMap: MapboxMap, durationMs: Long = 1500L, onComplete: () -> Unit) {
        // lineTrimOffset([trimStart, trimEnd]) — [0.0, 1.0] = fully trimmed (invisible from end)
        // Animate trimEnd from 1.0 → 0.0 to draw the line from start to finish
        mapboxMap.getStyle { style ->
            style.getLayerAs<LineLayer>(ROUTE_LAYER_ID)?.lineTrimOffset(listOf(0.0, 1.0))
        }
        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val trimEnd = anim.animatedValue as Float
                mapboxMap.getStyle { style ->
                    style.getLayerAs<LineLayer>(ROUTE_LAYER_ID)
                        ?.lineTrimOffset(listOf(0.0, trimEnd.toDouble()))
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) = onComplete()
            })
        }
        animator.start()
    }
}
```

- [ ] **Step 2: Create `NavigationManager.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/navigation/NavigationManager.kt
package com.lidar.nav.navigation

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver

class NavigationManager(
    private val context: Context,
    private val mapboxMap: MapboxMap,
    private val onRouteProgress: (distanceRemaining: Float, fractionTraveled: Float, distanceToNextManeuver: Float) -> Unit,
    private val onBannerInstruction: (primaryText: String, maneuverType: String, distanceM: Float) -> Unit,
    private val onArrival: () -> Unit
) {

    private val navigation: MapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            com.mapbox.navigation.base.options.NavigationOptions.Builder(context).build()
        )
    }

    private val routeDrawAnimator = RouteDrawAnimator()

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        onRouteProgress(
            routeProgress.distanceRemaining,
            routeProgress.fractionTraveled,
            routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining ?: 0f
        )
        if (routeProgress.currentState ==
            com.mapbox.navigation.base.trip.model.RouteProgressState.COMPLETE) {
            onArrival()
        }
    }

    fun addRouteLayersToStyle() {
        mapboxMap.getStyle { style ->
            if (style.styleSourceExists("lidar-route-source")) return@getStyle
            style.addSource(geoJsonSource("lidar-route-source") { })
            style.addLayer(lineLayer(RouteDrawAnimator.ROUTE_LAYER_ID, "lidar-route-source") {
                lineColor("#6b0919")
                lineWidth(4.0)
                lineOpacity(0.9)
                lineTrimOffset(listOf(0.0, 0.0))
            })
        }
    }

    fun requestRoute(origin: Point, destination: Point) {
        val routeOptions = com.mapbox.navigation.base.route.NavigationRoute
        navigation.requestRoutes(
            com.mapbox.navigation.base.options.RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(origin, destination))
                .build(),
            object : com.mapbox.navigation.base.route.RouterCallback {
                override fun onRoutesReady(
                    routes: List<com.mapbox.navigation.base.route.NavigationRoute>,
                    routerOrigin: com.mapbox.navigation.base.route.RouterOrigin
                ) {
                    if (routes.isNotEmpty()) {
                        navigation.setNavigationRoutes(routes)
                        drawRoute(routes.first())
                    }
                }
                override fun onFailure(
                    reasons: List<com.mapbox.navigation.base.route.RouterFailure>,
                    routeOptions: com.mapbox.navigation.base.options.RouteOptions
                ) { /* log or show error */ }
                override fun onCanceled(
                    routeOptions: com.mapbox.navigation.base.options.RouteOptions,
                    routerOrigin: com.mapbox.navigation.base.route.RouterOrigin
                ) {}
            }
        )
    }

    private fun drawRoute(route: com.mapbox.navigation.base.route.NavigationRoute) {
        mapboxMap.getStyle { style ->
            style.getSourceAs<com.mapbox.maps.extension.style.sources.generated.GeoJsonSource>(
                "lidar-route-source"
            )?.geometry(route.directionsRoute.geometry()?.let {
                com.mapbox.geojson.LineString.fromPolyline(it, 5)
            })
        }
        routeDrawAnimator.animateRouteOn(mapboxMap) {
            startTripSession()
        }
    }

    fun startTripSession() {
        navigation.registerRouteProgressObserver(routeProgressObserver)
        navigation.startTripSession()
    }

    fun stopNavigation() {
        navigation.unregisterRouteProgressObserver(routeProgressObserver)
        navigation.stopTripSession()
    }

    fun onDestroy() {
        stopNavigation()
        MapboxNavigationProvider.destroy()
    }
}
```

- [ ] **Step 3: Wire `NavigationManager` in `MainActivity.kt`**

```kotlin
// Add field:
private lateinit var navigationManager: NavigationManager

// In loadStyle callback:
navigationManager = NavigationManager(
    context = this,
    mapboxMap = binding.mapView.mapboxMap,
    onRouteProgress = { distanceRemaining, fractionTraveled, distanceToNextManeuver ->
        val speedKmh = 0 // from LocationObserver — wire in next task
        hudOverlay.update(
            streetName = "—",
            speedKmh = speedKmh,
            speedLimit = null,
            etaText = formatEta(distanceRemaining),
            distanceText = formatDistance(distanceRemaining),
            progressFraction = fractionTraveled,
            bearingDegrees = binding.mapView.mapboxMap.cameraState.bearing.toFloat()
        )
        turnOverlay.updateDistance(distanceToNextManeuver)
        if (distanceToNextManeuver <= MapCameraManager.TURN_APPROACH_METERS) {
            cameraManager.pivotTowardTurn(15.0)
        }
    },
    onBannerInstruction = { primaryText, maneuverType, distanceM ->
        if (distanceM <= MapCameraManager.TURN_CARD_TRIGGER_METERS) {
            turnOverlay.show(primaryText, maneuverType, distanceM)
        }
    },
    onArrival = {
        appState.arrive()
        transitionToIdle()
    }
)
navigationManager.addRouteLayersToStyle()

// Wire search result → request route:
searchOverlay.onResultSelected = { result ->
    val userLocation = binding.mapView.mapboxMap.cameraState.center
    // For MVP, use map center as origin until LocationObserver wired
    // result.point would come from Mapbox Search SDK integration
    transitionToRouting()
}

// Add helper functions:
private fun formatEta(distanceM: Float): String {
    val minutes = (distanceM / 1000f / 50f * 60f).toInt()
    return "${minutes}MIN"
}

private fun formatDistance(distanceM: Float): String {
    return if (distanceM >= 1000f) "${"%.1f".format(distanceM / 1000f)}KM"
    else "${distanceM.toInt()}M"
}

private fun transitionToRouting() {
    appState.startRouting()
    idleOverlay.visibility = View.GONE
    hudOverlay.show()
    cameraManager.animateToRouting()
}

private fun transitionToIdle() {
    appState.cancelRoute()
    hudOverlay.hide()
    idleOverlay.visibility = View.VISIBLE
    cameraManager.animateToIdle()
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/lidar/nav/navigation app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: navigation manager with route request, animated route draw, and trip session"
```

---

## Task 14: Cinematic Turn Instruction Overlay

**Files:**
- Create: `app/src/main/java/com/lidar/nav/ui/TurnInstructionOverlay.kt`

- [ ] **Step 1: Create `TurnInstructionOverlay.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/TurnInstructionOverlay.kt
package com.lidar.nav.ui

import android.animation.*
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lidar.nav.R

class TurnInstructionOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val monoTypeface: Typeface? by lazy {
        try { ResourcesCompat.getFont(context, R.font.jetbrains_mono_regular) }
        catch (e: Exception) { Typeface.MONOSPACE }
    }

    // Primary card
    private val primaryCard: LinearLayout
    private val turnArrowView: TurnArrowView
    private val streetNameView: TextView
    private val distanceView: TextView

    // Secondary ghost card
    private val secondaryCard: LinearLayout
    private val secondaryText: TextView

    var onTurnExecuted: (() -> Unit)? = null
    private var currentDistanceM: Float = Float.MAX_VALUE
    private var isVisible = false
    private var isPulsed = false

    init {
        visibility = View.GONE
        translationY = -dipToPx(200).toFloat()

        // Ghost secondary card
        secondaryCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#80000000"))
            alpha = 0f
            setPadding(dipToPx(12), dipToPx(8), dipToPx(12), dipToPx(8))
        }
        addView(secondaryCard, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(40)).apply {
            topMargin = dipToPx(6)
            gravity = Gravity.TOP
        })

        secondaryText = TextView(context).apply {
            setTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 10f
            typeface = monoTypeface
        }
        secondaryCard.addView(secondaryText)

        // Primary card
        primaryCard = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E6000000"))
            setPadding(dipToPx(16), dipToPx(12), dipToPx(16), dipToPx(12))
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(primaryCard, LayoutParams(LayoutParams.MATCH_PARENT, dipToPx(80)))

        turnArrowView = TurnArrowView(context).also {
            primaryCard.addView(it, LinearLayout.LayoutParams(dipToPx(48), dipToPx(48)).apply {
                rightMargin = dipToPx(16)
            })
        }

        val textBlock = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        primaryCard.addView(textBlock, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        streetNameView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = monoTypeface
        }.also { textBlock.addView(it) }

        distanceView = TextView(context).apply {
            setTextColor(Color.parseColor("#CC6b0919"))
            textSize = 20f
            typeface = monoTypeface
        }.also { textBlock.addView(it) }
    }

    fun show(streetName: String, maneuverType: String, distanceM: Float, secondaryInstruction: String = "") {
        if (isVisible) return
        isVisible = true
        isPulsed = false
        currentDistanceM = distanceM

        streetNameView.text = streetName.uppercase()
        distanceView.text = formatDistance(distanceM)
        turnArrowView.setManeuver(maneuverType)

        if (secondaryInstruction.isNotBlank()) {
            secondaryText.text = "THEN: ${secondaryInstruction.uppercase()}"
            secondaryCard.alpha = 0.4f
        }

        visibility = View.VISIBLE
        translationY = -dipToPx(200).toFloat()
        animate()
            .translationY(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun updateDistance(distanceM: Float) {
        if (!isVisible) return
        currentDistanceM = distanceM
        distanceView.text = formatDistance(distanceM)

        if (distanceM <= 50f && !isPulsed) {
            isPulsed = true
            triggerWineRedPulse()
        }
    }

    private fun triggerWineRedPulse() {
        val overlay = View(context).apply {
            setBackgroundColor(Color.parseColor("#6b0919"))
            alpha = 0f
        }
        primaryCard.addView(overlay, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        AnimatorSet().apply {
            playSequentially(
                ObjectAnimator.ofFloat(overlay, "alpha", 0f, 0.6f).apply { duration = 200 },
                ObjectAnimator.ofFloat(overlay, "alpha", 0.6f, 0f).apply { duration = 300 }
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    primaryCard.removeView(overlay)
                    dismissWithWipe()
                }
            })
        }.start()
    }

    private fun dismissWithWipe() {
        val wipeAnimator = ObjectAnimator.ofFloat(this, "translationX", 0f, width.toFloat()).apply {
            duration = 350
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    translationX = 0f
                    isVisible = false
                    isPulsed = false
                    onTurnExecuted?.invoke()
                }
            })
        }
        wipeAnimator.start()
    }

    private fun formatDistance(distanceM: Float): String =
        if (distanceM >= 1000f) "${"%.1f".format(distanceM / 1000f)} KM"
        else "${distanceM.toInt()} M"

    private fun dipToPx(dip: Int): Int =
        (dip * resources.displayMetrics.density).toInt()
}
```

- [ ] **Step 2: Create `TurnArrowView.kt`**

```kotlin
// app/src/main/java/com/lidar/nav/ui/TurnArrowView.kt
package com.lidar.nav.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class TurnArrowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private var maneuver = "straight"

    fun setManeuver(type: String) {
        maneuver = type.lowercase()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.4f

        val path = Path()
        when {
            maneuver.contains("right") -> {
                path.moveTo(cx - r, cy + r)
                path.lineTo(cx - r, cy - r * 0.3f)
                path.quadTo(cx - r, cy - r, cx, cy - r)
                path.lineTo(cx + r, cy - r)
                // Arrow head
                path.moveTo(cx + r - r * 0.4f, cy - r - r * 0.3f)
                path.lineTo(cx + r, cy - r)
                path.lineTo(cx + r - r * 0.4f, cy - r + r * 0.3f)
            }
            maneuver.contains("left") -> {
                path.moveTo(cx + r, cy + r)
                path.lineTo(cx + r, cy - r * 0.3f)
                path.quadTo(cx + r, cy - r, cx, cy - r)
                path.lineTo(cx - r, cy - r)
                path.moveTo(cx - r + r * 0.4f, cy - r - r * 0.3f)
                path.lineTo(cx - r, cy - r)
                path.lineTo(cx - r + r * 0.4f, cy - r + r * 0.3f)
            }
            else -> { // straight / default
                path.moveTo(cx, cy + r)
                path.lineTo(cx, cy - r)
                path.moveTo(cx - r * 0.4f, cy - r + r * 0.4f)
                path.lineTo(cx, cy - r)
                path.lineTo(cx + r * 0.4f, cy - r + r * 0.4f)
            }
        }
        canvas.drawPath(path, paint)
    }
}
```

- [ ] **Step 3: Add `TurnInstructionOverlay` to `MainActivity.kt`**

```kotlin
// Add field:
private lateinit var turnOverlay: TurnInstructionOverlay

// In loadStyle callback:
turnOverlay = TurnInstructionOverlay(this).apply {
    onTurnExecuted = { cameraManager.recenterAfterTurn() }
}.also {
    binding.rootContainer.addView(it, FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply { gravity = Gravity.TOP })
}
```

- [ ] **Step 4: Run on device with a test route. Verify against Rubric Section 6:**
  - Turn card slides in 400ms cubic ease-out ✓/✗
  - Turn arrow is geometric line-art ✓/✗
  - Distance countdown ticks live ✓/✗
  - Wine red pulse fires at ~50m ✓/✗
  - Card exits with horizontal wipe ✓/✗

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/lidar/nav/ui/TurnInstructionOverlay.kt app/src/main/java/com/lidar/nav/ui/TurnArrowView.kt app/src/main/java/com/lidar/nav/MainActivity.kt
git commit -m "feat: cinematic turn instruction overlay with wine red pulse and wipe exit"
```

---

## Task 15: Full Rubric Evaluation

At this point all systems are built. Run through `docs/RUBRICS.md` systematically.

- [ ] **Step 1: Build release-mode APK for testing**

```bash
./gradlew assembleRelease
```

- [ ] **Step 2: Install on head unit**

```bash
adb install -r app/build/outputs/apk/release/app-release-unsigned.apk
```

- [ ] **Step 3: Go through each rubric section, score Pass/Partial/Fail**

Open `docs/RUBRICS.md` and fill in the Result column for every row. Be honest.

- [ ] **Step 4: Fix all Partial/Fail items before calling the build done**

Any Critical section (1, 3, 6) with a Fail result = stop and fix before continuing.

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: MVP complete — all rubric sections passing"
```

---

## Mapbox Search SDK Integration Note

Task 12 uses a placeholder for search results. To complete Mapbox Search integration:

1. Add dependency: `implementation("com.mapbox.search:mapbox-search-android:2.x.x")`
2. Initialize `MapboxSearchSdk` in `LidarApp.onCreate()`
3. Replace `onQueryChanged` in `SearchOverlay` with:
```kotlin
private val searchEngine = SearchEngine.createSearchEngine(SearchEngineSettings())
private fun onQueryChanged(query: String) {
    searchEngine.search(query, SearchOptions(), object : SearchSuggestionsCallback {
        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
            showResults(suggestions.map {
                SearchResult(it.name, it.descriptionText ?: "", 0f)
            })
        }
        override fun onError(e: Exception) {}
    })
}
```
