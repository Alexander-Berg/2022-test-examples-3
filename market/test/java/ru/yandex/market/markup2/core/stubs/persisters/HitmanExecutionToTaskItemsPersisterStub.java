package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.HitmanExecutionToTaskItemsPersister;
import ru.yandex.market.markup2.entries.hitman.ExecutedDataItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 24.05.2018
 */
public class HitmanExecutionToTaskItemsPersisterStub extends HitmanExecutionToTaskItemsPersister
        implements IPersisterStub {
    private final DefaultPersisterStub<Long, ExecutedDataItem> storage;

    public HitmanExecutionToTaskItemsPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private HitmanExecutionToTaskItemsPersisterStub(DefaultPersisterStub<Long, ExecutedDataItem> storage) {
        this.storage = storage;
    }

    public HitmanExecutionToTaskItemsPersisterStub copy() {
        return new HitmanExecutionToTaskItemsPersisterStub(this.storage);
    }

    @Override
    public List<ExecutedDataItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<ExecutedDataItem> getValues(Collection<Long> dataItemIds) {
        return storage.getValues(dataItemIds);
    }

    @Override
    protected List<ExecutedDataItem> getValues(Long... ids) {
        return storage.getValues(ids);
    }

    @Override
    protected List<ExecutedDataItem> getExecutedDataItems(String... executionIds) {
        return getExecutedDataItems(Arrays.asList(executionIds));
    }

    @Override
    protected List<ExecutedDataItem> getExecutedDataItems(Collection<String> executionIds) {
        return storage.getByValues(executionIds, ExecutedDataItem::getHitmanExecutionId);
    }

    @Override
    public void persist(ExecutedDataItem executedDataItem) {
        storage.persist(executedDataItem.getDataItemId(), executedDataItem);
    }

    @Override
    public void persist(List<ExecutedDataItem> dataItemsExecutions) {
        dataItemsExecutions.forEach(this::persist);
    }
}
