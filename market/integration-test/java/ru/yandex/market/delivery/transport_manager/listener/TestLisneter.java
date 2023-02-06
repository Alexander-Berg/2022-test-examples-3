package ru.yandex.market.delivery.transport_manager.listener;

import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.context.event.EventListener;

@Getter
public class TestLisneter {
    private String currentThread;
    private String currentThread1;
    private String currentThread2;

    public void reset() {
        this.currentThread = "";
        this.currentThread1 = "";
        this.currentThread2 = "";
    }

    @EventListener
    public void listen(TestEvent event) {
        currentThread = Thread.currentThread().getName();
    }

    @EventListener
    @SneakyThrows
    public void listen1(TestEventParallelListeners event) {
        currentThread1 = Thread.currentThread().getName();
        if (event.getSleepSeconds() > 0) {
            Thread.sleep(event.getSleepSeconds() * 1000L);
        }
    }

    @EventListener
    @SneakyThrows
    public void listen2(TestEventParallelListeners event) {
        currentThread2 = Thread.currentThread().getName();
        if (event.getSleepSeconds() > 0) {
            Thread.sleep(event.getSleepSeconds() * 1000L);
        }
    }
}
