package ru.yandex.market.logistics.dbqueue.impl;

import lombok.RequiredArgsConstructor;

import ru.yandex.money.common.dbqueue.settings.ProcessingMode;
import ru.yandex.money.common.dbqueue.settings.TaskRetryType;

@RequiredArgsConstructor
public enum DbQueueTaskType implements DbQueueTaskTypeInterface {
    TEST_EVENT(
            UpdateBookingEventTaskPayload.class,
            1,
            null,
            ProcessingMode.WRAP_IN_TRANSACTION,
            30_000,
            TaskRetryType.LINEAR_BACKOFF
    ),
    TEST_QUOTA(
            TestPayload.class,
            1,
            null,
            ProcessingMode.WRAP_IN_TRANSACTION,
            30_000,
            TaskRetryType.LINEAR_BACKOFF);

    private Class<? extends DbQueueTaskPayloadInterface> dbqueueTaskPayloadClass;
    private int threadCount;
    private Integer delayBetweenTasks;
    private ProcessingMode processingMode;
    private int retryInterval;
    private TaskRetryType taskRetryType;

    DbQueueTaskType(
            Class<? extends DbQueueTaskPayloadInterface> dbqueueTaskPayloadClass, int threadCount,
            Integer delayBetweenTasks, ProcessingMode processingMode, int retryInterval,
            TaskRetryType taskRetryType) {
        this.dbqueueTaskPayloadClass = dbqueueTaskPayloadClass;
        this.threadCount = threadCount;
        this.delayBetweenTasks = delayBetweenTasks;
        this.processingMode = processingMode;
        this.retryInterval = retryInterval;
        this.taskRetryType = taskRetryType;
    }

    @Override
    public String getTaskName() {
        return this.name();
    }

    @Override
    public Class<? extends DbQueueTaskPayloadInterface> getPayloadClass() {
        return dbqueueTaskPayloadClass;
    }

    @Override
    public Integer getDelayBetweenTasks() {
        return delayBetweenTasks;
    }

    @Override
    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    @Override
    public int getRetryInterval() {
        return retryInterval;
    }

    @Override
    public TaskRetryType getTaskRetryType() {
        return taskRetryType;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

}
