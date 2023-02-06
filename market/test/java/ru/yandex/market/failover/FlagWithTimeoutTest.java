package ru.yandex.market.failover;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.failover.FailoverTestUtils.between;

public class FlagWithTimeoutTest {
    @Test
    public void testInitializeTrue() throws NoSuchFieldException, IllegalAccessException {
        long ts1 = System.currentTimeMillis();
        FlagWithTimeout flag = new FlagWithTimeout(true, MAX_VALUE);
        long ts2 = System.currentTimeMillis();

        long val = getFlagValue(flag);
        assertTrue(between(val, ts1, ts2));
    }

    @Test
    public void testInitializeFalse() throws NoSuchFieldException, IllegalAccessException {
        FlagWithTimeout flag = new FlagWithTimeout(false, MAX_VALUE);

        long val = getFlagValue(flag);
        assertEquals(FlagWithTimeout.FALSE, val);
    }

    @Test
    public void testSetTrue() throws NoSuchFieldException, IllegalAccessException {
        long ts1 = System.currentTimeMillis();
        FlagWithTimeout flag = new FlagWithTimeout(MAX_VALUE);
        flag.set(true);
        long ts2 = System.currentTimeMillis();

        long val = getFlagValue(flag);
        assertTrue(between(val, ts1, ts2));
    }

    @Test
    public void testSetFalse() throws NoSuchFieldException, IllegalAccessException {
        FlagWithTimeout flag = new FlagWithTimeout(MAX_VALUE);
        flag.set(false);

        long val = getFlagValue(flag);
        assertEquals(FlagWithTimeout.FALSE, val);
    }

    @Test
    public void testGetTrue() throws NoSuchFieldException, IllegalAccessException {
        FlagWithTimeout flag = new FlagWithTimeout(100L);
        long now = System.currentTimeMillis();
        setFlagValue(flag, now);
        assertTrue(flag.get());
    }

    @Test
    public void testGetFalse() throws NoSuchFieldException, IllegalAccessException {
        FlagWithTimeout flag = new FlagWithTimeout(100L);
        setFlagValue(flag, FlagWithTimeout.FALSE);
        assertFalse(flag.get());
    }

    @Test
    public void testGetExpiredFalse() throws NoSuchFieldException, IllegalAccessException {
        FlagWithTimeout flag = new FlagWithTimeout(100L);
        long now = System.currentTimeMillis();
        setFlagValue(flag, now - 101L);
        assertFalse(flag.get());
    }

    @Test
    public void testTrueToTrue() {
        FlagWithTimeout flag = new FlagWithTimeout(true, MAX_VALUE);
        flag.set(true);
        assertTrue(flag.get());
    }

    @Test
    public void testFalseToTrue() {
        FlagWithTimeout flag = new FlagWithTimeout(false, MAX_VALUE);
        flag.set(true);
        assertTrue(flag.get());
    }

    @Test
    public void testTrueToFalse() {
        FlagWithTimeout flag = new FlagWithTimeout(true, MAX_VALUE);
        flag.set(false);
        assertFalse(flag.get());
    }

    @Test
    public void testFalseToFalse() {
        FlagWithTimeout flag = new FlagWithTimeout(false, MAX_VALUE);
        flag.set(false);
        assertFalse(flag.get());
    }


    @Test
    public void testSetAndGetTrueToTrue() {
        FlagWithTimeout flag = new FlagWithTimeout(true, MAX_VALUE);
        boolean prevoius = flag.getAndSet(true);
        assertTrue(flag.get());
        assertTrue(prevoius);
    }

    @Test
    public void testSetAndGetFalseToTrue() {
        FlagWithTimeout flag = new FlagWithTimeout(false, MAX_VALUE);
        boolean prevoius = flag.getAndSet(true);
        assertTrue(flag.get());
        assertFalse(prevoius);
    }

    @Test
    public void testSetAndGetTrueToFalse() {
        FlagWithTimeout flag = new FlagWithTimeout(true, MAX_VALUE);
        boolean prevoius = flag.getAndSet(false);
        assertFalse(flag.get());
        assertTrue(prevoius);
    }

    @Test
    public void testSetAndGetFalseToFalse() {
        FlagWithTimeout flag = new FlagWithTimeout(false, MAX_VALUE);
        boolean prevoius = flag.getAndSet(false);
        assertFalse(flag.get());
        assertFalse(prevoius);
    }

    @Test
    public void assertExpire() throws InterruptedException {
        FlagWithTimeout flag = new FlagWithTimeout(true, 100);
        assertTrue(flag.get());
        sleep(102);
        assertFalse(flag.get());

        flag.set(true);
        assertTrue(flag.get());
        sleep(102);
        assertFalse(flag.get());
    }


    private long getFlagValue(FlagWithTimeout flag) throws NoSuchFieldException, IllegalAccessException {
        return FailoverTestUtils.<AtomicLong>getPrivate(flag, "lastSetTrueTs").get();
    }

    private void setFlagValue(FlagWithTimeout flag, long value) throws NoSuchFieldException, IllegalAccessException {
        FailoverTestUtils.<AtomicLong>getPrivate(flag, "lastSetTrueTs").set(value);
    }
}
