package ru.yandex.market.mbo.cardrender.app.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.cardrender.app.YtMockRequestUtils;
import ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig;
import ru.yandex.market.mbo.cardrender.app.model.base.DeleteModelRequest;
import ru.yandex.market.mbo.cardrender.app.model.base.RenderedModel;
import ru.yandex.market.mbo.export.ExportReportModels;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.yt.utils.UnstableInit;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.LookupRowsRequest;
import ru.yandex.yt.ytclient.proxy.ModifyRowsRequest;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.cardrender.app.YtMockRequestUtils.buildValue;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.CATEGORY_ID;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.MODEL_ID;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.BLUE_PUBLISHED;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.DATA;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.EXPORT_TS;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtModelRenderTableConfig.PUBLISHED;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.BOOLEAN;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.STRING;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.UINT64;

/**
 * @author apluhin
 * @created 6/9/21
 */
public class YtModelRenderRepositoryTest {

    private YtModelRenderRepository modelRenderRepository;

    //mock only 1 cluster
    private YtTableRpcApi hahnSkuRpcApi;
    private ApiServiceClient hahnClient;
    private YtModelRenderTableConfig tableConfig;


    @Before
    public void setUp() throws Exception {
        this.tableConfig = new YtModelRenderTableConfig();
        modelRenderRepository = new YtModelRenderRepository(
                mock(UnstableInit.class),
                mock(UnstableInit.class),
                tableConfig
        );
        hahnSkuRpcApi = mock(YtTableRpcApi.class);
        YtTableRpcApi hahnModelRpcApi = mock(YtTableRpcApi.class);
        hahnClient = mock(ApiServiceClient.class);
        when(hahnSkuRpcApi.getClient()).thenReturn(hahnClient);
        ReflectionTestUtils.setField(modelRenderRepository, "hahnSkuRpcApi", new AtomicReference<>(hahnSkuRpcApi));
        ReflectionTestUtils.setField(modelRenderRepository, "hahnModelRpcApi", new AtomicReference<>(hahnModelRpcApi));
        ReflectionTestUtils.setField(modelRenderRepository, "isInitialized", true);
    }

