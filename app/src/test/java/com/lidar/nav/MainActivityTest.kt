package com.lidar.nav

import android.view.View
import org.junit.Test
import org.junit.Assert.*

class MainActivityTest {
    @Test
    fun `immersive flags include IMMERSIVE_STICKY`() {
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY != 0
        )
    }

    @Test
    fun `immersive flags include HIDE_NAVIGATION`() {
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0
        )
    }

    @Test
    fun `immersive flags include FULLSCREEN`() {
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
        )
    }

    @Test
    fun `immersive flags include all layout flags`() {
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_LAYOUT_STABLE != 0
        )
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN != 0
        )
        assertTrue(
            MainActivity.IMMERSIVE_FLAGS and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION != 0
        )
    }
}
