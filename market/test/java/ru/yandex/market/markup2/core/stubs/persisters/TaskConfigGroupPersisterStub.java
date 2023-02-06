package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.TaskConfigGroupPersister;
import ru.yandex.market.markup2.entries.group.ITaskConfigGroupInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskConfigGroupPersisterStub extends TaskConfigGroupPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, ITaskConfigGroupInfo> storage;

    public TaskConfigGroupPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskConfigGroupPersisterStub(DefaultPersisterStub<Integer, ITaskConfigGroupInfo> storage) {
        this.storage = storage;
    }

    public TaskConfigGroupPersisterStub copy() {
        return new TaskConfigGroupPersisterStub(this.storage);
    }

    @Override
    public int getNextGroupConfigId() {
        return storage.generateNextInt();
    }

    @Override
    public List<ITaskConfigGroupInfo> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public void persist(ITaskConfigGroupInfo taskConfigGroupInfo) {
        storage.upsert(taskConfigGroupInfo, ITaskConfigGroupInfo::getId);
    }

    @Override
    public List<ITaskConfigGroupInfo> getValues(int typeId, int categoryId) {
        return storage.getAllValues().stream()
            .filter(t -> t.getCategoryId() == categoryId && t.getTypeInfoId() == typeId)
            .collect(Collectors.toList());
    }

    public List<ITaskConfigGroupInfo> getValues(Set<Integer> typeIds) {
        return storage.getAllValues().stream()
            .filter(t -> typeIds.contains(t.getTypeInfoId()))
            .collect(Collectors.toList());
    }
}
