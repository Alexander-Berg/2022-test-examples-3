package ru.yandex.market.ir.nirvana.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryerTest {

    private static final int RETRIES_COUNT = 3;
    private static final Logger log = LogManager.getLogger();

    @Test
    public void testLastAttemptPass() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();

        Retryer.exec(
            () -> {
                log.info("Attempt {}", i.incrementAndGet());
                if (i.get() != RETRIES_COUNT) {
                    throw new RuntimeException("Only 3rd/3 should pass");
                }
            },
            (e, att) -> {
                if (att == RETRIES_COUNT - 1) {
                    Assert.fail("Last attempt should not have failed: " + e);
                }
            },
            0, RETRIES_COUNT);
    }

    @Test
    public void testMiddleAttemptPass() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();

        Retryer.exec(
            () -> {
                log.info("Attempt {}", i.incrementAndGet());
                if (i.get() != 2) {
                    throw new RuntimeException("Only 2nd/3 attempt is ok");
                }
            },
            (e, att) -> {
                if (att == (2 - 1)) {
                    Assert.fail("Not the 1st attempt failed: " + e);
                }
            },
            0, RETRIES_COUNT);
    }

    @Test
    public void testAllAttemptsFail() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();
        try {
            Retryer.exec(
                () -> {
                    log.info("Attempt {}", i.incrementAndGet());
                    throw new IllegalStateException("Any attempt should fail");
                },
                (e, att) -> {},
                0, RETRIES_COUNT);
            Assert.fail("Exception should have been thrown earlier");
        } catch (IllegalStateException e) {
            //ok
            log.info("Caught expected exception: ", e);
        }
    }
}
