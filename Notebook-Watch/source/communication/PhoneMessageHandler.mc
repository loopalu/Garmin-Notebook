import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;

class PhoneMessageHandler {

    private const VERSION = 1;
    private const IMAGE_TRANSMISSION_ENCODING = "rgb222-index8";
    private const IMAGE_CREATION_ENCODING = "rgb222-index8-base64";

    private var uiController as NotebookUiController;
    private var repository as NotebookRepository;
    private var directoryService as DirectoryService;
    private var textService as TextService;
    private var imageService as ImageService;

    function initialize(
        uiController as NotebookUiController,
        repository as NotebookRepository,
        directoryService as DirectoryService,
        textService as TextService,
        imageService as ImageService
    ) {
        self.uiController = uiController;
        self.repository = repository;
        self.directoryService = directoryService;
        self.textService = textService;
        self.imageService = imageService;
    }

    function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        var rawData = message.data;
        if (rawData instanceof Array) {
            var list = rawData as Array<Object>;
            rawData = (list.size() == 1) ? list[0] : null;
        }
        if (!(rawData instanceof Dictionary)) {
            sendError("", "", "Request must be a dictionary.");
            return;
        }

        var request = rawData as Dictionary;
        var requestId = getStringValue(request, "requestId");
        var operation = getStringValue(request, "operation");
        if (requestId == null || requestId.length() == 0 || operation == null || operation.length() == 0) {
            sendError(requestId == null ? "" : requestId, operation == null ? "" : operation, "Request envelope is incomplete.");
            return;
        }

        if (!(request["v"] instanceof Number) || (request["v"] as Number) != VERSION) {
            sendError(requestId, operation, "Unsupported protocol version.");
            return;
        }
        var messageType = getStringValue(request, "type");
        if (messageType == null || !messageType.equals("request")) {
            sendError(requestId, operation, "Invalid message type.");
            return;
        }

        if (repository.hasProcessedRequest(requestId)) {
            sendSuccess(requestId, operation);
            return;
        }

