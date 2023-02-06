package ru.yandex.common.util.concurrent;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.junit.Test;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import ru.yandex.common.util.functional.Function;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created on 13:50:17 24.01.2008
 *
 * @author jkff
 */
public class ScheduleTest extends TestCase {
    @Test
    public void testAfter() throws InterruptedException {
        final AtomicInteger answer = new AtomicInteger(-1);
        Schedule.after(1000, new Concurrent() {
            public void run() throws Exception {
                answer.set(42);
            }
        });
        Thread.sleep(500);
        assertEquals(-1, answer.get());
        Thread.sleep(600);
        assertEquals(42, answer.get());
    }

    // Properties of timeConstrained:
    // 1. If the concurrent action finishes in time, FINISHED_IN_TIME is returned immediately after the action actually finishes
    //    (thus, a) the action does finished and b) there is a very short time between the action finishing and timeConstrained() returning)

    @Test
    public void testTimeConstrainedWhenFinishesInTime() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<Schedule.TimeoutKind> result = new AtomicReference<Schedule.TimeoutKind>();
        Schedule.concurrently(new Concurrent() {
            public void run() throws Exception {
                result.set(Schedule.timeConstrained(new Concurrent() {
                    public void run() throws Exception {
                        Thread.sleep(100);
                        latch.countDown();
                    }
                }, 300, 200).first);
                latch.countDown();
            }
        });
        assertTrue("Didn't finish after 150ms, whereas should have finished after 100ms", latch.await(150, TimeUnit.MILLISECONDS));
        assertEquals(Schedule.TimeoutKind.FINISHED_IN_TIME, result.get());
    }

    @Test
    public void testTimeConstrainedWhenWrapsUpInTime() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Schedule.TimeoutKind> result = new AtomicReference<Schedule.TimeoutKind>();
        final AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        final AtomicBoolean hasWrappedUp = new AtomicBoolean(false);
        Schedule.concurrently(new Concurrent() {
            public void run() throws Exception {
                result.set(Schedule.timeConstrained(new Concurrent() {
                    public void run() throws Exception {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            wasInterrupted.set(true);
                            Thread.sleep(100);
                            hasWrappedUp.set(true);
                        }
                    }
                }, 300, 200).first);
                latch.countDown();
            }
        });
        assertTrue("Didn't finish after 500ms, whereas should have finished after 400ms", latch.await(500, TimeUnit.MILLISECONDS));
        assertTrue("Wasn't interrupted after 300ms", wasInterrupted.get());
        assertTrue("Wasn't given enough time to wrap up", hasWrappedUp.get());
        assertEquals(Schedule.TimeoutKind.WRAPPED_UP_IN_TIME, result.get());
    }

    @Test
    public void testTimeConstrainedWhenDoesntWrapUpInTime() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Schedule.TimeoutKind> result = new AtomicReference<Schedule.TimeoutKind>();
        final AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        final AtomicBoolean hasWrappedUp = new AtomicBoolean(false);
        Schedule.concurrently(new Concurrent() {
            public void run() throws Exception {
                result.set(Schedule.timeConstrained(new Concurrent() {
                    public void run() throws Exception {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            wasInterrupted.set(true);
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException x) {
                                Thread.sleep(10000);
                            }
                            hasWrappedUp.set(true);
                            latch.countDown();
                        }
                    }
                }, 300, 200).first);
            }
        });
        assertFalse("Shouldn't have finished in 600ms", latch.await(600, TimeUnit.MILLISECONDS));
        assertTrue("Wasn't interrupted after 300ms", wasInterrupted.get());
        assertFalse("Shouldn't have been given enough time to wrap up", hasWrappedUp.get());
        assertEquals(Schedule.TimeoutKind.TIMED_OUT, result.get());
    }


    public void testTimeConstrainedWhenWaitingInterruptedDuringBasic() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            final AtomicReference<Schedule.TimeoutKind> result = new AtomicReference<Schedule.TimeoutKind>();
            final CountDownLatch finishLatch = new CountDownLatch(1);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch innerLatch = new CountDownLatch(1);
            final AtomicBoolean wasCompleted = new AtomicBoolean(false);
            final AtomicBoolean wasInterrupted = new AtomicBoolean(false);
            Thread t = new Thread() {
                public void run() {
                    result.set(Schedule.timeConstrained(new Concurrent() {
                        public void run() throws Exception {
                            try {
                                startLatch.countDown();
                                Thread.sleep(1000);
                                wasCompleted.set(true);
                            } catch (InterruptedException e) {
                                wasInterrupted.set(true);
                                innerLatch.countDown();
                            }
                        }
                    }, 1000, 200).first);
                    try {
                        innerLatch.await();
                    } catch (InterruptedException e) {
                        fail();
                    }
                    finishLatch.countDown();
                }
            };
            t.start();
            startLatch.await();
            Thread.sleep(200);
            t.interrupt();
            finishLatch.await();
            assertEquals(Schedule.TimeoutKind.WAITING_INTERRUPTED, result.get());
            assertFalse(wasCompleted.get());
            assertTrue(wasInterrupted.get());
        }
    }

    public void testTimeConstrainedWhenWaitingInterruptedDuringWrapUp() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            final AtomicReference<Schedule.TimeoutKind> result = new AtomicReference<Schedule.TimeoutKind>();
            final CountDownLatch finishLatch = new CountDownLatch(1);
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch innerLatch = new CountDownLatch(1);
            final AtomicBoolean wasCompleted = new AtomicBoolean(false);
            final AtomicBoolean wasInterrupted = new AtomicBoolean(false);
            final AtomicBoolean wasInterruptedDuringWrapUp = new AtomicBoolean(false);
            final AtomicBoolean wasNotInterruptedDuringWrapUp = new AtomicBoolean(false);
            Thread t = new Thread() {
                public void run() {
                    result.set(Schedule.timeConstrained(new Concurrent() {
                        public void run() throws Exception {
                            startLatch.countDown();
                            try {
                                Thread.sleep(1000);
                                wasCompleted.set(true);
                            } catch (InterruptedException e) {
                                wasInterrupted.set(true);
                                try {
                                    Thread.sleep(400);
                                    wasNotInterruptedDuringWrapUp.set(true);
                                } catch (InterruptedException x) {
                                    wasInterruptedDuringWrapUp.set(true);
                                    innerLatch.countDown();
                                }
                            }
                        }
                    }, 500, 500).first);
                    try {
                        innerLatch.await();
                    } catch (InterruptedException e) {
                        fail();
                    }
                    finishLatch.countDown();
                }
            };
            t.start();
            startLatch.await();
            // After 500ms the first interrupt occurs
            Thread.sleep(650); // and now the second one
            t.interrupt();
            finishLatch.await();
            assertEquals(Schedule.TimeoutKind.WAITING_INTERRUPTED, result.get());
            assertFalse(wasCompleted.get());
            assertTrue(wasInterrupted.get());
            assertFalse(wasNotInterruptedDuringWrapUp.get());
            assertTrue(wasInterruptedDuringWrapUp.get());
        }
    }

    @Test
    public void testParallelMap() throws InterruptedException {
        final Object bed = new Object();
        Function<Integer, String> sleepyToString = new Function<Integer, String>() {
            @Override
            public String apply(Integer arg) {
                synchronized (bed) {
                    // Sleep like a log
                    long deadline = System.currentTimeMillis() + 1000;
                    while (System.currentTimeMillis() < deadline) {
                        try {
                            Thread.sleep(deadline - System.currentTimeMillis());
                        } catch (InterruptedException e) {
                        }
                    }
                }

                return String.valueOf(arg);
            }
        };
        List<String> res = Schedule.parallelMap(5, list(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), sleepyToString, 5);
        int nonNulls = 0;
        for (String s : res) if (s != null) nonNulls++;
        assertTrue(nonNulls == 4 || nonNulls == 5);
        Thread.sleep(3000);
        int newNonNulls = 0;
        for (String s : res) if (s != null) newNonNulls++;
        assertTrue("List modified after exit from parallelMap", newNonNulls == nonNulls);
    }

    @Test
    public void testConvertToConcurrentDoesExactlyTheSameAsRunnable() {
        final Runnable r = EasyMock.createStrictMock(Runnable.class);
        r.run();
        EasyMock.replay(r);
        final Concurrent c = Schedule.convertToConcurrent(r);
        try {
            c.run();
        } catch (Exception e) {
            fail("unexpected exception " + e.getMessage());
        }
        EasyMock.verify(r);
    }

    @Test
    public void testConvertToConcurrentThrowsOnNullRunnable() {
        try {
            Schedule.convertToConcurrent(null);
            fail("exception is not thrown");
        } catch (RuntimeException ex) {
            //expected
        }
    }

    @Test
    public void testConcurrentWithExceptionHandlingReallyHandlesException() throws Exception {
        final Object executionLock = new Object();

        final AtomicBoolean exceptionHandled = new AtomicBoolean(false);

        synchronized (executionLock) {
            Schedule.concurrently(new Schedule.ConcurrentWithExceptionHandling() {
                @Override
                protected void doRun() throws Exception {
                    throw new RuntimeException("foo");
                }

                @Override
                protected void handleException(final Throwable t) {
                    synchronized (executionLock) {
                        exceptionHandled.set(true);
                        executionLock.notifyAll();
                    }
                }
            });
            executionLock.wait(); //yes, unconditional wait is bad, but we don't want to hand here
        }
        assertTrue("exception should be seen and handled", exceptionHandled.get());

    }
}
