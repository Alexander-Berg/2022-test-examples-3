package ru.yandex.market.psku.postprocessor.service.tracker.mock;

import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfoProducer;

import java.util.HashMap;
import java.util.Map;

public class CategoryTrackerInfoProducerMock implements CategoryTrackerInfoProducer {
    private final Map<Long, String> categoriesMap = new HashMap<>();
    public static final String DEFAULT_CATEGORY_NAME = "default_category_name";

    @Override
    public CategoryTrackerInfo getCategoryInfo(long categoryId) {
        return new CategoryTrackerInfo(categoryId, categoriesMap.getOrDefault(categoryId, DEFAULT_CATEGORY_NAME));
    }

    public void addCategoryInfo(Long categoryId, String categoryName) {
        categoriesMap.put(categoryId, categoryName);
    }
}
