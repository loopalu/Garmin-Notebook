import Toybox.Application.Storage;
import Toybox.Lang;
import Toybox.Test;

class ImageServiceTest {

    (:test)
    static function testRgb222ImageEncoding(logger as Logger) as Boolean {
        var ok = true;
        var rgb222Pixels = [0, 17, 63]b;
        var palette = NotebookTestSupport.makePalette();
        ok = NotebookTestSupport.expectNumber(logger, ImageService.getPaletteIndexAt(rgb222Pixels, 3, 1, 0), 17, "Returns rgb222 index8 byte.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, ImageService.getExpectedPixelSize(127, 128), 16256, "Returns rgb222 maximum-height payload size.") && ok;
        ok = NotebookTestSupport.expectNull(logger, ImageService.validateImageData(3, 1, palette, 3), "Validates rgb222 image.") && ok;
        return ok;
    }

    (:test)
    static function testRenderingInBatches(logger as Logger) as Boolean {
        var currentRow = 0;
        var numberOfRenderedBatches = 0;
        var ok = NotebookTestSupport.expectNumber(logger, ImageService.getRowsPerUpdate(), 4, "Returns maximum number of rows per one rendering attempt.");

        while (currentRow < 128) {
            var nextRow = ImageService.getNextRenderRow(currentRow, 128);
            ok = NotebookTestSupport.expect(logger, nextRow > currentRow, "With every batch new row gets rendered.") && ok;
            ok = NotebookTestSupport.expect(logger, nextRow - currentRow <= 4, "One rendering turn never exceeds four rows.") && ok;
            currentRow = nextRow;
            numberOfRenderedBatches += 1;
        }

        ok = NotebookTestSupport.expectNumber(logger, currentRow, 128, "All rows get rendered.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, numberOfRenderedBatches, 32, "128 rows require 32 batches.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, ImageService.getNextRenderRow(126, 128), 128, "Partial final batch ends at the image height.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, ImageService.getNextRenderRow(128, 128), 128, "Completed image rendering ends at the image height.") && ok;
        return ok;
    }

    (:test)
    static function testFailedCommitPreservesExistingItem(logger as Logger) as Boolean {
        Storage.clearValues();
        var repository = new NotebookRepository();
        var directoryService = new DirectoryService(repository);
        var textService = new TextService(repository, directoryService);
        var imageService = new ImageService(repository, directoryService);

        var ok = NotebookTestSupport.expectNull(logger, directoryService.createDirectory("Existing"), "Creates directory.");
        var directoryId = NotebookTestSupport.getFirstDirectoryId(directoryService);
        if (directoryId == null) {
            logger.error("Existing directory has no id.");
            return false;
        }
        ok = NotebookTestSupport.expectNull(logger, textService.createTextItem(directoryId, "note.txt", "keep me"), "Creates a text item.") && ok;
        ok = NotebookTestSupport.expectNull(
            logger,
            imageService.beginImageTransfer("failed-commit", directoryId, "replacement.png", 64, 64, NotebookTestSupport.makePalette(), 4096, 2),
            "Begins image transfer."
        ) && ok;

        var pixels = NotebookTestSupport.makePixels(4096);
        ok = NotebookTestSupport.expectNull(logger, imageService.storeImageChunk("failed-commit", 0, pixels.slice(0, NotebookTestSupport.CHUNK_SIZE)), 
        "Stores first image chunk.") && ok;
        ok = NotebookTestSupport.expectNull(logger, imageService.storeImageChunk("failed-commit", 1, pixels.slice(NotebookTestSupport.CHUNK_SIZE, 4096)), 
        "Stores second image chunk.") && ok;

        ok = NotebookTestSupport.expectString(logger, imageService.commitImageTransfer("failed-commit", 0, 0), "Image checksum does not match.", 
        "Comming with incorrect checksums gets rejected.") && ok;

        var items = directoryService.getItems(directoryId);
        ok = NotebookTestSupport.expectNumber(logger, items.size(), 1, "Failed commit does not add an item.") && ok;
        if (items.size() == 1) {
            ok = NotebookTestSupport.expect(logger, directoryService.isTypeOf(items[0], "text"), "Existing item remains a text file.") && ok;
            ok = NotebookTestSupport.expectString(logger, items[0]["text"] as String?, "keep me", "Existing text remains unchanged.") && ok;
        }
        Storage.clearValues();
        return ok;
    }
}
