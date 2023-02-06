package ru.yandex.market.sre.services.tms.eventdetector.enums;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventTypeTest {

    @Test
    public void isHigher() {
        assertTrue("State correctness", EventType.CRITICAL.isHigherThan(EventType.WARN));
        assertTrue("State correctness", EventType.WARN.isHigherThan(EventType.NO_DATA));
        assertTrue("State correctness", EventType.NO_DATA.isHigherThan(EventType.OK));
        assertTrue("State correctness", EventType.CRITICAL.isHigherThan(EventType.OK));
        assertTrue("State correctness", EventType.OK.isHigherThan(null));
    }

    @Test
    public void isNotOk() {
        assertTrue(EventType.CRITICAL.isNotOk());
        assertFalse(EventType.OK.isNotOk());
    }
}
