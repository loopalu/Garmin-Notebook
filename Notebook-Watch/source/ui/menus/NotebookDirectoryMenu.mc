import Toybox.Lang;
import Toybox.WatchUi;

class NotebookDirectoryMenu extends WatchUi.Menu2 {

    private const EMPTY_ITEM_ID = "empty_directory";

    private var directoryService as DirectoryService;
    private var directoryId as String;
    private var itemIds as Array<String> = [];

    function initialize(directoryService as DirectoryService, directoryId as String) {
        Menu2.initialize({ :title => Rez.Strings.AppName });
        self.directoryService = directoryService;
        self.directoryId = directoryId;
        refresh();
    }

    function refresh() as Void {
        clearItems();
        var directory = directoryService.getDirectory(directoryId);
        if (directory == null) {
            setTitle(Rez.Strings.AppName);
            addItem(new WatchUi.MenuItem(Rez.Strings.DirectoryDeleted, null, EMPTY_ITEM_ID, null));
            itemIds.add(EMPTY_ITEM_ID);
            return;
        }

        setTitle(directory["name"] as String);
        var items = directoryService.getItems(directoryId);
        if (items.size() == 0) {
            addItem(new WatchUi.MenuItem(Rez.Strings.EmptyDirectory, null, EMPTY_ITEM_ID, null));
            itemIds.add(EMPTY_ITEM_ID);
            return;
        }

        for (var index = 0; index < items.size(); index++) {
            var item = items[index];
            var id = item["id"] as String;
            var itemType = item["type"] as String;
            var detail = itemType.equals("text") ? Rez.Strings.TextItem : Rez.Strings.ImageItem;
            addItem(new WatchUi.MenuItem(item["name"] as String, detail, id, null));
            itemIds.add(id);
        }
    }

    function isEmptyItem(id as Object?) as Boolean {
        return (id instanceof String) && id.equals(EMPTY_ITEM_ID);
    }

    private function clearItems() as Void {
        for (var index = 0; index < itemIds.size(); index++) {
            var menuIndex = findItemById(itemIds[index]);
            if (menuIndex >= 0) {
                deleteItem(menuIndex);
            }
        }
        itemIds = [];
    }
}
