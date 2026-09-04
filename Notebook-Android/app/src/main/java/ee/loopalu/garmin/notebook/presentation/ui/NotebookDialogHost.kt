package ee.loopalu.garmin.notebook.presentation.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem
import ee.loopalu.garmin.notebook.presentation.ui.dialog.ConfirmDialog
import ee.loopalu.garmin.notebook.presentation.ui.dialog.DirectoryActionsDialog
import ee.loopalu.garmin.notebook.presentation.ui.dialog.NameDialog
import ee.loopalu.garmin.notebook.presentation.ui.dialog.TextItemDialog

@Composable
internal fun NotebookDialogHost(
    dialog: NotebookDialogState?,
    message: String?,
    onDismissDialog: () -> Unit,
    onDismissMessage: () -> Unit,
    onCreateDirectory: (String) -> Unit,
    onCreateTextItem: (String, String, String) -> Unit,
    onRenameItem: (String, String, String) -> Unit,
    onUpdateTextItem: (String, NotebookTextItem, String) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    onRenameDirectory: (String, String) -> Unit,
    onDeleteDirectory: (String) -> Unit
) {
    dialog?.let {
        NotebookOperationDialog(
            dialog = it,
            onDismiss = onDismissDialog,
            onCreateDirectory = onCreateDirectory,
            onCreateTextItem = onCreateTextItem,
            onRenameItem = onRenameItem,
            onUpdateTextItem = onUpdateTextItem,
            onDeleteItem = onDeleteItem,
            onRenameDirectory = onRenameDirectory,
            onDeleteDirectory = onDeleteDirectory
        )
    }
    message?.let {
        MessageDialog(it, onDismissMessage)
    }
}

@Composable
private fun NotebookOperationDialog(
    dialog: NotebookDialogState,
    onDismiss: () -> Unit,
    onCreateDirectory: (String) -> Unit,
    onCreateTextItem: (String, String, String) -> Unit,
    onRenameItem: (String, String, String) -> Unit,
    onUpdateTextItem: (String, NotebookTextItem, String) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    onRenameDirectory: (String, String) -> Unit,
    onDeleteDirectory: (String) -> Unit
) {
    when (dialog) {
        NotebookDialogState.CreateDirectory -> NameDialog(
            title = stringResource(R.string.new_directory),
            initialValue = "",
            onDismiss = onDismiss,
            onConfirm = {
                onCreateDirectory(it)
                onDismiss()
            }
        )
        is NotebookDialogState.CreateTextItem -> TextItemDialog(
            title = stringResource(R.string.new_text_item),
            initialName = "",
            initialText = "",
            showName = true,
            onDismiss = onDismiss,
            onConfirm = {
                name, text -> onCreateTextItem(dialog.directoryId, name, text)
                onDismiss()
            }
        )
        is NotebookDialogState.RenameItem -> NameDialog(
            title = stringResource(R.string.rename_item),
            initialValue = dialog.item.name,
            onDismiss = onDismiss,
            onConfirm = {
                onRenameItem(dialog.directoryId, dialog.item.id, it)
                onDismiss()
            }
        )
        is NotebookDialogState.EditTextItem -> TextItemDialog(
            title = stringResource(R.string.edit_named_item, dialog.item.name),
            initialName = dialog.item.name,
            initialText = dialog.item.text,
            showName = false,
            onDismiss = onDismiss,
            onConfirm = { _, text ->
                onUpdateTextItem(dialog.directoryId, dialog.item, text)
                onDismiss()
            }
        )
        is NotebookDialogState.DeleteItem -> ConfirmDialog(
            title = stringResource(R.string.delete_named_item, dialog.item.name),
            body = stringResource(R.string.delete_item_explanation),
            onDismiss = onDismiss,
            onConfirm = {
                onDeleteItem(dialog.directoryId, dialog.item.id)
                onDismiss()
            }
        )
        is NotebookDialogState.DirectoryActions -> DirectoryActionsDialog(
            directory = dialog.directory,
            onDismiss = onDismiss,
            onRename = {
                onRenameDirectory(dialog.directory.id, it)
            },
            onDelete = {
                onDeleteDirectory(dialog.directory.id)
            }
        )
    }
}

@Composable
private fun MessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        },
        title = {
            Text(stringResource(R.string.app_name))
        },
        text = {
            Text(message)
        }
    )
}
