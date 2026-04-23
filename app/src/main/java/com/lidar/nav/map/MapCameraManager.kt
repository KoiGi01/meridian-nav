package com.lidar.nav.map

import android.util.Log
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
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapCameraManager(private val mapView: MapView) {

    companion object {
        private const val TAG = "CameraMgr"
        const val IDLE_PITCH = 45.0
        const val ROUTING_PITCH = 55.0
        const val IDLE_ZOOM = 16.0
        const val FOLLOW_ZOOM = 17.5
        const val TURN_APPROACH_METERS = 50.0
        const val TURN_CARD_TRIGGER_METERS = 500.0

        // Jitter suppression
        private const val BEARING_THRESHOLD_DEG = 6.0
        private const val MOVE_THRESHOLD_M = 3.0
        private const val STILL_TIMEOUT_MS = 1500L
        private const val EARTH_RADIUS_M = 6_371_000.0
    }

    private val mapboxMap = mapView.mapboxMap
    private var followEnabled = false
    private var lastPoint: Point? = null
    private var lastMovedPoint: Point? = null
    private var lastMoveTime: Long = 0L
    private var smoothedBearing: Double = 0.0
    private var appliedBearing: Double = 0.0
    private var initialCentered = false

    private val positionListener = OnIndicatorPositionChangedListener { point ->
        val prev = lastPoint
        lastPoint = point
        val dist = if (prev != null) haversineMeters(prev, point) else 0.0
        if (prev == null || dist >= MOVE_THRESHOLD_M) {
            lastMovedPoint = point
            lastMoveTime = System.currentTimeMillis()
            if (prev != null) {
                val derived = bearingBetween(prev, point)
                smoothedBearing = lerpAngle(smoothedBearing, derived, 0.35)
                Log.d(TAG, "POS moved dist=%.2fm derivedBearing=%.1f smoothed=%.1f".format(dist, derived, smoothedBearing))
            }
        } else {
            Log.d(TAG, "POS tick dist=%.2fm (sub-threshold, ignored for bearing)".format(dist))
        }
        if (!initialCentered) {
            initialCentered = true
            mapboxMap.setCamera(CameraOptions.Builder().center(point).build())
        }
        if (followEnabled) applyFollowCamera()
    }

    private val bearingListener = OnIndicatorBearingChangedListener { bearing ->
        val moving = isMoving()
        Log.d(TAG, "BEARING raw=%.1f moving=%b smoothed=%.1f applied=%.1f".format(bearing, moving, smoothedBearing, appliedBearing))
        if (moving) {
            smoothedBearing = lerpAngle(smoothedBearing, bearing, 0.15)
            if (followEnabled) applyFollowCamera()
        }
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

    private fun isMoving(): Boolean =
        System.currentTimeMillis() - lastMoveTime < STILL_TIMEOUT_MS

    private fun applyFollowCamera() {
        val p = lastPoint ?: return
        val moving = isMoving()
        val diff = shortestAngleDiff(appliedBearing, smoothedBearing)
        val willUpdate = moving && diff >= BEARING_THRESHOLD_DEG
        val targetBearing = if (willUpdate) {
            appliedBearing = smoothedBearing
            smoothedBearing
        } else {
            appliedBearing
        }
        Log.d(TAG, "APPLY moving=%b diff=%.1f° updated=%b target=%.1f".format(moving, diff, willUpdate, targetBearing))
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
        val startBearing = if (isMoving()) smoothedBearing else mapboxMap.cameraState.bearing
        appliedBearing = startBearing
        val p = lastPoint ?: return
        mapboxMap.easeTo(
            CameraOptions.Builder()
                .center(p)
                .bearing(startBearing)
                .zoom(FOLLOW_ZOOM)
                .pitch(ROUTING_PITCH)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(700)
                interpolator(DecelerateInterpolator())
            }
        )
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

    private fun shortestAngleDiff(a: Double, b: Double): Double {
        val d = ((b - a + 540.0) % 360.0) - 180.0
        return abs(d)
    }

    private fun lerpAngle(from: Double, to: Double, t: Double): Double {
        val d = ((to - from + 540.0) % 360.0) - 180.0
        return (from + d * t + 360.0) % 360.0
    }

    private fun haversineMeters(a: Point, b: Point): Double {
        val lat1 = Math.toRadians(a.latitude())
        val lat2 = Math.toRadians(b.latitude())
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.longitude() - a.longitude())
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun bearingBetween(from: Point, to: Point): Double {
        val lat1 = Math.toRadians(from.latitude())
        val lat2 = Math.toRadians(to.latitude())
        val dLon = Math.toRadians(to.longitude() - from.longitude())
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }
}
