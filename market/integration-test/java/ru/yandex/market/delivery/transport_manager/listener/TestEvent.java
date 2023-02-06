package ru.yandex.market.delivery.transport_manager.listener;

import org.springframework.context.ApplicationEvent;

public class TestEvent extends ApplicationEvent {
    public TestEvent() {
        super("1");
    }
}
