package com.lidar.nav.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapCameraManagerTest {

    @Test
    fun `idle pitch is 45 degrees`() {
        assertEquals(45.0, MapCameraManager.IDLE_PITCH, 0.001)
    }

    @Test
    fun `routing pitch is 55 degrees`() {
        assertEquals(55.0, MapCameraManager.ROUTING_PITCH, 0.001)
    }

    @Test
    fun `turn approach distance is 50 meters`() {
        assertEquals(50.0, MapCameraManager.TURN_APPROACH_METERS, 0.001)
    }

    @Test
    fun `turn card trigger distance is 500 meters`() {
        assertEquals(500.0, MapCameraManager.TURN_CARD_TRIGGER_METERS, 0.001)
    }
}
