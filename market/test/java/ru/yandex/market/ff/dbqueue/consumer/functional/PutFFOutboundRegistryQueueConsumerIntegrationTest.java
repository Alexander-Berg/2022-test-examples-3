package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.PutFFOutboundRegistryQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.PutFFOutboundRegistryPayload;
import ru.yandex.market.ff.model.entity.RequestItemError;
import ru.yandex.market.ff.repository.RequestItemErrorRepository;
import ru.yandex.market.ff.service.implementation.utils.RegistryUtils;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FailedFreezeStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundMeta;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SimpleStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.AvailableStockResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryType;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.utils.DateTime;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PutFFOutboundRegistryQueueConsumerIntegrationTest extends IntegrationTestWithDbQueueConsumers {

    public static final String SSKU_123 = "ssku123";
    public static final String SSKU_124 = "ssku124";

    public static final SSItem SS_ITEM_1 = SSItem.of(SSKU_123, 2, 100);
    public static final SSItem SS_ITEM_2 = SSItem.of(SSKU_124, 3, 100);

    public static final SimpleStock SIMPLE_STOCK_1 = new SimpleStock(SSKU_123, 2L, SSKU_123, 100, 100);
    public static final SimpleStock SIMPLE_STOCK_2 = new SimpleStock(SSKU_124, 3L, SSKU_124, 100, 100);
    public static final SimpleStock SIMPLE_STOCK_3 = new SimpleStock(SSKU_124, 3L, SSKU_124, 2, 100);
    public static final SimpleStock SIMPLE_STOCK_4 = new SimpleStock(SSKU_124, 3L, SSKU_124, 0, 100);

    public static final OutboundItem OUTBOUND_ITEM_1 = OutboundItem.of(2, SSKU_123, 5);
    public static final OutboundItem OUTBOUND_ITEM_2 = OutboundItem.of(3, SSKU_124, 10);
    public static final OutboundItem OUTBOUND_ITEM_3 = OutboundItem.of(3, SSKU_124, 2);
    public static final OutboundItem OUTBOUND_ITEM_4 = OutboundItem.of(2, SSKU_123, 15);
    public static final FailedFreezeStock FAILED_FREEZE_STOCK = FailedFreezeStock.of(SSKU_123, 2L, 100, 3, 5);

    @Autowired
    private PutFFOutboundRegistryQueueConsumer putFFOutboundRegistryQueueConsumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RequestItemErrorRepository requestItemErrorRepository;

    /**
     * Сценарий #1.
     * Запрашивается 2 SSItem, которых достаточнно на стоке, фризится количество, которое было в реестре.
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/1/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successfulPutFFOutboundRegistry() {
        prepareMocks(StockFreezingResponse.success(3L), SIMPLE_STOCK_1, SIMPLE_STOCK_2);

        TaskExecutionResult result = executeTask(getItems());

        ArgumentCaptor<List<SSItem>> ssItemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OutboundItem>> outboundItemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StockType> stockTypeCaptor = ArgumentCaptor.forClass(StockType.class);
        ArgumentCaptor<OutboundMeta> outboundMetaCaptor = ArgumentCaptor.forClass(OutboundMeta.class);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyMocks(ssItemsCaptor, outboundItemCaptor, stockTypeCaptor, outboundMetaCaptor,
                OUTBOUND_ITEM_1, OUTBOUND_ITEM_2);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), any());
    }

    /**
     * Сценарий #2.
     * Запрашивается 2 SSItem: ssku123 - 5, ssku124 - 10,
     * на стоке есть: ssku123 - 5, ssku124 - 2,
     * должно зафризиться столько, сколько есть на стоке, в putFFOutboundRegistry должны попасть измененные значения,
     * значения в дочерних заявках должны измениться
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/2/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeAllWithLimit() {
        prepareMocks(StockFreezingResponse.success(1L), SIMPLE_STOCK_1, SIMPLE_STOCK_3);

        TaskExecutionResult result = executeTask(getItems());

        ArgumentCaptor<List<SSItem>> ssItemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OutboundItem>> outboundItemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StockType> stockTypeCaptor = ArgumentCaptor.forClass(StockType.class);
        ArgumentCaptor<OutboundMeta> outboundMetaCaptor = ArgumentCaptor.forClass(OutboundMeta.class);
        ArgumentCaptor<OutboundRegistry> outboundRegistryCaptor = ArgumentCaptor.forClass(OutboundRegistry.class);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyMocks(ssItemsCaptor, outboundItemCaptor, stockTypeCaptor, outboundMetaCaptor,
                OUTBOUND_ITEM_1, OUTBOUND_ITEM_3);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), outboundRegistryCaptor.capture());
        assertRightCountsSentToPartner(outboundRegistryCaptor);
    }

    private void assertRightCountsSentToPartner(ArgumentCaptor<OutboundRegistry> outboundRegistryCaptor) {
        OutboundRegistry sentRegistry = outboundRegistryCaptor.getValue();
        assertThat(sentRegistry.getItems().size(), Matchers.equalTo(2));

        assertThat(getQuantity(sentRegistry, SSKU_123), Matchers.equalTo(5));
        assertThat(getQuantity(sentRegistry, SSKU_124), Matchers.equalTo(2));
    }

    private Integer getQuantity(OutboundRegistry sentRegistry, String sku) {
        return sentRegistry.getItems().stream()
                .map(RegistryItem::getUnitInfo)
                .filter(unitInfo -> RegistryUtils.getPartialIdValue(unitInfo.getCompositeId(),
                        PartialIdType.ARTICLE).equals(sku))
                .map(UnitInfo::getCounts)
                .findFirst().get().stream()
                .findFirst().get()
                .getQuantity();
    }

    /**
     * Сценарий #3.
     * Запрашивается 1 SSItem, SS возвращает положительнное доступное количество на стоке,
     * а затем возвращает ошибку недостатка на стоке при фризе.
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/3/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void ssResponseWithNotEnoughOnStock() {
        prepareMocks(StockFreezingResponse.notEnough(3L, List.of(FAILED_FREEZE_STOCK)), SIMPLE_STOCK_1);

        TaskExecutionResult result = executeTask(getItems());

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verify(ffApiWithdrawService, never()).putOutboundRegistry(any(), any());
    }

    /**
     * Сценарий #5.
     * Запрашивается 2 SSItem: ssku123 - 5, ssku124 - 10,
     * на стоке есть: ssku123 - 5, ssku124 - 2,
     * должно зафризиться столько, сколько есть на стоке, в putFFOutboundRegistry должны попасть измененные значения,
     * значения в дочерних заявках должны измениться, должна сохраниться ошибка (request_item_error)
     * и должны сохранится невывезенные итемы
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/7/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/7/after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    public void ssFreezeWithLimitAndInterwarehouseTransportation() {
        prepareMocks(StockFreezingResponse.success(1L), SIMPLE_STOCK_1, SIMPLE_STOCK_3);

        TaskExecutionResult result = executeTask(getItems());

        ArgumentCaptor<List<SSItem>> ssItemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OutboundItem>> outboundItemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StockType> stockTypeCaptor = ArgumentCaptor.forClass(StockType.class);
        ArgumentCaptor<OutboundMeta> outboundMetaCaptor = ArgumentCaptor.forClass(OutboundMeta.class);
        ArgumentCaptor<OutboundRegistry> outboundRegistryCaptor = ArgumentCaptor.forClass(OutboundRegistry.class);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyMocks(ssItemsCaptor, outboundItemCaptor, stockTypeCaptor, outboundMetaCaptor,
            OUTBOUND_ITEM_1, OUTBOUND_ITEM_3);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), outboundRegistryCaptor.capture());
        assertRightCountsSentToPartner(outboundRegistryCaptor);

        List<RequestItemError> all = requestItemErrorRepository.findAll();
        assertions.assertThat(all.size()).isEqualTo(1);
        assertions.assertThat(all.get(0).getRequestId()).isEqualTo(3);
    }

    /**
     * Сценарий #4.
     * Одинаковый sku разложен в несколько паллет, должно зафризится суммрное количество товара.
     */
    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/4/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/4/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successfulPutFFOutboundRegistryWithDuplicatedItems() {
        prepareMocks(StockFreezingResponse.success(3L), SIMPLE_STOCK_1, SIMPLE_STOCK_2);

        TaskExecutionResult result = executeTask(geDuplicatedItems());

        ArgumentCaptor<List<SSItem>> ssItemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OutboundItem>> outboundItemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StockType> stockTypeCaptor = ArgumentCaptor.forClass(StockType.class);
        ArgumentCaptor<OutboundMeta> outboundMetaCaptor = ArgumentCaptor.forClass(OutboundMeta.class);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyMocks(ssItemsCaptor, outboundItemCaptor, stockTypeCaptor, outboundMetaCaptor,
                OUTBOUND_ITEM_4, OUTBOUND_ITEM_2);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/5/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/5/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successfulPutFFOutboundRegistryWithAlreadyExistingItems() {
        CompositeId compositeId = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku123"),
                new PartialId(PartialIdType.VENDOR_ID, "2")));

        List<RegistryItem> items = List.of(new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(10, UnitCountType.FIT)),
                                compositeId,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ));
        TaskExecutionResult result = executeTask(items);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyZeroInteractions(stockStorageOutboundClient);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), any());
    }

    @Test
    @DatabaseSetup("classpath:db-queue/consumer/put-ff-outbound-registry/6/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/put-ff-outbound-registry/6/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successfulPutFFOutboundRegistryWhenNoItemsForOneOfSuppliersAvailable() {
        prepareMocks(StockFreezingResponse.success(1L), SIMPLE_STOCK_1, SIMPLE_STOCK_4);

        TaskExecutionResult result = executeTask(getItems());

        ArgumentCaptor<List<SSItem>> ssItemsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<OutboundItem>> outboundItemCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<StockType> stockTypeCaptor = ArgumentCaptor.forClass(StockType.class);
        ArgumentCaptor<OutboundMeta> outboundMetaCaptor = ArgumentCaptor.forClass(OutboundMeta.class);
        ArgumentCaptor<OutboundRegistry> outboundRegistryCaptor = ArgumentCaptor.forClass(OutboundRegistry.class);

        assertThat(result, Matchers.equalTo(TaskExecutionResult.finish()));

        verifyMocks(ssItemsCaptor, outboundItemCaptor, stockTypeCaptor, outboundMetaCaptor,
                OUTBOUND_ITEM_1);
        verify(ffApiWithdrawService).putOutboundRegistry(any(), outboundRegistryCaptor.capture());
    }

    private void verifyMocks(ArgumentCaptor<List<SSItem>> ssItemsCaptor,
                             ArgumentCaptor<List<OutboundItem>> outboundItemCaptor,
                             ArgumentCaptor<StockType> stockTypeCaptor,
                             ArgumentCaptor<OutboundMeta> outboundMetaCaptor,
                             OutboundItem... outboundItems) {
        verify(stockStorageOutboundClient).getFreezes("3");
        verify(stockStorageOutboundClient).getAvailable(ssItemsCaptor.capture(), stockTypeCaptor.capture());
        assertThat(ssItemsCaptor.getValue(), Matchers.containsInAnyOrder(SS_ITEM_1, SS_ITEM_2));
        assertThat(stockTypeCaptor.getValue(), Matchers.equalTo(StockType.FIT));

        verify(stockStorageOutboundClient).freezeStocks(outboundMetaCaptor.capture(), outboundItemCaptor.capture());
        assertThat(outboundItemCaptor.getValue(), Matchers.containsInAnyOrder(outboundItems));
        assertThat(outboundMetaCaptor.getValue(), Matchers.equalTo(OutboundMeta.of(3, 100, StockType.FIT,
                DateTimeInterval.fromFormattedValue("2018-01-01T09:00:00+03:00/2018-01-01T09:00:00+03:00"))));
    }

    private void prepareMocks(StockFreezingResponse response, SimpleStock... simpleStocks) {
        when(stockStorageOutboundClient.getFreezes("3")).thenReturn(Collections.emptyList());
        when(stockStorageOutboundClient.getAvailable(any(), any()))
                .thenReturn(AvailableStockResponse.success(StockType.FIT, List.of(simpleStocks)));
        when(stockStorageOutboundClient.freezeStocks(any(), any())).thenReturn(response);
    }

    private TaskExecutionResult executeTask(List<RegistryItem> items) {
        PutFFOutboundRegistryPayload payload = new PutFFOutboundRegistryPayload(1L,
                OutboundRegistry.builder(ResourceId.builder().setYandexId("3").build(),
                        ResourceId.builder().setYandexId("3").build(),
                        RegistryType.PLANNED)
                        .setDate(DateTime.fromLocalDateTime(LocalDateTime.of(2018, 1, 5, 10, 0)))
                        .setComment("Comment")
                        .setBoxes(Collections.emptyList())
                        .setPallets(Collections.emptyList())
                        .setItems(items)
                        .build()
        );
        Task<PutFFOutboundRegistryPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> putFFOutboundRegistryQueueConsumer.execute(task));
    }

    private List<RegistryItem> getItems() {
        CompositeId compositeId = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku123"),
                new PartialId(PartialIdType.VENDOR_ID, "2")));
        CompositeId compositeId2 = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku124"),
                new PartialId(PartialIdType.VENDOR_ID, "3")));

        return List.of(new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(10, UnitCountType.FIT)),
                                compositeId,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ),
                new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(10, UnitCountType.FIT)),
                                compositeId2,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ));
    }

    private List<RegistryItem> geDuplicatedItems() {
        CompositeId compositeId = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku123"),
                new PartialId(PartialIdType.VENDOR_ID, "2")));
        CompositeId compositeId2 = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku123"),
                new PartialId(PartialIdType.VENDOR_ID, "2")));
        CompositeId compositeId3 = getCompositeId(List.of(new PartialId(PartialIdType.ARTICLE, "ssku124"),
                new PartialId(PartialIdType.VENDOR_ID, "3")));

        return List.of(new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(10, UnitCountType.FIT)),
                                compositeId,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ),
                new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(20, UnitCountType.FIT)),
                                compositeId2,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ),
                new RegistryItem(
                        new UnitInfo(
                                List.of(getUnitCount(10, UnitCountType.FIT)),
                                compositeId3,
                                Collections.emptyList(),
                                new Korobyte(20, 10, 30, BigDecimal.valueOf(40L), null, null), "item"),
                        List.of("vendorCode1", "vendorCode2"),
                        null, "name", BigDecimal.valueOf(100L), null, null, null, null, null, true, 10,
                        5, null, "Comment", null, null, null, null, null, null,
                        null, null, null, null, null, null
                ));
    }

    private CompositeId getCompositeId(List<PartialId> partialIds) {
        return new CompositeId(partialIds);
    }

    private UnitCount getUnitCount(int quantity, UnitCountType countType) {
        return new UnitCount.UnitCountBuilder()
                .setCountType(countType)
                .setQuantity(quantity)
                .build();
    }
}
