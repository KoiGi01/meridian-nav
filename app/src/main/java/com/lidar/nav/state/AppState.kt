package com.lidar.nav.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class AppState {
    object Idle : AppState()
    data class Routing(val destinationName: String = "") : AppState()
}

class AppStateController {
    private val _state = MutableStateFlow<AppState>(AppState.Idle)
    val state: StateFlow<AppState> = _state

    fun startRouting(destinationName: String = "") {
        _state.value = AppState.Routing(destinationName)
    }

    fun cancelRoute() {
        _state.value = AppState.Idle
    }

    fun arrive() {
        _state.value = AppState.Idle
    }
}
