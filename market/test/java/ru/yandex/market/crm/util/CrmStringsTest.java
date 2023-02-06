package ru.yandex.market.crm.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CrmStringsTest {

    private void checkAbbreviate(String expected, String str, int maxLength) {
        Assertions.assertEquals(expected, CrmStrings.abbreviate(str, maxLength));
    }

    @Test
    public void abbreviateNull() {
        checkAbbreviate(null, null, 10);
    }

    @Test
    public void abbreviateEmpty() {
        checkAbbreviate("", "", 4);
    }

    @Test
    public void abbreviateLatin() {
        checkAbbreviate("abc...", "abcdefg", 6);
        checkAbbreviate("abcdefg", "abcdefg", 7);
        checkAbbreviate("abcdefg", "abcdefg", 8);
        checkAbbreviate("a...", "abcdefg", 4);
    }

    @Test
    public void abbreviateTooShort() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CrmStrings.abbreviate("abcdefg", 3));
    }

    @Test
    public void abbreviateSurrogatePairs() {
        // PILE OF POO = "\uD83D\uDCA9". see: https://www.fileformat.info/info/unicode/char/1f4a9/index.htm
        checkAbbreviate("abc...", "abc\uD83D\uDCA9defg", 7);
    }

    private void checkTransliterate(String expected, String str) {
        Assertions.assertEquals(expected, CrmStrings.transliterate(str));
    }

    @Test
    public void transliterate() {
        checkTransliterate("Hello world", "Hello world");
        checkTransliterate("Privet, mir", "Привет, мир");
        checkTransliterate("zdAa!", "ζδΑα!");
        checkTransliterate("\uD83D\uDC4D", "\uD83D\uDC4D");
    }

    private void checkIdentifier(String expected, String str, int maxLength) {
        Assertions.assertEquals(expected, CrmStrings.makeIdentifier(str, maxLength));
    }

    @Test
    public void makeIdentifier() {
        checkIdentifier("Hello_world", "Hello world", 0);
        checkIdentifier("Hello_world", "*Hello world!", 0);
        checkIdentifier("Privet_mir", "Привет, мир", 0);
        checkIdentifier("o_w", "o\uD83D\uDC4Dw", 0);
        checkIdentifier("qwert", "qwerty", 5);
        checkIdentifier("qwerty", "qwerty", -2);
    }
}