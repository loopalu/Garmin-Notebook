import Toybox.Lang;
import Toybox.WatchUi;

class NotebookMenuDelegate extends WatchUi.Menu2InputDelegate {

    private var uiController as NotebookUiController;
    private var menu as NotebookMainMenu;

    function initialize(uiController as NotebookUiController, menu as NotebookMainMenu) {
        Menu2InputDelegate.initialize();
        self.uiController = uiController;
        self.menu = menu;
    }

    function onSelect(menuItem as WatchUi.MenuItem) as Void {
        var menuItemId = menuItem.getId();
        if (menu.isAddDirectoryItem(menuItemId)) {
            WatchUi.pushView(
                new WatchUi.TextPicker(""),
                new NotebookTextPickerDelegate(uiController),
                WatchUi.SLIDE_UP
            );
            return;
        }
        if (menuItemId instanceof String) {
            uiController.openDirectory(menuItemId);
        }
    }
}
