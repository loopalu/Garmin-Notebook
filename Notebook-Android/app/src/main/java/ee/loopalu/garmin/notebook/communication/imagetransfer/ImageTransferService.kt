package ee.loopalu.garmin.notebook.communication.imagetransfer

import ee.loopalu.garmin.notebook.communication.GarminClientInterface
import ee.loopalu.garmin.notebook.communication.NotebookResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ImageTransferService(
    private val garminClient: GarminClientInterface,
    private val coroutineScope: CoroutineScope,
    private val responseTimeoutMs: Long = DEFAULT_RESPONSE_TIMEOUT_MS,
    private val maximumAttempts: Int = DEFAULT_MAXIMUM_ATTEMPTS,
    private val onProgress: (Int) -> Unit,
    private val onCompleted: (NotebookResponse) -> Unit,
    private val onFailure: (String) -> Unit
) {
    companion object {
        const val DEFAULT_RESPONSE_TIMEOUT_MS = 20_000L
        const val DEFAULT_MAXIMUM_ATTEMPTS = 3
    }

    private class ActiveTransfer(val plan: ImageTransferPlan) {
        var step = 0
        var request = plan.createImageTransferRequest(step)
        var numberOfImageTransmissionAttempts = 0
        val requestIds = mutableSetOf<String>()

        val requestId: String get() = request.getValue("requestId") as String

        fun startNextRequest() {
            step += 1
            numberOfImageTransmissionAttempts = 0
            request = plan.createImageTransferRequest(step)
        }
    }

    private var activeTransfer: ActiveTransfer? = null
    private var timeoutJob: Job? = null
    private var completedRequestIds = emptySet<String>()

    val isImageTransferOngoing: Boolean get() = activeTransfer != null

    fun startImageTransfer(transferPlan: ImageTransferPlan): Boolean {
        if (isImageTransferOngoing) {
            return false
        }

        cancelCurrentTimeout()

        completedRequestIds = emptySet()
        activeTransfer = ActiveTransfer(transferPlan)
        onProgress(0)
        sendCurrentRequest()
        return true
    }

    fun handleWatchResponse(response: NotebookResponse): Boolean {
        if (response.requestId in completedRequestIds) {
            return true
        }
        val transfer = activeTransfer ?: return false

        if (response.requestId != transfer.requestId) {
            return response.requestId in transfer.requestIds
        }

        cancelCurrentTimeout()

        if (!response.isSuccess) {
            failTheTransfer(response.error ?: "The watch rejected the image data.")
            return true
        }

        val completedStep = transfer.step
        onProgress(transfer.plan.calculateImageTransferPercentage(completedStep))
        if (completedStep == transfer.plan.stepCount - 1) {
            completedRequestIds = transfer.requestIds.toSet()
            activeTransfer = null
            onCompleted(response)
        } else {
            transfer.startNextRequest()
            sendCurrentRequest()
        }
        return true
    }

    fun stopActiveImageTransfer(message: String? = null) {
        if (!isImageTransferOngoing) {
            return
        }

        cancelCurrentTimeout()

        activeTransfer?.let {
            completedRequestIds = it.requestIds.toSet()
        }
        activeTransfer = null
        message?.let(onFailure)
    }

    private fun sendCurrentRequest() {
        val transfer = activeTransfer ?: return
        if (transfer.numberOfImageTransmissionAttempts >= maximumAttempts) {
            failTheTransfer("Image transfer failed after $maximumAttempts attempts.")
            return
        }

        transfer.numberOfImageTransmissionAttempts += 1
        val requestId = transfer.requestId
        transfer.requestIds += requestId

        scheduleTimeoutForRequest(requestId)

        garminClient.sendRequest(transfer.request) { response ->
            coroutineScope.launch {
                val currentTransfer = activeTransfer
                if ((currentTransfer == null) || (currentTransfer.requestId != requestId)) {
                    return@launch
                }
                response.onFailure {
                    retry(requestId, it.message ?: "Garmin rejected the image transfer.")
                }
            }
        }
    }

    private fun scheduleTimeoutForRequest(requestId: String) {
        cancelCurrentTimeout()
        timeoutJob = coroutineScope.launch {
            setTimeout(responseTimeoutMs)
            retry(requestId, "The watch did not acknowledge the image data in time.")
        }
    }

    private suspend fun setTimeout(responseTimeoutMs: Long) {
        delay(responseTimeoutMs.milliseconds)
    }

    private fun retry(requestId: String, errorMessage: String) {
        val transfer = activeTransfer ?: return
        if (transfer.requestId != requestId) {
            return
        }

        cancelCurrentTimeout()

        if (transfer.numberOfImageTransmissionAttempts >= maximumAttempts) {
            failTheTransfer(errorMessage)
        } else {
            sendCurrentRequest()
        }
    }

    private fun failTheTransfer(message: String) {
        cancelCurrentTimeout()

        activeTransfer?.let {
            completedRequestIds = it.requestIds.toSet()
        }
        activeTransfer = null
        onFailure(message)
    }

    private fun cancelCurrentTimeout() {
        timeoutJob?.cancel()
    }
}
