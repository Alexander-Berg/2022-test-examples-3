package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.yang.YangTaskToDataItems;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 04.12.2019
 */
public class YangTaskToDataItemsPersisterStub extends YangTaskToDataItemsPersister implements IPersisterStub {
    private final DefaultPersisterStub<String, YangTaskToDataItems> storage;

    public YangTaskToDataItemsPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangTaskToDataItemsPersisterStub(DefaultPersisterStub<String, YangTaskToDataItems> storage) {
        this.storage = storage;
    }

    public YangTaskToDataItemsPersisterStub copy() {
        return new YangTaskToDataItemsPersisterStub(this.storage);
    }

    @Override
    public List<YangTaskToDataItems> getAllValues() {
        return storage.getAllValues();
    }

    public List<YangTaskToDataItems> getByPoolId(int poolId) {
        return storage.getByValue(poolId, YangTaskToDataItems::getPoolId);
    }

    public List<YangTaskToDataItems> getByPoolIds(Collection<Integer> poolIds) {
        return storage.getByValues(poolIds, YangTaskToDataItems::getPoolId);
    }

    public List<YangTaskToDataItems> getByMarkupTaskId(int markupTaskId) {
        return storage.getByValue(markupTaskId, YangTaskToDataItems::getMarkupTaskId);
    }

    public List<YangTaskToDataItems> getByMarkupTaskIds(Collection<Integer> markupTaskIds) {
        return storage.getByValues(markupTaskIds, YangTaskToDataItems::getMarkupTaskId);
    }

    public List<YangTaskToDataItems> getByTaskSuiteIds(List<String> taskSuiteIds) {
        return storage.getByValues(taskSuiteIds, YangTaskToDataItems::getTaskSuiteId);
    }

    @Override
    public void updatePriorities(List<YangTaskToDataItems> items) {
        for (YangTaskToDataItems item : items) {
            storage.updateFields(item.getTaskId(), item,
                "rawPriority", "issuingOrderOverride");
        }
    }

    @Override
    public void updateCancelled(YangTaskToDataItems items) {
        storage.updateFields(items.getTaskId(), items, "cancelled");
    }

    public void persist(YangTaskToDataItems value) {
        storage.persist(value.getTaskId(), value);
    }
}
