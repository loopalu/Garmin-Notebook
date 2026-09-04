import Toybox.Lang;

//! Decodes Base64 image data received from the Android application.
//!
//! Phone messages transport binary image chunks as Base64 strings. This class
//! validates that representation, converts each four-character block into at
//! most three bytes, handles final-block padding, and rejects malformed input.
class Base64Decoder {

    private static const UPPERCASE_A_CODE = 65;
    private static const UPPERCASE_Z_CODE = 90;
    private static const LOWERCASE_A_CODE = 97;
    private static const LOWERCASE_Z_CODE = 122;
    private static const DIGIT_ZERO_CODE = 48;
    private static const DIGIT_NINE_CODE = 57;
    private static const PLUS_CODE = 43;
    private static const SLASH_CODE = 47;
    private static const PADDING_CODE = 61;

    private static const LOWERCASE_VALUE_OFFSET = 26;
    private static const DIGIT_VALUE_OFFSET = 52;
    private static const PLUS_VALUE = 62;
    private static const SLASH_VALUE = 63;
    private static const PADDING_VALUE = -2;
    private static const INVALID_VALUE = -1;
    private static const LOWER_FOUR_BITS_MASK = 0x0F;
    private static const LOWER_TWO_BITS_MASK = 0x03;

    //! Decode a complete Base64 string into its binary representation.
    //! @param base64String Base64 text containing zero or more four-character blocks
    //! @return The decoded bytes, or null when the input is malformed
    static function decode(base64String as String) as ByteArray? {
        if (base64String.length() == 0) {
            return []b;
        }
        if ((base64String.length() % 4) != 0) {
            return null;
        }

        var charArray = base64String.toCharArray();
        var output = []b;

        for (var index = 0; index < charArray.size(); index += 4) {
            if (!decodeBlock(charArray, index, output)) {
                return null;
            }
        }
        return output;
    }

    //! Decode one four-character Base64 block and append its bytes to the output.
    //! @param charArray All characters from the encoded Base64 string
    //! @param startIndex Index of the first character in the current block
    //! @param output Destination byte array that receives up to three decoded bytes
    //! @return true when the block and its padding are valid; otherwise false
    private static function decodeBlock(charArray as Array<Char>, startIndex as Number, output as ByteArray) as Boolean {
        var firstBase64CharacterValue = value(charArray[startIndex].toNumber());
        var secondBase64CharacterValue = value(charArray[startIndex + 1].toNumber());
        var thirdBase64CharacterValue = value(charArray[startIndex + 2].toNumber());
        var fourthBase64CharacterValue = value(charArray[startIndex + 3].toNumber());

        if (firstBase64CharacterValue < 0 || secondBase64CharacterValue < 0) {
            return false;
        }
        //! Shift first six bits two places left, keep two highest bits of the second, and combine these with bitwise OR.
        output.add((firstBase64CharacterValue << 2) | (secondBase64CharacterValue >> 4));

        if (thirdBase64CharacterValue == PADDING_VALUE) {
            return fourthBase64CharacterValue == PADDING_VALUE && startIndex + 4 == charArray.size();
        }
        if (thirdBase64CharacterValue < 0) {
            return false;
        }
        //! Apply bit masks.
        output.add(((secondBase64CharacterValue & LOWER_FOUR_BITS_MASK) << 4) | (thirdBase64CharacterValue >> 2));

        if (fourthBase64CharacterValue == PADDING_VALUE) {
            return startIndex + 4 == charArray.size();
        }
        if (fourthBase64CharacterValue < 0) {
            return false;
        }
        output.add(((thirdBase64CharacterValue & LOWER_TWO_BITS_MASK) << 6) | fourthBase64CharacterValue);
        return true;
    }

    //! Convert one Base64 character code into its six-bit numeric value.
    //! @param code Numeric character code from the encoded input
    //! @return A decoded alphabet value, PADDING_VALUE, or INVALID_VALUE
    private static function value(code as Number) as Number {
        if (code >= UPPERCASE_A_CODE && code <= UPPERCASE_Z_CODE) {
            return code - UPPERCASE_A_CODE;
        }
        if (code >= LOWERCASE_A_CODE && code <= LOWERCASE_Z_CODE) {
            return code - LOWERCASE_A_CODE + LOWERCASE_VALUE_OFFSET;
        }
        if (code >= DIGIT_ZERO_CODE && code <= DIGIT_NINE_CODE) {
            return code - DIGIT_ZERO_CODE + DIGIT_VALUE_OFFSET;
        }
        if (code == PLUS_CODE) {
            return PLUS_VALUE;
        }
        if (code == SLASH_CODE) {
            return SLASH_VALUE;
        }
        if (code == PADDING_CODE) {
            return PADDING_VALUE;
        }
        return INVALID_VALUE;
    }
}
