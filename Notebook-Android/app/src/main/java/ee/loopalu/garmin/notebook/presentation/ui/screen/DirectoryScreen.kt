package ee.loopalu.garmin.notebook.presentation.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory

@Composable
internal fun DirectoryScreen(
    directories: List<NotebookDirectory>,
    deviceName: String,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.connected_to_device, deviceName), style = MaterialTheme.typography.labelLarge)
        DirectoryContent(directories, onOpen, Modifier.weight(1f).fillMaxWidth())
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_directory))
        }
    }
}

@Composable
private fun DirectoryContent(
    directories: List<NotebookDirectory>,
    onOpen: (String) -> Unit,
    modifier: Modifier
) {
    if (directories.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_directories))
        }
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(directories,
                key = {
                    it.id
                }) { directory ->
                    DirectoryCard(directory, onOpen)
            }
        }
    }
}

@Composable
private fun DirectoryCard(directory: NotebookDirectory, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable {
        onOpen(directory.id)
    }) {
        Text(directory.name, Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold)
    }
}
