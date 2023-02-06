package ru.yandex.market.books.diff.util;

import junit.framework.TestCase;

import static ru.yandex.market.books.diff.util.CharsReplacer.toCyrillic;
import static ru.yandex.market.books.diff.util.CharsReplacer.toLatin;

/**
 * todo описать предназначение
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class CharReplacerTest extends TestCase {
    public void testToCyrillic() {
        assertEquals("", toCyrillic(""));
        assertEquals("авс", toCyrillic("abc"));
        assertEquals("хiх век", toCyrillic("xix век"));
    }

    public void testToLatin() {
        assertEquals("", toLatin(""));
        assertEquals("aбb", toLatin("абв"));
        assertEquals("xix bek", toLatin("xix век"));
    }

    public void testYeYo() {
        assertEquals("ЕеЁе", toCyrillic("ЕеЁё"));
        assertEquals("ееее", toCyrillic("ЕеЁё".toLowerCase()));
    }
}
