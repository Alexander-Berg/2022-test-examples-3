package ru.yandex.market.markup2.core.stubs.persisters;

import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.markup2.dao.TaskConfigPersister;
import ru.yandex.market.markup2.entries.config.TaskConfigData;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.config.TaskConfigState;
import ru.yandex.market.markup2.entries.group.ITaskConfigGroupInfo;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 25.05.2018
 */
public class TaskConfigPersisterStub extends TaskConfigPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, TaskConfigData> storage;

    public TaskConfigPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private TaskConfigPersisterStub(DefaultPersisterStub<Integer, TaskConfigData> storage) {
        this.storage = storage;
    }

    public TaskConfigPersisterStub copy() {
        return new TaskConfigPersisterStub(this.storage);
    }

    @Override
    public List<TaskConfigData> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<TaskConfigData> getValues(Collection<Integer> ids) {
        return storage.getValues(ids);
    }

    @Override
    public List<TaskConfigData> getByCategoryId(int categoryId, Set<TaskConfigState> states) {
        Set<Integer> configsGroups = configGroupPersister.getAllValues().stream()
            .filter(cg -> cg.getCategoryId() == categoryId)
            .map(ITaskConfigGroupInfo::getId)
            .collect(Collectors.toSet());
        Set<TaskConfigState> effectiveStates = states.isEmpty() ? EnumSet.allOf(TaskConfigState.class) : states;

        return storage.getByCriteria(c -> configsGroups.contains(c.getGroupId()) &&
            effectiveStates.contains(c.getState()));
    }

    @Override
    public List<TaskConfigData> getByTypeIds(Set<Integer> typeIds, Set<TaskConfigState> states) {
        Set<Integer> configsGroups = configGroupPersister.getAllValues().stream()
            .filter(cg -> typeIds.contains(cg.getTypeInfoId()))
            .map(ITaskConfigGroupInfo::getId)
            .collect(Collectors.toSet());
        Set<TaskConfigState> effectiveStates = states.isEmpty() ? EnumSet.allOf(TaskConfigState.class) : states;

        return storage.getByCriteria(c -> configsGroups.contains(c.getGroupId()) &&
            effectiveStates.contains(c.getState()));
    }

    @Override
    public List<TaskConfigData> getFailedByTypeIds(Set<Integer> typeIds, Set<TaskConfigState> states) {
        Set<Integer> configsGroups = configGroupPersister.getAllValues().stream()
            .filter(cg -> typeIds.contains(cg.getTypeInfoId()))
            .map(ITaskConfigGroupInfo::getId)
            .collect(Collectors.toSet());
        Set<TaskConfigState> effectiveStates = states.isEmpty() ? EnumSet.allOf(TaskConfigState.class) : states;

        return storage.getByCriteria(c -> c.isFailedProcessing() && configsGroups.contains(c.getGroupId()) &&
            effectiveStates.contains(c.getState()));
    }

    @Override
    public List<TaskConfigData> getByCategoryIdAndTypeId(int categoryId, int typeId, Set<TaskConfigState> states) {
        Set<Integer> configsGroups = configGroupPersister.getAllValues().stream()
            .filter(cg -> cg.getTypeInfoId() == typeId && cg.getCategoryId() == categoryId)
            .map(ITaskConfigGroupInfo::getId)
            .collect(Collectors.toSet());
        Set<TaskConfigState> effectiveStates = states.isEmpty() ? EnumSet.allOf(TaskConfigState.class) : states;

        return storage.getByCriteria(c -> configsGroups.contains(c.getGroupId()) &&
            effectiveStates.contains(c.getState()));
    }

    @Override
    public int getNextConfigId() {
        return storage.generateNextInt();
    }

    @Override
    public void insert(TaskConfigInfo config) {
        storage.persist(config.getId(), new TaskConfigData(config));
    }

    @Override
    public void update(TaskConfigInfo config) {
        storage.persist(config.getId(), new TaskConfigData(config));
    }

    @Override
    public void updateDependConfig(TaskConfigInfo config) {
        TaskConfigData configData = storage.getValue(config.getId());
        ReflectionTestUtils.setField(configData, "dependConfigId", config.getDependConfigId());
    }

    @Override
    public void updateFailedProcessing(TaskConfigInfo config) {
        TaskConfigData configData = storage.getValue(config.getId());
        ReflectionTestUtils.setField(configData, "failedProcessing", config.isFailedProcessing());
    }

    @Override
    public void updateFailureReportUrl(TaskConfigInfo config) {
        TaskConfigData configData = storage.getValue(config.getId());
        ReflectionTestUtils.setField(configData, "failureReportUrl", config.getFailureReportUrl());
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
