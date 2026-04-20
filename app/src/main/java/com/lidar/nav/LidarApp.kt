package com.lidar.nav

import android.app.Application
import com.mapbox.maps.MapboxOptions

class LidarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapboxOptions.accessToken = BuildConfig.MAPBOX_ACCESS_TOKEN
    }
}
