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
    }

    fun animateRouteOn(mapboxMap: MapboxMap, durationMs: Long = 1500L, onComplete: () -> Unit) {
        mapboxMap.getStyle { style ->
            style.getLayerAs<LineLayer>(ROUTE_LAYER_ID)?.lineTrimOffset(listOf(0.0, 1.0))
        }
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val trimEnd = (anim.animatedValue as Float).toDouble()
                mapboxMap.getStyle { style ->
                    style.getLayerAs<LineLayer>(ROUTE_LAYER_ID)
                        ?.lineTrimOffset(listOf(0.0, trimEnd))
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) = onComplete()
            })
            start()
        }
    }
}
