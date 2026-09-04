import Toybox.Graphics;
import Toybox.Lang;

//! Owns image validation, pixel decoding, rendering calculations, creation,
//! and chunked transfer lifecycle.
class ImageService {

    private static const ROWS_PER_UPDATE = 4;
    private static const MAX_IMAGE_DIMENSION = 128;

    private const MAX_TRANSFER_ID_LENGTH = 64;
    private const IMAGE_TRANSFER_CHUNK_SIZE = 2048;
    private const MAX_IMAGE_TRANSFER_CHUNKS = 64;
    private const ADLER_MODULUS = 65521;

    private var repository as NotebookRepository;
    private var directoryService as DirectoryService;

    static function getRowsPerUpdate() as Number {
        return ROWS_PER_UPDATE;
    }

    static function getNextRenderRow(currentRow as Number, height as Number) as Number {
        var nextRow = currentRow + ROWS_PER_UPDATE;
        return nextRow < height ? nextRow : height;
    }

    static function getExpectedPixelSize(width as Number, height as Number) as Number {
        return width * height;
    }

    static function validateImageData(
        width as Number,
        height as Number,
        palette as Array<Object>,
        pixelSize as Number
    ) as String? {
        if (width < 1 || height < 1 || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            return "Image dimensions are invalid.";
        }
        if (palette.size() != 64) {
            return "Image palette size is invalid.";
        }
        for (var paletteIndex = 0; paletteIndex < palette.size(); paletteIndex++) {
            if (!(palette[paletteIndex] instanceof Number)) {
                return "Image palette is invalid.";
            }
            var color = palette[paletteIndex] as Number;
            if (color < 0 || color > 0xFFFFFF) {
                return "Image palette is invalid.";
            }
        }
        return pixelSize == getExpectedPixelSize(width, height) ? null : "Image pixel data is incomplete.";
    }

    static function getPaletteIndexAt(
        pixels as ByteArray,
        width as Number,
        x as Number,
        y as Number
    ) as Number {
        return pixels[y * width + x] as Number;
    }

    static function drawImageRow(
        display as Dc,
        y as Number,
        width as Number,
        scale as Number,
        palette as Array<Object>,
        pixels as ByteArray
    ) as Boolean {
        var currentXCoordinate = 0;
        var currentPaletteIndex = getPaletteIndexAt(pixels, width, 0, y);
        if (currentPaletteIndex < 0 || currentPaletteIndex >= palette.size()) {
            return false;
        }

        for (var x = 1; x <= width; x++) {
            var nextPaletteIndex = -1;
            if (x < width) {
                nextPaletteIndex = getPaletteIndexAt(pixels, width, x, y);
                if (nextPaletteIndex < 0 || nextPaletteIndex >= palette.size()) {
                    return false;
                }
            }
            if (x == width || nextPaletteIndex != currentPaletteIndex) {
                display.setColor(palette[currentPaletteIndex] as Number, Graphics.COLOR_TRANSPARENT);
                display.fillRectangle(currentXCoordinate * scale, y * scale, (x - currentXCoordinate) * scale, scale);
                currentXCoordinate = x;
                currentPaletteIndex = nextPaletteIndex;
            }
        }
        return true;
    }

    function initialize(repository as NotebookRepository, directoryService as DirectoryService) {
        self.repository = repository;
        self.directoryService = directoryService;
    }

    function createImageItem(
        directoryId as String,
        name as String,
        width as Number,
        height as Number,
        palette as Array<Object>,
        pixels as ByteArray
    ) as String? {
        var validationError = directoryService.validateNewItem(directoryId, name);
        if (validationError != null) {
            return validationError;
        }
        var imageError = validateImageData(width, height, palette, pixels.size());
        if (imageError != null) {
            return imageError;
        }

        return directoryService.addItem(directoryId, {
            "id" => repository.setNextId("i"),
            "name" => name,
            "type" => "image",
            "width" => width,
            "height" => height,
            "palette" => palette,
            "pixels" => pixels
        });
    }

