package ru.yandex.market.mboc.common.utils;

import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

/**
 * @author yuramalinov
 * @created 30.10.18
 */
public class ModelTestUtils {

    public static final int SKU_ID = 1042;
    public static final String SKU_TITLE = "Test #1042";
    public static final long SKU_CATEGORY_ID = 42;

    private ModelTestUtils() {
    }

    public static Model publishedSku() {
        return new Model()
            .setId(SKU_ID)
            .setCategoryId(SKU_CATEGORY_ID)
            .setTitle(SKU_TITLE)
            .setPublishedOnBlueMarket(true)
            .setModelType(Model.ModelType.SKU);
    }
}
