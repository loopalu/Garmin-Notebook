import Toybox.Lang;
import Toybox.Test;

class Base64DecoderTest {

    (:test)
    static function testBase64Decoding(logger as Logger) as Boolean {
        var ok = true;
        ok = NotebookTestSupport.expectBytes(logger, Base64Decoder.decode(""), []b, "Decodes empty base64 string.") && ok;
        ok = NotebookTestSupport.expectBytes(logger, Base64Decoder.decode("TQ=="), [77]b, "Decodes one base64 byte.") && ok;
        ok = NotebookTestSupport.expectBytes(logger, Base64Decoder.decode("TWE="), [77, 97]b, "Decodes two base64 bytes.") && ok;
        ok = NotebookTestSupport.expectBytes(logger, Base64Decoder.decode("TWFu"), [77, 97, 110]b, "Decodes three base64 bytes.") && ok;
        ok = NotebookTestSupport.expectBytes(logger, Base64Decoder.decode("TWFuTWE="), [77, 97, 110, 77, 97]b, "Decodes multiple base64 bytes.") && ok;
        ok = NotebookTestSupport.expect(logger, Base64Decoder.decode("abc") == null, "Rejects a non-multiple-of-four length.") && ok;
        ok = NotebookTestSupport.expect(logger, Base64Decoder.decode("TQ=A") == null, "Rejects invalid padding.") && ok;
        ok = NotebookTestSupport.expect(logger, Base64Decoder.decode("TW$u") == null, "Rejects an invalid alphabet character.") && ok;
        return ok;
    }
}
