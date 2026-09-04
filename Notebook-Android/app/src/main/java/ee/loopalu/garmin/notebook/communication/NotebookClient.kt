package ee.loopalu.garmin.notebook.communication

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NotebookClient(
    private val garminClient: GarminClientInterface,
    private val scope: CoroutineScope,
    private val responseTimeoutMs: Long = DEFAULT_RESPONSE_TIMEOUT_MS,
    private val onResponse: (NotebookResponse) -> Unit,
    private val onFailure: (String) -> Unit
) {
    companion object {
        const val DEFAULT_RESPONSE_TIMEOUT_MS = 20_000L
    }

    private var activeRequestId: String? = null
    private var timeoutJob: Job? = null

    val isRequestTransmissionOngoing: Boolean get() = activeRequestId != null

    fun sendRequest(request: Map<String, Any>): Boolean {
        if (isRequestTransmissionOngoing) {
            return false
        }
        val requestId = request["requestId"] as? String ?:
        throw IllegalArgumentException("Notebook request has no requestId.")

        activeRequestId = requestId
        scheduleTimeout(requestId)
        garminClient.sendRequest(request) { result ->
            scope.launch {
                if (activeRequestId != requestId) {
                    return@launch
                }
                result.onFailure {
                    fail(it.message ?: "Garmin message failed.")
                }
            }
        }
        return true
    }

    fun handleResponse(response: NotebookResponse): Boolean {
        if (response.requestId != activeRequestId) {
            return false
        }
        clearActiveRequest()
        onResponse(response)
        return true
    }

    fun cancel(message: String? = null) {
        if (!isRequestTransmissionOngoing) {
            return
        }
        clearActiveRequest()
        message?.let(onFailure)
    }

    private fun scheduleTimeout(requestId: String) {
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(responseTimeoutMs.milliseconds)
            if (activeRequestId == requestId) {
                fail("The watch did not respond to the request.")
            }
        }
    }

    private fun fail(message: String) {
        clearActiveRequest()
        onFailure(message)
    }

    private fun clearActiveRequest() {
        timeoutJob?.cancel()
        timeoutJob = null
        activeRequestId = null
    }
}
