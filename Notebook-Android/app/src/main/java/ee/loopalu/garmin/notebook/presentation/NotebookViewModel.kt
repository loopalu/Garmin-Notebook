package ee.loopalu.garmin.notebook.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.communication.GarminClient
import ee.loopalu.garmin.notebook.communication.GarminClientInterface
import ee.loopalu.garmin.notebook.communication.NotebookClient
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.LIST_DIRECTORIES
import ee.loopalu.garmin.notebook.communication.NotebookResponse
import ee.loopalu.garmin.notebook.communication.imagetransfer.ImageTransferService
import ee.loopalu.garmin.notebook.communication.imagetransfer.ImageTransferPlanner
import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem
import ee.loopalu.garmin.notebook.domain.model.NotebookUiState
import ee.loopalu.garmin.notebook.image.ImageEncoder
import ee.loopalu.garmin.notebook.image.SelectedImageEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotebookViewModel internal constructor(
    application: Application,
    private val client: GarminClientInterface,
    private val imageEncoder: SelectedImageEncoder
) : AndroidViewModel(application) {
    companion object {
        private const val MAX_TEXT_LENGTH = 8_000
    }

    @Suppress("unused")
    constructor(application: Application) : this(
        application,
        GarminClient(application),
        ImageEncoder(
            contentResolver = application.contentResolver,
            maximumImageDimension = application.resources.getInteger(
                R.integer.maximum_image_dimension_pixels
            )
        )
    )

    private val mutableUiState = MutableStateFlow(NotebookUiState())
    val uiState: StateFlow<NotebookUiState> = mutableUiState.asStateFlow()
    private var imageEncodingJob: Job? = null

    private val notebookClient = NotebookClient(
        garminClient = client,
        scope = viewModelScope,
        onResponse = ::handleRegularResponse,
        onFailure = ::failOperation
    )
    private val imageTransferService = ImageTransferService(
        garminClient = client,
        coroutineScope = viewModelScope,
        onProgress = { progress ->
            mutableUiState.update {
                it.copy(imageTransferProgress = progress)
            }
        },
        onCompleted = ::completeImageTransfer,
        onFailure = ::failOperation
    )

    init {
        viewModelScope.launch {
            client.connection.collect(::handleConnection)
        }
        viewModelScope.launch {
            client.messages.collect(::handleMessage)
        }
        client.initialize()
    }

    fun refreshDevices() = client.refreshDevices()

    fun connect(deviceId: Long) = client.connect(deviceId)

    fun selectDirectory(directoryId: String) {
        mutableUiState.update {
            it.copy(selectedDirectoryId = directoryId)
        }
    }

    fun closeDirectory() {
        mutableUiState.update {
            it.copy(selectedDirectoryId = null)
        }
    }

    fun clearMessage() {
        mutableUiState.update {
            it.copy(message = null)
        }
    }

    fun requestSnapshot() = sendRequest(NotebookCommunicationService.listDirectoriesRequest())

    fun createDirectory(name: String) {
        cleanName(name)?.let {
            sendRequest(NotebookCommunicationService.createDirectoryRequest(it))
        }
    }

    fun renameDirectory(directoryId: String, name: String) {
        cleanName(name)?.let {
            sendRequest(NotebookCommunicationService.renameDirectoryRequest(directoryId, it))
        }
    }

    fun deleteDirectory(directoryId: String) {
        closeDirectory()
        sendRequest(NotebookCommunicationService.deleteDirectoryRequest(directoryId))
    }

    fun createTextItem(directoryId: String, name: String, text: String) {
        val cleanName = cleanName(name) ?: return
        if (!validateTextLength(text)) {
            return
        }
        sendRequest(NotebookCommunicationService.createTextItemRequest(directoryId, cleanName, text))
    }

    fun updateTextItem(directoryId: String, item: NotebookTextItem, text: String) {
        if (!validateTextLength(text)) {
            return
        }
        sendRequest(NotebookCommunicationService.updateTextItemRequest(directoryId, item.id, text))
    }

    fun renameItem(directoryId: String, itemId: String, name: String) {
        cleanName(name)?.let {
            sendRequest(NotebookCommunicationService.renameItemRequest(directoryId, itemId, it))
        }
    }

    fun deleteItem(directoryId: String, itemId: String) {
        sendRequest(NotebookCommunicationService.deleteItemRequest(directoryId, itemId))
    }

    fun addImage(directoryId: String, imagePath: Uri) {
        if (operationIsActive()) {
            showMessage("Wait for the current watch operation to finish")
            return
        }
        mutableUiState.update {
            it.copy(isBusy = true, message = null)
        }
        imageEncodingJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    imageEncoder.encode(imagePath)
                }
            }
            imageEncodingJob = null
            result.onSuccess { image ->
                imageTransferService.startImageTransfer(
                    ImageTransferPlanner.createTransferPlan(directoryId, image))
            }.onFailure { error ->
                failOperation(error.message ?: "Could not process image")
            }
        }
    }

    override fun onCleared() {
        imageEncodingJob?.cancel()
        notebookClient.cancel()
        imageTransferService.stopActiveImageTransfer()
        client.shutdown()
    }

    private fun sendRequest(request: Map<String, Any>) {
        if (operationIsActive()) {
            showMessage("Wait for the current watch operation to finish")
            return
        }
        if (notebookClient.sendRequest(request)) {
            mutableUiState.update {
                it.copy(isBusy = true, message = null)
            }
        }
    }

    private fun handleConnection(connection: GarminConnectionState) {
        mutableUiState.update { state ->
            state.copy(
                connection = connection,
                isBusy = if (connection is GarminConnectionState.Connecting) true else state.isBusy
            )
        }
        if (connection is GarminConnectionState.Connected && !operationIsActive()) {
            requestSnapshot()
        } else if (connection !is GarminConnectionState.Connecting && operationIsActive()) {
            cancelOperations("The Garmin watch disconnected during the operation")
        } else if (connection !is GarminConnectionState.Connecting) {
            mutableUiState.update {
                it.copy(isBusy = false)
            }
        }
    }

    private fun handleMessage(message: Map<*, *>) {
        val response = NotebookCommunicationService.parseResponse(message)
        if (response == null) {
            if (operationIsActive()) cancelOperations("The watch returned an invalid Notebook response.")
            return
        }
        if (imageTransferService.handleWatchResponse(response)) {
            return
        }
        notebookClient.handleResponse(response)
    }

    private fun handleRegularResponse(response: NotebookResponse) {
        if (!response.isSuccess) {
            failOperation(response.error ?: "The watch rejected the request.")
            return
        }
        response.directories?.let(::applySnapshot)
        if (response.directories == null && response.operation != LIST_DIRECTORIES) {
            requestSnapshot()
        } else {
            mutableUiState.update {
                it.copy(isBusy = false)
            }
        }
    }

    private fun completeImageTransfer(response: NotebookResponse) {
        mutableUiState.update {
            it.copy(imageTransferProgress = null)
        }
        response.directories?.let(::applySnapshot)
        if (response.directories == null) requestSnapshot() else mutableUiState.update {
            it.copy(isBusy = false)
        }
    }

    private fun applySnapshot(directories: List<NotebookDirectory>) {
        mutableUiState.update { state ->
            val selectedStillExists = directories.any {
                it.id == state.selectedDirectoryId
            }
            state.copy(
                directories = directories,
                selectedDirectoryId = state.selectedDirectoryId.takeIf {
                    selectedStillExists
                }
            )
        }
    }

    private fun cancelOperations(message: String) {
        imageEncodingJob?.cancel()
        imageEncodingJob = null
        when {
            imageTransferService.isImageTransferOngoing -> imageTransferService.stopActiveImageTransfer(message)
            notebookClient.isRequestTransmissionOngoing -> notebookClient.cancel(message)
            else -> failOperation(message)
        }
    }

    private fun operationIsActive(): Boolean =
        imageEncodingJob?.isActive == true
        || notebookClient.isRequestTransmissionOngoing
        || imageTransferService.isImageTransferOngoing

    private fun cleanName(name: String): String? = name.trim().takeIf(String::isNotEmpty)

    private fun validateTextLength(text: String): Boolean {
        if (text.length <= MAX_TEXT_LENGTH) {
            return true
        }
        showMessage("Text items are currently limited to $MAX_TEXT_LENGTH characters.")
        return false
    }

    private fun failOperation(message: String) {
        mutableUiState.update {
            it.copy(isBusy = false, imageTransferProgress = null, message = message)
        }
    }

    private fun showMessage(message: String) {
        mutableUiState.update {
            it.copy(message = message)
        }
    }
}
