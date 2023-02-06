package ru.yandex.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.utils.PrettyPrint.*;


public class PrettyPrintTest {
    @Test
    public void testPrettyPrintTime() {
        assertEquals("1ms", prettyPrintTime(1));
        assertEquals("100ms", prettyPrintTime(100));
        assertEquals("2m3s330ms", prettyPrintTime(123330));
        assertEquals("2m", prettyPrintTime(120000));

        checkTime(2, 3, 4, 5, 6, 7, "2w3d4h5m6s7ms");
//        checkTime(2, 0, 4, 0, 0, 0, "2w4h0m0s0ms");
    }

    private void checkTime(int weeks, int days, int hours, int minutes, int seconds, int millis, String expectedString) {
        assertEquals(
            expectedString,
            prettyPrintTime(((((((weeks * 7) + days) * 24) + hours) * 60 + minutes) * 60 + seconds) * 1000 + millis)
        );
    }

    @Test
    public void testPrettyPrintCapacity() {
        assertEquals("1b", prettyPrintCapacity(1, Capacity.b));
        assertEquals("0kb", prettyPrintCapacity(1, Capacity.Kb));
        assertEquals("0tb", prettyPrintCapacity(1, Capacity.Tb));

        assertEquals("1kb", prettyPrintCapacity(1024, Capacity.b));
        assertEquals("1kb", prettyPrintCapacity(1024, Capacity.Kb));
        assertEquals("0mb", prettyPrintCapacity(1024, Capacity.Mb));

        assertEquals("1kb1b", prettyPrintCapacity(1025, Capacity.b));
        assertEquals("1kb", prettyPrintCapacity(1025, Capacity.Kb));
        assertEquals("0mb", prettyPrintCapacity(1025, Capacity.Mb));

        final long hugeVolume = ((((1L * 1024 + 2) * 1024 + 3) * 1024 + 4) * 1024 + 5) * 1024 + 6;
        assertEquals("1pb2tb3gb4mb5kb6b", prettyPrintCapacity(hugeVolume, Capacity.b));
        assertEquals("1pb2tb3gb4mb5kb", prettyPrintCapacity(hugeVolume, Capacity.Kb));
        assertEquals("1pb2tb3gb4mb", prettyPrintCapacity(hugeVolume, Capacity.Mb));
        assertEquals("1pb2tb3gb", prettyPrintCapacity(hugeVolume, Capacity.Gb));
        assertEquals("1pb2tb", prettyPrintCapacity(hugeVolume, Capacity.Tb));
        assertEquals("1pb", prettyPrintCapacity(hugeVolume, Capacity.Pb));
    }
}
