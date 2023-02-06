package ru.yandex.market.crm.core.schedule;

import org.springframework.context.annotation.Configuration;

@ClusterScheduledRules
@Configuration
public class Tasks {

    volatile boolean task1Invoked = false;
    volatile boolean task2Invoked = false;
    volatile boolean longTaskInvoked = false;

    @ClusterScheduled(intervalMs = 100)
    public void task1() {
        task1Invoked = true;
    }

    @ClusterScheduled(intervalMs = 100)
    public void task2() {
        task2Invoked = true;
    }

    @ClusterScheduled(intervalMs = 100)
    public void longTask() {
        longTaskInvoked = true;
        for (;;) {

        }
    }

}
