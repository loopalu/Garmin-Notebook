package ee.loopalu.garmin.notebook.communication

import ee.loopalu.garmin.notebook.communication.imagetransfer.ImageChecksum
import ee.loopalu.garmin.notebook.domain.model.NotebookDirectory
import ee.loopalu.garmin.notebook.domain.model.NotebookImageItem
import ee.loopalu.garmin.notebook.domain.model.NotebookItem
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem
import ee.loopalu.garmin.notebook.image.EncodedImage
import java.util.UUID
import kotlin.collections.get

object NotebookCommunicationService {
    const val VERSION = 1

    const val LIST_DIRECTORIES = "list_directories"
    const val CREATE_DIRECTORY = "create_directory"
    const val RENAME_DIRECTORY = "rename_directory"
    const val DELETE_DIRECTORY = "delete_directory"
    const val CREATE_TEXT_ITEM = "create_text_item"
    const val BEGIN_IMAGE_TRANSFER = "begin_image_transfer"
    const val IMAGE_CHUNK = "image_chunk"
    const val COMMIT_IMAGE_TRANSFER = "commit_image_transfer"
    const val UPDATE_TEXT_ITEM = "update_text_item"
    const val RENAME_ITEM = "rename_item"
    const val DELETE_ITEM = "delete_item"

    fun listDirectoriesRequest() = createRequest(LIST_DIRECTORIES)

    fun createDirectoryRequest(name: String) = createRequest(CREATE_DIRECTORY, mapOf("name" to name))

    fun renameDirectoryRequest(directoryId: String, name: String) = createRequest(
        RENAME_DIRECTORY,
        mapOf("directoryId" to directoryId, "name" to name)
    )

    fun deleteDirectoryRequest(directoryId: String) = createRequest(
        DELETE_DIRECTORY,
        mapOf("directoryId" to directoryId)
    )

    fun createTextItemRequest(directoryId: String, name: String, text: String) = createRequest(
        CREATE_TEXT_ITEM,
        mapOf("directoryId" to directoryId, "name" to name, "text" to text)
    )

    fun updateTextItemRequest(directoryId: String, itemId: String, text: String) = createRequest(
        UPDATE_TEXT_ITEM,
        mapOf("directoryId" to directoryId, "itemId" to itemId, "text" to text)
    )

    fun renameItemRequest(directoryId: String, itemId: String, name: String) = createRequest(
        RENAME_ITEM,
        mapOf("directoryId" to directoryId, "itemId" to itemId, "name" to name)
    )

    fun deleteItemRequest(directoryId: String, itemId: String) = createRequest(
        DELETE_ITEM,
        mapOf("directoryId" to directoryId, "itemId" to itemId)
    )

    fun beginImageTransferRequest(
        transferId: String,
        directoryId: String,
        image: EncodedImage,
        totalChunks: Int,
        encoding: String
    ) = createRequest(
        BEGIN_IMAGE_TRANSFER,
        mapOf(
            "transferId" to transferId,
            "directoryId" to directoryId,
            "name" to image.name,
            "width" to image.width,
            "height" to image.height,
            "palette" to image.palette,
            "encoding" to encoding,
            "totalBytes" to image.pixelPaletteIndexes.size,
            "totalChunks" to totalChunks
        )
    )

    fun imageChunkRequest(transferId: String, chunkIndex: Int, data: String) = createRequest(
        IMAGE_CHUNK,
        mapOf("transferId" to transferId, "chunkIndex" to chunkIndex, "data" to data)
    )

    fun commitImageTransferRequest(transferId: String, checksum: ImageChecksum) = createRequest(
        COMMIT_IMAGE_TRANSFER,
        mapOf(
            "transferId" to transferId,
            "checksumA" to checksum.sumOfByteValues,
            "checksumB" to checksum.cumulativeSum
        )
    )

    private fun createRequest(operation: String, fields: Map<String, Any> = emptyMap()): Map<String, Any> =
        buildMap {
            put("v", VERSION)
            put("type", "request")
            put("requestId", UUID.randomUUID().toString())
            put("operation", operation)
            putAll(fields)
        }

    fun unwrap(messageData: List<Any>): Map<*, *>? = when {
        messageData.size == 1 && messageData.first() is Map<*, *> -> messageData.first() as Map<*, *>
        else -> null
    }

    fun parseResponse(message: Map<*, *>): NotebookResponse? {
        if ((message["v"] as? Number)?.toInt() != VERSION || message["type"] != "response") {
            return null
        }
        val requestId = message["requestId"] as? String ?: return null
        val operation = message["operation"] as? String ?: return null
        val isSuccess = message["ok"] as? Boolean ?: return null
        val error = message["error"] as? String
        val directories = if (message.containsKey("directories")) {
            parseSnapshot(message["directories"]) ?: return null
        } else null
        return NotebookResponse(requestId, operation, isSuccess, error, directories)
    }

    private fun parseSnapshot(value: Any?): List<NotebookDirectory>? {
        val rawDirectories = value as? List<*> ?: return null
        return rawDirectories.map { rawDirectory ->
            val directory = rawDirectory as? Map<*, *> ?: return null
            val directoryId = directory["id"] as? String ?: return null
            val directoryName = directory["name"] as? String ?: return null
            val rawItems = directory["items"] as? List<*> ?: return null
            val items = rawItems.map {
                rawItem -> parseItem(rawItem) ?: return null
            }
            NotebookDirectory(id = directoryId, name = directoryName, items = items)
        }
    }

    private fun parseItem(value: Any?): NotebookItem? {
        val item = value as? Map<*, *> ?: return null
        val itemId = item["id"] as? String ?: return null
        val itemName = item["name"] as? String ?: return null
        return when (item["type"] as? String) {
            "text" -> NotebookTextItem(itemId, itemName, item["text"] as? String ?: return null)
            "image" -> {
                val width = (item["width"] as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
                val height = (item["height"] as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
                NotebookImageItem(itemId, itemName, width, height)
            }
            else -> null
        }
    }
}

data class NotebookResponse(
    val requestId: String,
    val operation: String,
    val isSuccess: Boolean,
    val error: String?,
    val directories: List<NotebookDirectory>?
)
