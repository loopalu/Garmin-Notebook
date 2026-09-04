package ee.loopalu.garmin.notebook.testsupport

import ee.loopalu.garmin.notebook.communication.GarminClientInterface
import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeGarminClientInterface : GarminClientInterface {
    val mutableConnection = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Initializing)
    override val connection: StateFlow<GarminConnectionState> = mutableConnection
    private val mutableMessages = MutableSharedFlow<Map<*, *>>(extraBufferCapacity = 1)
    override val messages: Flow<Map<*, *>> = mutableMessages

    val sentRequests = mutableListOf<Map<String, Any>>()
    private val sendCallbacks = mutableListOf<(Result<Unit>) -> Unit>()

    override fun initialize() = Unit
    override fun refreshDevices() = Unit
    override fun connect(deviceId: Long) = Unit
    override fun shutdown() = Unit

    override fun sendRequest(request: Map<String, Any>, onResult: (Result<Unit>) -> Unit) {
        sentRequests += request
        sendCallbacks += onResult
    }

    fun completeLastSend(result: Result<Unit> = Result.success(Unit)) {
        sendCallbacks.last()(result)
    }

    fun emitMessage(message: Map<*, *>) {
        check(mutableMessages.tryEmit(message))
    }
}
