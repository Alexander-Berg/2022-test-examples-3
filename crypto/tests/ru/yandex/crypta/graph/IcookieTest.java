package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class IcookieTest {
    @Test
    public void testIsValid() {
        Icookie test1 = new Icookie("");
        Icookie test2 = new Icookie("0");
        Icookie test3 = new Icookie("-1");
        Icookie test4 = new Icookie("10113701529442803");
        Icookie test5 = new Icookie("0011188541530035229");
        Icookie test6 = new Icookie("8322285710565070336");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "0");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "-1");
        assertEquals(test3.isValid(), false);
        assertEquals(test4.getValue(), "10113701529442803");
        assertEquals(test4.isValid(), true);
        assertEquals(test5.getValue(), "0011188541530035229");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "8322285710565070336");
        assertEquals(test6.isValid(), true);
    }

    @Test
    public void testgetType() {
        Icookie test = new Icookie("");
        assertEquals(test.getType(), EIdType.ICOOKIE);
    }
}
