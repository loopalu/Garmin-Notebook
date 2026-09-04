package ee.loopalu.garmin.notebook.presentation.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.GarminDevice
import ee.loopalu.garmin.notebook.domain.model.GarminDeviceStatus

@Composable
internal fun LoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
internal fun DeviceScreen(devices: List<GarminDevice>, onConnect: (Long) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.connect_to_watch), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.connection_requirements))
        if (devices.isEmpty()) {
            Text(stringResource(R.string.no_devices_found))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(devices, key = { it.id }) { device ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(device.name, fontWeight = FontWeight.SemiBold)
                                Text(getDeviceStatusLabel(device.status), style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = { onConnect(device.id) }, enabled = device.isConnected) {
                                Text(stringResource(R.string.connect))
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.refresh_devices))
        }
    }
}

@Composable
internal fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.connection_problem), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text(message)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.choose_device))
        }
    }
}

@Composable
private fun getDeviceStatusLabel(status: GarminDeviceStatus): String = stringResource(
    when (status) {
        GarminDeviceStatus.CONNECTED -> R.string.device_connected
        GarminDeviceStatus.NOT_CONNECTED -> R.string.device_not_connected
        GarminDeviceStatus.NOT_PAIRED -> R.string.device_not_paired
        GarminDeviceStatus.UNKNOWN -> R.string.device_status_unknown
    }
)
