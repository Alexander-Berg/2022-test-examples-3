package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangAssignmentPersister;
import ru.yandex.market.markup2.entries.yang.YangAssignmentInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author galaev
 * @since 2019-06-13
 */
public class YangAssignmentPersisterStub extends YangAssignmentPersister implements IPersisterStub {
    private final DefaultPersisterStub<String, YangAssignmentInfo> storage;

    public YangAssignmentPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangAssignmentPersisterStub(DefaultPersisterStub<String, YangAssignmentInfo> storage) {
        this.storage = storage;
    }

    public YangAssignmentPersisterStub copy() {
        return new YangAssignmentPersisterStub(this.storage);
    }

    @Override
    public List<YangAssignmentInfo> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<YangAssignmentInfo> getValues(Collection<String> assignmentIds) {
        return storage.getValues(assignmentIds);
    }

    @Override
    public List<YangAssignmentInfo> getByPoolId(int poolId) {
        return storage.getByValue(poolId, YangAssignmentInfo::getPoolId);
    }

    @Override
    public List<YangAssignmentInfo> getByTaskSuiteId(String taskSuiteId) {
        return storage.getByValue(taskSuiteId, YangAssignmentInfo::getTaskSuiteId);
    }

    @Override
    public List<YangAssignmentInfo> getByPoolIds(Collection<Integer> poolIds) {
        return storage.getByValues(poolIds, YangAssignmentInfo::getPoolId);
    }

    @Override
    public List<YangAssignmentInfo> getByTaskSuiteIds(Collection<String> taskSuiteIds) {
        return storage.getByValues(taskSuiteIds, YangAssignmentInfo::getTaskSuiteId);
    }

    @Override
    public void persist(YangAssignmentInfo assignmentInfo) {
        storage.persist(assignmentInfo.getAssignmentId(), assignmentInfo);
    }

    @Override
    public List<YangAssignmentInfo> getByTaskId(String taskId) {
        return storage.getByValue(taskId, YangAssignmentInfo::getTaskId);
    }

    @Override
    public void persist(List<YangAssignmentInfo> datas) {
        datas.forEach(this::persist);
    }

}
