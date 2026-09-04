import Toybox.Lang;

class DirectoryService {

    private const MAX_DIRECTORIES = 20;
    private const MAX_ITEMS_PER_DIRECTORY = 20;
    private const MAX_NAME_LENGTH = 64;

    private var repository as NotebookRepository;

    function initialize(repository as NotebookRepository) {
        self.repository = repository;
    }

    function getDirectoryCount() as Number {
        return repository.getDirectoryCount();
    }

    function getDirectoryAt(index as Number) as Dictionary? {
        return repository.getDirectoryAt(index);
    }

    function getDirectory(id as String) as Dictionary? {
        return repository.getDirectory(id);
    }

    function getItem(id as String) as Dictionary? {
        return repository.getItem(id);
    }

    function getItems(directoryId as String) as Array<Dictionary> {
        var result = [] as Array<Dictionary>;
        var directory = getDirectory(directoryId);
        if (directory == null) {
            return result;
        }

        var rawIds = directory["itemIds"];
        if (!(rawIds instanceof Array)) {
            return result;
        }

        var itemIds = rawIds as Array<Object>;
        for (var index = 0; index < itemIds.size(); index++) {
            if (itemIds[index] instanceof String) {
                var item = getItem(itemIds[index] as String);
                if (item != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    function createDirectory(name as String) as String? {
        var validationError = validateDirectoryName(name, null);
        if (validationError != null) {
            return validationError;
        }
        if (repository.getDirectoryCount() >= MAX_DIRECTORIES) {
            return "Directory limit reached";
        }

        var id = repository.setNextId("d");
        repository.saveDirectory(id, {
            "id" => id,
            "name" => name,
            "itemIds" => []
        });
        repository.addDirectoryId(id);
        return null;
    }

    function renameDirectory(id as String, name as String) as String? {
        var directory = getDirectory(id);
        if (directory == null) {
            return "Directory not found";
        }
        var validationError = validateDirectoryName(name, id);
        if (validationError != null) {
            return validationError;
        }
        directory["name"] = name;
        repository.saveDirectory(id, directory);
        return null;
    }

    function deleteDirectory(id as String) as String? {
        var directory = getDirectory(id);
        if (directory == null) {
            return "Directory not found";
        }

        repository.removeDirectoryId(id);
        var rawItemIds = directory["itemIds"];
        if (rawItemIds instanceof Array) {
            var itemIds = rawItemIds as Array<Object>;
            for (var itemIndex = 0; itemIndex < itemIds.size(); itemIndex++) {
                if (itemIds[itemIndex] instanceof String) {
                    repository.deleteItem(itemIds[itemIndex] as String);
                }
            }
        }
        repository.deleteDirectory(id);
        return null;
    }

    function renameItem(directoryId as String, itemId as String, name as String) as String? {
        var validationError = validateName(name);
        if (validationError != null) {
            return validationError;
        }
        var item = findItemInDirectory(directoryId, itemId);
        if (item == null) {
            return "Item not found";
        }
        item["name"] = name;
        repository.saveItem(itemId, item);
        return null;
    }

    function deleteItem(directoryId as String, itemId as String) as String? {
        var directory = getDirectory(directoryId);
        if (directory == null) {
            return "Directory not found";
        }

        var rawItemIds = directory["itemIds"];
        if (!(rawItemIds instanceof Array)) {
            return "Item not found";
        }
        var itemIds = rawItemIds as Array<Object>;
        var remainingItemIds = [] as Array<String>;
        var itemFound = false;
        for (var index = 0; index < itemIds.size(); index++) {
            if (itemIds[index] instanceof String) {
                var currentId = itemIds[index] as String;
                if (currentId.equals(itemId)) {
                    itemFound = true;
                } else {
                    remainingItemIds.add(currentId);
                }
            }
        }
        if (!itemFound) {
            return "Item not found";
        }

        directory["itemIds"] = remainingItemIds;
        repository.saveDirectory(directoryId, directory);
        repository.deleteItem(itemId);
        return null;
    }

    function getCurrentNotebookInformation() as Array<Dictionary> {
        var directories = [] as Array<Dictionary>;
        for (var directoryIndex = 0; directoryIndex < getDirectoryCount(); directoryIndex++) {
            var directory = getDirectoryAt(directoryIndex);
            if (directory != null && directory["id"] instanceof String) {
                var directoryId = directory["id"] as String;
                var directoryItems = [] as Array<Dictionary>;
                var items = getItems(directoryId);
                for (var itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    var item = items[itemIndex];
                    var itemInformation = {
                        "id" => item["id"],
                        "name" => item["name"],
                        "type" => item["type"]
                    };
                    if (isTypeOf(item, "text")) {
                        itemInformation["text"] = item["text"];
                    } else {
                        itemInformation["width"] = item["width"];
                        itemInformation["height"] = item["height"];
                    }
                    directoryItems.add(itemInformation);
                }
                directories.add({
                    "id" => directory["id"],
                    "name" => directory["name"],
                    "items" => directoryItems
                });
            }
        }
        return directories;
    }

    function isTypeOf(item as Dictionary, type as String) as Boolean {
        var storedType = item["type"];
        if (storedType instanceof String) {
            return (storedType as String).equals(type);
        }
        return false;
    }

    function validateNewItem(directoryId as String, name as String) as String? {
        if (getDirectory(directoryId) == null) {
            return "Directory not found";
        }
        if (getItems(directoryId).size() >= MAX_ITEMS_PER_DIRECTORY) {
            return "Item limit reached";
        }
        return validateName(name);
    }

    function findItemInDirectory(directoryId as String, itemId as String) as Dictionary? {
        var items = getItems(directoryId);
        for (var index = 0; index < items.size(); index++) {
            var storedId = items[index]["id"];
            if ((storedId instanceof String) && (storedId as String).equals(itemId)) {
                return items[index];
            }
        }
        return null;
    }

    function addItem(directoryId as String, item as Dictionary) as String? {
        var directory = getDirectory(directoryId);
        if (directory == null) {
            return "Directory not found";
        }
        var itemId = item["id"] as String;
        repository.saveItem(itemId, item);

        var itemIds = [] as Array<String>;
        var rawIds = directory["itemIds"];
        if (rawIds instanceof Array) {
            itemIds = stringArray(rawIds as Array<Object>);
        }
        itemIds.add(itemId);
        directory["itemIds"] = itemIds;
        repository.saveDirectory(directoryId, directory);
        return null;
    }

    private function validateDirectoryName(name as String, ignoredId as String?) as String? {
        var basicError = validateName(name);
        if (basicError != null) {
            return basicError;
        }
        for (var index = 0; index < getDirectoryCount(); index++) {
            var directory = getDirectoryAt(index);
            if (directory != null && directory["id"] instanceof String) {
                var directoryId = directory["id"] as String;
                var storedName = directory["name"];
                if ((ignoredId == null || !directoryId.equals(ignoredId)) &&
                    (storedName instanceof String) && (storedName as String).equals(name)) {
                    return "A directory with that name already exists";
                }
            }
        }
        return null;
    }

    private function validateName(name as String) as String? {
        if (name.length() == 0) {
            return "Name cannot be empty";
        }
        if (name.length() > MAX_NAME_LENGTH) {
            return "Name is too long";
        }
        return null;
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
}
