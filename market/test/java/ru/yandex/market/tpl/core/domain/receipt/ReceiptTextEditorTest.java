package ru.yandex.market.tpl.core.domain.receipt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author sekulebyakin
 */
class ReceiptTextEditorTest {

    private final ReceiptTextEditor textEditor = new ReceiptTextEditor();

    @Test
    void replaceInvalidCharsSuccessTest() {
        var result = textEditor.replaceInvalidChars("\u0080тест\tstring,\u00a0with\n\r3π invalid   chars.\t");
        assertEquals("тест string, with 3 invalid chars.", result);
    }

    @Test
    void replaceInvalidCharsEmptyInput() {
        var emptyString = "";
        assertSame(emptyString, textEditor.replaceInvalidChars(emptyString));
        assertNull(textEditor.replaceInvalidChars(null));
    }
}
