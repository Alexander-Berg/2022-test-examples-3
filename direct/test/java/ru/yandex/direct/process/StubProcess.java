package ru.yandex.direct.process;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import ru.yandex.direct.utils.CommonUtils;
import ru.yandex.direct.utils.MonotonicTime;
import ru.yandex.direct.utils.NanoTimeClock;

public class StubProcess extends Process {
    private Duration gracefulStopTime;
    private int normalStatus;
    private int termStatus;
    private MonotonicTime runDeadline;
    private boolean exited;
    private boolean destroyed;

    public StubProcess(Duration runTime, Duration gracefulStopTime, int normalStatus, int termStatus) {
        this.gracefulStopTime = gracefulStopTime;
        this.normalStatus = normalStatus;
        this.termStatus = termStatus;
        this.runDeadline = NanoTimeClock.now().plus(runTime);
        this.exited = false;
        this.destroyed = false;
    }

    @Override
    public OutputStream getOutputStream() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public InputStream getInputStream() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public InputStream getErrorStream() {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public synchronized int waitFor() throws InterruptedException {
        waitFor(runDeadline.minus(NanoTimeClock.now()).plus(Duration.ofNanos(1)).toNanos(), TimeUnit.NANOSECONDS);
        return exitValue();
    }

    @Override
    public synchronized boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        MonotonicTime waitDeadline = NanoTimeClock.now().plus(Duration.ofNanos(unit.toNanos(timeout)));
        while (true) {
            MonotonicTime now = NanoTimeClock.now();
            if (now.isAfter(runDeadline)) {
                exited = true;
                return true;
            }
            if (now.isAfter(waitDeadline)) {
                return false;
            }
            long delay = CommonUtils.min(waitDeadline, runDeadline).minus(now).toMillis();
            if (delay > 0) {
                wait(delay);
            }
        }
    }

    @Override
    public synchronized int exitValue() {
        if (NanoTimeClock.now().isAfter(runDeadline)) {
            exited = true;
        }
        if (exited) {
            if (destroyed) {
                return termStatus;
            } else {
                return normalStatus;
            }
        } else {
            throw new IllegalThreadStateException();
        }
    }

    @Override
    public synchronized void destroy() {
        runDeadline = NanoTimeClock.now().plus(gracefulStopTime);
        destroyed = true;
        notifyAll();
    }

    @Override
    public synchronized Process destroyForcibly() {
        runDeadline = NanoTimeClock.now().minus(Duration.ofNanos(1));
        destroyed = true;
        notifyAll();
        return this;
    }
}
