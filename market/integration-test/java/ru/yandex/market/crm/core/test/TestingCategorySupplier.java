package ru.yandex.market.crm.core.test;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import ru.yandex.market.crm.core.domain.categories.CategoryCacheData;
import ru.yandex.market.crm.core.suppliers.CategoriesDataSupplier;

public class TestingCategorySupplier implements CategoriesDataSupplier {

    private CategoryCacheData data = new CategoryCacheData(Int2ObjectMaps.emptyMap());

    @Override
    public CategoryCacheData get() {
        return data;
    }

    public void setData(CategoryCacheData data) {
        this.data = data;
    }
}
