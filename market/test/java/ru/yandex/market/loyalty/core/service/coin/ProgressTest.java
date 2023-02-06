package ru.yandex.market.loyalty.core.service.coin;

import org.junit.Test;

import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.utils.Progress;
import ru.yandex.market.loyalty.core.utils.ProgressDelay;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProgressTest {
    @Test
    public void testProcessDelayWhenProgressTooFast() throws InterruptedException {
        ClockForTests clock = new ClockForTests();
        ProgressDelay delay = mock(ProgressDelay.class);
        Progress progress = new Progress(0, 9_000, clock, delay);
        clock.spendTime(20, ChronoUnit.MINUTES);
        progress.advanceProgressAndDelayIfNecessary(4_500);
        verify(delay).sleep(TimeUnit.MINUTES.toMillis(10));
    }

    @Test
    public void testProcessDelayWhenProgressZero() throws InterruptedException {
        ClockForTests clock = new ClockForTests();
        ProgressDelay delay = mock(ProgressDelay.class);
        Progress progress = new Progress(0, 3600, clock, delay);
        clock.spendTime(1, ChronoUnit.SECONDS);
        progress.advanceProgressAndDelayIfNecessary(0);
        verify(delay, never()).sleep(anyLong());
        clock.spendTime(1, ChronoUnit.SECONDS);
        progress.advanceProgressAndDelayIfNecessary(0);
        verify(delay, never()).sleep(anyLong());
        clock.spendTime(1, ChronoUnit.SECONDS);
        progress.advanceProgressAndDelayIfNecessary(0);
        verify(delay, never()).sleep(anyLong());
        clock.spendTime(100, ChronoUnit.SECONDS);
        progress.advanceProgressAndDelayIfNecessary(100);
        verify(delay, never()).sleep(anyLong());
    }

    @Test
    public void testProcessDelayWhenProgressTooSlow() throws InterruptedException {
        ClockForTests clock = new ClockForTests();
        ProgressDelay delay = mock(ProgressDelay.class);
        Progress progress = new Progress(0, 9_000, clock, delay);
        clock.spendTime(20, ChronoUnit.MINUTES);
        progress.advanceProgressAndDelayIfNecessary(3000);
        verify(delay, never()).sleep(anyLong());
    }


    @Test
    public void testProcessDelayWhenProgressLoopTooFast() throws InterruptedException {
        ClockForTests clock = new ClockForTests();
        MockSleepDelay delay = spy(MockSleepDelay.class);
        delay.setClock(clock);
        Progress progress = new Progress(0, 9_000, clock, delay);
        for (int i = 0; i < 100; i++) {
            clock.spendTime(12, ChronoUnit.SECONDS);
            progress.advanceProgressAndDelayIfNecessary(45);
        }
        verify(delay, times(94)).sleep(TimeUnit.MILLISECONDS.toMillis(6000));
        verify(delay, times(3)).sleep(TimeUnit.MILLISECONDS.toMillis(6400));
        verify(delay, times(3)).sleep(TimeUnit.MILLISECONDS.toMillis(5600));
    }

    @Test
    public void testProcessDelayWhenProgressLoopTooFastWithInitialProgress() throws InterruptedException {
        ClockForTests clock = new ClockForTests();
        MockSleepDelay delay = spy(MockSleepDelay.class);
        delay.setClock(clock);
        Progress progress = new Progress(1000, 9_000, clock, delay);
        for (int i = 0; i < 100; i++) {
            clock.spendTime(12, ChronoUnit.SECONDS);
            progress.advanceProgressAndDelayIfNecessary(45);
        }
        verify(delay, times(94)).sleep(TimeUnit.MILLISECONDS.toMillis(6000));
        verify(delay, times(3)).sleep(TimeUnit.MILLISECONDS.toMillis(6400));
        verify(delay, times(3)).sleep(TimeUnit.MILLISECONDS.toMillis(5600));
    }

    private static class MockSleepDelay implements ProgressDelay {
        private ClockForTests clock;

        public MockSleepDelay() {
        }

        public void setClock(ClockForTests clock) {
            this.clock = clock;
        }

        @Override
        public void sleep(long millis) {
            clock.spendTime(millis, ChronoUnit.MILLIS);
        }
    }
}
