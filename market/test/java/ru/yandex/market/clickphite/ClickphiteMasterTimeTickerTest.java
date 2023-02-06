package ru.yandex.market.clickphite;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClickphiteMasterTimeTickerTest {

    private static final int SLEEP_MILLIS = 100;
    private static final double TIME_DELTA_MILLIS = 10.0;
    private static final double MILLIS_IN_NANOS = 1_000_000.0;
    private ClickphiteMasterTimeTicker timeTicker;

    @Before
    public void setup() {
        timeTicker = new ClickphiteMasterTimeTicker();
    }

    @Test
    public void initialTimeValue() {
        assertEquals(0, timeTicker.read());
    }

    @Test
    public void stationaryTimeWhenSlave() throws InterruptedException {
        Thread.sleep(SLEEP_MILLIS);
        assertEquals(0, timeTicker.read());
    }

    @Test
    public void movingTimeWhenMaster() throws InterruptedException {
        timeTicker.setLeader(true);
        Thread.sleep(SLEEP_MILLIS);
        assertEquals(SLEEP_MILLIS, timeTicker.read() / MILLIS_IN_NANOS, TIME_DELTA_MILLIS);
    }

    @Test
    public void stationaryAgain() throws InterruptedException {
        timeTicker.setLeader(true);
        Thread.sleep(SLEEP_MILLIS);
        timeTicker.setLeader(false);
        Thread.sleep(SLEEP_MILLIS);
        assertEquals(SLEEP_MILLIS, timeTicker.read() / MILLIS_IN_NANOS, TIME_DELTA_MILLIS);
    }

}
