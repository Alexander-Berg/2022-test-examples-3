package ru.yandex.util.timesource;

import org.hamcrest.MatcherAssert;
import org.hamcrest.number.OrderingComparison;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class TimeSourceTest extends TestBase {
    // Due to DISTBUILD-270, tests have no guaranteed CPU cores, so TimeSource
    // ticker thread could get too few invocations and provide very poor
    // precision
    private static final long TIMER_EPSILON = 1000L;

    public TimeSourceTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws InterruptedException {
        // TimeSource initializes with System.currentTimeMillis(), we need to
        // check that value is properly updated
        for (int i = 0; i < 3; ++i) {
            long actual = TimeSource.INSTANCE.currentTimeMillis();
            long expected = System.currentTimeMillis();
            MatcherAssert.assertThat(
                actual,
                OrderingComparison.lessThanOrEqualTo(expected));
            MatcherAssert.assertThat(
                actual,
                OrderingComparison.greaterThan(expected - TIMER_EPSILON));
            Thread.sleep(TIMER_EPSILON);
        }
    }
}

