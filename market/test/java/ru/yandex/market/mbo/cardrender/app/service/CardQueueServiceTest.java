package ru.yandex.market.mbo.cardrender.app.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.impl.YtUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.market.mbo.cardrender.app.YtMockRequestUtils;
import ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy;
import ru.yandex.market.mbo.cardrender.app.model.base.EventType;
import ru.yandex.market.mbo.cardrender.app.model.base.RenderEvent;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.yt.util.table.YtTableRpcApi;
import ru.yandex.yt.ytclient.proxy.ApiServiceClient;
import ru.yandex.yt.ytclient.proxy.ApiServiceTransaction;
import ru.yandex.yt.ytclient.proxy.SelectRowsRequest;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static ru.yandex.market.mbo.cardrender.app.YtMockRequestUtils.buildValue;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.CATEGORY_ID;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.CURRENT_TYPE;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.EVENT_TYPE;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.GROUP_MODEL_ID;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.MODEL_ID;
import static ru.yandex.market.mbo.cardrender.app.config.yt.YtCardQueueTableConfigCopy.TIMESTAMP;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.ANY;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.INT64;

/**
 * @author apluhin
 * @created 5/21/21
 */
public class CardQueueServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CardQueueService cardQueueService;
    private StorageKeyValueService storageKeyValueService;
    private YtTableRpcApi tableRpcApi;
    private ApiServiceClient client;

    @Before
    public void setUp() throws Exception {
        tableRpcApi = Mockito.mock(YtTableRpcApi.class);
        storageKeyValueService = Mockito.mock(StorageKeyValueService.class);
        cardQueueService = new CardQueueService(
                new YtCardQueueTableConfigCopy(),
                storageKeyValueService,
                null
        );
        client = Mockito.mock(ApiServiceClient.class);
        Mockito.when(tableRpcApi.getClient()).thenReturn(client);
        ReflectionTestUtils.setField(cardQueueService, "ytTableRpcApi", tableRpcApi);
    }

    @Test
    public void testSimpleRead() {
        mockRequest(0, 3, Arrays.asList(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, 0),
                new RenderEvent(2, 2, 2L, CommonModel.Source.SKU, 1, EventType.UPDATE, 1),
                new RenderEvent(3, 2, 3L, CommonModel.Source.SKU, 1, EventType.UPDATE, 2)
        ));

        List<RenderEvent> nextEventBatch = cardQueueService.getNextEventBatch();
        Assert.assertEquals(
                nextEventBatch.stream().map(it -> it.getModelId()).collect(Collectors.toList()),
                Arrays.asList(1L, 2L, 3L)
        );
        verifyOffset(3);
    }

    @Test
    public void testSmallerThenBatch() {
        mockRequest(0, 3, Arrays.asList(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, 0),
                new RenderEvent(2, 2, 2L, CommonModel.Source.SKU, 1, EventType.UPDATE, 1)
        ));

        List<RenderEvent> nextEventBatch = cardQueueService.getNextEventBatch();
        Assert.assertEquals(
                nextEventBatch.stream().map(it -> it.getModelId()).collect(Collectors.toList()),
                Arrays.asList(1L, 2L)
        );
        verifyOffset(2);
    }

    @Test
    public void testFindBegin() {
        mockRequest(10, 4, Collections.emptyList());
        mockFindQueueTailRequest(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, 150)
        );

        List<RenderEvent> nextEventBatch = cardQueueService.getNextEventBatch();
        Assert.assertTrue(nextEventBatch.isEmpty());
        verifyOffset(150);
    }

    @Test
    public void testEmptyBatch() {
        mockRequest(0, 0, Arrays.asList(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, 0),
                new RenderEvent(2, 2, 2L, CommonModel.Source.SKU, 1, EventType.UPDATE, 1),
                new RenderEvent(3, 2, 3L, CommonModel.Source.SKU, 1, EventType.UPDATE, 2)
        ));

        List<RenderEvent> nextEventBatch = cardQueueService.getNextEventBatch();
        Assert.assertTrue(nextEventBatch.isEmpty());
        Mockito.verify(storageKeyValueService, Mockito.times(0)).putValue(
                Mockito.eq("card_queue_offset"), Mockito.any());
    }

    @Test
    public void testNotUpdateToOldOffset() {
        mockRequest(1000, 10, Collections.emptyList());
        mockFindQueueTailRequest(
                new RenderEvent(1, 2, 1L, CommonModel.Source.SKU, 1, EventType.UPDATE, 1000)
        );
        List<RenderEvent> nextEventBatch = cardQueueService.getNextEventBatch();
        Assert.assertTrue(nextEventBatch.isEmpty());
        Mockito.verify(storageKeyValueService, Mockito.times(0)).putValue(
                Mockito.eq("card_queue_offset"), Mockito.any());
    }

    private void mockBatchConfig(long offset, long batchSize) {
        Mockito.when(storageKeyValueService.getLong(eq("card_queue_offset"), any())).thenReturn(offset);
        Mockito.when(storageKeyValueService.getLong(eq("card_queue_batch_size"), any())).thenReturn(batchSize);
    }

    private void buildModelStoreResponse(ApiServiceTransaction mockClient,
                                         List<RenderEvent> models) {
        YtMockRequestUtils.ConvertResult<RenderEvent> convertResult =
                new YtMockRequestUtils.ConvertResult<>(models, rowConverter());

        Mockito.when(mockClient.selectRows(Mockito.any(SelectRowsRequest.class))).thenReturn(
                CompletableFuture.completedFuture(convertResult.getRowset()));
        Mockito.when(tableRpcApi.getValue(Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<UnversionedValue>) invocation -> {
                    UnversionedRow argument = invocation.getArgument(0);
                    String fieldName = invocation.getArgument(1);
                    int id = argument.getValues().get(0).getId();
                    return convertResult.getValueMap().get(id).get(fieldName);
                });
    }

    private void mockRequest(long offset, long batchSize, List<RenderEvent> events) {
        mockBatchConfig(offset, batchSize);
        ApiServiceTransaction transactionMock = Mockito.mock(ApiServiceTransaction.class);
        buildModelStoreResponse(transactionMock, events);
        Mockito.doAnswer(invocation -> {
            Function<ApiServiceTransaction, Boolean> transactionalBlock = invocation.getArgument(1);
            transactionalBlock.apply(transactionMock);
            return null;
        }).when(tableRpcApi).doInTransaction(Mockito.any(), Mockito.any());
    }

    private void mockFindQueueTailRequest(RenderEvent tailEvent) {
        YtMockRequestUtils.ConvertResult convertResult = new YtMockRequestUtils.ConvertResult<>(
                Arrays.asList(tailEvent), rowConverter());
        Mockito.when(client.selectRows(Mockito.any(SelectRowsRequest.class))).thenReturn(
                CompletableFuture.completedFuture(convertResult.getRowset())
        );
    }

    private void verifyOffset(long expected) {
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(storageKeyValueService, Mockito.times(1)).putValue(Mockito.eq("card_queue_offset"),
                value.capture());

        Assert.assertEquals(expected, value.getValue());
    }

    private BiFunction<RenderEvent, AtomicInteger, Map<String, UnversionedValue>> rowConverter() {
        return (it, index) -> {
            Map<String, Object> event = new HashMap<>();
            event.put(GROUP_MODEL_ID, it.getGroupModelId());
            event.put(CATEGORY_ID, it.getCategoryId());
            event.put(MODEL_ID, it.getModelId());
            event.put(CURRENT_TYPE, it.getCurrentType().getId());
            event.put(TIMESTAMP, it.getTimestamp());
            event.put(EVENT_TYPE, it.getEventType());
            byte[] treeNode = null;
            try {
                treeNode = YtUtils.json2yson(
                        YTree.builder(),
                        objectMapper.readTree(objectMapper.writeValueAsString(event))).build().toBinary();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Map<String, UnversionedValue> rowValues = new HashMap<>();
            rowValues.put("$table_index", buildValue(index.get(), INT64, 0L));
            rowValues.put("$row_index", buildValue(index.get(), INT64, it.getRowIndex()));
            rowValues.put("event", buildValue(index.get(), ANY, treeNode));
            return rowValues;
        };
    }

}
