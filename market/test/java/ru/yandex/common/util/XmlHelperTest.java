package ru.yandex.common.util;

import junit.framework.TestCase;

/**
 * @author traff
 */
public class XmlHelperTest extends TestCase {
    public void testEscape() {
        String text = XmlHelper.escapeCharacters("текст & text и <т-е-к-с-т> and \"text'");
        assertEquals("текст &amp; text и &lt;т-е-к-с-т&gt; and \"text'", text);
    }
}
