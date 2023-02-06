package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskDataItemOperationStatusPersister;
import ru.yandex.market.markup2.workflow.general.TaskDataItemOperationStatus;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskDataItemOperationStatusPersisterStub extends TaskDataItemOperationStatusPersister
        implements IPersisterStub {
    private final DefaultPersisterStub<Long, TaskDataItemOperationStatus> storage;

    public TaskDataItemOperationStatusPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskDataItemOperationStatusPersisterStub(DefaultPersisterStub<Long, TaskDataItemOperationStatus> storage) {
        this.storage = storage;
    }

    public TaskDataItemOperationStatusPersisterStub copy() {
        return new TaskDataItemOperationStatusPersisterStub(this.storage);
    }

    @Override
    public TaskDataItemOperationStatus get(Long id) {
        return storage.getValue(id);
    }

    @Override
    public List<TaskDataItemOperationStatus> get(Collection<Long> ids) {
        return storage.getValues(ids);
    }

    @Override
    public void insert(Collection<TaskDataItemOperationStatus> statuses) {
        storage.upsertAll(statuses, TaskDataItemOperationStatus::getTaskDataItemId);
    }

    @Override
    public void upsert(TaskDataItemOperationStatus status) {
        storage.upsert(status, TaskDataItemOperationStatus::getTaskDataItemId);
    }

    @Override
    public void upsert(Collection<TaskDataItemOperationStatus> statuses) {
        storage.upsertAll(statuses, TaskDataItemOperationStatus::getTaskDataItemId);
    }

    @Override
    public long generateNextLong() {
        return storage.generateNextLong();
    }

    @Override
    public int generateNextInt() {
        return storage.generateNextInt();
    }

    @Override
    public List<Long> generateNextLongs(int count) {
        return storage.generateNextLongs(count);
    }

    @Override
    public List<Integer> generateNextInts(int count) {
        return storage.generateNextInts(count);
    }
}
