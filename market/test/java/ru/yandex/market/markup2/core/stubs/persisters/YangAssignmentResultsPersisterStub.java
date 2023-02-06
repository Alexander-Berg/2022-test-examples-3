package ru.yandex.market.markup2.core.stubs.persisters;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.markup2.dao.YangAssignmentResultsPersister;
import ru.yandex.market.toloka.model.ResultItem;

/**
 * @author york
 * @since 04.12.2019
 */
public class YangAssignmentResultsPersisterStub extends YangAssignmentResultsPersister implements IPersisterStub {
    private final DefaultPersisterStub<String, ResultItem> storage;

    public YangAssignmentResultsPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangAssignmentResultsPersisterStub(DefaultPersisterStub<String, ResultItem> storage) {
        this.storage = storage;
    }

    public YangAssignmentResultsPersisterStub copy() {
        return new YangAssignmentResultsPersisterStub(this.storage);
    }

    @Override
    public List<ResultItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<ResultItem> getValues(Collection<String> assignmentIds) {
        return storage.getByValues(assignmentIds, ResultItem::getId);
    }

    @Override
    public List<ResultItem> getByTaskSuiteId(String taskSuiteId) {
        return storage.getByValue(taskSuiteId, ResultItem::getTaskSuiteId);
    }

    @Override
    public List<ResultItem> getByPoolId(int poolId) {
        return storage.getByValue(poolId, ResultItem::getPoolId);
    }

    @Override
    public List<ResultItem> getByPoolAndTaskId(int poolId, String taskId) {
        return storage.getByCriteria(r -> r.getPoolId() == poolId
            && r.getTasks().stream().filter(t -> t.getId().equals(taskId)).findFirst().isPresent());
    }

    @Override
    public void persist(ResultItem item) {
        storage.persist(item.getId(), item);
    }

    @Override
    public List<Integer> getNotDownloadedTaskIds() {
        return Collections.emptyList();
    }
}
