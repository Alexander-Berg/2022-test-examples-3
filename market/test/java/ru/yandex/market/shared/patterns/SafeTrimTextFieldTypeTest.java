package ru.yandex.market.shared.patterns;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.SafeTrimTextFieldType;

/**
 * @author Tatiana Litvinenko <a href="mailto:tanlit@yandex-team.ru"/>
 * @date 27.01.14
 */
public class SafeTrimTextFieldTypeTest extends Assert {

    @Test
    public void testParse() throws Exception {
        SafeTrimTextFieldType parser = new SafeTrimTextFieldType();

        assertEquals("123 lll Title", parser.parse("     <p>123 lll<h1 attr=\"123\">Title</h1></p>   ", null));
        assertEquals("5 < 6 123 > lll Title", parser.parse(" 5 < 6 123 > lll<h1 attr=\"123\">Title</h1></p>   ", null));
        assertEquals("c1 > c2 > c3", parser.parse("c1 > c2 > c3", null));
        assertEquals("c1 < c2 < c3 < jj<>", parser.parse("c1 < c2 < c3 < jj<>", null));
        assertEquals("c1<c2", parser.parse("c1<c2<c3>", null));
        assertEquals("c1< 1>c2<c3", parser.parse("c1< 1>c2<c3", null));
        assertEquals("c1 < c2>", parser.parse("c1</t> < c2>", null));
        assertEquals("c1<<< c2> 123 text", parser.parse("c1<<< c2><g>123<p>text</p></g>", null));
    }
}
    