import Toybox.Lang;
import Toybox.WatchUi;

class NotebookMainMenu extends WatchUi.Menu2 {

    private const ADD_DIRECTORY_ID = "add_directory";

    private var directoryService as DirectoryService;
    private var itemIds as Array<String> = [];

    function initialize(directoryService as DirectoryService) {
        Menu2.initialize({ :title => Rez.Strings.AppName });
        self.directoryService = directoryService;
        refresh();
    }

    function refresh() as Void {
        clearItems();
        for (var index = 0; index < directoryService.getDirectoryCount(); index++) {
            var directory = directoryService.getDirectoryAt(index);
            if (directory != null) {
                var directoryId = directory["id"] as String;
                addItem(createDirectoryItem(directory));
                itemIds.add(directoryId);
            }
        }
        addItem(createAddDirectoryButton());
        itemIds.add(ADD_DIRECTORY_ID);
    }

    function isAddDirectoryItem(id as Object?) as Boolean {
        return (id instanceof String) && id.equals(ADD_DIRECTORY_ID);
    }

    private function createDirectoryItem(directory as Dictionary) as WatchUi.MenuItem {
        var name = directory["name"] as String;
        var id = directory["id"] as String;
        return new WatchUi.MenuItem(name, null, id, null);
    }

    private function createAddDirectoryButton() as WatchUi.MenuItem {
        return new WatchUi.MenuItem(Rez.Strings.AddDirectory, null, ADD_DIRECTORY_ID, null);
    }

    private function clearItems() as Void {
        for (var index = 0; index < itemIds.size(); index++) {
            var itemIndex = findItemById(itemIds[index]);
            if (itemIndex >= 0) {
                deleteItem(itemIndex);
            }
        }
        itemIds = [];
    }
}
