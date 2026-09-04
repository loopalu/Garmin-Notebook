package ee.loopalu.garmin.notebook.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory
import ee.loopalu.garmin.notebook.domain.model.NotebookImageItem
import ee.loopalu.garmin.notebook.domain.model.NotebookItem
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem

@Composable
internal fun ItemScreen(
    directory: NotebookDirectory,
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onEdit: (NotebookTextItem) -> Unit,
    onRename: (NotebookItem) -> Unit,
    onDelete: (NotebookItem) -> Unit,
    onDirectoryActions: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DirectoryActionsButton(onDirectoryActions)
        ItemContent(directory.items, onEdit, onRename, onDelete, Modifier.weight(1f).fillMaxWidth())
        AddItemActions(onAddText, onAddImage)
    }
}

@Composable
private fun DirectoryActionsButton(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClick) {
            Text(stringResource(R.string.directory_actions))
        }
    }
}

@Composable
private fun ItemContent(
    items: List<NotebookItem>,
    onEdit: (NotebookTextItem) -> Unit,
    onRename: (NotebookItem) -> Unit,
    onDelete: (NotebookItem) -> Unit,
    modifier: Modifier
) {
    if (items.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.directory_empty))
        }
    } else {
        LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items,
                key = { it.id
                }) { item ->
                    NotebookItemCard(item, onEdit, onRename, onDelete)
            }
        }
    }
}

@Composable
private fun NotebookItemCard(
    item: NotebookItem,
    onEdit: (NotebookTextItem) -> Unit,
    onRename: (NotebookItem) -> Unit,
    onDelete: (NotebookItem) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.name, fontWeight = FontWeight.SemiBold)
            NotebookItemDescription(item)
            NotebookItemActions(item, onEdit, onRename, onDelete)
        }
    }
}

@Composable
private fun NotebookItemDescription(item: NotebookItem) {
    when (item) {
        is NotebookTextItem -> Text(
            item.text.ifEmpty {
                stringResource(R.string.empty_text_item)
            },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        is NotebookImageItem -> Text(stringResource(R.string.image_dimensions, item.width, item.height))
    }
}

@Composable
private fun NotebookItemActions(
    item: NotebookItem,
    onEdit: (NotebookTextItem) -> Unit,
    onRename: (NotebookItem) -> Unit,
    onDelete: (NotebookItem) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (item is NotebookTextItem) {
            FilledTonalButton(onClick = {
                onEdit(item)
            }) {
                Text(stringResource(R.string.edit_text))
            }
        }
        OutlinedButton(onClick = {
            onRename(item)
        }) {
            Text(stringResource(R.string.rename))
        }
        TextButton(onClick = {
            onDelete(item)
        }) {
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
private fun AddItemActions(onAddText: () -> Unit, onAddImage: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onAddText,
            modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.add_text))
        }
        Button(
            onClick = onAddImage,
            modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.add_image))
        }
    }
}
