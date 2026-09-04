package ee.loopalu.garmin.notebook.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory
import ee.loopalu.garmin.notebook.domain.model.NotebookItem
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem

internal sealed interface NotebookDialogState {
    data object CreateDirectory : NotebookDialogState
    data class CreateTextItem(val directoryId: String) : NotebookDialogState
    data class RenameItem(val directoryId: String, val item: NotebookItem) : NotebookDialogState
    data class EditTextItem(val directoryId: String, val item: NotebookTextItem) : NotebookDialogState
    data class DeleteItem(val directoryId: String, val item: NotebookItem) : NotebookDialogState
    data class DirectoryActions(val directory: NotebookDirectory) : NotebookDialogState
}

@Stable
internal class NotebookAppState {
    var dialog by mutableStateOf<NotebookDialogState?>(null)
        private set

    fun showDialog(dialog: NotebookDialogState) {
        this.dialog = dialog
    }

    fun dismissDialog() {
        dialog = null
    }
}

@Composable
internal fun rememberNotebookAppState(): NotebookAppState = remember {
    NotebookAppState()
}
