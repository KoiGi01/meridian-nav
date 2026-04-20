package com.lidar.nav

import android.app.Application
import com.mapbox.common.MapboxOptions

class LidarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        check(BuildConfig.MAPBOX_ACCESS_TOKEN.isNotEmpty()) {
            "MAPBOX_ACCESS_TOKEN is not set. Add it to local.properties."
        }
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
