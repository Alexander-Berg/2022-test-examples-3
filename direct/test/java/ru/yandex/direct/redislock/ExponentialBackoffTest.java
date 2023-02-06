package ru.yandex.direct.redislock;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ExponentialBackoffTest {
    @Test
    public void testNonRandomizedBackoff() {
        //expected values: 200 * (2 ** no_of_iteration); no randomization
        final long[] expectedValues = new long[]{
                0L,
                200L,
                400L,
                800L,
                1_600L,
                3_200L,
                6_400L,
                12_800L,
                25_600L,
                51_200L
        };
        ExponentialBackoffCalculator nonRandomizedCalculator = new ExponentialBackoffCalculator(200L, 2, 0);
        long sleepTime = 0;
        for (int attemptNo = 0; attemptNo < expectedValues.length; ++attemptNo) {
            long expected = expectedValues[attemptNo];
            assertThat(sleepTime, is(expected));
            sleepTime = nonRandomizedCalculator.sleepTime(attemptNo);
        }
    }
}
