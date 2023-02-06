package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.RegExpFieldType;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 20.06.13
 */
public class RegExpFieldTypeTest extends Assert {

    @Test
    public void testParse() throws Exception {
        RegExpFieldType parser = new RegExpFieldType("([A-Z0-9]{2,6}[\\s\u00A0-]*[A-Z0-9]{1,6})");
        assertEquals("6W-761", parser.parse("Aeroflot 6W-761 ", null));
        assertEquals("PZ 111", parser.parse("asdasd d PZ 111 dsdsddsd", null));
        assertEquals("HZ 111", parser.parse("HZ\u00A0111", null));
    }
}
