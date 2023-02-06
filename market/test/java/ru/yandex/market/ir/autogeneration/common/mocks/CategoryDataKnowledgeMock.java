package ru.yandex.market.ir.autogeneration.common.mocks;

import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;

import java.util.HashMap;
import java.util.Map;

public class CategoryDataKnowledgeMock extends CategoryDataKnowledge {

    private Map<Long, CategoryData> categoryDataMap;

    public CategoryDataKnowledgeMock() {
        this.categoryDataMap = new HashMap<>();
    }

    @Override
    public void afterPropertiesSet() {
        // No op
    }

    @Override
    public CategoryData getCategoryData(long categoryId) {
        return categoryDataMap.get(categoryId);
    }

    public void addCategoryData(Long categoryId, CategoryData categoryData) {
        categoryDataMap.put(categoryId, categoryData);
    }
}
