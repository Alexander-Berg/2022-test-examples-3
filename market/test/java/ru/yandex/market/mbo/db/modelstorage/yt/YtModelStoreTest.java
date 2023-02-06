package ru.yandex.market.mbo.db.modelstorage.yt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.micrometer.core.instrument.Metrics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.common.YtTimestamp;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.yt.fast.delivery.YtCardQueue;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.market.yt.util.table.model.YtTableModel;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.LookupRowsRequest;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static ru.yandex.market.mbo.db.modelstorage.yt.YtMockingHelper.buildValue;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.BOOLEAN;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.INT64;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.STRING;


/**
 * @author moskovkin@yandex-team.ru
 * @since 25.04.18
 */
@SuppressWarnings({"checkstyle:magicnumber"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class YtModelStoreTest {
    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(04062004)
        .stringLengthRange(3, 10)
        .collectionSizeRange(1, 5)
        .build();

    private YtModelStore modelStore;

    @Mock
    private YtDynamicTableProcessService ytTableModelProcessService;
    @Mock
    private YtTableRpcApi rpcApi;
    @Mock
    private YtTableModel modelTableModel;

    @Before
    public void init() {
        modelStore = new YtModelStore(modelTableModel, null,
            Mockito.mock(YtCardQueue.class), Metrics.globalRegistry);
        modelStore.setTableProcessService(ytTableModelProcessService);
        modelStore.setRpcApi(rpcApi);
        ReflectionTestUtils.setField(modelStore, "maxBatchSize", 5);
    }

    @Test
    public void testPingOk() {
        setTestData(null, null, null, null);

        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        CompletableFuture futureMock = Mockito.mock(CompletableFuture.class);
        UnversionedRowset rowsetMock = Mockito.mock(UnversionedRowset.class);

        String path = "//test/models";
        Mockito.when(modelTableModel.getPath()).thenReturn(path);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(clientMock.selectRows(Mockito.anyString())).thenReturn(futureMock);
        Mockito.when(futureMock.join()).thenReturn(rowsetMock);
        Mockito.when(rowsetMock.getYTreeRows()).thenReturn(Collections.singletonList(null));
        MonitoringResult result = modelStore.monitoring();

        Assert.assertEquals(MonitoringResult.OK, result);

        Mockito.verify(clientMock, Mockito.times(1))
            .selectRows(Mockito.eq(String.format(YtModelStore.MONITORING_QUERY, path)));
    }

    @Test
    public void testPingNoData() {
        setTestData(null, null, null, null);

        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        CompletableFuture futureMock = Mockito.mock(CompletableFuture.class);
        UnversionedRowset rowsetMock = Mockito.mock(UnversionedRowset.class);

        String path = "//test/models";
        Mockito.when(modelTableModel.getPath()).thenReturn(path);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(clientMock.selectRows(Mockito.anyString())).thenReturn(futureMock);
        Mockito.when(futureMock.join()).thenReturn(rowsetMock);
        Mockito.when(rowsetMock.getYTreeRows()).thenReturn(Collections.emptyList());
        MonitoringResult result = modelStore.monitoring();

        Assert.assertNotEquals(MonitoringResult.OK, result);

        Mockito.verify(clientMock, Mockito.times(1))
            .selectRows(Mockito.eq(String.format(YtModelStore.MONITORING_QUERY, path)));
    }

    @Test
    public void testProcessGuruModels() throws ModelStoreInterface.ModelStoreException {
        List<ModelStorage.Model> testModels = setTestData(null, CommonModel.Source.GURU, null, null);
        List<ModelStorage.Model> processedModels = new ArrayList<>();

        modelStore.processCategoryModels(1, CommonModel.Source.GURU, null, null, processedModels::add);

        Assert.assertArrayEquals(testModels.toArray(), processedModels.toArray());
        Mockito.verify(ytTableModelProcessService, Mockito.times(1))
            .process(Mockito.any(), Mockito.eq("category_id = 1 AND type = 'GURU'"), Mockito.any());
    }

    @Test
    public void testProcessCategoryModels() {
        List<ModelStorage.Model> testModels = setTestData(25L, CommonModel.Source.BOOK, true, 10);
        List<ModelStorage.Model> processedModels = new ArrayList<>();

        modelStore.processCategoryModels(25L, CommonModel.Source.BOOK, true, 10, processedModels::add, new ReadStats());

        Assert.assertArrayEquals(testModels.toArray(), processedModels.toArray());
        Mockito.verify(ytTableModelProcessService, Mockito.times(1))
            .process(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(ytTableModelProcessService, Mockito.times(1))
            .process(
                Mockito.any(),
                Mockito.eq("category_id = 25 AND type = 'BOOK' AND deleted = true"),
                Mockito.eq(10)
            );
    }

    @Test
    public void testGetModelsWithInputOrderingByCategory() {
        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        Mockito.when(clientMock.generateTimestamps()).thenReturn(CompletableFuture.completedFuture(YtTimestamp.MAX));

        List<ModelStorage.Model> models = generateTestModels(
            1L,
            CommonModel.Source.GURU,
            false,
            10);
        List<Long> collect = models.stream()
            .map(it -> it.getId())
            .collect(Collectors.toList());
        Collections.shuffle(collect);

        YtMockingHelper.buildModelStoreResponse(clientMock, rpcApi, models, this::convertModel);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(rpcApi.createLookupRowRequest()).thenReturn(Mockito.mock(LookupRowsRequest.class));

        List<ModelStorage.Model> result = modelStore.getModels(1L, collect, new ReadStats());
        Assert.assertFalse(result.isEmpty());
        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(collect.get(i), (Long) result.get(i).getId());
        }
    }

    @Test
    public void testNotFoundModel() {
        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        Mockito.when(clientMock.generateTimestamps()).thenReturn(CompletableFuture.completedFuture(YtTimestamp.MAX));

        YtMockingHelper.buildModelStoreResponse(clientMock, rpcApi, Collections.emptyList(), this::convertModel);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(rpcApi.createLookupRowRequest()).thenReturn(Mockito.mock(LookupRowsRequest.class));

        List<ModelStorage.Model> result = modelStore.getModels(1L, Arrays.asList(1L), new ReadStats());
        Assert.assertTrue(result.isEmpty());
    }


    @Test
    public void testGetModelsWithInputOrdering() {
        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        Mockito.when(clientMock.generateTimestamps()).thenReturn(CompletableFuture.completedFuture(YtTimestamp.MAX));

        List<ModelStorage.Model> models = generateRandomModels(10);
        List<CategoryModelId> collect = models.stream()
            .map(it -> new CategoryModelId(it.getCategoryId(), it.getId()))
            .collect(Collectors.toList());
        Collections.shuffle(collect);

        YtMockingHelper.buildModelStoreResponse(clientMock, rpcApi, models, this::convertModel);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(rpcApi.createLookupRowRequest()).thenReturn(Mockito.mock(LookupRowsRequest.class));

        List<ModelStorage.Model> result = modelStore.getModels(collect, new ReadStats());
        Assert.assertFalse(result.isEmpty());
        for (int i = 0; i < result.size(); i++) {
            Assert.assertEquals(collect.get(i).getModelId(), result.get(i).getId());
        }
    }

    @Test
    public void testResultStatusClearedOnFailedUpdateAttempt() throws ModelStoreInterface.ModelStoreException {
        ApiServiceClient clientMock = Mockito.mock(ApiServiceClient.class);
        Mockito.when(clientMock.generateTimestamps()).thenReturn(CompletableFuture.completedFuture(YtTimestamp.MAX));

        List<ModelStorage.Model> models = generateTestModels(1L, CommonModel.Source.SKU, false, 2);
        ModelStorage.Model existingModel = models.get(0);
        ModelStorage.Model missingModel = models.get(1);

        YtMockingHelper.buildModelStoreResponse(clientMock, rpcApi,
            Collections.singletonList(existingModel), this::convertModel);
        Mockito.when(rpcApi.getClient()).thenReturn(clientMock);
        Mockito.when(rpcApi.createLookupRowRequest()).thenReturn(Mockito.mock(LookupRowsRequest.class));
        Mockito.doAnswer(invocation -> {
            Function<ApiServiceTransaction, Boolean> transactionalBlock = invocation.getArgument(1);
            transactionalBlock.apply(Mockito.mock(ApiServiceTransaction.class));
            return null;
        }).when(rpcApi).doInTransaction(Mockito.any(), Mockito.any());

        ModelStoreSaveGroup saveGroup = new ModelStoreSaveGroup(
            Arrays.asList(existingModel, missingModel),
            Collections.emptyList(),
            Collections.emptyMap(),
            ImmutableMap.of(1L, ImmutableSet.of(existingModel.getId(), missingModel.getId()))
        );

        ModelStoreResult result = modelStore.saveModels(saveGroup, 0L, new OperationStats());
        Assert.assertTrue(result.getModelIdsToAudit().isEmpty());
        Assert.assertTrue(result.getUpdatedModelIds().isEmpty());
        Assert.assertEquals(1, result.getModelStoreStatus().getStatusModelIds().size());
        Assert.assertEquals((long) result.getModelStoreStatus().getStatusModelIds().get(0), missingModel.getId());
        Assert.assertEquals(result.getModelStoreStatus().getStatus(), ModelStorage.OperationStatusType.MODEL_NOT_FOUND);
    }

    private List<ModelStorage.Model> generateRandomModels(int count) {
        List<ModelStorage.Model> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ModelStorage.Model model = ModelStorage.Model.newBuilder()
                .setId(i)
                .setGroupModelId(i)
                .setCategoryId(RANDOM.nextLong())
                .setDeleted(RANDOM.nextBoolean())
                .setCurrentType(RANDOM.nextObject(CommonModel.Source.class).name())
                .build();
            result.add(model);
        }
        return result;
    }

    private List<ModelStorage.Model> generateTestModels(
        @Nullable Long categoryId,
        @Nullable CommonModel.Source type,
        @Nullable Boolean deleted,
        @Nullable Integer maxRows) {

        if (maxRows == null) {
            maxRows = 100;
        }

        List<ModelStorage.Model> result = new ArrayList<>();
        Collection<ModelStorage.Model> randomModels = generateRandomModels(maxRows);

        for (ModelStorage.Model model : randomModels) {
            ModelStorage.Model.Builder modelBuilder = model.toBuilder();

            if (categoryId != null) {
                modelBuilder.setCategoryId(categoryId);
            }

            if (type != null) {
                modelBuilder.setCurrentType(type.name());
            }

            if (deleted != null) {
                modelBuilder.setDeleted(deleted);
            }

            result.add(modelBuilder.build());
        }

        return result;
    }

    private List<YTreeMapNode> convertToRecords(List<ModelStorage.Model> models) {
        List<YTreeMapNode> result = models.stream()
            .map(m -> YtModelUtil.modelToMap(m))
            .map(map -> YTree.builder().value(map).build().mapNode())
            .collect(Collectors.toList());
        return result;
    }

    private void processDataByConsumer(InvocationOnMock invocation, List<YTreeMapNode> data) {
        Consumer<YTreeMapNode> consumer = invocation.getArgument(0);
        if (consumer == null) {
            return;
        }
        data.forEach(consumer);
        return;
    }

    private void mockProcessCalls(List<YTreeMapNode> data) {
        Mockito.doAnswer((Answer<ProcessStats>) invocation -> {
            processDataByConsumer(invocation, data);
            return new ProcessStats();
        })
            .when(ytTableModelProcessService)
            .process(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer((Answer<ProcessStats>) invocation -> {
            processDataByConsumer(invocation, data);
            return new ProcessStats();
        })
            .when(ytTableModelProcessService)
            .process(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doAnswer((Answer<ProcessStats>) invocation -> {
            processDataByConsumer(invocation, data);
            return new ProcessStats();
        })
            .when(ytTableModelProcessService)
            .process(Mockito.any(), Mockito.any());

        Mockito.doAnswer((Answer<ProcessStats>) invocation -> {
            processDataByConsumer(invocation, data);
            return new ProcessStats();
        })
            .when(ytTableModelProcessService)
            .process(Mockito.any());
    }

    private List<ModelStorage.Model> setTestData(
        @Nullable Long categoryId,
        @Nullable CommonModel.Source type,
        @Nullable Boolean deleted,
        @Nullable Integer maxRows) {

        List<ModelStorage.Model> result = generateTestModels(categoryId, type, deleted, maxRows);
        List<YTreeMapNode> records = convertToRecords(result);
        mockProcessCalls(records);

        return result;
    }

    private Map<String, UnversionedValue> convertModel(Integer row, ModelStorage.Model model) {
        Map<String, UnversionedValue> rowValues = new HashMap<>();
        rowValues.put(YtModelColumns.MODEL_ID, buildValue(row, INT64, model.getId()));
        rowValues.put(YtModelColumns.CATEGORY_ID, buildValue(row, INT64, model.getCategoryId()));
        rowValues.put(YtModelColumns.GROUP_MODEL_ID, buildValue(row, INT64, model.getGroupModelId()));
        rowValues.put(YtModelColumns.TYPE, buildValue(row, STRING, model.getCurrentType().getBytes()));
        rowValues.put(YtModelColumns.DELETED, buildValue(row, BOOLEAN, model.getDeleted()));
        rowValues.put(YtModelColumns.DATA, buildValue(row, STRING,
            ModelStorage.Model.newBuilder().setId(model.getId()).build().toByteArray())
        );
        rowValues.put(YtModelColumns.SEARCH_MICROCARD, buildValue(row, ColumnValueType.NULL, null));
        return rowValues;
    }

}
