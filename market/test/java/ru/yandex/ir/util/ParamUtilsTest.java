package ru.yandex.ir.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.ir.util.ParamUtils.parseTimeDuration;
import static ru.yandex.ir.util.ParamUtils.parseVolume;


public class ParamUtilsTest {
    @Test
    public void testParseTimeDuration() {
        assertEquals(1, parseTimeDuration("1ms"));
        assertEquals(2000, parseTimeDuration("2s"));
        assertEquals(3L * 60 * 1000, parseTimeDuration("3m"));
        assertEquals(2L * 60 * 60 * 1000, parseTimeDuration("2h"));
        assertEquals(3L * 24 * 60 * 60 * 1000, parseTimeDuration("3d"));
    }

    @Test
    public void testParseVolume() {
        assertEquals(1, parseVolume("1b"));
        assertEquals(3 * 1024, parseVolume("3kb"));
        assertEquals(6L * 1024 * 1024, parseVolume("6mb"));
        assertEquals(11L * 1024 * 1024 * 1024, parseVolume("11gb"));
    }
}
