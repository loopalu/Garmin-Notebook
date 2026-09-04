import Toybox.Application.Storage;
import Toybox.Graphics;
import Toybox.Lang;
import Toybox.Test;

class ImageTransferRegressionTest {

    (:test)
    static function testTransfersLargeRgb222EncodedFiles(logger as Logger) as Boolean {
        var ok = transferLargeFile(logger, 95, 128, "portrait-95x128");
        ok = transferLargeFile(logger, 127, 128, "qr-127x128") && ok;
        return ok;
    }

    private static function transferLargeFile(logger as Logger, width as Number, height as Number, transferId as String) as Boolean {
        Storage.clearValues();
        var repository = new NotebookRepository();
        var directoryService = new DirectoryService(repository);
        var imageService = new ImageService(repository, directoryService);

        if (directoryService.createDirectory("Regression") != null) {
            logger.error(transferId + ": could not create directory.");
            return false;
        }
        var directoryId = NotebookTestSupport.getFirstDirectoryId(directoryService);
        if (directoryId == null) {
            logger.error(transferId + ": directory id missing.");
            return false;
        }

        var pixels = NotebookTestSupport.makePixels(width * height);
        var totalChunks = (pixels.size() + NotebookTestSupport.CHUNK_SIZE - 1) / NotebookTestSupport.CHUNK_SIZE;
        var error = imageService.beginImageTransfer(
            transferId,
            directoryId,
            transferId + ".png",
            width,
            height,
            NotebookTestSupport.makePalette(),
            pixels.size(),
            totalChunks
        );
        if (error != null) {
            logger.error(transferId + ": begin failed: " + error);
            return false;
        }

        for (var chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            var start = chunkIndex * NotebookTestSupport.CHUNK_SIZE;
            var end = start + NotebookTestSupport.CHUNK_SIZE;
            if (end > pixels.size()) {
                end = pixels.size();
            }
            error = imageService.storeImageChunk(transferId, chunkIndex, pixels.slice(start, end));
            if (error != null) {
                logger.error(transferId + ": chunk " + chunkIndex + " failed: " + error);
                return false;
            }
        }

        var checksum = NotebookTestSupport.getChecksumFor(pixels);
        error = imageService.commitImageTransfer(transferId, checksum[0] as Number, checksum[1] as Number);
        if (error != null) {
            logger.error(transferId + ": commit failed: " + error);
            return false;
        }

        var items = directoryService.getItems(directoryId);
        var ok = NotebookTestSupport.expectNumber(logger, items.size(), 1, transferId + ": exactly one item committed.");
        if (items.size() != 1 || !(items[0]["pixels"] instanceof ByteArray)) {
            logger.error(transferId + ": committed pixels missing.");
            return false;
        }

        ok = NotebookTestSupport.expectBytes(logger, items[0]["pixels"] as ByteArray, pixels, transferId + ": reconstructed bytes match.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, items[0]["width"] as Number, width, transferId + ": width matches.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, items[0]["height"] as Number, height, transferId + ": height matches.") && ok;

        var row = 0;
        var numberOfRenderedBatches = 0;
        while (row < height) {
            row = ImageService.getNextRenderRow(row, height);
            numberOfRenderedBatches += 1;
        }
        ok = NotebookTestSupport.expectNumber(logger, numberOfRenderedBatches, 32, transferId + ": rendering is split across 32 batches.") && ok;

        var itemId = items[0]["id"];
        if (!(itemId instanceof String)) {
            logger.error(transferId + ": committed item id missing.");
            return false;
        }
        var screen = Graphics.createBufferedBitmap({
            :width => 280,
            :height => 280
        }).get() as Graphics.BufferedBitmap;
        var view = new NotebookView(directoryService, itemId as String);
        for (var batchIndex = 0; batchIndex < numberOfRenderedBatches; batchIndex++) {
            view.onUpdate(screen.getDc());
        }

        view.onUpdate(screen.getDc());
        view.onHide();

        Storage.clearValues();
        return ok;
    }
}
