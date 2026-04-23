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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.lidar.nav.databinding.ActivityMainBinding
import com.lidar.nav.map.MapCameraManager
import com.lidar.nav.search.SearchService
import com.mapbox.bindgen.Value
import com.mapbox.maps.Style
import com.lidar.nav.navigation.NavigationManager
import com.lidar.nav.state.AppStateController
import com.lidar.nav.ui.HudOverlay
import com.lidar.nav.ui.IdleOverlay
import com.lidar.nav.ui.SearchOverlay
import com.lidar.nav.ui.SearchResult
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
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
    private lateinit var hudOverlay: HudOverlay
    private lateinit var searchOverlay: SearchOverlay
    val appState = AppStateController()
    private var currentSpeedMph: Int? = null
    private var currentSpeedLimitMph: Int? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) enableLocationComponent()
        
        if (grants[Manifest.permission.RECORD_AUDIO] == true) {
            startForegroundService(Intent(this, MusicReactivityService::class.java))
        }
    }

    private val audioIntensityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val intensity = intent?.getFloatExtra(MusicReactivityService.EXTRA_INTENSITY, 0f) ?: 0f
            binding.reactivityGrid.onBeat(intensity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enforceFullscreen()
        binding.mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            style.setStyleImportConfigProperty("basemap", "lightPreset", Value("night"))
            style.setStyleImportConfigProperty("basemap", "theme", Value("monochrome"))
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
            binding.mapView.logo.updateSettings {
                position = Gravity.BOTTOM or Gravity.END
                marginBottom = 24f
                marginRight = 24f
                marginLeft = 0f
            }
            binding.mapView.attribution.updateSettings {
                enabled = false
            }
            cameraManager = MapCameraManager(binding.mapView)
            cameraManager.animateToIdle()
            idleOverlay = IdleOverlay(this).also {
                binding.rootContainer.addView(
                    it,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            hudOverlay = HudOverlay(this).apply {
                visibility = View.GONE
                alpha = 0f
            }.also {
                binding.rootContainer.addView(
                    it,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
            }
            searchOverlay = SearchOverlay(this).also {
                binding.rootContainer.addView(
                    it,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.BOTTOM }
                )
            }
            idleOverlay.searchButton.setOnClickListener { searchOverlay.show() }
            hudOverlay.turnCard.onTurnExecuted = { cameraManager.recenterAfterTurn() }
            hudOverlay.onRecenter = { cameraManager.recenterAfterTurn() }
            hudOverlay.onZoomIn = { zoomBy(+1.0) }
            hudOverlay.onZoomOut = { zoomBy(-1.0) }
            hudOverlay.onSearch = { searchOverlay.show() }
            hudOverlay.onVoice = { }
            hudOverlay.onSettings = { }
            navigationManager = NavigationManager(
                context = this,
                mapboxMap = binding.mapView.mapboxMap,
                onRouteProgress = { distanceRemaining, durationRemaining, fractionTraveled, distanceToNextManeuver, speedLimitMph ->
                    currentSpeedLimitMph = speedLimitMph
                    hudOverlay.update(
                        distanceText = formatDistance(distanceRemaining),
                        durationText = formatDuration(durationRemaining),
                        arrivalText = formatArrivalClock(durationRemaining),
                        progressFraction = fractionTraveled,
                        speedMph = currentSpeedMph,
                        speedLimitMph = speedLimitMph
                    )
                    hudOverlay.turnCard.updateDistance(distanceToNextManeuver)
                    if (distanceToNextManeuver <= MapCameraManager.TURN_APPROACH_METERS) {
                        cameraManager.pivotTowardTurn(15.0)
                    }
                },
                onBannerInstruction = { primaryText, maneuverType, distanceM ->
                    if (distanceM <= MapCameraManager.TURN_CARD_TRIGGER_METERS) {
                        hudOverlay.turnCard.show(primaryText, maneuverType, distanceM)
                    }
                },
                onArrival = {
                    appState.arrive()
                    transitionToIdle()
                }
            )
            hudOverlay.onCancelRoute = {
                navigationManager.stopNavigation()
                transitionToIdle()
            }
            navigationManager.addRouteLayersToStyle()
            binding.mapView.mapboxMap.subscribeCameraChanged {
                hudOverlay.compass.setBearing(binding.mapView.mapboxMap.cameraState.bearing.toFloat())
            }
            searchOverlay.onQuery = { query -> runSearch(query) }
            searchOverlay.onResultSelected = { result -> startRouteTo(result) }
            ensurePermissions()
            
            LocalBroadcastManager.getInstance(this).registerReceiver(
                audioIntensityReceiver,
                IntentFilter(MusicReactivityService.ACTION_MUSIC_INTENSITY_UPDATE)
            )
        }
    }

    private fun ensurePermissions() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val audio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent()
        }
        if (audio == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(Intent(this, MusicReactivityService::class.java))
        }

        val toRequest = mutableListOf<String>()
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            toRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (audio != PackageManager.PERMISSION_GRANTED) {
            toRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    private fun enableLocationComponent() {
        val chevron = ImageHolder.from(createChevronBitmap())
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = false
            puckBearingEnabled = true
            puckBearing = PuckBearing.COURSE
            locationPuck = LocationPuck2D(
                topImage = null,
                bearingImage = chevron,
                shadowImage = null
            )
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
                it.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = IMMERSIVE_FLAGS
        }
    }

    private fun createChevronBitmap(): Bitmap {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val path = Path().apply {
            moveTo(size / 2f, size * 0.12f)
            lineTo(size * 0.82f, size * 0.86f)
            lineTo(size / 2f, size * 0.68f)
            lineTo(size * 0.18f, size * 0.86f)
            close()
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, outline)
        return bitmap
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(audioIntensityReceiver)
        stopService(Intent(this, MusicReactivityService::class.java))
        if (::navigationManager.isInitialized) navigationManager.onDestroy()
        binding.mapView.onDestroy()
    }

    private fun formatDuration(durationSec: Double): String {
        val minutes = (durationSec / 60.0).toInt()
        return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}min"
        else "${minutes} min"
    }

    private fun formatDistance(distanceM: Float): String {
        val miles = distanceM / 1609.344f
        return if (miles >= 0.1f) "%.1f mi".format(miles)
        else "${(distanceM * 3.28084f).toInt()} ft"
    }

    private fun formatArrivalClock(durationSec: Double): String =
        LocalTime.now().plusSeconds(durationSec.toLong())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    private fun zoomBy(delta: Double) {
        val currentZoom = binding.mapView.mapboxMap.cameraState.zoom
        binding.mapView.mapboxMap.easeTo(
            CameraOptions.Builder().zoom(currentZoom + delta).build(),
            MapAnimationOptions.mapAnimationOptions { duration(250L) }
        )
    }

    private fun runSearch(query: String) {
        val proximity = cameraManager.lastKnownLocation()
        lifecycleScope.launch {
            val hits = SearchService.forward(query, proximity)
            searchOverlay.showResults(hits.map { SearchResult(it.name, it.address, it.point) })
        }
    }

    private fun startRouteTo(result: SearchResult) {
        val origin = cameraManager.lastKnownLocation()
        if (origin == null) {
            Toast.makeText(this, "Waiting for location fix", Toast.LENGTH_SHORT).show()
            return
        }
        navigationManager.requestRoute(origin, result.point)
        transitionToRouting()
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
}
