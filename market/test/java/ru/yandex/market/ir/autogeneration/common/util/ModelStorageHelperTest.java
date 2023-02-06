package ru.yandex.market.ir.autogeneration.common.util;

import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

/**
 * @author s-ermakov
 */
public class ModelStorageHelperTest {

    private ModelStorageHelper modelStorageHelper;
    private ModelStorageService modelStorageService;

    @Before
    public void setUp() {
        this.modelStorageService = Mockito.mock(ModelStorageService.class);
        this.modelStorageHelper = new ModelStorageHelper(modelStorageService, modelStorageService);
    }

    @Test
    public void getSkuByModelMapReturnsEmptyListIfGuruDoentContainSku() {
        ModelStorage.Model guruModel = ModelBuilder.newBuilder(10, 100, 1000).build();
        Long2ObjectMap<List<ModelStorage.Model>> result = modelStorageHelper.getSkuByModelMap(Collections.singleton(guruModel));

        Assertions.assertThat(result)
            .hasSize(1)
            .containsKeys(10L);

        Assertions.assertThat(result.get(10)).isEmpty();
    }
}
