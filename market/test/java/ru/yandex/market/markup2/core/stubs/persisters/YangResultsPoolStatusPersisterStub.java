package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangResultsPoolStatusPersister;
import ru.yandex.market.markup2.entries.yang.YangPoolStatusInfo;
import ru.yandex.market.toloka.model.PoolStatus;

import java.util.List;

/**
 * @author york
 * @since 19.06.2020
 */
public class YangResultsPoolStatusPersisterStub extends YangResultsPoolStatusPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, YangPoolStatusInfo> storage;

    public YangResultsPoolStatusPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangResultsPoolStatusPersisterStub(DefaultPersisterStub<Integer, YangPoolStatusInfo> storage) {
        this.storage = storage;
    }

    public YangResultsPoolStatusPersisterStub copy() {
        return new YangResultsPoolStatusPersisterStub(this.storage);
    }

    @Override
    public YangPoolStatusInfo getValue(Integer poolId) {
        return storage.getValue(poolId);
    }

    @Override
    public YangPoolStatusInfo getYangPoolStatusInfo(Integer pid, boolean isYang) {
        return storage.getAllValues().stream()
                .filter(value -> value.getId().equals(pid))
                .filter(value -> value.isYang() == isYang)
                .findFirst().orElse(null);
    }

    @Override
    public List<YangPoolStatusInfo> getYangOpenPools() {
        return storage.getByCriteria(yangPoolStatusInfo -> yangPoolStatusInfo.getStatus() == PoolStatus.OPEN &&
            yangPoolStatusInfo.isYang());
    }


    @Override
    public List<YangPoolStatusInfo> getOpenPools() {
        return storage.getByCriteria(yangPoolStatusInfo -> yangPoolStatusInfo.getStatus() == PoolStatus.OPEN);
    }

    @Override
    public void persist(YangPoolStatusInfo value) {
        storage.persist(value.getId(), value);
    }
}
