package ru.yandex.market.supercontroller.utils;

import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov
 */
public class SessionsToCleanAccumulatorTest {

    @Test(expected = IllegalArgumentException.class)
    public void invalidSessionIdFormat_shouldBeRejected_by_addCandidate() {
        new SessionsToCleanAccumulator().addCandidate("2017_05_02_0001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSessionIdFormat_shouldBeRejected_by_addSessionToKeep() {
        new SessionsToCleanAccumulator().addSessionToKeep("20170502_00_01");
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSessionIdFormat_shouldBeRejected_by_canRemoveSession() {
        new SessionsToCleanAccumulator().canRemoveSession("2017-05-02T00:01");
    }

    @Test
    public void testSortedSessionsToRemove() {
        // arrange
        var accumulator = new SessionsToCleanAccumulator();
        accumulator.addCandidate("20170502_0001");
        accumulator.addCandidate("20170502_0002");
        accumulator.addCandidate("20170501_0001");
        accumulator.addCandidate("20160502_0001");
        accumulator.addSessionToKeep("20170501_0001");

        // act
        List<String> result = accumulator.sortedSessionsToRemove();

        // assert
        assertEquals(Arrays.asList("20160502_0001", "20170502_0001", "20170502_0002"), result);
        assertTrue(accumulator.canRemoveSession("20160502_0005"));
        assertFalse(accumulator.canRemoveSession("20170501_0001"));
    }
}
