package com.lidar.nav.search

import com.lidar.nav.BuildConfig
import com.mapbox.geojson.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GeocodingHit(val name: String, val address: String, val point: Point)

object SearchService {

    private const val ENDPOINT = "https://places.googleapis.com/v1/places:searchText"
    private const val FIELD_MASK =
        "places.displayName,places.formattedAddress,places.location"
    private const val BIAS_RADIUS_METERS = 50_000.0

    suspend fun forward(
        query: String,
        proximity: Point? = null,
        limit: Int = 10
    ): List<GeocodingHit> = withContext(Dispatchers.IO) {
        val key = BuildConfig.GOOGLE_PLACES_API_KEY
        if (key.isEmpty() || query.isBlank()) return@withContext emptyList()

        val body = JSONObject().apply {
            put("textQuery", query)
            put("pageSize", limit)
            proximity?.let {
                put(
                    "locationBias",
                    JSONObject().put(
                        "circle",
                        JSONObject()
                            .put(
                                "center",
                                JSONObject()
                                    .put("latitude", it.latitude())
                                    .put("longitude", it.longitude())
                            )
                            .put("radius", BIAS_RADIUS_METERS)
                    )
                )
            }
        }.toString()

        val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5000
            readTimeout = 5000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Goog-Api-Key", key)
            setRequestProperty("X-Goog-FieldMask", FIELD_MASK)
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            parsePlaces(conn.inputStream.bufferedReader().readText())
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    private fun parsePlaces(json: String): List<GeocodingHit> {
        val root = JSONObject(json)
        val places = root.optJSONArray("places") ?: return emptyList()
        val out = ArrayList<GeocodingHit>(places.length())
        for (i in 0 until places.length()) {
            val p = places.optJSONObject(i) ?: continue
            val loc = p.optJSONObject("location") ?: continue
            if (!loc.has("latitude") || !loc.has("longitude")) continue
            val name = p.optJSONObject("displayName")?.optString("text").orEmpty()
            val address = p.optString("formattedAddress")
            out += GeocodingHit(
                name = name.ifEmpty { address },
                address = address,
                point = Point.fromLngLat(loc.getDouble("longitude"), loc.getDouble("latitude"))
            )
        }
        return out
    }
}
