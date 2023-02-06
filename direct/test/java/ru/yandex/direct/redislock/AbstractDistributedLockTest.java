package ru.yandex.direct.redislock;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.internal.stubbing.answers.ReturnsElementsOf;

import ru.yandex.direct.redislock.clock.LockClock;
import ru.yandex.direct.redislock.clock.ManualClock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


public class AbstractDistributedLockTest {
    @Test
    public void testSpinLockMultiRetry() throws InterruptedException {
        AbstractDistributedLock spyLock = spy(new AbstractDistributedLockStub(Long.MAX_VALUE));
        final int repRetries = 6;
        final int totalRetries = repRetries + 1;
        List<Boolean> retries = new ArrayList<>(Collections.nCopies(repRetries, Boolean.FALSE));
        retries.add(Boolean.TRUE);
        doAnswer(new ReturnsElementsOf(retries)).when(spyLock).tryLock();
        doAnswer(new ReturnsElementsOf(retries)).when(spyLock).isLocked();

        assertThat(spyLock.lock(), is(true));
        verify(spyLock, times(totalRetries)).tryLock();
    }

    @Test
    public void testSpinLockSingleTry() throws InterruptedException {
        AbstractDistributedLock spyLock = spy(new AbstractDistributedLockStub(Long.MAX_VALUE));
        doReturn(true).when(spyLock).tryLock();
        doReturn(true).when(spyLock).isLocked();

        assertThat(spyLock.lock(), is(true));
        verify(spyLock, times(1)).tryLock();
    }

    @Test
    public void testSpinLockCutOffTime() throws InterruptedException {
        final long cutOffTimeMillis = 3_000L;
        LockClock clock = new ManualClock();

        AbstractDistributedLock spyLock = spy(new AbstractDistributedLockStub(cutOffTimeMillis, clock));
        doReturn(false).when(spyLock).tryLock();
        doReturn(false).when(spyLock).isLocked();

        long currentTime = clock.nanoTime();
        boolean ret = spyLock.lock();
        long elapsed = clock.nanoTime() - currentTime;

        assertThat(ret, is(false));
        assertThat(elapsed, is(lessThanOrEqualTo(TimeUnit.MILLISECONDS.toNanos((long) (cutOffTimeMillis * 1.2)))));
    }
}


class AbstractDistributedLockStub extends AbstractDistributedLock {
    public AbstractDistributedLockStub(long lockAttemptTimeout) {
        this(lockAttemptTimeout, null);
    }

    public AbstractDistributedLockStub(long lockAttemptTimeout, LockClock clock) {
        super(lockAttemptTimeout, clock);
    }

    @Override
    public boolean tryLock() throws DistributedLockException {
        return false;
    }

    @Override
    public boolean unlock() throws DistributedLockException {
        return false;
    }

    @Override
    public void unlockByEntry() throws DistributedLockException {

    }
}
