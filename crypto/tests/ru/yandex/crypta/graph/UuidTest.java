package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class UuidTest {
    @Test
    public void testIsValid() {
        Uuid test1 = new Uuid("");
        Uuid test2 = new Uuid("f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        Uuid test3 = new Uuid("f1d2aee4fde74e18a6124eab70dc2fcf");
        Uuid test4 = new Uuid("f1d2aee4fde74e18a6124eab70dc2Fcf");
        Uuid test5 = new Uuid("f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        Uuid test6 = new Uuid("f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        Uuid test7 = new Uuid("f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf");
        Uuid test8 = new Uuid("f1d2aee4-0000-4e18-a612-4eab70dc2fcf");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "f1d2aee4fde74e18a6124eab70dc2fcf");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "f1d2aee4fde74e18a6124eab70dc2Fcf");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        assertEquals(test6.isValid(), false);
        assertEquals(test7.getValue(), "f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf");
        assertEquals(test7.isValid(), false);
        assertEquals(test8.getValue(), "f1d2aee4-0000-4e18-a612-4eab70dc2fcf");
        assertEquals(test8.isValid(), false);
    }

    @Test
    public void testgetType() {
        Uuid test = new Uuid("");
        assertEquals(test.getType(), EIdType.UUID);
    }
}
