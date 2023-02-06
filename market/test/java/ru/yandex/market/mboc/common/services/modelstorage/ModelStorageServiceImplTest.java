package ru.yandex.market.mboc.common.services.modelstorage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anySet;

public class ModelStorageServiceImplTest extends BaseDbTestClass {
    private ModelStorageCachingServiceImpl modelStorageCachingService;

    @Autowired
    private MskuRepository mskuRepository;
    private MboModelsService modelsService;

    @Before
    public void setup() {
        modelsService = Mockito.mock(MboModelsService.class);
        Mockito.when(modelsService.loadModels(anyIterable(), anySet(), anySet(), anyBoolean()))
            .then(call -> call.<Collection<Long>>getArgument(0).stream()
                .collect(Collectors.toMap(id -> id, id -> new Model().setId(id).setPublishedOnBlueMarket(true))));

        modelStorageCachingService = new ModelStorageCachingServiceImpl(modelsService, mskuRepository);

        mskuRepository.save(new Msku()
            .setMarketSkuId(123L)
            .setSkuType(SkuTypeEnum.SKU)
            .setParentModelId(1L)
            .setCategoryId(42L)
            .setVendorId(24L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now())
            .setTitle("Test title"));
    }

    @Test
    public void shouldLoadModelsFromDb() {
        Map<Long, Model> models = modelStorageCachingService.getModelsFromPgThenMbo(List.of(123L));
        assertThat(models).hasSize(1);
        assertThat(models.get(123L)).isNotNull();

        Mockito.verifyZeroInteractions(modelsService);
    }

    @Test
    public void shouldRequestFromServerIfNotFound() {
        Map<Long, Model> models = modelStorageCachingService.getModelsFromPgThenMbo(List.of(321L));
        assertThat(models).hasSize(1);
        assertThat(models.get(321L)).isNotNull();

        Mockito.verify(modelsService).loadModels(List.of(321L), Set.of(), Set.of(), false);

    }

    @Test
    public void shouldReloadFromServerIfNotMatching() {
        Map<Long, Model> models = modelStorageCachingService
            .getModelsFromPgThenMbo(List.of(123L), m -> !m.isPublishedOnBlueMarket(), false);
        assertThat(models).hasSize(1);
        assertThat(models.get(123L)).isNotNull();
        assertThat(models.get(123L).isPublishedOnBlueMarket()).isTrue();

        Mockito.verify(modelsService).loadModels(List.of(123L), Set.of(), Set.of(), false);

    }
}
