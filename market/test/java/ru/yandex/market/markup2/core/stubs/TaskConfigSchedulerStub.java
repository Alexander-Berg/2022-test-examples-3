package ru.yandex.market.markup2.core.stubs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.processors.taskConfig.TaskConfigScheduler;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskConfigSchedulerStub extends TaskConfigScheduler {
    private static final Logger log = LogManager.getLogger();

    @Override
    public void scheduleTaskConfig(int configId, long processTime) {
        super.scheduleTaskConfig(configId, 0);
        try {
            processNextConfigFromQueue();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
