package ee.loopalu.garmin.notebook.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.loopalu.garmin.notebook.presentation.NotebookViewModel

@Composable
fun NotebookApp(viewModel: NotebookViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appState = rememberNotebookAppState()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { imagePath ->
        val directoryId = state.selectedDirectoryId
        if (imagePath != null && directoryId != null) {
            viewModel.addImage(directoryId, imagePath)
        }
    }

    NotebookContent(
        state = state,
        onBack = viewModel::closeDirectory,
        onSync = viewModel::requestSnapshot,
        onConnect = viewModel::connect,
        onRefreshDevices = viewModel::refreshDevices,
        onSelectDirectory = viewModel::selectDirectory,
        onShowDialog = appState::showDialog,
        onSelectImage = {
            imagePicker.launch(arrayOf("image/*"))
        }
    )

    NotebookDialogHost(
        dialog = appState.dialog,
        message = state.message,
        onDismissDialog = appState::dismissDialog,
        onDismissMessage = viewModel::clearMessage,
        onCreateDirectory = viewModel::createDirectory,
        onCreateTextItem = viewModel::createTextItem,
        onRenameItem = viewModel::renameItem,
        onUpdateTextItem = viewModel::updateTextItem,
        onDeleteItem = viewModel::deleteItem,
        onRenameDirectory = viewModel::renameDirectory,
        onDeleteDirectory = viewModel::deleteDirectory
    )
}
