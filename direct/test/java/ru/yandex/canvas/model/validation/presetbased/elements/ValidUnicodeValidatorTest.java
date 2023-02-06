package ru.yandex.canvas.model.validation.presetbased.elements;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidUnicodeValidatorTest {

    @Test
    public void testASCII() {
        assertTrue(ValidUnicodeSymbolsValidator.isValid("qwe"));
    }

    @Test
    public void testCyr() {
        assertTrue(ValidUnicodeSymbolsValidator.isValid("йцу"));
    }

    @Test
    public void testNull() {
        assertTrue(ValidUnicodeSymbolsValidator.isValid(null));
    }

    @Test
    public void testEmpty() {
        assertTrue(ValidUnicodeSymbolsValidator.isValid(""));
    }

    @Test
    public void testUnicodeForkSymbol() {
        assertFalse(ValidUnicodeSymbolsValidator.isValid("qwe \uD83C\uDF74 qwe"));
    }

    @Test
    public void testAllowedSpecSymbols() {
        assertTrue(ValidUnicodeSymbolsValidator.isValid("™®©’°²\u20BD"));
    }
}
