package ru.yandex.crypta.lib.entity.counters;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntityCountersTest {
    @Test
    public void testIsValid() {
        var counterInfo = EntityCounters.getCounterInfo(1033);
        assertEquals("Edadeal_SegmentLvl3", counterInfo.type);
        assertEquals(1034, counterInfo.ageCounterId);

        counterInfo = EntityCounters.getCounterInfo(1034);
        assertEquals("Edadeal_SegmentLvl3", counterInfo.type);
    }
}
