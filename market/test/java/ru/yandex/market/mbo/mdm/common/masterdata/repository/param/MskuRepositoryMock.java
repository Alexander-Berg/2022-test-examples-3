package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue.Key;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;

/**
 * Наследование от {@link ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock} тут
 * не подходит, т.к. тот использует Kryo, который не умеет работать с немутабельными коллекциями. А они обширно юзаются
 * в {@link MskuParamValue}.
 */
public class MskuRepositoryMock
    implements GenericMapperRepository<MskuParamValue, Key>, MskuRepository {

    private Map<Key, MskuParamValue> stored = new HashMap<>();

    @Override
    public MskuParamValue insert(MskuParamValue instance) {
        stored.put(instance.getKey(), instance);
        return instance;
    }

    @Override
    public List<MskuParamValue> insertBatch(Collection<MskuParamValue> instances) {
        instances.forEach(this::insert);
        return new ArrayList<>(instances);
    }

    @Override
    public MskuParamValue update(MskuParamValue mskuParamValue) {
        insert(mskuParamValue);
        return mskuParamValue;
    }

    @Override
    public List<MskuParamValue> updateBatch(Collection<MskuParamValue> instances, int batchSize) {
        return instances.stream().map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(List<Key> ids) {
        ids.forEach(stored::remove);
    }

    @Override
    public void delete(MskuParamValue mskuParamValue) {
        stored.remove(mskuParamValue.getKey());
    }

    @Override
    public void deleteBatch(Collection<MskuParamValue> mskuParamValues) {
        mskuParamValues.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        stored = new HashMap<>();
    }

    @Override
    public List<MskuParamValue> insertOrUpdateAll(Collection<MskuParamValue> mskuParamValues) {
        return insertBatch(mskuParamValues);
    }

    @Override
    public Integer insertOrUpdateAllIfDifferent(Collection<MskuParamValue> mskuParamValues) {
        throw new NotImplementedException("");
    }

    @Override
    public MskuParamValue findById(Key id) {
        return stored.get(id);
    }

    @Override
    public MskuParamValue findByIdForUpdate(Key id) {
        return findById(id);
    }

    @Override
    public List<MskuParamValue> findByIds(Collection<Key> ids) {
        return ids.stream().map(stored::get).collect(Collectors.toList());
    }

    @Override
    public List<MskuParamValue> findByIdsForUpdate(Collection<Key> ids) {
        return findByIds(ids);
    }

    @Override
    public List<MskuParamValue> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public int totalCount() {
        return stored.size();
    }

    @Override
    public void deleteMskus(Collection<Long> mskuIds) {
        Set<Long> idsSet = new LinkedHashSet<>(mskuIds);

        List<Key> toRemove = stored.values().stream()
            .filter(v -> idsSet.contains(v.getMskuId()))
            .map(MskuParamValue::getKey)
            .collect(Collectors.toList());

        toRemove.forEach(stored::remove);
    }

    @Override
    public Map<Long, CommonMsku> findAllMskus() {
        return stored.values().stream()
            .collect(Collectors.groupingBy(MskuParamValue::getMskuId, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> new CommonMsku(new ModelKey(0, entry.getKey()), entry.getValue()))
            .collect(Collectors.toMap(CommonMsku::getMskuId, Function.identity()));
    }

    @Override
    public Map<Long, CommonMsku> findMskus(Collection<Long> mskuIds) {
        return findByMskuIds(mskuIds).stream()
            .collect(Collectors.groupingBy(MskuParamValue::getMskuId, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> new CommonMsku(new ModelKey(0, entry.getKey()), entry.getValue()))
            .collect(Collectors.toMap(CommonMsku::getMskuId, Function.identity()));
    }

    @Override
    public CommonMsku insertOrUpdateMsku(CommonMsku msku) {
        deleteMsku(msku.getMskuId());
        Set<MskuParamValue> paramValues = msku.getValues();
        List<MskuParamValue> resultedParamValues = insertOrUpdateAll(paramValues);
        return new CommonMsku(msku.getKey(), resultedParamValues);
    }

    @Override
    public Map<Long, CommonMsku> insertOrUpdateMskus(Collection<CommonMsku> mskus) {
        List<Long> mskuIds = mskus.stream().map(CommonMsku::getMskuId).collect(Collectors.toList());
        mskuIds.forEach(this::deleteMsku);
        mskus.stream()
            .map(CommonMsku::getValues)
            .flatMap(Collection::stream)
            .forEach(this::insertOrUpdate);
        return findMskus(mskuIds);
    }

    private List<MskuParamValue> findByMskuIds(Collection<Long> mskuIds) {
        return stored.values().stream()
            .filter(value -> mskuIds.contains(value.getMskuId()))
            .collect(Collectors.toList());
    }

    @Override
    public Map<Long, CommonMsku> findByBmdmId(Collection<Long> bmdmIds) {
        return findAllMskus().entrySet().stream()
            .filter(it -> bmdmIds.contains(it.getValue().getBmdmId()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
