package ru.yandex.market.aliasmaker.cache.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.rocksdb.RocksDBException;

import ru.yandex.market.mbo.http.ModelStorage;

public class DiskCategoryCacheTest {
    private static final int CATEGORY_ID = 1003092;

    private CategoryModelsCache guruCache;
    private CategoryModelsCache skuCache;

    @Before
    public void setUp() throws RocksDBException {
        CategoryModelsCacheFactory factory = new CategoryModelsCacheFactory(Collections.singleton(CATEGORY_ID),
                System.getProperty("java.io.tmpdir") + "/prod",
                System.getProperty("java.io.tmpdir") + "/tmp"
        );
        guruCache = factory.createCache(CATEGORY_ID, ModelStorage.ModelType.GURU, false, true);
        skuCache = factory.createCache(CATEGORY_ID, ModelStorage.ModelType.SKU, false, true);
    }

    @Test
    public void testInsert() {
        Set<ModelStorage.Model> beforeGuru = new HashSet<>();
        Set<ModelStorage.Model> beforeSku = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            ModelStorage.Model model = ModelStorage.Model.getDefaultInstance();
            model = model.toBuilder()
                    .setId(i)
                    .setVendorId(1 + i / 3)
                    .setCurrentType(ModelStorage.ModelType.GURU.name())
                    .build();
            guruCache.putModel(model);
            beforeGuru.add(model);
            ModelStorage.Model sku = model.toBuilder()
                    .setCurrentType(ModelStorage.ModelType.SKU.name())
                    .build();
            skuCache.putModel(sku);
            beforeSku.add(sku);
        }
        for (ModelStorage.Model model : beforeGuru) {
            Assertions.assertThat(guruCache.getModel(model.getVendorId(), model.getId())).isEqualTo(model);
        }
        Assertions.assertThat(guruCache.getModelsForVendor(1L)).hasSize(3);
        Assertions.assertThat(skuCache.getModelsForVendor(1L)).hasSize(3);
        Set<ModelStorage.Model> after = guruCache.getModelsStream().collect(Collectors.toSet());
        Assertions.assertThat(after).isEqualTo(beforeGuru);
    }


}
