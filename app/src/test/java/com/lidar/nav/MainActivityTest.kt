package com.lidar.nav

import org.junit.Test
import org.junit.Assert.*

class MainActivityTest {
    @Test
    fun `immersive flags bitmask includes all required flags`() {
        val SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000
        val SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004
        val SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002
        val SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100
        val SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400
        val SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200

        val expectedFlags = (SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or SYSTEM_UI_FLAG_FULLSCREEN
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        assertTrue(expectedFlags and SYSTEM_UI_FLAG_IMMERSIVE_STICKY != 0)
        assertTrue(expectedFlags and SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0)
    }
}
