package com.lidar.nav.map

import android.view.animation.DecelerateInterpolator
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo

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
        mapboxMap.easeTo(
            CameraOptions.Builder()
                .pitch(ROUTING_PITCH)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(600) }
        )
    }
}
