import Toybox.Application.Storage;
import Toybox.Lang;

class NotebookRepository {

    private const SCHEMA_KEY = "notebook_schema";
    private const SCHEMA_VERSION = 2;
    private const DIRECTORY_IDS_KEY = "notebook_directory_ids";
    private const NEXT_ID_KEY = "notebook_next_id";
    private const RECENT_REQUESTS_KEY = "notebook_recent_requests";
    private const DIRECTORY_KEY_PREFIX = "notebook_dir_";
    private const ITEM_KEY_PREFIX = "notebook_item_";
    private const ACTIVE_IMAGE_TRANSFER_KEY = "notebook_image_transfer";
    private const IMAGE_TRANSFER_CHUNK_PREFIX = "notebook_image_chunk_";
    private const MAX_RECENT_REQUESTS = 64;

    private var directoryIds as Array<String> = [];
    private var nextId as Number = 1;
    private var recentRequests as Array<String> = [];

    function initialize() {
        load();
    }

    function getDirectoryCount() as Number {
        return directoryIds.size();
    }

    function getDirectoryAt(index as Number) as Dictionary? {
        if (index < 0 || index >= directoryIds.size()) {
            return null;
        }
        return getDirectory(directoryIds[index]);
    }

    function getDirectory(id as String) as Dictionary? {
        var storedDirectory = Storage.getValue(DIRECTORY_KEY_PREFIX + id);
        return (storedDirectory instanceof Dictionary) ? storedDirectory as Dictionary : null;
    }

    function saveDirectory(id as String, directory as Dictionary) as Void {
        Storage.setValue(DIRECTORY_KEY_PREFIX + id, directory as Dictionary<Storage.KeyType, Storage.ValueType>);
    }

    function addDirectoryId(id as String) as Void {
        directoryIds.add(id);
        saveDirectoryIds();
    }

    function removeDirectoryId(id as String) as Void {
        var remainingIds = [] as Array<String>;
        for (var index = 0; index < directoryIds.size(); index++) {
            if (!directoryIds[index].equals(id)) {
                remainingIds.add(directoryIds[index]);
            }
        }
        directoryIds = remainingIds;
        saveDirectoryIds();
    }

    function deleteDirectory(id as String) as Void {
        Storage.deleteValue(DIRECTORY_KEY_PREFIX + id);
    }

    function getItem(id as String) as Dictionary? {
        var storedItem = Storage.getValue(ITEM_KEY_PREFIX + id);
        return (storedItem instanceof Dictionary) ? storedItem as Dictionary : null;
    }

    function saveItem(id as String, item as Dictionary) as Void {
        Storage.setValue(ITEM_KEY_PREFIX + id, item as Dictionary<Storage.KeyType, Storage.ValueType>);
    }

    function deleteItem(id as String) as Void {
        Storage.deleteValue(ITEM_KEY_PREFIX + id);
    }

    function setNextId(prefix as String) as String {
        var id = prefix + nextId.toString();
        nextId += 1;
        Storage.setValue(NEXT_ID_KEY, nextId);
        return id;
    }

    function hasProcessedRequest(requestId as String) as Boolean {
        for (var index = 0; index < recentRequests.size(); index++) {
            if (recentRequests[index].equals(requestId)) {
                return true;
            }
        }
        return false;
    }

    function markRequestProcessed(requestId as String) as Void {
        if (hasProcessedRequest(requestId)) {
            return;
        }
        recentRequests.add(requestId);
        if (recentRequests.size() > MAX_RECENT_REQUESTS) {
            recentRequests.remove(recentRequests[0]);
        }
        Storage.setValue(RECENT_REQUESTS_KEY, recentRequests as Array<Storage.ValueType>);
    }

    function getActiveImageTransfer() as Dictionary? {
        var imageTransfer = Storage.getValue(ACTIVE_IMAGE_TRANSFER_KEY);
        return (imageTransfer instanceof Dictionary) ? imageTransfer as Dictionary : null;
    }

    function saveActiveImageTransfer(transfer as Dictionary) as Void {
        Storage.setValue(ACTIVE_IMAGE_TRANSFER_KEY, transfer as Dictionary<Storage.KeyType, Storage.ValueType>);
    }

    function getImageChunk(transferId as String, chunkIndex as Number) as ByteArray? {
        var imageChunk = Storage.getValue(imageChunkKey(transferId, chunkIndex));
        return (imageChunk instanceof ByteArray) ? imageChunk as ByteArray : null;
    }

