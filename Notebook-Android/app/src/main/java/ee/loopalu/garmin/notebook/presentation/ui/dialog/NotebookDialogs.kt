package ee.loopalu.garmin.notebook.presentation.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory

@Composable
internal fun NameDialog(title: String, initialValue: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember(initialValue) {
        mutableStateOf(initialValue)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                },
                label = {
                    Text(stringResource(R.string.name))
                })
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(value)
                },
                enabled = value.isNotBlank()) {
                    Text(stringResource(R.string.save))
                }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun TextItemDialog(
    title: String,
    initialName: String,
    initialText: String,
    showName: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var fileName by remember(initialName) {
        mutableStateOf(initialName)
    }
    var text by remember(initialText) {
        mutableStateOf(initialText)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (showName) {
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = {
                            fileName = it
                        },
                        label = {
                            Text(stringResource(R.string.file_name))
                        }
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                    },
                    label = {
                        Text(stringResource(R.string.text))
                    },
                    minLines = 6
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(fileName, text)
                },
                enabled = !showName || fileName.isNotBlank()) {
                    Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun ConfirmDialog(title: String, body: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(body)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun DirectoryActionsDialog(
    directory: NotebookDirectory,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit
) {
    var directoryName by remember(directory.id) {
        mutableStateOf(directory.name)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.directory))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = directoryName,
                    onValueChange = {
                        directoryName = it
                    },
                    label = {
                        Text(stringResource(R.string.name))
                    }
                )
                OutlinedButton(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.delete_directory))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onRename(directoryName)
                onDismiss()
            }) {
                Text(stringResource(R.string.save_name))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
