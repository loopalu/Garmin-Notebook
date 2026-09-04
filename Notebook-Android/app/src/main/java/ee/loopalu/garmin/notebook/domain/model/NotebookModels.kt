package ee.loopalu.garmin.notebook.domain.model

sealed interface NotebookItem {
    val id: String
    val name: String
}

data class NotebookTextItem(
    override val id: String,
    override val name: String,
    val text: String
) : NotebookItem

data class NotebookImageItem(
    override val id: String,
    override val name: String,
    val width: Int,
    val height: Int
) : NotebookItem

data class NotebookDirectory(
    val id: String,
    val name: String,
    val items: List<NotebookItem>
)

enum class GarminDeviceStatus {
    CONNECTED,
    NOT_CONNECTED,
    NOT_PAIRED,
    UNKNOWN
}

data class GarminDevice(
    val id: Long,
    val name: String,
    val status: GarminDeviceStatus
) {
    val isConnected: Boolean get() = status == GarminDeviceStatus.CONNECTED
}

sealed interface GarminConnectionState {
    data object Initializing : GarminConnectionState
    data class SelectDevice(val devices: List<GarminDevice>) : GarminConnectionState
    data class Connecting(val deviceName: String) : GarminConnectionState
    data class Connected(val deviceName: String) : GarminConnectionState
    data class Error(val message: String) : GarminConnectionState
}

data class NotebookUiState(
    val connection: GarminConnectionState = GarminConnectionState.Initializing,
    val directories: List<NotebookDirectory> = emptyList(),
    val selectedDirectoryId: String? = null,
    val isBusy: Boolean = false,
    val imageTransferProgress: Int? = null,
    val message: String? = null
) {
    val selectedDirectory: NotebookDirectory?
        get() = directories.firstOrNull {
            it.id == selectedDirectoryId
        }
}
