import Toybox.Lang;

//! Creates and updates text items.
class TextService {

    private const MAX_TEXT_LENGTH = 8000;

    private var repository as NotebookRepository;
    private var directoryService as DirectoryService;

    function initialize(repository as NotebookRepository, directoryService as DirectoryService) {
        self.repository = repository;
        self.directoryService = directoryService;
    }

    function createTextItem(directoryId as String, name as String, text as String) as String? {
        var validationError = directoryService.validateNewItem(directoryId, name);
        if (validationError != null) {
            return validationError;
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            return "Text is too long";
        }

        return directoryService.addItem(directoryId, {
            "id" => repository.setNextId("i"),
            "name" => name,
            "type" => "text",
            "text" => text
        });
    }

    function updateTextItem(directoryId as String, itemId as String, text as String) as String? {
        if (text.length() > MAX_TEXT_LENGTH) {
            return "Text is too long";
        }
        var item = directoryService.findItemInDirectory(directoryId, itemId);
        if (item == null) {
            return "Item not found";
        }
        if (!directoryService.isTypeOf(item, "text")) {
            return "Only text items can be edited";
        }
        item["text"] = text;
        repository.saveItem(itemId, item);
        return null;
    }
}
