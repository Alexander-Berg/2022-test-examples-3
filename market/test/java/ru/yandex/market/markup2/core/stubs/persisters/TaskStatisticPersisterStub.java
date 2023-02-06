package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskStatisticPersister;
import ru.yandex.market.markup2.entries.task.TaskStatisticData;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskStatisticPersisterStub extends TaskStatisticPersister implements IPersisterStub {
    private DefaultPersisterStub<Integer, TaskStatisticData> storage = new DefaultPersisterStub<>();

    public TaskStatisticPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskStatisticPersisterStub(DefaultPersisterStub<Integer, TaskStatisticData> storage) {
        this.storage = storage;
    }

    public TaskStatisticPersisterStub copy() {
        return new TaskStatisticPersisterStub(this.storage);
    }

    @Override
    public List<TaskStatisticData> getValues(Collection<Integer> values) {
        return storage.getValues(values);
    }

    @Override
    public void insert(TaskStatisticData data) {
        storage.persist(data.getTaskId(), data);
    }

    @Override
    public void updateGenerationStatistic(TaskStatisticData data) {
        storage.updateFields(data.getTaskId(), data, "generatedEntitiesCount", "generatedCount",
            "inaneGenerateCount", "lastGenerateTime", "generationStatus");
    }

    @Override
    public void updateSendStatistic(TaskStatisticData data) {
        storage.updateFields(data.getTaskId(), data,
            "sentCount", "lastSendTime", "sendingStatus");
    }

    @Override
    public void updateReceiveStatistic(TaskStatisticData data) {
        storage.updateFields(data.getTaskId(), data, "receivedCount", "lostCount",
            "receivedFailedCount", "lastReceiveTime", "receivingStatus");
    }

    @Override
    public void updateProcessingStatistic(TaskStatisticData data) {
        storage.updateFields(data.getTaskId(), data, "processedCount", "failedProcessingCount",
            "cannotCount", "lastProcessTime", "processingStatus");
    }

    @Override
    public void updateFinalizationStatistic(TaskStatisticData data) {
        storage.updateFields(data.getTaskId(), data, "lastProcessTime");
    }

    @Override
    public void updateFull(TaskStatisticData data) {
        storage.persist(data.getTaskId(), data);
    }
}
