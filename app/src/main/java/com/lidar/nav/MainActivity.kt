package com.lidar.nav

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.lidar.nav.databinding.ActivityMainBinding
import com.lidar.nav.map.LidarStyleBuilder
import com.lidar.nav.map.MapCameraManager
import com.lidar.nav.navigation.NavigationManager
import com.lidar.nav.state.AppStateController
import com.lidar.nav.ui.HudOverlay
import com.lidar.nav.ui.IdleOverlay
import com.lidar.nav.ui.SearchOverlay
import com.lidar.nav.ui.TurnInstructionOverlay
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.ImageHolder
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo

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
    private lateinit var turnOverlay: TurnInstructionOverlay
    val appState = AppStateController()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) enableLocationComponent()
        else Toast.makeText(this, "Location permission required to navigate", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enforceFullscreen()
        binding.mapView.mapboxMap.loadStyle(LidarStyleBuilder.build()) {
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
            cameraManager = MapCameraManager(binding.mapView.mapboxMap)
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
            turnOverlay = TurnInstructionOverlay(this).apply {
                onTurnExecuted = { cameraManager.recenterAfterTurn() }
            }.also {
                binding.rootContainer.addView(
                    it,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.TOP }
                )
            }
            navigationManager = NavigationManager(
                context = this,
                mapboxMap = binding.mapView.mapboxMap,
                onRouteProgress = { distanceRemaining, fractionTraveled, distanceToNextManeuver ->
                    hudOverlay.update(
                        streetName = "—",
                        speedKmh = 0,
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
            searchOverlay.onResultSelected = { _ -> transitionToRouting() }
            ensureLocationPermission()
        }
    }

    private fun ensureLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            enableLocationComponent()
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun enableLocationComponent() {
        val crosshair = ImageHolder.from(createCrosshairBitmap())
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = false
            locationPuck = LocationPuck2D(
                topImage = crosshair,
                bearingImage = crosshair,
                shadowImage = null
            )
        }
        binding.mapView.location.addOnIndicatorPositionChangedListener(object :
            com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                binding.mapView.mapboxMap.setCamera(
                    CameraOptions.Builder().center(point).build()
                )
                binding.mapView.location.removeOnIndicatorPositionChangedListener(this)
            }
        })
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

    private fun createCrosshairBitmap(): Bitmap {
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val cx = size / 2f
        val cy = size / 2f
        val r = size * 0.35f
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawLine(cx, 0f, cx, cy - r, paint)
        canvas.drawLine(cx, cy + r, cx, size.toFloat(), paint)
        canvas.drawLine(0f, cy, cx - r, cy, paint)
        canvas.drawLine(cx + r, cy, size.toFloat(), cy, paint)
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
        if (::navigationManager.isInitialized) navigationManager.onDestroy()
        binding.mapView.onDestroy()
    }

    private fun formatEta(distanceM: Float): String {
        val minutes = (distanceM / 1000f / 50f * 60f).toInt()
        return "${minutes}MIN"
    }

    private fun formatDistance(distanceM: Float): String =
        if (distanceM >= 1000f) "${"%.1f".format(distanceM / 1000f)}KM"
        else "${distanceM.toInt()}M"

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
