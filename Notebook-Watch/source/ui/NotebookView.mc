import Toybox.Graphics;
import Toybox.Lang;
import Toybox.WatchUi;

class NotebookView extends WatchUi.View {

    private const TITLE_DISTANCE_FROM_TOP_OF_DISPLAY = 18;
    private const CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY = 52;
    private const CONTENT_DISTANCE_FROM_LEFT_AND_RIGHT_OF_DISPLAY = 24;
    private const TEXT_DISTANCE_FROM_BOTTOM_OF_DISPLAY = 24;
    private const SPACE_BELOW_SCALABLE_IMAGE = 34;
    private const SPACE_BELOW_VERTICALLY_POSITIONED_IMAGE = 10;
    private const MINIMUM_IMAGE_SCALE = 1;

    private var directoryService as DirectoryService;
    private var itemId as String;
    private var imageBuffer as Graphics.BufferedBitmap?;
    private var numberOfRowsRendered as Number = 0;

    function initialize(directoryService as DirectoryService, itemId as String) {
        View.initialize();
        self.directoryService = directoryService;
        self.itemId = itemId;
    }

    function onLayout(dc as Dc) as Void {
    }

    function onShow() as Void {
    }

    function onUpdate(display as Dc) as Void {
        display.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
        display.clear();

        var item = directoryService.getItem(itemId);
        if (item == null) {
            drawCenteredMessage(display, WatchUi.loadResource(Rez.Strings.ItemDeleted) as String);
            return;
        }

        drawTitle(display, item["name"] as String);
        if (directoryService.isTypeOf(item, "text")) {
            drawTextItem(display, item);
        } else {
            drawImageItem(display, item);
        }
    }

    function onHide() as Void {
        imageBuffer = null;
        numberOfRowsRendered = 0;
    }

    private function drawTitle(display as Dc, name as String) as Void {
        display.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        display.drawText(
            display.getWidth() / 2,
            TITLE_DISTANCE_FROM_TOP_OF_DISPLAY,
            Graphics.FONT_XTINY,
            name,
            Graphics.TEXT_JUSTIFY_CENTER
        );
    }

    private function drawTextItem(display as Dc, item as Dictionary) as Void {
        var text = item["text"];
        if (!(text instanceof String) || (text as String).length() == 0) {
            text = Rez.Strings.EmptyText;
        }
        var contentWidth = display.getWidth() - (CONTENT_DISTANCE_FROM_LEFT_AND_RIGHT_OF_DISPLAY * 2);
        var contentHeight = display.getHeight() - CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY - TEXT_DISTANCE_FROM_BOTTOM_OF_DISPLAY;
        var area = new WatchUi.TextArea({
            :text => text,
            :color => Graphics.COLOR_WHITE,
            :backgroundColor => Graphics.COLOR_TRANSPARENT,
            :font => Graphics.FONT_XTINY,
            :justification => Graphics.TEXT_JUSTIFY_LEFT,
            :locX => CONTENT_DISTANCE_FROM_LEFT_AND_RIGHT_OF_DISPLAY,
            :locY => CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY,
            :width => contentWidth,
            :height => contentHeight
        });
        area.draw(display);
    }

    private function drawImageItem(display as Dc, item as Dictionary) as Void {
        if (!(item["width"] instanceof Number) || !(item["height"] instanceof Number) ||
            !(item["palette"] instanceof Array) || !(item["pixels"] instanceof ByteArray)) {
            drawCenteredMessage(display, WatchUi.loadResource(Rez.Strings.InvalidImage) as String);
            return;
        }

        var imageWidth = item["width"] as Number;
        var imageHeight = item["height"] as Number;
        var palette = item["palette"] as Array<Object>;
        var pixels = item["pixels"] as ByteArray;
        var expectedPixelSize = ImageService.getExpectedPixelSize(imageWidth, imageHeight);
        if (pixels.size() != expectedPixelSize) {
            drawCenteredMessage(display, WatchUi.loadResource(Rez.Strings.InvalidImage) as String);
            return;
        }
        var availableImageWidth = display.getWidth() - (CONTENT_DISTANCE_FROM_LEFT_AND_RIGHT_OF_DISPLAY * 2);
        var availableImageHeight = display.getHeight() - CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY - SPACE_BELOW_SCALABLE_IMAGE;
        var horizontalScale = availableImageWidth / imageWidth;
        var verticalScale = availableImageHeight / imageHeight;
        var scale = horizontalScale < verticalScale ? horizontalScale : verticalScale;
        if (scale < MINIMUM_IMAGE_SCALE) {
            scale = MINIMUM_IMAGE_SCALE;
        }
        var bitmapWidth = imageWidth * scale;
        var bitmapHeight = imageHeight * scale;
        var startX = (display.getWidth() - bitmapWidth) / 2;
        var imagePositionAreaHeight = display.getHeight() - CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY - SPACE_BELOW_VERTICALLY_POSITIONED_IMAGE;
        var startY = CONTENT_DISTANCE_FROM_TOP_OF_DISPLAY + ((imagePositionAreaHeight - bitmapHeight) / 2);

        if (imageBuffer == null) {
            imageBuffer = Graphics.createBufferedBitmap({
                :width => bitmapWidth,
                :height => bitmapHeight
            }).get() as Graphics.BufferedBitmap;
            numberOfRowsRendered = 0;
            var background = (imageBuffer as Graphics.BufferedBitmap).getDc();
            background.setColor(Graphics.COLOR_BLACK, Graphics.COLOR_BLACK);
            background.clear();
        }

        if (numberOfRowsRendered < imageHeight) {
            var lastRowToRenderNext = ImageService.getNextRenderRow(numberOfRowsRendered, imageHeight);
            var bufferedContent = (imageBuffer as Graphics.BufferedBitmap).getDc();
            for (var y = numberOfRowsRendered; y < lastRowToRenderNext; y++) {
                if (!ImageService.drawImageRow(bufferedContent, y, imageWidth, scale, palette, pixels)) {
                    imageBuffer = null;
                    numberOfRowsRendered = 0;
                    drawCenteredMessage(display, WatchUi.loadResource(Rez.Strings.InvalidImage) as String);
                    return;
                }
            }
            numberOfRowsRendered = lastRowToRenderNext;
        }

        if (numberOfRowsRendered < imageHeight) {
            drawCenteredMessage(display, WatchUi.loadResource(Rez.Strings.LoadingImage) as String);
            WatchUi.requestUpdate();
            return;
        }

        display.drawBitmap(startX, startY, imageBuffer as Graphics.BufferedBitmap);
    }

    private function drawCenteredMessage(display as Dc, message as String) as Void {
        display.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);
        display.drawText(
            display.getWidth() / 2,
            display.getHeight() / 2,
            Graphics.FONT_SMALL,
            message,
            Graphics.TEXT_JUSTIFY_CENTER | Graphics.TEXT_JUSTIFY_VCENTER
        );
    }
}
