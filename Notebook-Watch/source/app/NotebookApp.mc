import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;
import Toybox.WatchUi;

class NotebookApp extends Application.AppBase {

    private var repository as NotebookRepository;
    private var directoryService as DirectoryService;
    private var textService as TextService;
    private var imageService as ImageService;
    private var uiController as NotebookUiController;
    private var phoneMessageHandler as PhoneMessageHandler;

    function initialize() {
        AppBase.initialize();
        self.repository = new NotebookRepository();
        self.directoryService = new DirectoryService(self.repository);
        self.textService = new TextService(self.repository, self.directoryService);
        self.imageService = new ImageService(self.repository, self.directoryService);
        self.uiController = new NotebookUiController(self.directoryService);
        self.phoneMessageHandler = new PhoneMessageHandler(
            self.uiController,
            self.repository,
            self.directoryService,
            self.textService,
            self.imageService
        );

        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));
        if (Communications has :registerForPhoneAppMessageErrors) {
            Communications.registerForPhoneAppMessageErrors(method(:onPhoneMessageError));
        }
    }

    function onStart(state as Dictionary?) as Void {
    }

    function onStop(state as Dictionary?) as Void {
    }

    function getInitialView() as [Views] or [Views, InputDelegates] {
        return uiController.getInitialView();
    }

    function onPhoneMessage(message as Communications.PhoneAppMessage) as Void {
        phoneMessageHandler.onPhoneMessage(message);
    }

    function onPhoneMessageError(error as Communications.PhoneAppMessageError) as Void {
        System.println("Notebook phone message error: " + error.toString());
    }

}
