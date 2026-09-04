import Toybox.Application.Storage;
import Toybox.Lang;
import Toybox.Test;

class NotebookRepositoryTest {

    (:test)
    static function testSchemaUpgradeRemovesInvalidItem(logger as Logger) as Boolean {
        var directoryId = "d1";
        var invalidImageItemId = "i2";
        var textItemId = "i3";
        var validImageItemId = "i4";
        Storage.clearValues();
        Storage.setValue("notebook_schema", 1);
        Storage.setValue("notebook_next_id", 5);
        Storage.setValue("notebook_directory_ids", [directoryId]);
        Storage.setValue("notebook_dir_d1", {
            "id" => directoryId,
            "name" => "Production",
            "itemIds" => [invalidImageItemId, textItemId, validImageItemId]
        });
        Storage.setValue("notebook_item_i2", {
            "id" => invalidImageItemId,
            "name" => "obsolete.png",
            "type" => "image",
            "width" => 32,
            "height" => 32
        });
        Storage.setValue("notebook_item_i3", {
            "id" => textItemId,
            "name" => "note.txt",
            "type" => "text",
            "text" => "preserve me"
        });
        Storage.setValue("notebook_item_i4", {
            "id" => validImageItemId,
            "name" => "preserve.png",
            "type" => "image",
            "width" => 2,
            "height" => 2,
            "palette" => NotebookTestSupport.makePalette(),
            "pixels" => NotebookTestSupport.makePixels(4)
        } as Dictionary<Storage.KeyType, Storage.ValueType>);

        var repository = new NotebookRepository();
        var directoryService = new DirectoryService(repository);
        var items = directoryService.getItems(directoryId);

        var ok = NotebookTestSupport.expectNumber(logger, items.size(), 2, "Schema migration removes only invalid items.");
        if (items.size() == 2) {
            ok = NotebookTestSupport.expect(logger, directoryService.isTypeOf(items[0], "text"), "Schema migration preserves valid text.") && ok;
            ok = NotebookTestSupport.expectString(logger, items[0]["text"] as String?, "preserve me", "Schema migration preserves text content.") && ok;
            ok = NotebookTestSupport.expect(logger, directoryService.isTypeOf(items[1], "image"), "Schema migration preserves valid image.") && ok;
            var storedPalette = items[1]["palette"];
            if (storedPalette instanceof Array) {
                ok = NotebookTestSupport.expectNumber(logger, (storedPalette as Array<Object>).size(), 64, "Valid image palette contains 64 colors") && ok;
            } else {
                logger.error("Valid image palette is missing or is not an Array.");
                ok = false;
            }
            var storedPixels = items[1]["pixels"];
            if (storedPixels instanceof ByteArray) {
                ok = NotebookTestSupport.expectNumber(logger, (storedPixels as ByteArray).size(), 4, "Valid image contains four pixel indices") && ok;
            } else {
                logger.error("Valid image pixel data is missing or is not a ByteArray.");
                ok = false;
            }
        }
        ok = NotebookTestSupport.expect(logger, Storage.getValue("notebook_item_i2") == null, "Schema migration deletes invalid item storage.") && ok;
        ok = NotebookTestSupport.expectNumber(logger, Storage.getValue("notebook_schema") as Number, 2, "Schema migration advances version.") && ok;
        Storage.clearValues();
        return ok;
    }
}
