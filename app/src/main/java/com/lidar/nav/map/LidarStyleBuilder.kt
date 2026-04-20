package com.lidar.nav.map

import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.dsl.generated.get
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.layers.generated.backgroundLayer
import com.mapbox.maps.extension.style.layers.generated.fillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.sources.generated.rasterDemSource
import com.mapbox.maps.extension.style.sources.generated.vectorSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.extension.style.terrain.generated.terrain

object LidarStyleBuilder {

    const val WINE_RED = "#6b0919"
    const val LIDAR_WHITE = "#FFFFFF"
    const val LIDAR_BLACK = "#000000"

    const val ROUTE_SOURCE_ID = "lidar-route-source"
    const val ROUTE_LAYER_ID = "lidar-route-line"

    fun build() = style("mapbox://styles/mapbox/empty-v9") {

        // ── Sources ──────────────────────────────────────────────────────────

        +rasterDemSource("mapbox-dem") {
            url("mapbox://mapbox.mapbox-terrain-dem-v1")
            tileSize(512)
            maxzoom(14L)
        }

        +vectorSource("mapbox-terrain") {
            url("mapbox://mapbox.mapbox-terrain-v2")
        }

        +vectorSource("mapbox-streets") {
            url("mapbox://mapbox.mapbox-streets-v8")
        }

        // ── 3D Terrain ───────────────────────────────────────────────────────
        +terrain("mapbox-dem") {
            exaggeration(1.3)
        }

        // ── Background ───────────────────────────────────────────────────────
        +backgroundLayer("lidar-background") {
            backgroundColor(LIDAR_BLACK)
        }

        // ── Contour lines — the core LiDAR aesthetic ─────────────────────────
        +lineLayer("contour-minor", "mapbox-terrain") {
            sourceLayer("contour")
            filter(
                eq {
                    get("index")
                    literal(1)
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.15)
            lineWidth(0.5)
        }

        +lineLayer("contour-major", "mapbox-terrain") {
            sourceLayer("contour")
            filter(
                eq {
                    get("index")
                    literal(5)
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.35)
            lineWidth(0.8)
        }

        // ── Roads — thin white, brightness hierarchy ──────────────────────────
        +lineLayer("roads-residential", "mapbox-streets") {
            sourceLayer("road")
            filter(
                match {
                    get("class")
                    literal(listOf("street", "street_limited", "residential", "service"))
                    literal(true)
                    literal(false)
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.15)
            lineWidth(0.5)
        }

        +lineLayer("roads-arterial", "mapbox-streets") {
            sourceLayer("road")
            filter(
                match {
                    get("class")
                    literal(listOf("secondary", "tertiary"))
                    literal(true)
                    literal(false)
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.3)
            lineWidth(0.7)
        }

        +lineLayer("roads-primary", "mapbox-streets") {
            sourceLayer("road")
            filter(
                match {
                    get("class")
                    literal(listOf("primary", "trunk"))
                    literal(true)
                    literal(false)
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.5)
            lineWidth(1.0)
        }

        +lineLayer("roads-highway", "mapbox-streets") {
            sourceLayer("road")
            filter(
                eq {
                    get("class")
                    literal("motorway")
                }
            )
            lineColor(LIDAR_WHITE)
            lineOpacity(0.7)
            lineWidth(1.5)
        }

        // ── Buildings — LiDAR wireframe (edge lines, ghost faces) ────────────
        +fillExtrusionLayer("buildings", "mapbox-streets") {
            sourceLayer("building")
            fillExtrusionColor(LIDAR_WHITE)
            fillExtrusionOpacity(0.05)
            fillExtrusionHeight(get("height"))
            fillExtrusionBase(get("min_height"))
            fillExtrusionLineWidth(0.8)
            fillExtrusionEmissiveStrength(1.0)
        }

        // ── Road labels ───────────────────────────────────────────────────────
        +symbolLayer("road-labels", "mapbox-streets") {
            sourceLayer("road_label")
            textField(get("name"))
            textSize(9.0)
            textColor(LIDAR_WHITE)
            textOpacity(0.4)
            textMaxAngle(30.0)
            symbolPlacement(SymbolPlacement.LINE)
        }
    }
}
