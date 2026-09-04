import Toybox.Lang;
import Toybox.WatchUi;

class NotebookTextPickerDelegate extends WatchUi.TextPickerDelegate {

    private var uiController as NotebookUiController;

    function initialize(uiController as NotebookUiController) {
        TextPickerDelegate.initialize();
        self.uiController = uiController;
    }

    function onTextEntered(text as String, changed as Boolean) as Boolean {
        if (changed) {
            uiController.addDirectory(text);
        }
        return true;
    }

    function onCancel() as Boolean {
        return true;
    }
}