        try {
            var error = perform(operation, request);
            if (error != null) {
                sendError(requestId, operation, error);
                return;
            }
            if (!operation.equals("list_directories")) {
                repository.markRequestProcessed(requestId);
            }
            if (!operation.equals("list_directories") && !isImageTransferOngoing(operation)) {
                uiController.onNotebookDataChanged();
            }
            sendSuccess(requestId, operation);
        } catch (exception) {
            System.println("Notebook request failed: " + exception.toString());
            sendError(requestId, operation, "The watch could not save this change.");
        }
    }

    private function perform(operation as String, request as Dictionary) as String? {
        switch (operation) {
            case "list_directories":
                return listDirectories();
            case "create_directory":
                return createDirectory(request);
            case "rename_directory":
                return renameDirectory(request);
            case "delete_directory":
                return deleteDirectory(request);
            case "create_text_item":
                return createTextItem(request);
            case "update_text_item":
                return updateTextItem(request);
            case "rename_item":
                return renameItem(request);
            case "delete_item":
                return deleteItem(request);
            case "create_image_item":
                return createImageItem(request);
            case "begin_image_transfer":
                return beginImageTransfer(request);
            case "image_chunk":
                return storeImageChunk(request);
            case "commit_image_transfer":
                return commitImageTransfer(request);
            default:
                return "Unknown operation";
        }
    }

    private function listDirectories() as String? {
        return null;
    }

    private function createDirectory(request as Dictionary) as String? {
        var directoryName = getStringValue(request, "name");
        if (directoryName == null || directoryName.length() == 0) {
            return "name is required";
        }
        return directoryService.createDirectory(directoryName as String);
    }

    private function renameDirectory(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var newDirectoryName = getStringValue(request, "name");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (newDirectoryName == null || newDirectoryName.length() == 0) {
            return "name is required";
        }
        return directoryService.renameDirectory(directoryId as String, newDirectoryName as String);
    }

    private function deleteDirectory(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        return directoryService.deleteDirectory(directoryId as String);
    }

    private function createTextItem(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var fileName = getStringValue(request, "name");
        var text = getStringValue(request, "text");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (fileName == null || fileName.length() == 0) {
            return "name is required";
        }
        if (text == null) {
            return "text is required";
        }
        return textService.createTextItem(directoryId as String, fileName as String, text as String);
    }

    private function updateTextItem(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var itemId = getStringValue(request, "itemId");
        var text = getStringValue(request, "text");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (itemId == null || itemId.length() == 0) {
            return "itemId is required";
        }
        if (text == null) {
            return "text is required";
        }
        return textService.updateTextItem(directoryId as String, itemId as String, text as String);
    }

    private function renameItem(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var itemId = getStringValue(request, "itemId");
        var itemName = getStringValue(request, "name");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (itemId == null || itemId.length() == 0) {
            return "itemId is required";
        }
        if (itemName == null || itemName.length() == 0) {
            return "name is required";
        }
        return directoryService.renameItem(directoryId as String, itemId as String, itemName as String);
    }

    private function deleteItem(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var itemId = getStringValue(request, "itemId");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (itemId == null || itemId.length() == 0) {
            return "itemId is required";
        }
        return directoryService.deleteItem(directoryId as String, itemId as String);
    }

    private function beginImageTransfer(request as Dictionary) as String? {
        var transferId = getStringValue(request, "transferId");
        var directoryId = getStringValue(request, "directoryId");
        var fileName = getStringValue(request, "name");
        var encoding = getStringValue(request, "encoding");
        if (transferId == null || transferId.length() == 0) {
            return "transferId is required";
        }
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (fileName == null || fileName.length() == 0) {
            return "name is required";
        }
        if (encoding == null || !encoding.equals(IMAGE_TRANSMISSION_ENCODING)) {
            return "Unsupported image encoding.";
        }
        if (!(request["width"] instanceof Number) || !(request["height"] instanceof Number) ||
            !(request["totalBytes"] instanceof Number) || !(request["totalChunks"] instanceof Number)) {
            return "Image transfer dimensions are invalid.";
        }
        if (!(request["palette"] instanceof Array)) {
            return "Image transfer palette is invalid.";
        }
        return imageService.beginImageTransfer(
            transferId as String,
            directoryId as String,
            fileName as String,
            request["width"] as Number,
            request["height"] as Number,
            request["palette"] as Array<Object>,
            request["totalBytes"] as Number,
            request["totalChunks"] as Number
        );
    }

    private function storeImageChunk(request as Dictionary) as String? {
        var transferId = getStringValue(request, "transferId");
        var encodedData = getStringValue(request, "data");
        if (transferId == null || transferId.length() == 0) {
            return "transferId is required";
        }
        if (encodedData == null || encodedData.length() == 0) {
            return "data is required";
        }
        if (!(request["chunkIndex"] instanceof Number)) {
            return "Image chunk index is invalid.";
        }
        var pixels = Base64Decoder.decode(encodedData as String);
        if (pixels == null) {
            return "Image chunk data is invalid.";
        }
        return imageService.storeImageChunk(
            transferId as String,
            request["chunkIndex"] as Number,
            pixels
        );
    }

    //! checksumA and checksumB are the two parts of an Adler-32 checksum.
    //! The watch compares them with its calculated values to verify that all
    //! of the image data was transferred correctly and without corruption.
    //! @param request Transfer identifier and checksum values
    //! @return An error message, or null when successful
    private function commitImageTransfer(request as Dictionary) as String? {
        var transferId = getStringValue(request, "transferId");
        if (transferId == null || transferId.length() == 0) {
            return "transferId is required";
        }
        if (!(request["checksumA"] instanceof Number) || !(request["checksumB"] instanceof Number)) {
            return "Image checksum is invalid.";
        }
        return imageService.commitImageTransfer(
            transferId as String,
            request["checksumA"] as Number,
            request["checksumB"] as Number
        );
    }

    private function createImageItem(request as Dictionary) as String? {
        var directoryId = getStringValue(request, "directoryId");
        var fileName = getStringValue(request, "name");
        var encoding = getStringValue(request, "encoding");
        if (directoryId == null || directoryId.length() == 0) {
            return "directoryId is required";
        }
        if (fileName == null || fileName.length() == 0) {
            return "name is required";
        }
        if (encoding == null || !encoding.equals(IMAGE_CREATION_ENCODING)) {
            return "Unsupported image encoding";
        }
        if (!(request["width"] instanceof Number) || !(request["height"] instanceof Number)) {
            return "Image dimensions are required.";
        }
        if (!(request["palette"] instanceof Array) || !(request["pixels"] instanceof String)) {
            return "Image data is incomplete.";
        }

        var pixels = Base64Decoder.decode(request["pixels"] as String);
        if (pixels == null) {
            return "Image pixel data is invalid";
        }
        return imageService.createImageItem(
            directoryId as String,
            fileName as String,
            request["width"] as Number,
            request["height"] as Number,
            request["palette"] as Array<Object>,
            pixels
        );
    }

    private function getStringValue(request as Dictionary, key as String) as String? {
        var value = request[key];
        return (value instanceof String) ? value as String : null;
    }

    private function sendSuccess(requestId as String, operation as String) as Void {
        var response = baseResponse(requestId, operation);
        response["ok"] = true;
        if (!isImageTransferOngoing(operation)) {
            response["directories"] = directoryService.getCurrentNotebookInformation();
        }
        transmit(response);
    }

    private function isImageTransferOngoing(operation as String) as Boolean {
        return operation.equals("begin_image_transfer") || operation.equals("image_chunk");
    }

    private function sendError(requestId as String, operation as String, error as String) as Void {
        var response = baseResponse(requestId, operation);
        response["ok"] = false;
        response["error"] = error;
        transmit(response);
    }

    private function baseResponse(requestId as String, operation as String) as Dictionary {
        return {
            "v" => VERSION,
            "type" => "response",
            "requestId" => requestId,
            "operation" => operation
        };
    }

    private function transmit(response as Dictionary) as Void {
        Communications.transmit(
            response as Dictionary<Communications.TransmitKeyType, Communications.TransmitType>,
            null,
            new TransmissionCallbackListener()
        );
    }
}
