package ru.yandex.market.failover;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

public class PeriodicalTest {

    private static final int START_DELAY = 1_00;
    private static final int RUN_PERIOD = 1_00;
    private static final int CHECK_PERIOD = 3 * RUN_PERIOD;
    private static final int POLL_PERIOD = 10;

    private PeriodicalForTest periodical;

    @Before
    public void setUp() throws Exception {
        periodical = new PeriodicalForTest();
        periodical.setStartDelay(START_DELAY);
        periodical.setRunPeriod(RUN_PERIOD);
        FailoverTestUtils.setPrivate(periodical, "failoverThreadPriority", 8);
        FailoverTestUtils.setPrivate(periodical, "applicationContext", mock(AbstractApplicationContext.class));
        periodical.start();
    }

    @After
    public void tearDown() {
        periodical.stop();
    }

    /**
     * Проверка того, что после старта счётчик запусков увеличивается
     */
    @Test
    public void testRun() throws InterruptedException {
        final AtomicInteger execCounter = periodical.execCounter;

        // Проверит что изначально установленное значение корректно
        assertEquals(0, execCounter.get());

        // Проверить что после запуска periodical, значение не поменялось (так как START_DELAY)
        periodical.startTimer();
        assertEquals(0, execCounter.get());

        // Проверить что значение periodical изменится в течении CHECK_PERIOD (заведомо больше RUN_PERIOD)
        awaitCounterChanges(execCounter);
    }

    /**
     * Проверка того, что после остановки счётчик запусков не меняется
     */
    @Test
    public void testStop() throws InterruptedException {
        final AtomicInteger execCounter = periodical.execCounter;

        // Запустить periodical, подождать когда он отработает хотябы раз (CHECK_PERIOD > RUN_PERIOD), остановить его
        periodical.startTimer();
        awaitCounterChanges(execCounter);
        periodical.stop();

        // Проверить, что после остановки periodical, значение счетчика не должно поменяться (в течении CHECK_PERIOD)
        final int runCount = execCounter.get();
        Thread.sleep(CHECK_PERIOD);
        assertEquals(runCount, execCounter.get());
    }

    private void awaitCounterChanges(final AtomicInteger execCounter) throws InterruptedException {
        int limit = 0;
        int initialCounter = execCounter.get();
        int currentCounter = initialCounter;

        while (initialCounter == currentCounter && limit < CHECK_PERIOD) {
            Thread.sleep(POLL_PERIOD);
            limit += POLL_PERIOD;
            currentCounter = execCounter.get();
        }

        assertNotEquals(initialCounter, currentCounter);
    }

    private class PeriodicalForTest extends Periodical {
        private final AtomicInteger execCounter = new AtomicInteger(0);

        @Override
        protected boolean runTask() {
            execCounter.incrementAndGet();
            return true;
        }
    }

}
