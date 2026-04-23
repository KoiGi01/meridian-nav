package com.lidar.nav.navigation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayerAs

class RouteDrawAnimator {

    companion object {
        const val ROUTE_LAYER_ID = "lidar-route-line"
        const val CASING_LAYER_ID = "lidar-route-casing"
        const val GLOW_LAYER_ID = "lidar-route-glow"
    }

    private val layerIds = listOf(GLOW_LAYER_ID, CASING_LAYER_ID, ROUTE_LAYER_ID)

    fun animateRouteOn(mapboxMap: MapboxMap, durationMs: Long = 1500L, onComplete: () -> Unit) {
        mapboxMap.getStyle { style ->
            layerIds.forEach { id ->
                style.getLayerAs<LineLayer>(id)?.lineTrimOffset(listOf(0.0, 1.0))
            }
        }
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val trimEnd = (anim.animatedValue as Float).toDouble()
                mapboxMap.getStyle { style ->
                    layerIds.forEach { id ->
                        style.getLayerAs<LineLayer>(id)?.lineTrimOffset(listOf(0.0, trimEnd))
                    }
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = onComplete()
            })
            start()
        }
    }
}
