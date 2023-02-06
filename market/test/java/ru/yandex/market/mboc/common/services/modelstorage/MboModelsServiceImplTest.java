package ru.yandex.market.mboc.common.services.modelstorage;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.http.ModelCardMdmApiService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MboModelsServiceImplTest {

    private MboModelsServiceImpl mboModelsService;

    private ModelStorageService readModelStorageServiceMock;
    private CategoryModelsService categoryModelsServiceMock;

    @Before
    public void setUp() {
        readModelStorageServiceMock = Mockito.mock(ModelStorageService.class);
        categoryModelsServiceMock = Mockito.mock(CategoryModelsService.class);

        mboModelsService = new MboModelsServiceImpl(
            readModelStorageServiceMock,
            Mockito.mock(ModelStorageService.class),
            Mockito.mock(ModelCardMdmApiService.class),
            categoryModelsServiceMock
        );
    }

    @Test
    public void loadModels() {
        ModelStorage.Model model1 = ModelStorage.Model.newBuilder().setId(1L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();
        ModelStorage.Model model2 = ModelStorage.Model.newBuilder().setId(2L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();
        ModelStorage.Model model3 = ModelStorage.Model.newBuilder().setId(3L).setCategoryId(2L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(2L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();
        ModelStorage.Model model4 = ModelStorage.Model.newBuilder().setId(4L).setParentId(4L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();
        ModelStorage.Model model5 = ModelStorage.Model.newBuilder().setId(5L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();
        ModelStorage.Model model6 = ModelStorage.Model.newBuilder().setId(6L).setCategoryId(2L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(2L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .build();

        when(categoryModelsServiceMock.getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(1).addAllModelId(List.of(1L, 2L, 4L, 5L)).build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addAllModels(List.of(model4, model5)).build());

        when(categoryModelsServiceMock.getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(2).addAllModelId(List.of(3L, 6L)).build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().addModels(model6).build());

        when(categoryModelsServiceMock.getModels(MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(1).addAllModelId(List.of(1L, 2L)).build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().addModels(model1).build());

        when(categoryModelsServiceMock.getModels(MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(2).addAllModelId(List.of(3L)).build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().addModels(model3).build());

        Multimap<Long, Long> data = MultimapBuilder.hashKeys().linkedHashSetValues().build();
        data.putAll(1L, List.of(1L, 2L, 4L, 5L));
        data.putAll(2L, List.of(3L, 6L));

        assertEquals(new HashMap<>(Map.of(
                model1.getId(), ModelConverter.convert(model1, Collections.emptySet(), Collections.emptySet()),
                model3.getId(), ModelConverter.convert(model3, Collections.emptySet(), Collections.emptySet()),
                model4.getId(), ModelConverter.convert(model4, Collections.emptySet(), Collections.emptySet()),
                model5.getId(), ModelConverter.convert(model5, Collections.emptySet(), Collections.emptySet()),
                model6.getId(), ModelConverter.convert(model6, Collections.emptySet(), Collections.emptySet()))),
            mboModelsService.loadModels(data));
    }

    @Test
    public void deletedModelIsNotLoadedWhenWithDeletedIsFalse() {
        ModelStorage.Model deletedSku = ModelStorage.Model.newBuilder().setId(100L).setParentId(1L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(true)
            .build();

        when(readModelStorageServiceMock
            .findModels(ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(deletedSku.getId()).build()))
            .thenReturn(ModelStorage.GetModelsResponse.newBuilder().build());

        Map<Long, Model> loadedModels = mboModelsService.loadModels(
            List.of(100L),
            Set.of(),
            Set.of(),
            false
        );

        assertThat(loadedModels).isEmpty();
    }

    @Test
    public void deletedModelIsLoadedWhenWithDeletedIsTrue() {
        ModelStorage.Model deletedSku = ModelStorage.Model.newBuilder().setId(100L).setParentId(1L).setCategoryId(1L)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(false)
            .build();

        when(readModelStorageServiceMock
            .findModels(ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(deletedSku.getId())
                .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALL).build()))
            .thenReturn(ModelStorage.GetModelsResponse.newBuilder()
                .addModels(deletedSku).build());

        when(categoryModelsServiceMock
            .getModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(deletedSku.getId())
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());
        when(categoryModelsServiceMock
            .getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(deletedSku.getId())
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());
        when(categoryModelsServiceMock
            .getDeletedModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(deletedSku.getId())
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addModels(deletedSku).build());

        Map<Long, Model> loadedModels = mboModelsService.loadModels(
            List.of(100L),
            Set.of(),
            Set.of(),
            true
        );

        assertThat(loadedModels).containsOnlyKeys(deletedSku.getId());
    }

    @Test
    public void aliveModelIsChosenWhenWithDeletedIsTrue() {
        long skuId = 100L;
        long deletedCategoryId = 1L;
        long aliveCategoryId = 2L;
        ModelStorage.Model deletedSku = ModelStorage.Model.newBuilder().setId(skuId).setParentId(1L)
            .setCategoryId(deletedCategoryId)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(true)
            .build();
        ModelStorage.Model aliveSku = ModelStorage.Model.newBuilder().setId(skuId).setParentId(1L)
            .setCategoryId(aliveCategoryId)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(2L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(false)
            .build();

        when(readModelStorageServiceMock
            .findModels(ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(skuId)
                .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALL).build()))
            .thenReturn(ModelStorage.GetModelsResponse.newBuilder()
                .addAllModels(List.of(deletedSku, aliveSku)).build());

        when(categoryModelsServiceMock
            .getModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());

        when(categoryModelsServiceMock
            .getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(aliveSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addModels(aliveSku).build());
        when(categoryModelsServiceMock
            .getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());

        when(categoryModelsServiceMock
            .getDeletedModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(deletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addModels(deletedSku).build());

        Map<Long, Model> loadedModels = mboModelsService.loadModels(
            List.of(100L),
            Set.of(),
            Set.of(),
            true
        );

        assertThat(loadedModels).containsOnlyKeys(skuId);
        assertThat(loadedModels.get(skuId)).isNotNull();
        assertThat(loadedModels.get(skuId).getCategoryId()).isEqualTo(aliveSku.getCategoryId());
    }

    @Test
    public void moreRecentDeletedModelIsChosenWhenWithDeletedIsTrue() {
        long skuId = 100L;
        long oldCategoryId = 1L;
        long newCategoryId = 2L;
        long oldTimestamp = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
        long newTimestamp = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
        ModelStorage.Model oldDeletedSku = ModelStorage.Model.newBuilder().setId(skuId).setParentId(1L)
            .setCategoryId(oldCategoryId)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(1L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(true)
            .setCreatedDate(oldTimestamp)
            .build();
        ModelStorage.Model newDeletedSku = ModelStorage.Model.newBuilder().setId(skuId).setParentId(1L)
            .setCategoryId(newCategoryId)
            .addRelations(ModelStorage.Relation.newBuilder().setId(1).setCategoryId(2L)
                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL).build())
            .setDeleted(true)
            .setCreatedDate(newTimestamp)
            .build();

        when(readModelStorageServiceMock
            .findModels(ModelStorage.FindModelsRequest.newBuilder()
                .addModelIds(skuId)
                .setDeleted(ModelStorage.FindModelsRequest.DeletedState.ALL).build()))
            .thenReturn(ModelStorage.GetModelsResponse.newBuilder()
                .addAllModels(List.of(oldDeletedSku, newDeletedSku)).build());

        when(categoryModelsServiceMock
            .getModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(newDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());
        when(categoryModelsServiceMock
            .getModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(oldDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());

        when(categoryModelsServiceMock
            .getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(newDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());
        when(categoryModelsServiceMock
            .getSkus(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(oldDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder().build());

        when(categoryModelsServiceMock
            .getDeletedModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(newDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addModels(newDeletedSku).build());
        when(categoryModelsServiceMock
            .getDeletedModels(MboExport.GetCategoryModelsRequest.newBuilder()
                .setCategoryId(oldDeletedSku.getCategoryId())
                .addModelId(skuId)
                .build()))
            .thenReturn(MboExport.GetCategoryModelsResponse.newBuilder()
                .addModels(oldDeletedSku).build());

        Map<Long, Model> loadedModels = mboModelsService.loadModels(
            List.of(100L),
            Set.of(),
            Set.of(),
            true
        );

        assertThat(loadedModels).containsOnlyKeys(skuId);
        assertThat(loadedModels.get(skuId)).isNotNull();
        assertThat(loadedModels.get(skuId).getCategoryId()).isEqualTo(newDeletedSku.getCategoryId());
    }
}
