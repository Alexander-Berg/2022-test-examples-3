package ru.yandex.market.aliasmaker.cache.models;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModelServiceTest {

    private ModelStorageService modelStorageService;
    private ModelService modelService;

    @Before
    public void setUp() {
        modelStorageService = mock(ModelStorageService.class);

        modelService = new ModelService();
        modelService.setModelStorageService(modelStorageService);
    }

    @Test
    public void whenFindAliveAndDeletedModelsThenReturnAlive() {
        long modelId = 1L;
        long firstCategoryId = 1L;
        long secondCategoryId = 2L;
        var deletedModel = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(firstCategoryId)
                .setDeleted(true)
                .build();
        var aliveModel = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(secondCategoryId)
                .setDeleted(false)
                .build();
        when(modelStorageService.findModels(any()))
                .thenReturn(ModelStorage.GetModelsResponse.newBuilder()
                        .addModels(deletedModel)
                        .addModels(aliveModel)
                        .build()
                );

        var findModelsRequest = ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(modelId)
                .build();

        var response = modelService.findModels(findModelsRequest);

        assertThat(response.getModelList())
                .containsExactly(aliveModel);
    }

    @Test
    public void whenFindSeveralDeletedModelsThenReturnMostRecent() {
        long modelId = 1L;
        long firstCategoryId = 1L;
        long secondCategoryId = 2L;
        Instant oldCreatedDate = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant newCreatedDate = Instant.now().minus(1, ChronoUnit.DAYS);
        var deletedModelOld = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(firstCategoryId)
                .setDeleted(true)
                .setCreatedDate(oldCreatedDate.toEpochMilli())
                .build();
        var deletedModelNew = ModelStorage.Model.newBuilder()
                .setId(modelId)
                .setCategoryId(secondCategoryId)
                .setDeleted(true)
                .setCreatedDate(newCreatedDate.toEpochMilli())
                .build();
        when(modelStorageService.findModels(any()))
                .thenReturn(ModelStorage.GetModelsResponse.newBuilder()
                        .addModels(deletedModelOld)
                        .addModels(deletedModelNew)
                        .build()
                );

        var findModelsRequest = ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(modelId)
                .build();

        var response = modelService.findModels(findModelsRequest);

        assertThat(response.getModelList())
                .containsExactly(deletedModelNew);
    }
}
