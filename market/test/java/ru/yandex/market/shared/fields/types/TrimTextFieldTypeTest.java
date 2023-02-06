package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.TrimTextFieldType;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 06.09.11
 */
public class TrimTextFieldTypeTest extends Assert {
    private final TrimTextFieldType trimTextFieldType = new TrimTextFieldType();

    @Test
    public void testParse() throws Exception {
        assertEquals("test", parse("     test    "));
        assertEquals("a b c", parse("a\nb\tc"));
        assertEquals("test", parse("\"test\""));
        assertEquals("a b", parse("a   b"));
        assertEquals("a b c", parse("a   b  c"));
        assertEquals("test", parse('\u00A0' + "test")); //No-Break space
        assertEquals("test", parse(" '   \"  test \" ' "));
        assertEquals("test \"abc\"", parse("test  \"abc\""));
        assertEquals("\"", parse("\""));

    }

    private String parse(String string) {
        return (String) trimTextFieldType.parse(string, "");
    }
}