    @Test
    public void testSimpleModelMerge() {
        long tableTs = 90;
        long candidateTs = 100;
        ModelStorage.Model exportModel = ModelStorage.Model.newBuilder().setId(1).setExportTs(tableTs).build();
        ModifyRowsRequest transactionMock = defaultMockRequest(Collections.singletonList(exportModel));
        modelRenderRepository.loadModelToStore(Collections.singletonList(makeRenderModel(1, candidateTs)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionMock, times(1)).addUpdate(captor.capture());

        Assert.assertEquals(Long.valueOf(candidateTs), captor.getValue().get(EXPORT_TS));
    }

    @Test
    public void testSkipModeWithOldExportsTs() {
        long tableTs = 100;
        long candidateTs = 90;
        ModelStorage.Model exportModel = ModelStorage.Model.newBuilder().setId(1).setExportTs(tableTs).build();
        ModifyRowsRequest transactionMock = defaultMockRequest(Collections.singletonList(exportModel));
        modelRenderRepository.loadModelToStore(Collections.singletonList(makeRenderModel(1, candidateTs)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionMock, times(0)).addUpdate(anyMap());
    }

    @Test
    public void testSkipCluster() {
        long tableTs = 100;
        long candidateTs = 90;
        ModelStorage.Model exportModel = ModelStorage.Model.newBuilder()
                .setId(1).setExportTs(tableTs).setCurrentType(CommonModel.Source.CLUSTER.name()).build();
        ModifyRowsRequest transactionMock = defaultMockRequest(Collections.singletonList(exportModel));
        modelRenderRepository.loadModelToStore(Collections.singletonList(makeRenderModel(1, candidateTs)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionMock, times(0)).addUpdate(anyMap());
    }

    @Test
    public void testModelDeleteMarkUpdate() {
        long tableTs = 90;
        long candidateTs = 100;
        ModelStorage.Model exportModel = ModelStorage.Model.newBuilder().setId(1).setCategoryId(2L).setExportTs(tableTs).build();
        ModelStorage.Model exportModelWithOldCategory = ModelStorage.Model.newBuilder().setId(2).setCategoryId(3L).setExportTs(tableTs).build();
        ModifyRowsRequest transactionMock = mockRequest(Arrays.asList(exportModel, exportModelWithOldCategory), keyRowConverter());
        modelRenderRepository.deleteFullModels(Arrays.asList(
                new DeleteModelRequest(1L, 2L, candidateTs),
                new DeleteModelRequest(2L, 4L, candidateTs)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionMock, times(1)).addDelete(captor.capture());

        long modelId = (long) captor.getValue().get(MODEL_ID);
        Assert.assertEquals(modelId, exportModel.getId());
    }


    @Test
    public void testInitMergeModel() {
        long candidateTs = 100;
        ModifyRowsRequest transactionMock = defaultMockRequest(Collections.emptyList());
        modelRenderRepository.loadModelToStore(Collections.singletonList(makeRenderModel(1, candidateTs)));

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(transactionMock, times(1)).addUpdate(captor.capture());

        Assert.assertEquals(Long.valueOf(candidateTs), captor.getValue().get(EXPORT_TS));
    }

    @Test
    public void testMergeLargeModel() {
        long candidateTs = 100;
        ModifyRowsRequest transactionMock = defaultMockRequest(Collections.emptyList());
        RenderedModel o = makeRenderModel(1, candidateTs);
        o.setData(new byte[17 * 1024 * 1024]);
        modelRenderRepository.loadModelToStore(Collections.singletonList(o));

        verify(transactionMock, times(0)).addUpdate(Mockito.anyMap());
    }

    private RenderedModel makeRenderModel(long modelId, long exportTs) {
        ExportReportModels.ExportReportModel model = ExportReportModels.ExportReportModel.newBuilder()
                .setId(modelId)
                .setExportTs(exportTs)
                .setCurrentType(CommonModel.Source.SKU.name())
                .build();
        return new RenderedModel(model);
    }

    private ModifyRowsRequest defaultMockRequest(List<ModelStorage.Model> events) {
        return mockRequest(events, rowConverter());
    }


    private ModifyRowsRequest mockRequest(List<ModelStorage.Model> events,
                                          BiFunction<ModelStorage.Model, AtomicInteger, Map<String, UnversionedValue>> converter) {
        ApiServiceTransaction transactionMock = mock(ApiServiceTransaction.class);
        when(transactionMock.modifyRows(any())).thenReturn(CompletableFuture.completedFuture(null));
        ModifyRowsRequest rowsRequest = buildModelStoreResponse(transactionMock, events, converter);
        doAnswer(invocation -> {
            Function<ApiServiceTransaction, Boolean> transactionalBlock = invocation.getArgument(1);
            transactionalBlock.apply(transactionMock);
            return null;
        }).when(hahnSkuRpcApi).doInTransaction(any(), any());
        return rowsRequest;
    }

    private ModifyRowsRequest buildModelStoreResponse(ApiServiceTransaction mockClient,
                                                      List<ModelStorage.Model> models,
                                                      BiFunction<ModelStorage.Model, AtomicInteger, Map<String, UnversionedValue>> converter) {
        YtMockRequestUtils.ConvertResult<ModelStorage.Model> convertResult =
                new YtMockRequestUtils.ConvertResult<>(models, converter);

        when(mockClient.selectRows(any(String.class))).thenReturn(
                CompletableFuture.completedFuture(convertResult.getRowset()));
        when(hahnSkuRpcApi.createLookupRowRequest()).thenReturn(Mockito.mock(LookupRowsRequest.class));
        when(hahnClient.lookupRows(any())).thenReturn(
                CompletableFuture.completedFuture(convertResult.getRowset()));
        ModifyRowsRequest mock = mock(ModifyRowsRequest.class);
        when(hahnSkuRpcApi.createModifyRowRequest()).thenReturn(mock);
        when(hahnSkuRpcApi.getValue(any(), any()))
                .thenAnswer((Answer<UnversionedValue>) invocation -> {
                    UnversionedRow argument = invocation.getArgument(0);
                    String fieldName = invocation.getArgument(1);
                    int id = argument.getValues().get(0).getId();
                    return convertResult.getValueMap().get(id).get(fieldName);
                });
        return mock;
    }


    private BiFunction<ModelStorage.Model, AtomicInteger, Map<String, UnversionedValue>> rowConverter() {
        return (it, index) -> {
            Map<String, UnversionedValue> rowValues = new HashMap<>();
            rowValues.put(MODEL_ID, buildValue(0, UINT64, it.getId()));
            rowValues.put(EXPORT_TS, buildValue(1, UINT64, it.getExportTs()));
            rowValues.put(CATEGORY_ID, buildValue(2, UINT64, it.getCategoryId()));
            rowValues.put(PUBLISHED, buildValue(9, BOOLEAN, it.getPublished()));
            rowValues.put(BLUE_PUBLISHED, buildValue(10, BOOLEAN, it.getBluePublished()));
            rowValues.put(DATA, buildValue(13, STRING, it.toByteArray()));
            return rowValues;
        };
    }

    private BiFunction<ModelStorage.Model, AtomicInteger, Map<String, UnversionedValue>> keyRowConverter() {
        return (it, index) -> {
            Map<String, UnversionedValue> rowValues = new HashMap<>();
            rowValues.put(MODEL_ID, buildValue(0, UINT64, it.getId()));
            rowValues.put(CATEGORY_ID, buildValue(1, UINT64, it.getCategoryId()));
            return rowValues;
        };
    }
}
