package com.lidar.nav.navigation

import android.content.Context
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.api.directions.v5.models.RouteOptions

class NavigationManager(
    private val context: Context,
    private val mapboxMap: MapboxMap,
    private val onRouteProgress: (distanceRemaining: Float, fractionTraveled: Float, distanceToNextManeuver: Float) -> Unit,
    private val onBannerInstruction: (primaryText: String, maneuverType: String, distanceM: Float) -> Unit,
    private val onArrival: () -> Unit
) {

    companion object {
        const val ROUTE_SOURCE_ID = "lidar-route-source"
    }

    private val navigation: MapboxNavigation by lazy {
        MapboxNavigationProvider.create(NavigationOptions.Builder(context).build())
    }

    private val routeDrawAnimator = RouteDrawAnimator()

    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        onRouteProgress(
            routeProgress.distanceRemaining,
            routeProgress.fractionTraveled,
            routeProgress.currentLegProgress?.currentStepProgress?.distanceRemaining ?: 0f
        )
        if (routeProgress.currentState == RouteProgressState.COMPLETE) {
            onArrival()
        }
    }

    fun addRouteLayersToStyle() {
        mapboxMap.getStyle { style ->
            if (style.styleSourceExists(ROUTE_SOURCE_ID)) return@getStyle
            style.addSource(geoJsonSource(ROUTE_SOURCE_ID) { })
            style.addLayer(lineLayer(RouteDrawAnimator.ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                lineColor("#6b0919")
                lineWidth(4.0)
                lineOpacity(0.9)
                lineTrimOffset(listOf(0.0, 0.0))
            })
        }
    }

    fun requestRoute(origin: Point, destination: Point) {
        navigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(origin, destination))
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: String
                ) {
                    if (routes.isNotEmpty()) {
                        navigation.setNavigationRoutes(routes)
                        drawRoute(routes.first())
                    }
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {}

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: String
                ) {}
            }
        )
    }

    private fun drawRoute(route: NavigationRoute) {
        mapboxMap.getStyle { style ->
            val geometry = route.directionsRoute.geometry()
            if (geometry != null) {
                style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)
                    ?.geometry(LineString.fromPolyline(geometry, 5))
            }
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
