package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class Md5Test {
    @Test
    public void testIsValid() {
        Md5 test1 = new Md5("0123456789abcdefABCDEF9876543210");
        Md5 test2 = new Md5("0123456789abcdefABCDEF98765432100");
        Md5 test3 = new Md5("0123456789qbcdefABCDEF9876543210");

        assertEquals(test1.getValue(), "0123456789abcdefABCDEF9876543210");
        assertEquals(test1.isValid(), true);
        assertEquals(test2.getValue(), "0123456789abcdefABCDEF98765432100");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "0123456789qbcdefABCDEF9876543210");
        assertEquals(test3.isValid(), false);
    }

    @Test
    public void testgetType() {
        Md5 test = new Md5("");
        assertEquals(test.getType(), EIdType.MD5);
    }
}
