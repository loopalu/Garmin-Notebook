package ee.loopalu.garmin.notebook.communication

import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GarminClientInterface {
    val connection: StateFlow<GarminConnectionState>
    val messages: Flow<Map<*, *>>

    fun initialize()
    fun refreshDevices()
    fun connect(deviceId: Long)
    fun sendRequest(request: Map<String, Any>, onResult: (Result<Unit>) -> Unit)
    fun shutdown()
}
