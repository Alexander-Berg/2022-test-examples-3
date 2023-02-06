package ru.yandex.market.robot.utis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.utils.Retryer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author alex-pekin
 */
public class RetryerTest {

    private static final int RETRIES_COUNT = 3;
    private static final Logger log = LogManager.getLogger();

    @Test
    public void testZeroAttemptPass() {
        final AtomicInteger i = new AtomicInteger();
        Retryer.exec(
            () -> { log.info("Attempt {}", i.incrementAndGet()); return Void.TYPE; },
            (e, att) -> Assert.fail("Empty procedure should not ever fail"),
            RuntimeException.class,
            Optional.empty(), RETRIES_COUNT);
    }

    @Test
    public void testZeroAttemptPassWithDelay() {
        final AtomicInteger i = new AtomicInteger();
        Retryer.exec(
            () -> { log.info("Attempt {}", i.incrementAndGet()); return Void.TYPE; },
            (e, att) -> Assert.fail("Empty procedure should not ever fail"),
            RuntimeException.class,
            Optional.of(10L), RETRIES_COUNT);
    }

    @Test
    public void testLastAttemptPass() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();
        Retryer.exec(
            () -> { log.info("Attempt {}", i.incrementAndGet());
                if (i.get() != RETRIES_COUNT) { throw new RuntimeException("Only 3rd/3 should pass"); }
                return Void.TYPE; },
            (e, att) -> { if (att == RETRIES_COUNT - 1) { Assert.fail("Last attempt should not have failed: " + e); } },
            Optional.empty(), RETRIES_COUNT);
    }

    @Test
    public void testMiddleAttemptPass() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();
        Retryer.exec(
            () -> { log.info("Attempt {}", i.incrementAndGet());
                if (i.get() != 2) { throw new RuntimeException("Only 2nd/3 attempt is ok"); }
                return Void.TYPE; },
            (e, att) -> { if (att == (2 - 1)) { Assert.fail("Not the 1st attempt failed: " + e); } },
            Optional.empty(), RETRIES_COUNT);
    }

    @Test
    public void testAllAttemptsFail() throws InterruptedException {
        final AtomicInteger i = new AtomicInteger();
        try {
            Retryer.exec(
                () -> { log.info("Attempt {}", i.incrementAndGet());
                    throw new IllegalStateException("Any attempt should fail");
                },
                (e, att) -> {
                },
                Optional.empty(), RETRIES_COUNT);
            Assert.fail("Exception should have been thrown earlier");
        } catch (IllegalStateException e) {
            //ok
            log.info("Caught expected exception: ", e);
        }
    }
}
