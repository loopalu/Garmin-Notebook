package ee.loopalu.garmin.notebook.communication

import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.CREATE_DIRECTORY
import ee.loopalu.garmin.notebook.communication.NotebookCommunicationService.LIST_DIRECTORIES
import ee.loopalu.garmin.notebook.domain.model.NotebookImageItem
import ee.loopalu.garmin.notebook.domain.model.NotebookTextItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotebookCommunicationServiceTest {

    @Test
    fun requestHasCorrectStructure() {
        val createDirectoryRequest = NotebookCommunicationService.createDirectoryRequest("Trips")

        assertEquals(1, createDirectoryRequest["v"])
        assertEquals("request", createDirectoryRequest["type"])
        assertEquals(CREATE_DIRECTORY, createDirectoryRequest["operation"])
        assertEquals("Trips", createDirectoryRequest["name"])
        assertNotNull(createDirectoryRequest["requestId"])
    }

    @Test
    fun responseParsesTextAndImageItems() {
        val response = NotebookCommunicationService.parseResponse(responseWith(createValidDirectories()))

        val items = response?.directories?.single()?.items
        assertEquals("Trips", response?.directories?.single()?.name)
        assertTrue(items?.first() is NotebookTextItem)
        assertTrue(items?.last() is NotebookImageItem)
    }

    @Test
    fun rejectsEntireSnapshotWhenOneItemIsMalformed() {
        val directories = createValidDirectories().toMutableList()
        val directory = directories.single().toMutableMap()
        directory["items"] = (directory.getValue("items") as List<*>) +
            mapOf(
                "id" to "broken",
                "name" to "missing dimensions",
                "type" to "image"
            )
        directories[0] = directory

        assertNull(NotebookCommunicationService.parseResponse(responseWith(directories)))
    }

    @Test
    fun rejectsResponseWithWrongStructure() {
        assertNull(NotebookCommunicationService.parseResponse(responseWith(createValidDirectories()) + ("v" to 2)))
        assertNull(NotebookCommunicationService.parseResponse(responseWith(createValidDirectories()) - "requestId"))
        assertNull(NotebookCommunicationService.parseResponse(responseWith(createValidDirectories()) + ("type" to "request")))
    }

    private fun responseWith(directories: List<Any>): Map<String, Any> = mapOf(
        "v" to NotebookCommunicationService.VERSION,
        "type" to "response",
        "requestId" to "request-1",
        "operation" to LIST_DIRECTORIES,
        "ok" to true,
        "directories" to directories
    )

    private fun createValidDirectories(): List<Map<String, Any>> = listOf(
        mapOf(
            "id" to "d1",
            "name" to "Trips",
            "items" to listOf(
                mapOf(
                    "id" to "t1",
                    "name" to "notes.txt",
                    "type" to "text",
                    "text" to "Hello"
                ),
                mapOf(
                    "id" to "i1",
                    "name" to "map",
                    "type" to "image",
                    "width" to 64,
                    "height" to 48
                )
            )
        )
    )
}