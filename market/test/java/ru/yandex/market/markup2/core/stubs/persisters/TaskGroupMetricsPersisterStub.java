package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskGroupMetricsPersister;
import ru.yandex.market.markup2.entries.group.ITaskGroupMetricsData;
import ru.yandex.market.markup2.entries.group.TaskGroupMetrics;
import ru.yandex.market.markup2.entries.group.TaskGroupMetricsStub;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskGroupMetricsPersisterStub extends TaskGroupMetricsPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, TaskGroupMetricsStub> storage;

    public TaskGroupMetricsPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskGroupMetricsPersisterStub(DefaultPersisterStub<Integer, TaskGroupMetricsStub> storage) {
        this.storage = storage;
    }

    public TaskGroupMetricsPersisterStub copy() {
        return new TaskGroupMetricsPersisterStub(this.storage);
    }

    @Override
    public void upsert(int taskTypeId, TaskGroupMetrics metrics) {
        storage.upsert(convert(taskTypeId, metrics), TaskGroupMetrics::getGroupId);
    }

    @Override
    public List<TaskGroupMetricsStub> getValues(Collection<Integer> values) {
        return storage.getValues(values);
    }

    private TaskGroupMetricsStub convert(int taskTypeId, TaskGroupMetrics i) {
        try {
            String data = writeMetricsData(taskTypeId, (ITaskGroupMetricsData) i.getMetricsData());
            return new TaskGroupMetricsStub(
                i.getGroupId(),
                data,
                i.getLoggingTime()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
