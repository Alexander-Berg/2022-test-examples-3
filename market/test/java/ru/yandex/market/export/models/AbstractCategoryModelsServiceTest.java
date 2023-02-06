package ru.yandex.market.export.models;

import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.mbo.db.modelstorage.ModelReadOnlyStorage;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
public class AbstractCategoryModelsServiceTest {

    private static final long CATEGORY_ID = 666L;

    private AbstractCategoryModelsService modelsService;
    private ModelReadOnlyStorage readOnlyStorage;
    private Map<Long, ModelStorage.Model> storage;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        readOnlyStorage = mock(ModelReadOnlyStorage.class);
        modelsService = new AbstractCategoryModelsService(readOnlyStorage, readOnlyStorage) {
        };
        putIntoStorage(1L, ModelStorage.ModelType.SKU.name());
        putIntoStorage(2L, ModelStorage.ModelType.FAST_SKU.name());
        putIntoStorage(3L, ModelStorage.ModelType.PARTNER_SKU.name());
        putIntoStorage(4L, ModelStorage.ModelType.EXPERIMENTAL_SKU.name());
        putIntoStorage(5L, ModelStorage.ModelType.GURU.name());
        putIntoStorage(6L, ModelStorage.ModelType.PARTNER.name());
        putIntoStorage(7L, ModelStorage.ModelType.EXPERIMENTAL.name());
        putIntoStorage(8L, ModelStorage.ModelType.CLUSTER.name());
        putIntoStorage(9L, ModelStorage.ModelType.ANY.name());

        when(readOnlyStorage.getModels(anyLong(), anyCollection()))
            .thenAnswer(invocation -> {
                Set<Long> modelIds = new HashSet<>(invocation.getArgument(1));
                return loadFromStorage(modelIds);
            });
    }

    @Test
    public void testGetModelSkuRequest() {
        testGetModelRequest(1L, ModelStorage.ModelType.SKU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelFastSkuRequest() {
        testGetModelRequest(2L, ModelStorage.ModelType.FAST_SKU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelExpSkuRequest() {
        testGetModelRequest(4L, ModelStorage.ModelType.EXPERIMENTAL_SKU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelParentSkuRequest() {
        testGetModelRequest(3L, ModelStorage.ModelType.PARTNER_SKU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelClusterRequestFail() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Unsupported model type CLUSTER for method");
        testGetModelRequest(8L, ModelStorage.ModelType.CLUSTER,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelGuruRequest() {
        testGetModelRequest(5L, ModelStorage.ModelType.GURU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelPartnerRequest() {
        testGetModelRequest(6L, ModelStorage.ModelType.PARTNER,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelExpRequest() {
        testGetModelRequest(7L, ModelStorage.ModelType.EXPERIMENTAL,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelAnyRequest() {
        testGetModelRequest(9L, ModelStorage.ModelType.ANY,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelEmptyRequest() {
        testGetModelRequest(Collections.emptyList(), ModelStorage.ModelType.ANY,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelAllRequest() {
        List<Long> modelIds = LongStream.range(0, 20).boxed().collect(Collectors.toList());
        testGetModelRequest(modelIds, ModelStorage.ModelType.GURU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetModelUnsupportedRequest() {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Мах model ids list size is 5000");

        List<Long> modelIds = LongStream.range(0, AbstractCategoryModelsService.MAX_MODEL_IDS + 1).boxed()
            .collect(Collectors.toList());
        testGetModelRequest(modelIds, ModelStorage.ModelType.GURU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetSkuGuruRequest() {
        testGetModelRequest(5L, ModelStorage.ModelType.GURU,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetSkuPartnerRequest() {
        testGetModelRequest(6L, ModelStorage.ModelType.PARTNER,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetSkuExpRequest() {
        testGetModelRequest(7L, ModelStorage.ModelType.EXPERIMENTAL,
            request -> modelsService.getModels(request));
    }

    @Test
    public void testGetSkuClusterRequestFail() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Unsupported model type CLUSTER for method");
        testGetModelRequest(Collections.emptyList(), ModelStorage.ModelType.CLUSTER,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuSkuRequest() {
        testGetModelRequest(1L, ModelStorage.ModelType.SKU,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuFastSkuRequest() {
        testGetModelRequest(2L, ModelStorage.ModelType.FAST_SKU,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuPartnerSkuRequest() {
        testGetModelRequest(3L, ModelStorage.ModelType.PARTNER_SKU,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuExpSkuRequest() {
        testGetModelRequest(4L, ModelStorage.ModelType.EXPERIMENTAL_SKU,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuAnyRequest() {
        testGetModelRequest(9L, ModelStorage.ModelType.ANY,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuEmptyRequest() {
        testGetModelRequest(Collections.emptyList(), ModelStorage.ModelType.ANY,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuAllRequest() {
        List<Long> modelIds = LongStream.range(0, 20).boxed().collect(Collectors.toList());
        testGetModelRequest(modelIds, ModelStorage.ModelType.FAST_SKU,
            request -> modelsService.getSkus(request));
    }

    @Test
    public void testGetSkuUnsupportedRequest() {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Мах model ids list size is 5000");

        List<Long> modelIds = LongStream.range(0, AbstractCategoryModelsService.MAX_MODEL_IDS + 1).boxed()
            .collect(Collectors.toList());
        testGetModelRequest(modelIds, ModelStorage.ModelType.PARTNER_SKU,
            request -> modelsService.getSkus(request));
    }

    private void testGetModelRequest(long modelId, ModelStorage.ModelType modelType,
                                     Function<MboExport.GetCategoryModelsRequest,
                                         MboExport.GetCategoryModelsResponse> method) {
        testGetModelRequest(Collections.singletonList(modelId), modelType, method);
    }

    private void testGetModelRequest(Collection<Long> modelIds, ModelStorage.ModelType modelType,
                                     Function<MboExport.GetCategoryModelsRequest,
                                         MboExport.GetCategoryModelsResponse> method) {
        MboExport.GetCategoryModelsRequest request = MboExport.GetCategoryModelsRequest.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .addAllModelId(modelIds)
            .setType(modelType)
            .build();

        MboExport.GetCategoryModelsResponse response = method.apply(request);
        List<ModelStorage.Model> models = response.getModelsList();
        assertThat(models, is(notNullValue()));

        if (CollectionUtils.isEmpty(modelIds)) {
            assertThat(models, empty());
            return;
        }

        List<Long> resultIds = models.stream()
            .map(ModelStorage.Model::getId)
            .collect(Collectors.toList());

        assertThat(resultIds, everyItem(isIn(modelIds)));

        Set<ModelStorage.ModelType> resultTypes = models.stream()
            .map(ModelStorage.Model::getCurrentType)
            .map(ModelStorage.ModelType::valueOf)
            .collect(Collectors.toSet());

        if (ModelStorage.ModelType.ANY.equals(modelType)) {
            assertThat(resultTypes, IsCollectionWithSize.hasSize(greaterThanOrEqualTo(1)));
        } else {
            assertThat(resultTypes, IsCollectionWithSize.hasSize(1));
        }
    }

    private List<ModelStorage.Model> loadFromStorage(Collection<Long> modelIds) {
        if (CollectionUtils.isEmpty(modelIds)) {
            return Collections.emptyList();
        }
        return storage.entrySet().stream()
            .filter(entry -> modelIds.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .collect(Collectors.toList());
    }

    private void putIntoStorage(long id, String type) {
        if (storage == null) {
            storage = new HashMap<>();
        }
        storage.put(id,
            ModelStorage.Model.newBuilder()
                .setId(id)
                .setCurrentType(type)
                .build()
        );
    }
}
