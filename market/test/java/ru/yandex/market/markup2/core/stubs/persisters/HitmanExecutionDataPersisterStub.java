package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.dao.HitmanExecutionDataPersister;
import ru.yandex.market.markup2.entries.hitman.HitmanExecutionData;

import java.util.Collection;
import java.util.List;

/**
 * @author york
 * @since 24.05.2018
 */
public class HitmanExecutionDataPersisterStub extends HitmanExecutionDataPersister implements IPersisterStub {
    private final DefaultPersisterStub<String, HitmanExecutionData> storage;

    public HitmanExecutionDataPersisterStub() {
        this(new DefaultPersisterStub<>());
    }

    private HitmanExecutionDataPersisterStub(DefaultPersisterStub<String, HitmanExecutionData> storage) {
        this.storage = storage;
    }

    public HitmanExecutionDataPersisterStub copy() {
        return new HitmanExecutionDataPersisterStub(this.storage);
    }

    @Override
    public List<HitmanExecutionData> getAllValues() {
        return storage.getAllValues();
    }

    @Override
    public List<HitmanExecutionData> getValues(Collection<String> executionIds) {
        return storage.getValues(executionIds);
    }

    @Override
    public List<HitmanExecutionData> getByTaskId(int taskId) {
        return storage.getByValue(taskId, HitmanExecutionData::getTaskId);
    }

    @Override
    public List<HitmanExecutionData> getByTaskIds(Collection<Integer> taskIds) {
        return storage.getByValues(taskIds, HitmanExecutionData::getTaskId);
    }

    @Override
    public void persist(HitmanExecutionData statisticInfo) {
        storage.persist(statisticInfo.getHitmanExecutionId(), statisticInfo);
    }

    @Override
    public void persist(List<HitmanExecutionData> datas) {
        datas.forEach(this::persist);
    }

}
