package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class MmDeviceIdTest {
    @Test
    public void testIsValid() {
        MmDeviceId test1 = new MmDeviceId("");
        MmDeviceId test2 = new MmDeviceId("f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        MmDeviceId test3 = new MmDeviceId("f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf");
        MmDeviceId test4 = new MmDeviceId("f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        MmDeviceId test5 = new MmDeviceId("f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        MmDeviceId test6 = new MmDeviceId("f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf");
        MmDeviceId test7 = new MmDeviceId("f1d2aee4-0000-4e18-a612-4eab70dc2fcf");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcf");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2Fcf");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "f1d2aee4-fde7-4e18-a612-4eab70dc2fcfs");
        assertEquals(test4.isValid(), false);
        assertEquals(test5.getValue(), "f1d2aee4-fde7-4e18-a612-4ea2fcff1d2-0dc2fcf");
        assertEquals(test5.isValid(), false);
        assertEquals(test6.getValue(), "f1d2aee4-fde7-4e18-a612-4ed2-0d2fcf");
        assertEquals(test6.isValid(), false);
        assertEquals(test7.getValue(), "f1d2aee4-0000-4e18-a612-4eab70dc2fcf");
        assertEquals(test7.isValid(), true);
    }

    @Test
    public void testgetType() {
        MmDeviceId test = new MmDeviceId("");
        assertEquals(test.getType(), EIdType.MM_DEVICE_ID);
    }
}
