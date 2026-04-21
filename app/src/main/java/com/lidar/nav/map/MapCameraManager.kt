package com.lidar.nav.map

import android.view.animation.DecelerateInterpolator
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class MapCameraManager(private val mapView: MapView) {

    companion object {
        const val IDLE_PITCH = 45.0
        const val ROUTING_PITCH = 55.0
        const val IDLE_ZOOM = 16.0
        const val FOLLOW_ZOOM = 17.5
        const val TURN_APPROACH_METERS = 50.0
        const val TURN_CARD_TRIGGER_METERS = 500.0
        private const val BEARING_THRESHOLD_DEG = 4.0
    }

    private val mapboxMap = mapView.mapboxMap
    private var followEnabled = false
    private var lastPoint: Point? = null
    private var lastBearing: Double = 0.0
    private var smoothedBearing: Double = 0.0
    private var appliedBearing: Double = 0.0
    private var initialCentered = false

    private val positionListener = OnIndicatorPositionChangedListener { point ->
        lastPoint = point
        if (!initialCentered) {
            initialCentered = true
            mapboxMap.setCamera(CameraOptions.Builder().center(point).build())
        }
        if (followEnabled) applyFollowCamera()
    }

    private val bearingListener = OnIndicatorBearingChangedListener { bearing ->
        lastBearing = bearing
        smoothedBearing = lerpAngle(smoothedBearing, bearing, 0.15)
        if (followEnabled) applyFollowCamera()
    }

    private val moveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            followEnabled = false
        }
        override fun onMove(detector: MoveGestureDetector): Boolean = false
        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }

    init {
        mapView.location.addOnIndicatorPositionChangedListener(positionListener)
        mapView.location.addOnIndicatorBearingChangedListener(bearingListener)
        mapView.gestures.addOnMoveListener(moveListener)
    }

    private fun applyFollowCamera() {
        val p = lastPoint ?: return
        val targetBearing = if (shortestAngleDiff(appliedBearing, smoothedBearing) >= BEARING_THRESHOLD_DEG) {
            appliedBearing = smoothedBearing
            smoothedBearing
        } else {
            appliedBearing
        }
        mapboxMap.easeTo(
            CameraOptions.Builder()
                .center(p)
                .bearing(targetBearing)
                .zoom(FOLLOW_ZOOM)
                .pitch(ROUTING_PITCH)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(350) }
        )
    }

    fun startFollow() {
        followEnabled = true
        smoothedBearing = lastBearing
        appliedBearing = lastBearing
        val p = lastPoint ?: return
        mapboxMap.easeTo(
            CameraOptions.Builder()
                .center(p)
                .bearing(lastBearing)
                .zoom(FOLLOW_ZOOM)
                .pitch(ROUTING_PITCH)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(700)
                interpolator(DecelerateInterpolator())
            }
        )
    }

    private fun shortestAngleDiff(a: Double, b: Double): Double {
        val d = ((b - a + 540.0) % 360.0) - 180.0
        return kotlin.math.abs(d)
    }

    private fun lerpAngle(from: Double, to: Double, t: Double): Double {
        val d = ((to - from + 540.0) % 360.0) - 180.0
        return (from + d * t + 360.0) % 360.0
    }

    fun stopFollow() {
        followEnabled = false
    }

    fun lastKnownLocation(): Point? = lastPoint

    fun animateToIdle() {
        followEnabled = false
        val builder = CameraOptions.Builder().pitch(IDLE_PITCH).zoom(IDLE_ZOOM).bearing(0.0)
        lastPoint?.let { builder.center(it) }
        mapboxMap.easeTo(
            builder.build(),
            MapAnimationOptions.mapAnimationOptions { duration(500) }
        )
    }

    fun animateToRouting() {
        startFollow()
    }

    fun pivotTowardTurn(bearingDelta: Double) {
        val currentBearing = mapboxMap.cameraState.bearing
        val targetBearing = currentBearing + bearingDelta
        mapboxMap.easeTo(
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
        startFollow()
    }
}
