import Toybox.Lang;
import Toybox.WatchUi;

//! Coordinates Notebook navigation and refreshes active menus after data changes.
class NotebookUiController {

    private var directoryService as DirectoryService;
    private var mainMenu as NotebookMainMenu?;
    private var directoryMenu as NotebookDirectoryMenu?;

    function initialize(directoryService as DirectoryService) {
        self.directoryService = directoryService;
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        var menu = new NotebookMainMenu(directoryService);
        self.mainMenu = menu;
        return [menu, new NotebookMenuDelegate(self, menu)];
    }

    function onNotebookDataChanged() as Void {
        if (mainMenu != null) {
            mainMenu.refresh();
        }
        if (directoryMenu != null) {
            directoryMenu.refresh();
        }
        WatchUi.requestUpdate();
    }

    function openDirectory(directoryId as String) as Void {
        if (directoryService.getDirectory(directoryId) == null) {
            return;
        }
        var menu = new NotebookDirectoryMenu(directoryService, directoryId);
        directoryMenu = menu;
        WatchUi.pushView(
            menu,
            new NotebookDirectoryMenuDelegate(self, menu),
            WatchUi.SLIDE_UP
        );
    }

    function closeDirectoryMenu() as Void {
        directoryMenu = null;
    }

    function openItem(itemId as String) as Void {
        if (directoryService.getItem(itemId) == null) {
            return;
        }
        WatchUi.pushView(
            new NotebookView(directoryService, itemId),
            new NotebookDelegate(),
            WatchUi.SLIDE_UP
        );
    }

    function addDirectory(name as String) as Boolean {
        var error = directoryService.createDirectory(name);
        if (error != null) {
            return false;
        }
        onNotebookDataChanged();
        return true;
    }
}
