package ru.yandex.market.markup2.core.stubs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.workflow.TaskProcessManager;

import java.util.function.Consumer;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskProcessManagerStub extends TaskProcessManager {
    private static final Logger log = LogManager.getLogger();

    public void processTask(int taskId) {
        if (pollSpecificTaskId(taskId) != null) {
            processEvent(taskId);
        } else {
            throw new RuntimeException("Not in queue: " + taskId);
        }
    }

    public int processNextTask() {
        Integer taskId = pollNextTaskId();
        processEvent(taskId);
        return taskId;
    }

    public void processAll(Consumer<Integer> beforeTask) {
        log.debug("processAll. queue " + processedTasks);
        Integer taskId;
        boolean processed = false;
        while ((taskId = pollNextTaskId()) != null) {
            processed = true;
            beforeTask.accept(taskId);
            processEvent(taskId);
        }
        if (!processed) {
            log.debug("no tasks in queue");
        }
    }

    public void processAll() {
        processAll((a) -> { });
    }

}
