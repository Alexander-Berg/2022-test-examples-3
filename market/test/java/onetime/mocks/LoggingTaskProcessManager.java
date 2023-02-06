package onetime.mocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.yandex.market.markup2.workflow.ITaskProcessManager;

/**
 * @author anmalysh
 */
public class LoggingTaskProcessManager implements ITaskProcessManager {

    Logger log = LogManager.getLogger();

    @Override
    public void addTask(int taskId) {
        log.info("Task {} added", taskId);
    }

    @Override
    public void interruptTaskProcess(int taskId) {
        log.info("Task {} interrupted", taskId);
    }
}
