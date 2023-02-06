package ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.test.GenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;

public class CategoryRslRepositoryMock extends GenericMapperRepositoryMock<CategoryRsl, CategoryRsl.Key>
    implements CategoryRslRepository {

    public CategoryRslRepositoryMock() {
        super(null, CategoryRsl::getKey);
    }

    @Override
    protected CategoryRsl.Key nextId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, List<CategoryRsl>> findByCategoryIds(Collection<Long> ids) {
        Set<Long> uniqueKeys = new HashSet<>(ids);
        return findAll().stream()
            .filter(i -> uniqueKeys.contains(i.getCategoryId()))
            .collect(Collectors.groupingBy(CategoryRsl::getCategoryId));
    }
}
