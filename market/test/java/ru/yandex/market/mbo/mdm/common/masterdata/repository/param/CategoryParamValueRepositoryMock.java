package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;

import ru.yandex.market.mbo.lightmapper.GenericMapperRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue.Key;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.filter.CategorySearchFilter;

public class CategoryParamValueRepositoryMock
    implements CategoryParamValueRepository, GenericMapperRepository<CategoryParamValue, Key> {

    private Map<CategoryParamValue.Key, CategoryParamValue> stored = new HashMap<>();

    @Override
    public Map<Long, List<CategoryParamValue>> findCategoryParamValues(Collection<Long> categoryIds) {
        return stored.values().stream()
            .filter(v -> categoryIds.contains(v.getCategoryId()))
            .collect(Collectors.groupingBy(CategoryParamValue::getCategoryId));
    }

    @Override
    public CategoryParamValue insert(CategoryParamValue instance) {
        stored.put(instance.getKey(), instance);
        return instance;
    }

    @Override
    public List<CategoryParamValue> insertBatch(Collection<CategoryParamValue> instances) {
        instances.forEach(this::insert);
        return new ArrayList<>(instances);
    }

    @Override
    public CategoryParamValue update(CategoryParamValue categoryParamValue) {
        insert(categoryParamValue);
        return categoryParamValue;
    }

    @Override
    public List<CategoryParamValue> updateBatch(Collection<CategoryParamValue> instances, int batchSize) {
        return instances.stream().map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(List<CategoryParamValue.Key> ids) {
        ids.forEach(stored::remove);
    }

    @Override
    public void delete(CategoryParamValue categoryParamValue) {
        stored.remove(categoryParamValue.getKey());
    }

    @Override
    public void deleteBatch(Collection<CategoryParamValue> categoryParamValues) {
        categoryParamValues.forEach(this::delete);
    }

    @Override
    public void deleteAll() {
        stored = new HashMap<>();
    }

    @Override
    public List<CategoryParamValue> insertOrUpdateAll(Collection<CategoryParamValue> categoryParamValues) {
        return insertBatch(categoryParamValues);
    }

    @Override
    public Integer insertOrUpdateAllIfDifferent(Collection<CategoryParamValue> categoryParamValues) {
        throw new NotImplementedException("");
    }

    @Override
    public CategoryParamValue findById(CategoryParamValue.Key id) {
        return stored.get(id);
    }

    @Override
    public CategoryParamValue findByIdForUpdate(Key id) {
        return findById(id);
    }

    @Override
    public List<CategoryParamValue> findByIds(Collection<CategoryParamValue.Key> ids) {
        return ids.stream().filter(stored::containsKey).map(stored::get).collect(Collectors.toList());
    }

    @Override
    public List<CategoryParamValue> findByIdsForUpdate(Collection<Key> ids) {
        return findByIds(ids);
    }

    @Override
    public List<CategoryParamValue> findAll() {
        return new ArrayList<>(stored.values());
    }

    @Override
    public int totalCount() {
        return stored.size();
    }

    @Override
    public void findNotProcessed(int limit, Consumer<List<CategoryParamValue>> action) {
        throw new NotImplementedException("");
    }

    @Override
    public List<CategoryParamValue> findNotProcessed(CategoryParamValue.Key from, int limit, boolean lock) {
        throw new NotImplementedException("");
    }

    @Override
    public List<CategoryParamValue> findCategoryParamValues(long categoryId) {
        return findAll().stream()
            .filter(p -> p.getCategoryId() == categoryId)
            .collect(Collectors.toList());
    }

    @Override
    public List<CategoryParamValue> findByParamId(long mdmParamId) {
        return findAll().stream()
            .filter(pv -> pv.getMdmParamId() == mdmParamId)
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> findCategoryIdsByFilter(CategorySearchFilter filter) {
        throw new UnsupportedOperationException();
    }
}
