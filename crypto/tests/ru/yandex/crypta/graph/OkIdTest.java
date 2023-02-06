package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class OkIdTest {
    @Test
    public void testIsValid() {
        OkId test1 = new OkId("");
        OkId test2 = new OkId("1234");
        OkId test3 = new OkId("001234");
        OkId test4 = new OkId("73645sdas");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "1234");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "001234");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "73645sdas");
        assertEquals(test4.isValid(), false);
    }

    @Test
    public void testgetType() {
        OkId test = new OkId("");
        assertEquals(test.getType(), EIdType.OK_ID);
    }
}
