package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskConfigGroupStatisticPersister;
import ru.yandex.market.markup2.entries.group.TaskGroupStatisticInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskConfigGroupStatisticPersisterStub extends TaskConfigGroupStatisticPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, TaskGroupStatisticInfo> storage;

    public TaskConfigGroupStatisticPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskConfigGroupStatisticPersisterStub(DefaultPersisterStub<Integer, TaskGroupStatisticInfo> storage) {
        this.storage = storage;
    }

    public TaskConfigGroupStatisticPersisterStub copy() {
        return new TaskConfigGroupStatisticPersisterStub(this.storage);
    }

    @Override
    public List<TaskGroupStatisticInfo> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<TaskGroupStatisticInfo> getValues(Collection<Integer> values) {
        return storage.getValues(values);
    }

    @Override
    public void upsert(TaskGroupStatisticInfo statisticInfo) {
        storage.upsert(statisticInfo, TaskGroupStatisticInfo::getGroupId);
    }

    @Override
    public void update(TaskGroupStatisticInfo statisticInfo) {
        storage.upsert(statisticInfo, TaskGroupStatisticInfo::getGroupId);
    }

    @Override
    public TaskGroupStatisticInfo getValue(Integer id) {
        return storage.getValue(id);
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
