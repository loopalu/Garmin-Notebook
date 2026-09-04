package ee.loopalu.garmin.notebook.communication

import android.content.Context
import android.util.Log
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import com.garmin.android.connectiq.exception.InvalidStateException
import com.garmin.android.connectiq.exception.ServiceUnavailableException
import ee.loopalu.garmin.notebook.R
import ee.loopalu.garmin.notebook.domain.model.GarminConnectionState
import ee.loopalu.garmin.notebook.domain.model.GarminDevice
import ee.loopalu.garmin.notebook.domain.model.GarminDeviceStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

class GarminClient(context: Context) : GarminClientInterface {
    companion object {
        // Connect IQ APIs use the manifest UUID without separators.
        const val NOTEBOOK_APP_ID = "e774b6d4cd9a4fbb8060f01dbbfdd596"
        private const val TAG = "GarminClient"
    }

    private val appContext = context.applicationContext
    private val connectIQ = ConnectIQ.getInstance(appContext, ConnectIQ.IQConnectType.WIRELESS)
    private val devicesById = mutableMapOf<Long, IQDevice>()
    private var activeDevice: IQDevice? = null
    private var activeApp: IQApp? = null

    private val mutableConnection = MutableStateFlow<GarminConnectionState>(GarminConnectionState.Initializing)
    override val connection: StateFlow<GarminConnectionState> = mutableConnection.asStateFlow()

    private val messageChannel = Channel<Map<*, *>>(Channel.UNLIMITED)
    override val messages: Flow<Map<*, *>> = messageChannel.receiveAsFlow()

    override fun initialize() {
        mutableConnection.value = GarminConnectionState.Initializing
        connectIQ.initialize(appContext, true, object : ConnectIQ.ConnectIQListener {
            override fun onInitializeError(errStatus: ConnectIQ.IQSdkErrorStatus) {
                mutableConnection.value = GarminConnectionState.Error(
                    appContext.getString(R.string.garmin_sdk_initialization_failed, errStatus.name)
                )
            }

            override fun onSdkReady() {
                refreshDevices()
            }

            override fun onSdkShutDown() {
                activeDevice = null
                activeApp = null
                mutableConnection.value = GarminConnectionState.Error(
                    appContext.getString(R.string.garmin_communication_stopped),
                )
            }
        })
    }

    override fun refreshDevices() {
        try {
            val knownDevices = connectIQ.knownDevices.orEmpty()
            val connectedDeviceIds = connectIQ.connectedDevices.orEmpty()
                .mapTo(mutableSetOf()) {
                    it.deviceIdentifier
                }

            devicesById.clear()
            knownDevices.forEach {
                devicesById[it.deviceIdentifier] = it
            }

            mutableConnection.value = GarminConnectionState.SelectDevice(
                getGarminDevices(connectedDeviceIds)
            )
        } catch (error: Exception) {
            mutableConnection.value = GarminConnectionState.Error(
                error.message ?: appContext.getString(R.string.unable_to_list_garmin_devices)
            )
        }
    }

    private fun getGarminDevices(connectedDeviceIds: MutableSet<Long>): List<GarminDevice> =
        devicesById.values.map { iQDevice ->
            val reportedStatus = runCatching {
                GarminDeviceStatus.valueOf(iQDevice.status?.name.orEmpty())
            }.getOrDefault(GarminDeviceStatus.UNKNOWN)
            getGarminDevice(iQDevice, connectedDeviceIds, reportedStatus)
        }.sortedBy {
            it.name
        }

    private fun getGarminDevice(
        iQDevice: IQDevice,
        connectedDeviceIds: MutableSet<Long>,
        reportedStatus: GarminDeviceStatus
    ): GarminDevice = GarminDevice(
        id = iQDevice.deviceIdentifier,
        name = iQDevice.friendlyName,
        status = if (iQDevice.deviceIdentifier in connectedDeviceIds) {
            GarminDeviceStatus.CONNECTED
        } else {
            reportedStatus.takeUnless {
                it == GarminDeviceStatus.CONNECTED
            } ?: GarminDeviceStatus.NOT_CONNECTED
        },
    )

