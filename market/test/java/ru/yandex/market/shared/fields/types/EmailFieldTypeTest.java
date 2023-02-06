package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.EmailFieldType;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 24.01.12
 */
public class EmailFieldTypeTest extends Assert {
    @Test
    public void testParse() throws Exception {
        EmailFieldType type = new EmailFieldType();
        assertEquals("test@test.ru", type.parse("mailto:test@test.ru", ""));
        assertEquals("вася@президент.рф", type.parse(" ввв вася@президент.рф    ввв", ""));
    }
}
