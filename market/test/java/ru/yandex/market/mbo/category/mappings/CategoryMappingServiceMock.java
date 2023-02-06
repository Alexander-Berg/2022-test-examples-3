package ru.yandex.market.mbo.category.mappings;

import ru.yandex.market.mbo.db.errors.CategoryNotFoundException;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class CategoryMappingServiceMock implements CategoryMappingService {

    private final Map<Long, CategoryMapping> data = new HashMap<>();

    public void addMapping(long categoryId, Long guruCategoryId) {
        data.put(categoryId, new CategoryMapping(categoryId, guruCategoryId));
    }

    @Nullable
    @Override
    public Long getGuruCategoryByCategoryId(long categoryId) {
        if (!data.containsKey(categoryId)) {
            throw new CategoryNotFoundException("Category with id: " + categoryId + " doesn't exist");
        }
        return data.get(categoryId).getGuruCategoryId();
    }

    @Override
    public long getCategoryIdByGuruCategoryId(long guruCategoryId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<CategoryMapping> getAllGuruCategoryMappings() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<CategoryMapping> getCategoryMappingsByCategoryIds(Collection<Long> categoryIds) {
        return categoryIds.stream()
            .distinct()
            .map(data::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<CategoryMapping> getCategoryMappingsByGuruCategoryIds(Collection<Long> guruCategoryIds) {
        throw new RuntimeException("Not implemented");
    }
}
