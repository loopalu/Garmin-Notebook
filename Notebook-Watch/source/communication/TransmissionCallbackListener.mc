import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;

class TransmissionCallbackListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() as Void {
    }

    function onError() as Void {
        System.println("Notebook response transmission failed");
    }
}
