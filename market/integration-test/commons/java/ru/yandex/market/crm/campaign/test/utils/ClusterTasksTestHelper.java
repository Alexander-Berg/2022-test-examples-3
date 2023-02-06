package ru.yandex.market.crm.campaign.test.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.tasks.domain.Control;
import ru.yandex.market.crm.tasks.domain.ExecutionResult;
import ru.yandex.market.crm.tasks.domain.Task;
import ru.yandex.market.crm.tasks.domain.TaskInstanceInfo;
import ru.yandex.market.crm.tasks.domain.TaskStatus;
import ru.yandex.market.crm.tasks.services.ClusterTasksDAO;
import ru.yandex.misc.thread.ThreadUtils;

/**
 * @author apershukov
 */
@Component
public class ClusterTasksTestHelper {

    private final ClusterTasksDAO clusterTasksDAO;

    public ClusterTasksTestHelper(ClusterTasksDAO clusterTasksDAO) {
        this.clusterTasksDAO = clusterTasksDAO;
    }

    /**
     * Простая обёртка над Task, которая позволяет запихнуть его на исполнение в ClusterTaskService
     */
    public static class StepWrapper<ContextT, DataT> implements Task<Void, DataT> {
        private final Task<ContextT, DataT> wrapped;
        private final ContextT taskContext;

        public StepWrapper(Task<ContextT, DataT> wrapped, ContextT taskContext) {
            this.wrapped = wrapped;
            this.taskContext = taskContext;
        }

        @Nonnull
        @Override
        public ExecutionResult run(Void context, DataT data, Control<DataT> control) throws Exception {
            return wrapped.run(taskContext, data, control);
        }

        @Override
        public String getId() {
            return wrapped.getId();
        }

        @Nonnull
        @Override
        public Class<DataT> getDataClass() {
            return wrapped.getDataClass();
        }
    }

    public void waitCompleted(long taskId, Duration timeout) {
        LocalDateTime deadline = LocalDateTime.now().plus(timeout);

        while (true) {
            TaskInstanceInfo taskInstanceInfo = clusterTasksDAO.getTask(taskId);
            if (taskInstanceInfo == null) {
                throw new IllegalStateException("No task with id '" + taskId + "'");
            }

            TaskStatus status = taskInstanceInfo.getStatus();

            if (status == TaskStatus.FAILING || status == TaskStatus.FAILED) {
                throw new TaskFailedException("Task failed", taskInstanceInfo);
            }

            if (status == TaskStatus.CANCELLING || status == TaskStatus.CANCELLED) {
                throw new IllegalStateException("Task is cancelled");
            }

            if (status == TaskStatus.COMPLETED) {
                return;
            }

            if (LocalDateTime.now().isAfter(deadline)) {
                throw new IllegalStateException("Waiting timeout is expired. Task is still in '" + status + "' state");
            }

            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }
    }
}
