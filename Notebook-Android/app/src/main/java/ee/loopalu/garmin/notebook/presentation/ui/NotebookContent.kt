package ee.loopalu.garmin.notebook.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import ee.loopalu.garmin.notebook.domain.model.NotebookUiState
import ee.loopalu.garmin.notebook.presentation.ui.screen.DeviceScreen
import ee.loopalu.garmin.notebook.presentation.ui.screen.DirectoryScreen
import ee.loopalu.garmin.notebook.presentation.ui.screen.ErrorScreen
import ee.loopalu.garmin.notebook.presentation.ui.screen.ItemScreen
import ee.loopalu.garmin.notebook.presentation.ui.screen.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotebookContent(
    state: NotebookUiState,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onConnect: (Long) -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDirectory: (String) -> Unit,
    onShowDialog: (NotebookDialogState) -> Unit,
    onSelectImage: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                NotebookTopBar(
                    state = state,
                    onBack = onBack,
                    onSync = onSync
                )
            }
        ) { padding ->
            NotebookScreen(
                state = state,
                modifier = Modifier.fillMaxSize().padding(padding),
                onConnect = onConnect,
                onRefreshDevices = onRefreshDevices,
                onSelectDirectory = onSelectDirectory,
                onShowDialog = onShowDialog,
                onSelectImage = onSelectImage
            )
        }
        if (state.isBusy) {
            BusyOverlay(state.imageTransferProgress)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotebookTopBar(state: NotebookUiState, onBack: () -> Unit, onSync: () -> Unit) {
    TopAppBar(
        title = {
            Text(state.selectedDirectory?.name ?: stringResource(R.string.app_name))
        },
        navigationIcon = {
            if (state.selectedDirectory != null) {
                TextButton(
                    onClick = onBack,
                    enabled = !state.isBusy) {
                    Text(stringResource(R.string.back))
                }
            }
        },
        actions = {
            if (state.connection is GarminConnectionState.Connected) {
                TextButton(
                    onClick = onSync,
                    enabled = !state.isBusy) {
                    Text(stringResource(R.string.sync))
                }
            }
        }
    )
}

@Composable
private fun NotebookScreen(
    state: NotebookUiState,
    modifier: Modifier,
    onConnect: (Long) -> Unit,
    onRefreshDevices: () -> Unit,
    onSelectDirectory: (String) -> Unit,
    onShowDialog: (NotebookDialogState) -> Unit,
    onSelectImage: () -> Unit
) {
    Box(modifier) {
        when (val connection = state.connection) {
            GarminConnectionState.Initializing -> LoadingScreen(stringResource(R.string.starting_garmin_connect))
            is GarminConnectionState.Connecting -> LoadingScreen(
                stringResource(R.string.connecting_to_device, connection.deviceName)
            )
            is GarminConnectionState.SelectDevice -> DeviceScreen(connection.devices, onConnect, onRefreshDevices)
            is GarminConnectionState.Error -> ErrorScreen(connection.message, onRefreshDevices)
            is GarminConnectionState.Connected -> ConnectedNotebookScreen(
                state = state,
                deviceName = connection.deviceName,
                onSelectDirectory = onSelectDirectory,
                onShowDialog = onShowDialog,
                onSelectImage = onSelectImage
            )
        }
    }
}

@Composable
private fun ConnectedNotebookScreen(
    state: NotebookUiState,
    deviceName: String,
    onSelectDirectory: (String) -> Unit,
    onShowDialog: (NotebookDialogState) -> Unit,
    onSelectImage: () -> Unit
) {
    val directory = state.selectedDirectory
    if (directory == null) {
        DirectoryScreen(
            directories = state.directories,
            deviceName = deviceName,
            onOpen = onSelectDirectory,
            onAdd = {
                onShowDialog(NotebookDialogState.CreateDirectory)
            }
        )
    } else {
        ItemScreen(
            directory = directory,
            onAddText = {
                onShowDialog(NotebookDialogState.CreateTextItem(directory.id))
            },
            onAddImage = onSelectImage,
            onEdit = {
                onShowDialog(NotebookDialogState.EditTextItem(directory.id, it))
            },
            onRename = {
                onShowDialog(NotebookDialogState.RenameItem(directory.id, it))
            },
            onDelete = {
                onShowDialog(NotebookDialogState.DeleteItem(directory.id, it))
            },
            onDirectoryActions = {
                onShowDialog(NotebookDialogState.DirectoryActions(directory))
            }
        )
    }
}

@Composable
private fun BusyOverlay(progress: Int?) {
    val interactionSource = remember {
        MutableInteractionSource()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.20f))
            .clickable(interactionSource = interactionSource, indication = null) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            progress?.let {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sending_image_progress, it))
            }
        }
    }
}
