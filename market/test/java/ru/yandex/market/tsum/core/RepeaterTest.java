package ru.yandex.market.tsum.core;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Nikolay Firov
 * @date 15.12.2017
 */
public class RepeaterTest {
    @Test
    @Ignore
    public void testRun() throws Throwable {
        AtomicInteger calls = new AtomicInteger();

        Repeater.repeat(calls::incrementAndGet)
            .interval(100, TimeUnit.MILLISECONDS)
            .timeout(300, TimeUnit.MILLISECONDS)
            .run();

        Assert.assertEquals(3, calls.get());
    }
}
