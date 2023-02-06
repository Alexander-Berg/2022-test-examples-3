package ru.yandex.market.logistic.gateway.service.executor.common;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistic.gateway.common.model.payload.RequestFlow;
import ru.yandex.market.logistic.gateway.model.TaskStatus;
import ru.yandex.market.logistic.gateway.model.entity.ClientTask;
import ru.yandex.market.logistic.gateway.utils.FileReadingUtils;

@ParametersAreNonnullByDefault
public final class ClientTaskFactory {

    private ClientTaskFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static ClientTask createClientTask(long taskId, long parentId, RequestFlow flow, String filename) {
        return createClientTask(taskId, flow, filename).setParentId(parentId).setRootId(parentId);
    }

    @Nonnull
    public static ClientTask createClientTask(long taskId, long parentId, RequestFlow flow) {
        return createClientTask(taskId, flow).setParentId(parentId).setRootId(parentId);
    }

    @Nonnull
    public static ClientTask createClientTask(long taskId, RequestFlow flow, String filename) {
        return createClientTask(taskId, flow).setMessage(FileReadingUtils.getFileContent(filename));
    }

    @Nonnull
    public static ClientTask createClientTask(long taskId, RequestFlow flow) {
        return new ClientTask()
            .setId(taskId)
            .setStatus(TaskStatus.IN_PROGRESS)
            .setFlow(flow);
    }
}
