package ru.yandex.direct.interruption;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Только для ручного запуска: используются таймауты.")
public class TimeoutInterrupterTest {

    @Before
    public void before() {
        Thread.interrupted();
    }

    @Test(expected = IllegalArgumentException.class)
    public void impossibleToCreateWithZeroTimeout() throws Exception {
        new TimeoutInterrupter(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void impossibleToCreateWithNegativeTimeout() throws Exception {
        new TimeoutInterrupter(-1);
    }

    @Test
    public void dontInterruptBeforeTimeout() throws Exception {
        TimeoutInterrupter timeoutInterrupter = new TimeoutInterrupter(10);
        timeoutInterrupter.start();
        Thread.sleep(1000);
    }

    @Test(expected = InterruptedException.class)
    public void interruptsAfterTimeout() throws Exception {
        TimeoutInterrupter timeoutInterrupter = new TimeoutInterrupter(1);
        timeoutInterrupter.start();
        Thread.sleep(2000);
    }

    @Test
    public void stopCancelsInterruption() throws Exception {
        TimeoutInterrupter timeoutInterrupter = new TimeoutInterrupter(1);
        timeoutInterrupter.start();
        timeoutInterrupter.stop();
        Thread.sleep(200);
    }
}
