package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.dao.TaskPersister;
import ru.yandex.market.markup2.entries.task.TaskData;
import ru.yandex.market.markup2.entries.task.TaskInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskPersisterStub extends TaskPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, TaskData> storage;

    public TaskPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskPersisterStub(DefaultPersisterStub<Integer, TaskData> storage) {
        this.storage = storage;
    }

    public TaskPersisterStub copy() {
        return new TaskPersisterStub(this.storage);
    }

    @Override
    protected TaskData getValue(Integer integer) {
        return storage.getValue(integer);
    }

    @Override
    public List<TaskData> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<TaskData> getValues(Integer... integers) {
        return storage.getValues(integers);
    }

    @Override
    public List<TaskData> getValues(Collection<Integer> integers) {
        return storage.getValues(integers);
    }

    @Override
    public int getNextConfigId() {
        return storage.generateNextInt();
    }

    @Override
    public void insert(TaskInfo task) {
        storage.persist(task.getId(), new TaskData(task));
    }

    @Override
    public void updateState(TaskInfo task) {
        TaskData data = storage.getValue(task.getId());
        MockMarkupTestBase.setField(data, "state", task.getState());
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
