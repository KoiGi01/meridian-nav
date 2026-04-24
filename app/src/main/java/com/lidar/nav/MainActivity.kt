package com.lidar.nav

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lidar.nav.databinding.ActivityMainBinding
import com.lidar.nav.map.MapCameraManager
import com.lidar.nav.search.SearchService
import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.lidar.nav.navigation.NavigationManager
import com.lidar.nav.state.AppStateController
import com.lidar.nav.ui.IdleOverlay
import com.lidar.nav.ui.MapControlsView
import com.lidar.nav.ui.SearchOverlay
import com.lidar.nav.ui.SearchResult
import com.lidar.nav.ui.SpeedBubble
import com.lidar.nav.ui.TripSheet
import com.lidar.nav.ui.TurnCardView
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    companion object {
        @Suppress("DEPRECATION")
        const val IMMERSIVE_FLAGS = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: MapCameraManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var idleOverlay: IdleOverlay
    private lateinit var turnCard: TurnCardView
    private lateinit var speedBubble: SpeedBubble
    private lateinit var tripSheet: TripSheet
    private lateinit var mapControls: MapControlsView
    private lateinit var searchOverlay: SearchOverlay
    val appState = AppStateController()

    private var convoyActive = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) enableLocationComponent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enforceFullscreen()

        binding.mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            // Cyan/teal map palette
            style.setStyleImportConfigProperty("basemap", "lightPreset", Value("night"))
            style.setStyleImportConfigProperty("basemap", "theme", Value("default"))
            style.setStyleImportConfigProperty("basemap", "show3dObjects", Value(true))
            style.setStyleImportConfigProperty("basemap", "showRoadLabels", Value(true))
            style.setStyleImportConfigProperty("basemap", "showPointOfInterestLabels", Value(false))
            style.setStyleImportConfigProperty("basemap", "showTransitLabels", Value(false))

            binding.mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(-89.5926, 20.9674))
                    .pitch(45.0)
                    .zoom(16.0)
                    .build()
            )

            val d = resources.displayMetrics.density

            binding.mapView.logo.updateSettings {
                position = Gravity.BOTTOM or Gravity.END
                marginBottom = 16f
                marginRight = 16f
            }
            binding.mapView.attribution.updateSettings { enabled = false }

            cameraManager = MapCameraManager(binding.mapView)
            cameraManager.animateToIdle()

            // ── IDLE OVERLAY ──────────────────────────────────────────────────────
            idleOverlay = IdleOverlay(this).also {
                binding.rootContainer.addView(it, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ))
            }

            // ── TURN CARD — top-left floating ─────────────────────────────────────
            turnCard = TurnCardView(this).also {
                binding.rootContainer.addView(it, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    leftMargin = (20 * d).toInt()
                    topMargin = (20 * d).toInt()
                    width = (320 * d).toInt()
                })
            }

            // ── SPEED BUBBLE — bottom-left ────────────────────────────────────────
            speedBubble = SpeedBubble(this).also {
                binding.rootContainer.addView(it, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    leftMargin = (20 * d).toInt()
                    bottomMargin = (100 * d).toInt()
                })
            }

            // ── MAP CONTROLS — right edge, center ────────────────────────────────
            mapControls = MapControlsView(this).apply {
                onRecenter = { cameraManager.recenterAfterTurn() }
                onZoomIn = { zoomBy(+1.0) }
                onZoomOut = { zoomBy(-1.0) }
            }
            binding.rootContainer.addView(mapControls, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                rightMargin = (16 * d).toInt()
            })

            // ── TRIP SHEET — bottom, slides up when navigating ────────────────────
            tripSheet = TripSheet(this).apply {
                onCancel = {
                    navigationManager.stopNavigation()
                    transitionToIdle()
                }
            }
            binding.rootContainer.addView(tripSheet, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM })

            // ── SEARCH OVERLAY ────────────────────────────────────────────────────
            searchOverlay = SearchOverlay(this).also {
                binding.rootContainer.addView(it, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.BOTTOM })
            }

            // ── WIRING ────────────────────────────────────────────────────────────
            idleOverlay.searchButton.setOnClickListener { searchOverlay.show() }

            idleOverlay.onPairMesh = {
                Toast.makeText(this, "Scanning for Meshtastic device…", Toast.LENGTH_SHORT).show()
            }
            idleOverlay.onSettings = {
                Toast.makeText(this, "Settings — coming soon", Toast.LENGTH_SHORT).show()
            }
            idleOverlay.onConvoyToggle = { toggleConvoyMode() }

            addConvoyMapLayers()

            navigationManager = NavigationManager(
                context = this,
                mapboxMap = binding.mapView.mapboxMap,
                onRouteProgress = { distanceRemaining, durationRemaining, fractionTraveled, distanceToNextManeuver, _ ->
                    tripSheet.update(
                        distanceText = formatDistance(distanceRemaining),
                        durationText = formatDuration(durationRemaining),
                        arrivalText = formatArrivalClock(durationRemaining),
                        fraction = fractionTraveled
                    )
                    turnCard.updateDistance(distanceToNextManeuver)
                    if (distanceToNextManeuver <= MapCameraManager.TURN_APPROACH_METERS) {
                        cameraManager.pivotTowardTurn(15.0)
                    }
                },
                onBannerInstruction = { primaryText, maneuverType, distanceM ->
                    turnCard.showManeuver(primaryText, maneuverType, distanceM)
                },
                onArrival = {
                    appState.arrive()
                    transitionToIdle()
                }
            )

            navigationManager.addRouteLayersToStyle()
            searchOverlay.onQuery = { query -> runSearch(query) }
            searchOverlay.onResultSelected = { result -> startRouteTo(result) }
            ensurePermissions()
        }
    }

    private fun ensurePermissions() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun enableLocationComponent() {
        val chevron = ImageHolder.from(createChevronBitmap())
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = false
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            locationPuck = LocationPuck2D(topImage = null, bearingImage = chevron, shadowImage = null)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enforceFullscreen()
    }

    private fun enforceFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = IMMERSIVE_FLAGS
        }
    }

    private fun createChevronBitmap(): Bitmap {
        val size = 72
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 0, 30, 50)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val path = Path().apply {
            moveTo(size / 2f, size * 0.10f)
            lineTo(size * 0.84f, size * 0.88f)
            lineTo(size / 2f, size * 0.66f)
            lineTo(size * 0.16f, size * 0.88f)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
        return bitmap
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onDestroy() {
        super.onDestroy()
        if (::navigationManager.isInitialized) navigationManager.onDestroy()
        binding.mapView.onDestroy()
    }

    private fun formatDuration(sec: Double): String {
        val m = (sec / 60.0).toInt()
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
    }

    private fun formatDistance(m: Float): String {
        val miles = m / 1609.344f
        return if (miles >= 0.1f) "%.1f mi".format(miles) else "${(m * 3.28084f).toInt()} ft"
    }

    private fun formatArrivalClock(sec: Double): String =
        LocalTime.now().plusSeconds(sec.toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun zoomBy(delta: Double) {
        val z = binding.mapView.mapboxMap.cameraState.zoom
        binding.mapView.mapboxMap.easeTo(
            CameraOptions.Builder().zoom(z + delta).build(),
            MapAnimationOptions.mapAnimationOptions { duration(250L) }
        )
    }

    private fun runSearch(query: String) {
        lifecycleScope.launch {
            val hits = SearchService.forward(query, cameraManager.lastKnownLocation())
            searchOverlay.showResults(hits.map { SearchResult(it.name, it.address, it.point) })
        }
    }

    private fun startRouteTo(result: SearchResult) {
        val origin = cameraManager.lastKnownLocation() ?: run {
            Toast.makeText(this, "Waiting for location fix", Toast.LENGTH_SHORT).show()
            return
        }
        navigationManager.requestRoute(origin, result.point)
        transitionToRouting()
    }

    private fun transitionToRouting() {
        appState.startRouting()
        idleOverlay.visibility = View.GONE
        speedBubble.visibility = if (convoyActive) View.GONE else View.VISIBLE
        if (convoyActive) tripSheet.showConvoyStrip()
        tripSheet.slideUp()
        cameraManager.animateToRouting()
    }

    private fun transitionToIdle() {
        appState.cancelRoute()
        turnCard.reset()
        tripSheet.slideDown()
        speedBubble.visibility = View.GONE
        idleOverlay.visibility = View.VISIBLE
        cameraManager.animateToIdle()
    }

    private fun toggleConvoyMode() {
        convoyActive = !convoyActive
        if (convoyActive) {
            idleOverlay.setMeshStatus("MESH · 3 UNITS", true)
            Toast.makeText(this, "CONVOY MODE ACTIVE", Toast.LENGTH_SHORT).show()
        } else {
            idleOverlay.setMeshStatus("MESH · OFFLINE", false)
            tripSheet.hideConvoyStrip()
            Toast.makeText(this, "CONVOY MODE OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addConvoyMapLayers() {
        val style = binding.mapView.mapboxMap.style ?: return

        // Stub unit positions near the initial camera center (-89.5926, 20.9674)
        val unitBlue = Feature.fromGeometry(Point.fromLngLat(-89.5905, 20.9688))
        val unitAmber = Feature.fromGeometry(Point.fromLngLat(-89.5940, 20.9660))

        style.addSource(geoJsonSource("convoy-blue-src") {
            featureCollection(FeatureCollection.fromFeatures(listOf(unitBlue)))
        })
        style.addSource(geoJsonSource("convoy-amber-src") {
            featureCollection(FeatureCollection.fromFeatures(listOf(unitAmber)))
        })

        style.addLayer(circleLayer("convoy-blue", "convoy-blue-src") {
            circleRadius(10.0)
            circleColor("#4A9EFF")
            circleOpacity(0.92)
            circleStrokeWidth(2.0)
            circleStrokeColor("#1A4A9EFF")
        })
        style.addLayer(circleLayer("convoy-amber", "convoy-amber-src") {
            circleRadius(10.0)
            circleColor("#FFB300")
            circleOpacity(0.92)
            circleStrokeWidth(2.0)
            circleStrokeColor("#1AFFB300")
        })
    }
}
