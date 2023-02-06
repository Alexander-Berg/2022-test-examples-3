package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangPoolToTaskItemsPersister;
import ru.yandex.market.markup2.entries.yang.YangPoolDataItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author galaev
 * @since 2019-05-28
 */
public class YangPoolToTaskItemsPersisterStub extends YangPoolToTaskItemsPersister implements IPersisterStub {
    private final DefaultPersisterStub<Long, YangPoolDataItem> storage;

    public YangPoolToTaskItemsPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangPoolToTaskItemsPersisterStub(DefaultPersisterStub<Long, YangPoolDataItem> storage) {
        this.storage = storage;
    }

    public YangPoolToTaskItemsPersisterStub copy() {
        return new YangPoolToTaskItemsPersisterStub(this.storage);
    }

    @Override
    public List<YangPoolDataItem> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<YangPoolDataItem> getValues(Collection<Long> dataItemIds) {
        return storage.getValues(dataItemIds);
    }

    @Override
    protected List<YangPoolDataItem> getValues(Long... ids) {
        return storage.getValues(ids);
    }

    @Override
    protected List<YangPoolDataItem> getYangPoolDataItems(Integer... poolIds) {
        return getYangPoolDataItems(Arrays.asList(poolIds));
    }

    @Override
    protected List<YangPoolDataItem> getYangPoolDataItems(Collection<Integer> poolIds) {
        return storage.getByValues(poolIds, YangPoolDataItem::getPoolId);
    }

    @Override
    public void persist(YangPoolDataItem yangPoolDataItem) {
        storage.persist(yangPoolDataItem.getDataItemId(), yangPoolDataItem);
    }

    @Override
    public void persist(List<YangPoolDataItem> dataItemsExecutions) {
        dataItemsExecutions.forEach(this::persist);
    }
}
