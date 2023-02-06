package ru.yandex.market.delivery.transport_manager.listener;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import ru.yandex.market.delivery.transport_manager.event.ParallelListeners;

@ParallelListeners
@Getter
public class TestEventParallelListeners extends ApplicationEvent {
    private final int sleepSeconds;

    public TestEventParallelListeners() {
        super("1");
        sleepSeconds = 0;
    }

    public TestEventParallelListeners(int sleepSeconds) {
        super("1");
        this.sleepSeconds = sleepSeconds;
    }
}
