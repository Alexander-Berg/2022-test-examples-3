package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.YangPoolInfoPersister;
import ru.yandex.market.markup2.entries.yang.YangPoolInfo;

import java.util.Collection;
import java.util.List;

/**
 * @author galaev
 * @since 2019-05-28
 */
public class YangPoolInfoPersisterStub extends YangPoolInfoPersister implements IPersisterStub {
    private final DefaultPersisterStub<Integer, YangPoolInfo> storage;

    public YangPoolInfoPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private YangPoolInfoPersisterStub(DefaultPersisterStub<Integer, YangPoolInfo> storage) {
        this.storage = storage;
    }

    public YangPoolInfoPersisterStub copy() {
        return new YangPoolInfoPersisterStub(this.storage);
    }

    @Override
    public List<YangPoolInfo> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<YangPoolInfo> getValues(Collection<Integer> poolIds) {
        return storage.getValues(poolIds);
    }

    @Override
    public YangPoolInfo getValue(Integer poolId) {
        return storage.getValue(poolId);
    }

    @Override
    public List<YangPoolInfo> getByTaskId(int taskId) {
        return storage.getByValue(taskId, YangPoolInfo::getTaskId);
    }

    @Override
    public List<YangPoolInfo> getByTaskIds(Collection<Integer> taskIds) {
        return storage.getByValues(taskIds, YangPoolInfo::getTaskId);
    }

    @Override
    public void persist(YangPoolInfo poolInfo) {
        storage.persist(poolInfo.getPoolId(), poolInfo);
    }

    @Override
    public void persist(List<YangPoolInfo> datas) {
        datas.forEach(this::persist);
    }

    @Override
    public void updatePoolName(YangPoolInfo poolInfo) {
        storage.updateFields(poolInfo.getPoolId(), poolInfo, "poolName");
    }
}
