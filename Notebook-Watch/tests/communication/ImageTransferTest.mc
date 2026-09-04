import Toybox.Application.Storage;
import Toybox.Lang;
import Toybox.Test;

class ImageTransferTest {

    (:test)
    static function testImageTransfer(logger as Logger) as Boolean {
        Storage.clearValues();
        var repository = new NotebookRepository();
        var directoryService = new DirectoryService(repository);
        var imageService = new ImageService(repository, directoryService);
        var ok = NotebookTestSupport.expectNull(logger, directoryService.createDirectory("Protocol"), "Creates protocol directory.");
        var directoryId = NotebookTestSupport.getFirstDirectoryId(directoryService);
        if (directoryId == null) {
            logger.error("Protocol directory has no id.");
            return false;
        }

        var palette = NotebookTestSupport.makePalette();
        ok = NotebookTestSupport.expectString(
            logger,
            imageService.beginImageTransfer("bad-count", directoryId, "bad.png", 64, 64, palette, 4096, 3),
            "Image chunk count is invalid.",
            "Rejects an incorrect chunk count."
        ) && ok;

        ok = NotebookTestSupport.expectNull(
            logger,
            imageService.beginImageTransfer("protocol-transfer", directoryId, "image.png", 64, 64, palette, 4096, 2),
            "Begins valid transfer."
        ) && ok;

        var pixels = NotebookTestSupport.makePixels(4096);
        var firstChunk = pixels.slice(0, NotebookTestSupport.CHUNK_SIZE);
        var secondChunk = pixels.slice(NotebookTestSupport.CHUNK_SIZE, 4096);
        ok = NotebookTestSupport.expectString(logger, imageService.storeImageChunk("protocol-transfer", 1, secondChunk), "Image chunk is out of sequence.", "Rejects out-of-order chunk.") && ok;
        ok = NotebookTestSupport.expectString(logger, imageService.storeImageChunk("other-transfer", 0, firstChunk), "Image transfer ID does not match.", "Rejects wrong transfer id.") && ok;
        ok = NotebookTestSupport.expectString(logger, imageService.storeImageChunk("protocol-transfer", 0, [1]b), "Image chunk size is invalid.", "Rejects wrong chunk size.") && ok;
        ok = NotebookTestSupport.expectNull(logger, imageService.storeImageChunk("protocol-transfer", 0, firstChunk), "stores first chunk") && ok;
        ok = NotebookTestSupport.expectString(logger, imageService.storeImageChunk("protocol-transfer", 0, firstChunk), "Image chunk is out of sequence.", "Rejects duplicate chunk.") && ok;
        ok = NotebookTestSupport.expectString(logger, imageService.commitImageTransfer("protocol-transfer", 0, 0), "Image transfer is incomplete.", "Rejects incomplete transfer.") && ok;
        ok = NotebookTestSupport.expectNull(logger, imageService.storeImageChunk("protocol-transfer", 1, secondChunk), "Stores second chunk.") && ok;

        var checksum = NotebookTestSupport.getChecksumFor(pixels);
        ok = NotebookTestSupport.expectString(
            logger,
            imageService.commitImageTransfer("protocol-transfer", (checksum[0] as Number) + 1, checksum[1] as Number),
            "Image checksum does not match.",
            "Rejects checksum mismatch."
        ) && ok;
        ok = NotebookTestSupport.expectNull(
            logger,
            imageService.commitImageTransfer("protocol-transfer", checksum[0] as Number, checksum[1] as Number),
            "Commits after valid checksum."
        ) && ok;

        var items = directoryService.getItems(directoryId);
        ok = NotebookTestSupport.expectNumber(logger, items.size(), 1, "Commit creates exactly one item.") && ok;
        if (items.size() == 1 && items[0]["pixels"] instanceof ByteArray) {
            ok = NotebookTestSupport.expectBytes(logger, items[0]["pixels"] as ByteArray, pixels, "Commit preserves every byte.") && ok;
        } else {
            logger.error("Committed image pixels are missing.");
            ok = false;
        }
        Storage.clearValues();
        return ok;
    }
}
