package ru.yandex.market.mbo.export.modelstorage;

import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CategoryInfoLoaderMock extends CategoryInfoLoader {

    private Map<Long, CategoryInfo> mocks = new HashMap<>();

    public CategoryInfoLoaderMock() {
    }

    public CategoryInfoLoaderMock(CategoryInfo... categoryInfos) {
        Arrays.stream(categoryInfos).forEach(this::addCategoryInfo);
    }

    public CategoryInfoLoaderMock addCategoryInfo(CategoryInfo categoryInfo) {
        this.mocks.put(categoryInfo.getCategoryId(), categoryInfo);
        return this;
    }

    @Override
    public CategoryInfo create(long categoryHid) {
        return mocks.get(categoryHid);
    }

    @Override
    public CategoryInfo create(TovarCategory category) {
        return mocks.get(category.getHid());
    }
}