    function saveImageChunk(transferId as String, chunkIndex as Number, pixels as ByteArray) as Void {
        Storage.setValue(imageChunkKey(transferId, chunkIndex), pixels);
    }

    function cleanupActiveImageTransfer() as Void {
        var transfer = getActiveImageTransfer();
        if (transfer != null && (transfer["transferId"] instanceof String) &&
            (transfer["totalChunks"] instanceof Number)) {
            var transferId = transfer["transferId"] as String;
            var totalChunks = transfer["totalChunks"] as Number;
            for (var index = 0; index < totalChunks; index++) {
                Storage.deleteValue(imageChunkKey(transferId, index));
            }
        }
        Storage.deleteValue(ACTIVE_IMAGE_TRANSFER_KEY);
    }

    private function load() as Void {
        var storedSchema = Storage.getValue(SCHEMA_KEY);
        var storedNextId = Storage.getValue(NEXT_ID_KEY);
        if (storedNextId instanceof Number) {
            nextId = storedNextId as Number;
        }

        var storedRequests = Storage.getValue(RECENT_REQUESTS_KEY);
        if (storedRequests instanceof Array) {
            recentRequests = stringArray(storedRequests as Array<Object>);
        }

        var storedIds = Storage.getValue(DIRECTORY_IDS_KEY);
        if (storedIds instanceof Array) {
            directoryIds = stringArray(storedIds as Array<Object>);
            if (!(storedSchema instanceof Number) || (storedSchema as Number) < SCHEMA_VERSION) {
                removeInvalidStoredItems();
                Storage.setValue(SCHEMA_KEY, SCHEMA_VERSION);
            }
            return;
        }

        Storage.clearValues();
        directoryIds = [];
        nextId = 1;
        recentRequests = [];
        saveDirectoryIds();
        Storage.setValue(SCHEMA_KEY, SCHEMA_VERSION);
    }

    private function removeInvalidStoredItems() as Void {
        for (var directoryIndex = 0; directoryIndex < directoryIds.size(); directoryIndex++) {
            var directoryId = directoryIds[directoryIndex];
            var directory = getDirectory(directoryId);
            if (directory == null) {
                continue;
            }

            var validItemIds = [] as Array<String>;
            var rawItemIds = directory["itemIds"];
            if (rawItemIds instanceof Array) {
                var itemIds = rawItemIds as Array<Object>;
                for (var itemIndex = 0; itemIndex < itemIds.size(); itemIndex++) {
                    if (itemIds[itemIndex] instanceof String) {
                        var itemId = itemIds[itemIndex] as String;
                        var item = getItem(itemId);
                        if (item != null && isValidStoredItem(item)) {
                            validItemIds.add(itemId);
                        } else {
                            deleteItem(itemId);
                        }
                    }
                }
            }
            directory["itemIds"] = validItemIds;
            saveDirectory(directoryId, directory);
        }
    }

    private function isValidStoredItem(item as Dictionary) as Boolean {
        if (isTypeOf(item, "text")) {
            var text = item["text"];
            if (text instanceof String) {
                return true;
            }
            return false;
        }
        if (!isTypeOf(item, "image") || !(item["width"] instanceof Number) ||
            !(item["height"] instanceof Number) || !(item["palette"] instanceof Array) ||
            !(item["pixels"] instanceof ByteArray)) {
            return false;
        }

        return ImageService.validateImageData(
            item["width"] as Number,
            item["height"] as Number,
            item["palette"] as Array<Object>,
            (item["pixels"] as ByteArray).size()
        ) == null;
    }

    private function isTypeOf(item as Dictionary, type as String) as Boolean {
        var storedType = item["type"];
        if (storedType instanceof String) {
            return (storedType as String).equals(type);
        }
        return false;
    }

    private function saveDirectoryIds() as Void {
        Storage.setValue(DIRECTORY_IDS_KEY, directoryIds as Array<Storage.ValueType>);
    }

    private function stringArray(values as Array<Object>) as Array<String> {
        var strings = [] as Array<String>;
        for (var index = 0; index < values.size(); index++) {
            if (values[index] instanceof String) {
                strings.add(values[index] as String);
            }
        }
        return strings;
    }

    private function imageChunkKey(transferId as String, chunkIndex as Number) as String {
        return IMAGE_TRANSFER_CHUNK_PREFIX + transferId + "_" + chunkIndex.toString();
    }
}