    override fun connect(deviceId: Long) {
        val knownDevice = devicesById[deviceId]
        if (knownDevice == null) {
            mutableConnection.value = GarminConnectionState.Error(
                appContext.getString(R.string.selected_garmin_device_unavailable)
            )
            return
        }

        val device = try {
            connectIQ.connectedDevices.orEmpty().firstOrNull {
                it.deviceIdentifier == deviceId
            }
        } catch (error: Exception) {
            mutableConnection.value = GarminConnectionState.Error(
                error.message ?: appContext.getString(R.string.unable_to_check_garmin_connection)
            )
            return
        }
        if (device == null) {
            mutableConnection.value = GarminConnectionState.Error(
                appContext.getString(R.string.wait_for_device_connection, knownDevice.friendlyName)
            )
            return
        }

        devicesById[deviceId] = device
        mutableConnection.value = GarminConnectionState.Connecting(device.friendlyName)
        try {
            getApplicationInfo(device)
        } catch (_: InvalidStateException) {
            mutableConnection.value = GarminConnectionState.Error(
                appContext.getString(R.string.garmin_connect_not_ready)
            )
        } catch (_: ServiceUnavailableException) {
            mutableConnection.value = GarminConnectionState.Error(
                appContext.getString(R.string.garmin_connect_service_unavailable)
            )
        }
    }

    private fun getApplicationInfo(iQDevice: IQDevice) {
        connectIQ.getApplicationInfo(
            NOTEBOOK_APP_ID,
            iQDevice,
            object : ConnectIQ.IQApplicationInfoListener {
                override fun onApplicationInfoReceived(app: IQApp) {
                    activeDevice = iQDevice
                    activeApp = app
                    registerForEvents(iQDevice, app)
                    mutableConnection.value = GarminConnectionState.Connected(iQDevice.friendlyName)
                }

                override fun onApplicationNotInstalled(applicationId: String) {
                    mutableConnection.value = GarminConnectionState.Error(
                        appContext.getString(
                            R.string.notebook_not_installed_on_device,
                            iQDevice.friendlyName
                        )
                    )
                }
            })
    }

    override fun sendRequest(request: Map<String, Any>, onResult: (Result<Unit>) -> Unit) {
        val device = activeDevice
        val app = activeApp
        if (device == null || app == null) {
            onResult(Result.failure(IllegalStateException(appContext.getString(R.string.connect_to_garmin_watch_first))))
            return
        }
        try {
            connectIQ.sendMessage(device, app, listOf(request)) { _, _, status ->
                handleRequestStatus(status, onResult)
            }
        } catch (error: Exception) {
            onResult(Result.failure(error))
        }
    }

    private fun handleRequestStatus(status: ConnectIQ.IQMessageStatus?, onResult: (Result<Unit>) -> Unit) {
        if (status == ConnectIQ.IQMessageStatus.SUCCESS) {
            onResult(Result.success(Unit))
        } else {
            onResult(Result.failure(IllegalStateException(appContext.getString(
                R.string.garmin_rejected_message,
                status?.name ))))
        }
    }

    override fun shutdown() {
        try {
            connectIQ.unregisterAllForEvents()
            connectIQ.shutdown(appContext)
        } catch (error: InvalidStateException) {
            Log.w(TAG, appContext.getString(R.string.garmin_sdk_already_stopped), error)
        } finally {
            activeDevice = null
            activeApp = null
            messageChannel.close()
        }
    }

    private fun registerForEvents(device: IQDevice, app: IQApp) {
        try {
            connectIQ.unregisterAllForEvents()
            registerForDeviceEvents(device)
            registerForAppEvents(device, app)
        } catch (_: InvalidStateException) {
            mutableConnection.value = GarminConnectionState.Error(
                appContext.getString(R.string.could_not_listen_for_notebook_messages)
            )
        }
    }

    private fun registerForAppEvents(device: IQDevice, app: IQApp) {
        connectIQ.registerForAppEvents(device, app) { _, _, messageData, status ->
            Log.d(TAG, "Watch event: status=$status, data=$messageData")
            if (status == ConnectIQ.IQMessageStatus.SUCCESS) {
                NotebookCommunicationService.unwrap(messageData)?.let { message ->
                    if (messageChannel.trySend(message).isFailure) {
                        mutableConnection.value = GarminConnectionState.Error(
                            appContext.getString(R.string.could_not_receive_notebook_response)
                        )
                    }
                }
            }
        }
    }

    private fun registerForDeviceEvents(device: IQDevice) {
        connectIQ.registerForDeviceEvents(device) { _, status ->
            if (status != IQDevice.IQDeviceStatus.CONNECTED) {
                activeDevice = null
                activeApp = null
                mutableConnection.value = GarminConnectionState.Error(
                    appContext.getString(R.string.device_disconnected, device.friendlyName)
                )
            }
        }
    }
}
