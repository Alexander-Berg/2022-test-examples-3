package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class GaidTest {
    @Test
    public void testIsValid() {
        Gaid test1 = new Gaid("");
        Gaid test2 = new Gaid("f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        Gaid test3 = new Gaid("f1d2aee4-fde7-4e18-a612-4eab70dc2Qcf");
        Gaid test4 = new Gaid("f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        Gaid test5 = new Gaid("f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        Gaid test6 = new Gaid("f1d2aee4-0000-4e18-a612-4eab70dc2fcf");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2Qcf");
        assertEquals(test3.isValid(), false);
        assertEquals(test4.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "f1d2aee4-0000-4e18-a612-4eab70dc2fcf");
        assertEquals(test6.isValid(), true);
    }

    @Test
    public void testgetType() {
        Gaid test = new Gaid("");
        assertEquals(test.getType(), EIdType.GAID);
    }
}
