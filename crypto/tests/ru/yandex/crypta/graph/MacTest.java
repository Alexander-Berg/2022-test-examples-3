package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class MacTest {
    @Test
    public void testIsValid() {
        Mac test1 = new Mac("00:11:22:33:ee:c1");
        Mac test2 = new Mac("11:FF:22:AA:33:CC");
        Mac test3 = new Mac("001122334455");
        Mac test4 = new Mac("00:11:22:33:ee:u1");
        Mac test5 = new Mac("0011222334455");

        assertEquals(test1.getValue(), "00:11:22:33:ee:c1");
        assertEquals(test1.isValid(), true);
        assertEquals(test2.getValue(), "11:FF:22:AA:33:CC");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "001122334455");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "00:11:22:33:ee:u1");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "0011222334455");
        assertEquals(test5.isValid(), false);
    }

    @Test
    public void testgetType() {
        Mac test = new Mac("");
        assertEquals(test.getType(), EIdType.MAC);
    }
}
