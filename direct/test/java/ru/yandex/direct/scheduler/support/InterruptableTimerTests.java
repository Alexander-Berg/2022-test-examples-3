package ru.yandex.direct.scheduler.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InterruptableTimerTests {
    private static final int timerPeriod = 10;
    private static final long setTimeMills = 1571313988000L;

    @Test
    public void awaitsASetNumberOfSecondsAfterSet() throws InterruptedException {
        CountDownLatch countDownLatch = mock(CountDownLatch.class);
        AtomicLong nowMills = new AtomicLong(setTimeMills);
        Supplier<Long> nowMillsGetter = nowMills::get;
        InterruptableTimer timer = new InterruptableTimer(countDownLatch, nowMillsGetter);

        timer.set(timerPeriod);

        timer.await();

        verify(countDownLatch).await(timerPeriod * 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void awaitsUntilTheFireTimeIfTheStartIsDelayed() throws InterruptedException {
        int delayBeforeAwaitMills = 5000;

        CountDownLatch countDownLatch = mock(CountDownLatch.class);
        AtomicLong nowMills = new AtomicLong(setTimeMills);
        Supplier<Long> nowMillsGetter = nowMills::get;

        InterruptableTimer timer = new InterruptableTimer(countDownLatch, nowMillsGetter);
        timer.set(timerPeriod);

        long startTime = setTimeMills + delayBeforeAwaitMills;
        nowMills.set(startTime);
        timer.await();

        verify(countDownLatch).await(delayBeforeAwaitMills, TimeUnit.MILLISECONDS);
    }

    @Test
    public void awaitingSecondTimeAfterALongJobQueuesTimerFiringRightAway()
            throws InterruptedException {
        int longJobDelayMills = 15000;

        CountDownLatch countDownLatch = mock(CountDownLatch.class);
        AtomicLong nowMills = new AtomicLong(setTimeMills);
        Supplier<Long> nowMillsGetter = nowMills::get;

        InterruptableTimer timer = new InterruptableTimer(countDownLatch, nowMillsGetter);
        timer.set(timerPeriod);

        long startTimeMills = setTimeMills + longJobDelayMills;
        nowMills.set(startTimeMills);
        timer.await();

        verify(countDownLatch).await(0, TimeUnit.MILLISECONDS);
    }

    @Test
    public void awaitingAfterALongJobQueuesOnlyOneFiringOfTheTrigger() throws InterruptedException {
        int longJobDelayMills = 35_000;
        long fireTimeMills = setTimeMills + timerPeriod * 1000;

        CountDownLatch countDownLatch = mock(CountDownLatch.class);
        AtomicLong nowMills = new AtomicLong(setTimeMills);
        Supplier<Long> nowMillsGetter = nowMills::get;

        InterruptableTimer timer = new InterruptableTimer(countDownLatch, nowMillsGetter);
        timer.set(timerPeriod);

        long startTimeMills = fireTimeMills + longJobDelayMills;
        nowMills.set(startTimeMills);
        timer.await();

        long secondIterationStartTimeMills = startTimeMills;
        nowMills.set(secondIterationStartTimeMills);   // job finishes instantly
        timer.await();

        verify(countDownLatch).await(timerPeriod * 1000, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void awaitWithoutSetThrowsException() {
        InterruptableTimer timer = new InterruptableTimer();
        timer.await();
    }
}
