package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class PuidTest {
    @Test
    public void testIsValid() {
        Puid test1 = new Puid("");
        Puid test2 = new Puid("0");
        Puid test3 = new Puid("12341234");
        Puid test4 = new Puid("-123521345");
        Puid test5 = new Puid("123987dfdsf");
        Puid test6 = new Puid("4294967294");
        Puid test7 = new Puid("4294967295");
        Puid test8 = new Puid("4294967296");
        Puid test9 = new Puid("42949672967645");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "0");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "12341234");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "-123521345");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "123987dfdsf");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "4294967294");
        assertEquals(test6.isValid(), true);
        assertEquals(test7.getValue(), "4294967295");
        assertEquals(test7.isValid(), true);
        assertEquals(test8.getValue(), "4294967296");
        assertEquals(test8.isValid(), true);
        assertEquals(test9.getValue(), "42949672967645");
        assertEquals(test9.isValid(), true);
    }

    @Test
    public void testgetType() {
        Puid test = new Puid("");
        assertEquals(test.getType(), EIdType.PUID);
    }
}
