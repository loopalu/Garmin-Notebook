import Toybox.Lang;
import Toybox.Test;

class NotebookTestSupport {

    static const CHUNK_SIZE = 2048;

    static function getFirstDirectoryId(directoryService as DirectoryService) as String? {
        var directory = directoryService.getDirectoryAt(0);
        if (directory == null || !(directory["id"] instanceof String)) {
            return null;
        }
        return directory["id"] as String;
    }

    static function makePalette() as Array<Object> {
        var palette = [] as Array<Object>;
        for (var index = 0; index < 64; index++) {
            var component = index * 4;
            palette.add((component << 16) | (component << 8) | component);
        }
        return palette;
    }

    static function makePixels(size as Number) as ByteArray {
        var pixels = []b;
        for (var index = 0; index < size; index++) {
            pixels.add((index * 37 + 11) % 64);
        }
        return pixels;
    }

    static function getChecksumFor(pixels as ByteArray) as Array<Number> {
        var checksumA = 1;
        var checksumB = 0;
        for (var index = 0; index < pixels.size(); index++) {
            checksumA = (checksumA + (pixels[index] as Number)) % 65521;
            checksumB = (checksumB + checksumA) % 65521;
        }
        return [checksumA, checksumB];
    }

    static function expect(logger as Logger, condition as Boolean, description as String) as Boolean {
        if (!condition) {
            logger.error(description);
        }
        return condition;
    }

    static function expectNull(logger as Logger, actual as Object?, description as String) as Boolean {
        return expect(logger, actual == null, description + "; actual=" + actual);
    }

    static function expectNumber(logger as Logger, actual as Number, expected as Number, description as String) as Boolean {
        return expect(logger, actual == expected, description + "; expected=" + expected + ", actual=" + actual);
    }

    static function expectString(logger as Logger, actual as String?, expected as String, description as String) as Boolean {
        return expect(logger, actual != null && (actual as String).equals(expected), description + "; expected=" + expected + ", actual=" + actual);
    }

    static function expectBytes(logger as Logger, actual as ByteArray?, expected as ByteArray, description as String) as Boolean {
        return expect(logger, actual != null && (actual as ByteArray).equals(expected), description);
    }
}
