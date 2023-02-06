package ru.yandex.market.loyalty.core.test;

import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.monitoring.PushMonitor;

import java.util.List;

@Component
public class MonitorCleaner {
    private final List<PushMonitor> monitorList;

    public MonitorCleaner(List<PushMonitor> monitorList) {
        this.monitorList = monitorList;
    }

    public void clean() {
        for (PushMonitor c : monitorList) {
            c.clear();
        }
    }
}
