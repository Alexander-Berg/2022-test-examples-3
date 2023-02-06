package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * created on 28.10.2016
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PositionLineReaderTest {

    @Test
    public void oneLine() {
        String s = "hello world";
        PositionLineReader reader = new PositionLineReader(s);
        assertEquals(0, reader.getPosition());
        assertEquals(0, reader.getLineNumber());
        assertEquals(s, reader.readLine());
        assertNull(reader.readLine());
        assertEquals(1, reader.getLineNumber());
        assertEquals(s.length(), reader.getPosition());
    }

    @Test
    public void multipleLines() {
        String first = "hello world";
        String second = "try 123";

        String str =
            first + "\n" +
            "hello\n" +
            "postman\n" +
            second + "\n" +
            "  usage few times\n" +
            "qwerty\n" +
            "rty";

        PositionLineReader reader = new PositionLineReader(str);

        assertEquals(first, reader.readLine());
        assertEquals(12, reader.getPosition());

        reader.skipLines(2);

        assertEquals(second, reader.readLine());
        assertEquals(4, reader.getLineNumber());
        assertEquals(34, reader.getPosition());
    }

    @Test
    public void multipleLfs() {
        String first = "help123 33";
        String second = "1434234";

        String str =
            "\r\n" +
            "\r\n" +
            first + "\r\n" +
            "\r\n" +
            second;

        PositionLineReader reader = new PositionLineReader(str);

        assertEquals("", reader.readLine());
        assertEquals(1, reader.getLineNumber());
        assertEquals(2, reader.getPosition());
        assertEquals("", reader.readLine());
        assertEquals(2, reader.getLineNumber());
        assertEquals(first, reader.readLine());
        assertEquals(3, reader.getLineNumber());
        assertEquals(16, reader.getPosition());
        assertEquals("", reader.readLine());
        assertEquals(4, reader.getLineNumber());
        assertEquals(second, reader.readLine());
        assertNull(reader.readLine());
        assertEquals(str.length(), reader.getPosition());
    }

    @Test
    public void onlyCarriage() {
        String first = "help";
        String second = "123";
        String third = "help";

        String str =
            "\r" +
            first + "\r" +
            second + "\r" +
            "\r" +
            third;

        PositionLineReader reader = new PositionLineReader(str);

        assertEquals("", reader.readLine());
        assertEquals(1, reader.getLineNumber());
        assertEquals(1, reader.getPosition());
        assertEquals(first, reader.readLine());
        assertEquals(second, reader.readLine());
        assertEquals("", reader.readLine());
        assertEquals(third, reader.readLine());
        assertEquals(5, reader.getLineNumber());
    }
}
