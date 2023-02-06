package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.PhoneFieldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"/>
 * @date 26.01.12
 */
public class PhoneFieldTypeTest extends Assert {
    @Test
    public void testParse() throws Exception {
        PhoneFieldType phoneField = new PhoneFieldType();
        assertEquals("+7 916 178-87-90", phoneField.parse(" +7 916 178-87-90 ", ""));
        assertEquals("516-33-38; 567-88-88", phoneField.parse(" 516-33-38, 567-88-88 ", ""));
        assertEquals("+380 44 502-65-66", phoneField.parse("asdas +380 44 502-65-66 asdasdasd", ""));

        assertEquals("+7 (8652) 21-89-96; +7 (928) 321-89-96",
            phoneField.parse(" +7 (8652) 21-89-96 +7 (928) 321-89-96 ", ""));

        assertEquals("+7 (865) 12-34; +7 (928) 56-96", phoneField.parse("+7 (865) 12-34 +7 (928) 56-96 ", ""));
        assertEquals("+7 (865) 12-34; +7 (928) 56-96",
            phoneField.parse(" wow now +7 (865) 12-34 qwa qwa +7 (928) 56-96 here 23 and there1!1 ", ""));
    }

}
