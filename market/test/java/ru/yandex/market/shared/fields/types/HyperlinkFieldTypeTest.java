package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.HyperlinkFieldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 24.01.12
 */
public class HyperlinkFieldTypeTest extends Assert {
    @Test
    public void testParse() throws Exception {
        HyperlinkFieldType type = new HyperlinkFieldType();
        assertEquals("http://www.mail.ru", type.parse("  www.mail.ru  ", ""));
        assertEquals("http://www.foresta.ru", type.parse("http://www.foresta.ru,", ""));
        assertEquals("http://www.mail.ru/hello/", type.parse("a  http://www.mail.ru/hello/  yandex.ru", ""));
        assertEquals("http://www.mercure-helios.polturizm.ru", type.parse("http://www.mercure-helios.polturizm.ru,<br>", ""));
        List<Object> values = new ArrayList<>();
        type.parse(values, "  http://mail.ru  http://yandex.ru  a", "", Collections.<String, String>emptyMap());
        List<Object> expected = new ArrayList<>();
        expected.add("http://mail.ru");
        expected.add("http://yandex.ru");
        assertEquals(expected, values);
    }
}
