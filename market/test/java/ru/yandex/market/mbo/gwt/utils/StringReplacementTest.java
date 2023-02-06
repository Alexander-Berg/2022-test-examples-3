package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 23.09.2015
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class StringReplacementTest {
    @Test
    public void testCalculate() throws Exception {
        assertEquals("0:4:", str(StringReplacement.calculate("test", "")));
        assertEquals("0:0:added", str(StringReplacement.calculate("", "added")));
        assertEquals("0:4:more", str(StringReplacement.calculate("test", "more")));
        assertEquals("0:4:more-and-more", str(StringReplacement.calculate("test", "more-and-more")));
        assertEquals("6:3:yellow", str(StringReplacement.calculate("green-red-white", "green-yellow-white")));
        assertEquals("6:6:red", str(StringReplacement.calculate("green-yellow-white", "green-red-white")));

        assertEquals("13:5:brown", str(StringReplacement.calculate("green-yellow-white", "green-yellow-brown")));
        assertEquals("13:5:red", str(StringReplacement.calculate("green-yellow-white", "green-yellow-red")));
        assertEquals("13:5:red-and-brown", str(StringReplacement.calculate("green-yellow-white", "green-yellow-red-and-brown")));

        assertEquals("0:5:red", str(StringReplacement.calculate("green-yellow-white", "red-yellow-white")));
        assertEquals("0:5:red-and-pink", str(StringReplacement.calculate("green-yellow-white", "red-and-pink-yellow-white")));

        assertEquals("0:0:", str(StringReplacement.calculate("equals", "equals")));
        assertEquals("8:0: ", str(StringReplacement.calculate("with    spaces", "with     spaces")));
        assertEquals("8:1:", str(StringReplacement.calculate("with     spaces", "with    spaces")));

        assertEquals("right orientation remove", "12:4:", str(StringReplacement.calculate("foo bar bar bar pig", "foo bar bar pig")));
        assertEquals("right orientation add", "12:0:bar ", str(StringReplacement.calculate("foo bar bar pig", "foo bar bar bar pig")));
        assertEquals("pattern remove", "6:0:123", str(StringReplacement.calculate("123123", "123123123")));
        assertEquals("pattern add", "6:3:", str(StringReplacement.calculate("123123123", "123123")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateWithCaretPositionNoAmbiguous() throws Exception {
        StringReplacement.calculate("green-red-white", "green-yellow-white", 0);
    }

    @Test
    public void testCalculateWithCaretPositionAdding() throws Exception {
        assertEquals("0:0:123", str(StringReplacement.calculate("123123", "123123123", 3)));
    }


    @Test
    public void testCalculateWithCaretPositionRemoving() throws Exception {
        assertEquals("3:3:", str(StringReplacement.calculate("123123123", "123123", 3)));
    }

    @Test
    public void testCalculateWithCaretShiftRemove() throws Exception {
        assertEquals("1:2:", str(StringReplacement.calculate("121212", "1212", 1)));
        assertEquals("2:2:", str(StringReplacement.calculate("121212", "1212", 2)));
        assertEquals("3:2:", str(StringReplacement.calculate("121212", "1212", 3)));
    }

    @Test
    public void testCalculateWithCaretShiftAdd() throws Exception {
        assertEquals("0:0:12", str(StringReplacement.calculate("1212", "121212", 2)));
        assertEquals("1:0:21", str(StringReplacement.calculate("1212", "121212", 3)));
        assertEquals("2:0:12", str(StringReplacement.calculate("1212", "121212", 4)));
        assertEquals("3:0:21", str(StringReplacement.calculate("1212", "121212", 5)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateWithCaretWrongPosition() {
        StringReplacement.calculate("123", "123123", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCalculateWithCaretWrongTail() {
        StringReplacement.calculate("123", "123133", 3);
    }

    @Test
    public void testCalculateWithCaretEmptyChanges() throws Exception {
        assertEquals("0:0:", str(StringReplacement.calculate("qwerty", "qwerty", 0)));
        assertEquals("1:0:", str(StringReplacement.calculate("qwerty", "qwerty", 1)));
        assertEquals("2:0:", str(StringReplacement.calculate("qwerty", "qwerty", 2)));
        assertEquals("3:0:", str(StringReplacement.calculate("qwerty", "qwerty", 3)));
        assertEquals("4:0:", str(StringReplacement.calculate("qwerty", "qwerty", 4)));
        assertEquals("5:0:", str(StringReplacement.calculate("qwerty", "qwerty", 5)));
    }

    private static String str(Replacement replacement) {
        return replacement.getStart() + ":" + replacement.getLength() + ":" + replacement.getText();
    }
}

