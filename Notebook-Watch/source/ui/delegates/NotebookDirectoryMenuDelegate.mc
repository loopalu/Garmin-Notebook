import Toybox.Lang;
import Toybox.WatchUi;

class NotebookDirectoryMenuDelegate extends WatchUi.Menu2InputDelegate {

    private var uiController as NotebookUiController;
    private var menu as NotebookDirectoryMenu;

    function initialize(uiController as NotebookUiController, menu as NotebookDirectoryMenu) {
        Menu2InputDelegate.initialize();
        self.uiController = uiController;
        self.menu = menu;
    }

    function onSelect(menuItem as WatchUi.MenuItem) as Void {
        var menuItemId = menuItem.getId();
        if (menu.isEmptyItem(menuItemId)) {
            return;
        }
        if (menuItemId instanceof String) {
            uiController.openItem(menuItemId);
        }
    }

    function onBack() as Void {
        uiController.closeDirectoryMenu();
        WatchUi.popView(WatchUi.SLIDE_DOWN);
    }
}
