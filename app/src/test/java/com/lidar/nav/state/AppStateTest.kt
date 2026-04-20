package com.lidar.nav.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppStateTest {

    @Test
    fun `initial state is IDLE`() = runTest {
        val controller = AppStateController()
        assertEquals(AppState.Idle, controller.state.first())
    }

    @Test
    fun `startRouting transitions to ROUTING`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        assertEquals(AppState.Routing::class, controller.state.first()::class)
    }

    @Test
    fun `cancelRoute transitions back to IDLE`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        controller.cancelRoute()
        assertEquals(AppState.Idle, controller.state.first())
    }

    @Test
    fun `arrive transitions back to IDLE`() = runTest {
        val controller = AppStateController()
        controller.startRouting()
        controller.arrive()
        assertEquals(AppState.Idle, controller.state.first())
    }
}