    function beginImageTransfer(
        transferId as String,
        directoryId as String,
        name as String,
        width as Number,
        height as Number,
        palette as Array<Object>,
        totalBytes as Number,
        totalChunks as Number
    ) as String? {
        if (transferId.length() == 0 || transferId.length() > MAX_TRANSFER_ID_LENGTH) {
            return "Transfer ID is invalid.";
        }
        var validationError = directoryService.validateNewItem(directoryId, name);
        if (validationError != null) {
            return validationError;
        }
        var imageError = validateImageData(width, height, palette, totalBytes);
        if (imageError != null) {
            return imageError;
        }
        var expectedChunks = (totalBytes + IMAGE_TRANSFER_CHUNK_SIZE - 1) / IMAGE_TRANSFER_CHUNK_SIZE;
        if (totalChunks < 1 || totalChunks > MAX_IMAGE_TRANSFER_CHUNKS || totalChunks != expectedChunks) {
            return "Image chunk count is invalid.";
        }

        repository.cleanupActiveImageTransfer();
        repository.saveActiveImageTransfer({
            "transferId" => transferId,
            "directoryId" => directoryId,
            "name" => name,
            "width" => width,
            "height" => height,
            "palette" => palette,
            "totalBytes" => totalBytes,
            "totalChunks" => totalChunks,
            "nextChunk" => 0,
            "checksumA" => 1,
            "checksumB" => 0
        });
        return null;
    }

    function storeImageChunk(transferId as String, chunkIndex as Number, pixels as ByteArray) as String? {
        var transfer = repository.getActiveImageTransfer();
        if (transfer == null || !(transfer["transferId"] instanceof String) ||
            !(transfer["nextChunk"] instanceof Number) || !(transfer["totalChunks"] instanceof Number) ||
            !(transfer["totalBytes"] instanceof Number) || !(transfer["checksumA"] instanceof Number) ||
            !(transfer["checksumB"] instanceof Number)) {
            return "Image transfer is not active.";
        }
        if (!(transfer["transferId"] as String).equals(transferId)) {
            return "Image transfer ID does not match.";
        }
        var nextChunk = transfer["nextChunk"] as Number;
        var totalChunks = transfer["totalChunks"] as Number;
        var totalBytes = transfer["totalBytes"] as Number;
        if (chunkIndex != nextChunk || chunkIndex < 0 || chunkIndex >= totalChunks) {
            return "Image chunk is out of sequence.";
        }
        var remainingBytes = totalBytes - chunkIndex * IMAGE_TRANSFER_CHUNK_SIZE;
        var expectedSize = remainingBytes < IMAGE_TRANSFER_CHUNK_SIZE ? remainingBytes : IMAGE_TRANSFER_CHUNK_SIZE;
        if (pixels.size() != expectedSize) {
            return "Image chunk size is invalid.";
        }

        repository.saveImageChunk(transferId, chunkIndex, pixels);
        var checksumA = transfer["checksumA"] as Number;
        var checksumB = transfer["checksumB"] as Number;
        for (var index = 0; index < pixels.size(); index++) {
            checksumA = (checksumA + (pixels[index] as Number)) % ADLER_MODULUS;
            checksumB = (checksumB + checksumA) % ADLER_MODULUS;
        }
        transfer["nextChunk"] = nextChunk + 1;
        transfer["checksumA"] = checksumA;
        transfer["checksumB"] = checksumB;
        repository.saveActiveImageTransfer(transfer);
        return null;
    }

    function commitImageTransfer(transferId as String, checksumA as Number, checksumB as Number) as String? {
        var transfer = repository.getActiveImageTransfer();
        if (transfer == null || !(transfer["transferId"] instanceof String) ||
            !(transfer["nextChunk"] instanceof Number) || !(transfer["totalChunks"] instanceof Number) ||
            !(transfer["totalBytes"] instanceof Number) || !(transfer["checksumA"] instanceof Number) ||
            !(transfer["checksumB"] instanceof Number)) {
            return "Image transfer is not active.";
        }
        if (!(transfer["transferId"] as String).equals(transferId)) {
            return "Image transfer ID does not match.";
        }
        var totalChunks = transfer["totalChunks"] as Number;
        if ((transfer["nextChunk"] as Number) != totalChunks) {
            return "Image transfer is incomplete.";
        }
        if ((transfer["checksumA"] as Number) != checksumA || (transfer["checksumB"] as Number) != checksumB) {
            return "Image checksum does not match.";
        }

        var pixels = []b;
        for (var chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
            var chunk = repository.getImageChunk(transferId, chunkIndex);
            if (chunk == null) {
                return "Image chunk is missing.";
            }
            pixels.addAll(chunk);
        }
        if (pixels.size() != (transfer["totalBytes"] as Number) ||
            !(transfer["directoryId"] instanceof String) || !(transfer["name"] instanceof String) ||
            !(transfer["width"] instanceof Number) || !(transfer["height"] instanceof Number) ||
            !(transfer["palette"] instanceof Array)) {
            return "Image transfer metadata is invalid.";
        }

        var error = createImageItem(
            transfer["directoryId"] as String,
            transfer["name"] as String,
            transfer["width"] as Number,
            transfer["height"] as Number,
            transfer["palette"] as Array<Object>,
            pixels
        );
        if (error != null) {
            return error;
        }
        repository.cleanupActiveImageTransfer();
        return null;
    }
}
