package ru.yandex.market.delivery.transport_manager.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;

public class TestSpringScheduler {
    private final AtomicBoolean throwEx = new AtomicBoolean(false);

    public void setThrowEx(boolean throwEx) {
        this.throwEx.set(throwEx);
    }

    @Scheduled(fixedRate = 100)
    public void doSomething() {
        if (throwEx.get()) {
            throw new IllegalStateException("Error message sample");
        }
    }
}
