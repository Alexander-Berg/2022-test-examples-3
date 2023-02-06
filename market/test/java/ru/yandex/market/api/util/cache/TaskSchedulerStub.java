package ru.yandex.market.api.util.cache;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

/**
 *
 * Created by apershukov on 15.01.17.
 */
@Profile("test")
@Component
public class TaskSchedulerStub implements TaskScheduler {

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
        return null;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
        return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
        return null;
    }
}
