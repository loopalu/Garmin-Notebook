import Toybox.Application.Storage;
import Toybox.Lang;
import Toybox.Test;

class DirectoryServiceTest {

    (:test)
    static function testAllowDuplicateItemNames(logger as Logger) as Boolean {
        Storage.clearValues();
        var repository = new NotebookRepository();
        var directoryService = new DirectoryService(repository);
        var textService = new TextService(repository, directoryService);

        var ok = NotebookTestSupport.expectNull(logger, directoryService.createDirectory("Notes"), "Creates a directory.");
        var directoryId = NotebookTestSupport.getFirstDirectoryId(directoryService);
        ok = NotebookTestSupport.expect(logger, directoryId != null, "Directory has an id.") && ok;
        if (directoryId == null) {
            Storage.clearValues();
            return false;
        }

        ok = NotebookTestSupport.expectNull(logger, textService.createTextItem(directoryId as String, "note.txt", "first"), "Creates the first item.") && ok;
        ok = NotebookTestSupport.expectNull(logger, textService.createTextItem(directoryId as String, "note.txt", "second"), "Creates the item with existing name.") && ok;

        var items = directoryService.getItems(directoryId as String);
        ok = NotebookTestSupport.expectNumber(logger, items.size(), 2, "Both same-name items are returned.") && ok;
        if (items.size() == 2) {
            ok = NotebookTestSupport.expectString(logger, items[0]["name"] as String?, "note.txt", "first item name") && ok;
            ok = NotebookTestSupport.expectString(logger, items[1]["name"] as String?, "note.txt", "second item name") && ok;
        }

        Storage.clearValues();
        return ok;
    }
}
