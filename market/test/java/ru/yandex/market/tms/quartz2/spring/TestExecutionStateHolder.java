package ru.yandex.market.tms.quartz2.spring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestExecutionStateHolder {

    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean success = new AtomicBoolean(false);

    public TestExecutionStateHolder() {
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void markSuccess() {
        success.set(true);
    }

    public boolean isSuccess() {
        return success.get();
    }
}
