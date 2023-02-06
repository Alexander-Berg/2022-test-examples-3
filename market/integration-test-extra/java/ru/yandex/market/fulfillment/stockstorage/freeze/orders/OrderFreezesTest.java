package ru.yandex.market.fulfillment.stockstorage.freeze.orders;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.domain.converter.SSEntitiesConverter;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezeData;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezingMeta;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.Stock;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockFreeze;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnfreezeJob;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.repository.StockFreezeRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.StockRepository;
import ru.yandex.market.fulfillment.stockstorage.repository.UnfreezeJobRepository;
import ru.yandex.market.fulfillment.stockstorage.service.StocksAvailabilityCheckingService;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.freezing.UnfreezeJobExecutor;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.backorder.BackorderService;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.group.StocksWarehouseGroupCache;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.ORDER;
import static ru.yandex.market.fulfillment.stockstorage.util.ModelUtil.resourceId;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class OrderFreezesTest extends AbstractContextualTest {

    @Autowired
    private UnfreezeJobExecutor unfreezeJobExecutor;

    @Spy
    @Autowired
    private StocksAvailabilityCheckingService availabilityCheckingService;

    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private UnfreezeJobRepository unfreezeJobRepository;
    @Autowired
    private StockFreezeRepository stockFreezeRepository;
    @Autowired
    private FulfillmentClient lgwClient;
    @Autowired
    protected BackorderService backorderService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private StocksWarehouseGroupCache stocksWarehouseGroupCache;

    private boolean shouldDoInsideCall = true;

    @BeforeEach
    void loadCache() {
        stocksWarehouseGroupCache.reload();
    }

    @Test
    public void freezeStocksFailedDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc
                .perform(post(ORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("orderId", "items", "freezeVersion");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    public void freezeStocksFailedDueToRequestWithEmptyStocks() throws Exception {
        String contentAsString = mockMvc
                .perform(post(ORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\":123, \"items\":[], \"freezeVersion\": 0}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("items")
                .doesNotContain("orderId")
                .doesNotContain("freezeVersion");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_unfreeze_scheduled_nothing_is_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToRequestWithEmptyItemAmountData() throws Exception {
        String contentAsString = mockMvc
                .perform(post(ORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\":12345, \"items\":[{}]}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("item", "amount");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    public void freezeStocksFailedDueToRequestWithDuplicatedStocks() throws Exception {
        String contentAsString = mockMvc
                .perform(post(ORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\":12345, " +
                                "\"items\":[" +
                                "    {\"item\":{\"shopSku\":\"sku0\", \"vendorId\": 12, \"warehouseId\":1}," +
                                "\"amount\":123,\"backorder\":true}," +
                                "    {\"item\":{\"shopSku\":\"sku0\", \"vendorId\": 12, \"warehouseId\":1}," +
                                "\"amount\":124,\"backorder\":false}" +
                                "], " +
                                "\"freezeVersion\": 0}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate items found for id", "sku0", "12");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_unfreeze_scheduled_nothing_is_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToRequestWithDuplicatedFreeze() throws Exception {
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(123, false)),
                status().is(StockStorageErrorStatusCode.DUPLICATE_FREEZE.getCode()
                ))
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate freeze: 12345 type: ORDER")
                .contains("409_BAD_REQUEST");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedDueToNoStocksFound() throws Exception {
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(123, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksBackorderFailedDueBackorderNotAllowed() throws Exception {
        backorderService.setBackorderAllowed(1, false);
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(123, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Backorder not allowed to warehouse");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз для отстутствующий sku не ислолнется при отключенной
     * {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE}.
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/system_property_with_disabled_creating_sku_on_freeze.xml")
    public void freezeStocksBackorderFailedDueToNoSkuFoundWithDisabledProperty() throws Exception {
        backorderService.setBackorderAllowed(1, true);
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(123, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    /**
     * Проверка:
     * Запрос на фриз для отстутствующий sku с backorder=false не ислолнется даже при включенной
     * {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE}.
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    public void freezeStocksFailedDueToNoSkuFoundWithEnabledProperty() throws Exception {
        backorderService.setBackorderAllowed(1, true);
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(123, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    /**
     * Проверка:
     * Запрос на фриз с запросом на отстутствующий sku ислолнится, при включенной
     * {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE}.
     * <p>
     * Результат:
     * Код 200 - Будет создана ску с фризом на заданном стоке
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_with_backorder_created_on_freeze.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksBackorderOnNoExistSku() throws Exception {
        backorderService.setBackorderAllowed(1, true);
        sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(100, true)),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService, times(2)).logSkuCreated(any(), any());
        verify(skuEventAuditService).logStockFreeze(any());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(4)).handle(anyList());
    }

    /**
     * Проверяем, что не будет исключений по уникальному ключу при попытке конкурентного создания ску во время фриза.
     * <p>
     * ПОСЛЕ заверщения вызова {@link StocksAvailabilityCheckingService#checkFreezeStocksAvailable}
     * с получением 2-х ску для создания, конкурентно вызываем еще один фриз тех же ску.
     * <p>
     * Как результат: в первом вызове записи в sku/stock/event_audit создадутся, а во втором нет, хотя для обоих вызовов
     * был checkingService вернул 2 ску.
     * <p>
     * Результат:
     * Код 200 - Будет созданы 2 ску + 4 фриза на 2 заказа
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/freeze/stocks_with_backorder_created_on_concurrent_freeze.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void concurrentFreezeStocksBackorderOnNoExistSku() throws Exception {
        backorderService.setBackorderAllowed(1, true);

        doOnceAfterCheckFreezeStockAvailable(() -> {
            sendFreezeRequest(
                    "256",
                    ImmutableMap.of("sku0", FreezeData.of(40, true), "sku1", FreezeData.of(10, true)),
                    status().is2xxSuccessful());
        });

        sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(5, true), "sku1", FreezeData.of(7, true)),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();


        verify(skuEventAuditService, times(2)).logSkuCreated(any(), anyLong());
        verify(skuEventAuditService, times(2)).logStockFreeze(any());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("256", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(6)).handle(anyList());
    }

    /**
     * Проверка:
     * Запрос на фриз по ску без стока будет фейлится, даже с backorder=true и включенной
     * {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE}.
     * <p>
     * Запрос:
     * - фриз на unknown_sku потенциально праваильный
     * - фриз на sku2, которая есть в БД, но для нее нет фитового стока - фейлим
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_with_sku_without_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/stocks_with_sku_without_fit.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void freezeStocksBackorderFailedDueToNoStock() throws Exception {
        backorderService.setBackorderAllowed(1, true);
        String contentAsString = sendFreezeRequest(
                "12345",
                ImmutableMap.of("unknown_sku", FreezeData.of(123, true), "sku2", FreezeData.of(123, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku2:1}", "{12:unknown_sku:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_are_frozen_by_one_order.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void freezeStocksSuccessful() throws Exception {
        sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(50, false)),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    /**
     * Проверяет фриз общих остатков. Фриз сохраняется на склад 1, а не на склад 999, который передаем в запросе.
     */
    @Test
    @DatabaseSetup("classpath:database/states/shared_stocks.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/shared_stocks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeSharedStocksSuccessful() throws Exception {
        sendRefreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(100, false)),
                0L,
                999,
                13L,
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_with_backorder_are_frozen_by_one_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksBackorderSuccessful() throws Exception {
        backorderService.setBackorderAllowed(1, true);
        sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(100, true)),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_korobytes_pushed.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze" +
            "/stocks_with_backorder_overrun_are_frozen_by_one_order.xml", assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksBackorderOverrunSuccessful() throws Exception {
        sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(100001, true), "sku1", FreezeData.of(100, true)),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/no_fit_stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/states/no_fit_stocks_korobytes_pushed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void notEnoughOnNoFitStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(50, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/no_fit_stocks_korobytes_pushed.xml"})
    @ExpectedDatabase(value = "classpath:database/states/no_fit_stocks_korobytes_pushed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void notEnoughOnNoFitStocksBackorder() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(50, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    public void freezeDisabledStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(50, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}")
                .doesNotContain("{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_pushed_disable_one_sku.xml"})
    public void freezeDisabledStocksBackorder() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(50, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}")
                .doesNotContain("{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    public void freezeDisabledStocksBackorderNotAllowed() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(50, true)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Backorder not allowed to warehouse")
                .doesNotContain("{12:sku1:1}")
                .doesNotContain("{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/korobytes/stocks_korobytes_pushed_nothing_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(100001, false), "sku1", FreezeData.of(50, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku0", "required 100001", "found 100000")
                .doesNotContain("sku1");

        contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(100, false), "sku1", FreezeData.of(100051, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku1", "required 100051", "found 100000")
                .doesNotContain("sku0");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/sku0_stocks_are_frozen.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void freezeAllStocksByFiveRequestsSuccessful() throws Exception {
        List<String> orderIds = Arrays.asList("1231", "1232", "1233", "1234", "1235");

        orderIds.forEach(orderId ->
                sendFreezeRequest(
                        orderId,
                        ImmutableMap.of("sku0", FreezeData.of(20000, false)),
                        status().is2xxSuccessful()));

        //and try to freeze when all stocks are already frozen
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(50, false)),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku0", "required 50", "found 0")
                .doesNotContain("sku1");

        verify(skuEventAuditService, times(5)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(5)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());

        orderIds.forEach(orderId ->
                verify(freezeEventAuditService)
                        .logFreezeSuccessful(
                                eq(FreezingMeta.of(FreezeReason.of(orderId, FreezeReasonType.ORDER), StockType.FIT,
                                        null, 0)),
                                anyMap()
                        ));
        verify(stockEventsHandler, times(10)).handle(anyList());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_korobytes_pushed.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze" +
            "/stocks_with_backorder_overrun_are_frozen_by_many_orders.xml", assertionMode = NON_STRICT_UNORDERED)
    public void freezeAllStocksBackorderBySixRequestsSuccessful() throws Exception {
        List<String> orderIds = Arrays.asList("1231", "1232", "1233", "1234", "1235", "123");

        orderIds.forEach(orderId ->
                sendFreezeRequest(
                        orderId,
                        ImmutableMap.of("sku0", FreezeData.of(20000, true)),
                        status().is2xxSuccessful()));

        verify(skuEventAuditService, times(6)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(6)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());

        orderIds.forEach(orderId ->
                verify(freezeEventAuditService)
                        .logFreezeSuccessful(
                                eq(FreezingMeta.of(FreezeReason.of(orderId, FreezeReasonType.ORDER), StockType.FIT,
                                        null, 0)),
                                anyMap()
                        ));
        verify(stockEventsHandler, times(12)).handle(anyList());
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_orders_unfreeze_scheduled.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreeze_maked_despite_unfreeze_initialized.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeSuccessfulWhenUnfreezeScheduled() {
        sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(10, true)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("12345", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    public void refreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(50, false)),
                1L,
                status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);

        // with backorder
        contentAsString = sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(50, true)),
                1L,
                status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/outdated_refreeze_skipped.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void skipRefreezeIfOutdated() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());

        // with backorder
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, true)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/outdated_refreeze_skipped.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeBackorderFailedDueBackorderNotAllowed() throws Exception {
        backorderService.setBackorderAllowed(1, false);
        String contentAsString = sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku1", FreezeData.of(80, true)),
                1,
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Backorder not allowed to warehouse");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_by_second_order.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeStocksSuccessful() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_twice_by_second_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksTwice() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku0", FreezeData.of(90, false), "sku1", FreezeData.of(100000, false)),
                2,
                status().is2xxSuccessful());

        verify(skuEventAuditService, times(2)).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 2)),
                        anyMap()
                );

        verify(stockEventsHandler, times(5)).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_by_second_order.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeBackorderStocksSuccessful() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, true)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_backorder_overrun_by_second_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksBackorderOverrunSuccessful() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(100001, true)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders_backorder.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_already_backorder_by_second_order.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksAlreadyBackorder() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(200000, true)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders_backorder.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze" +
            "/refreezed_already_backorder_by_second_order_backwards.xml", assertionMode = NON_STRICT_UNORDERED)
    public void refreezeStocksAlreadyBackorderBackwards() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/failed_to_refreeze_first_order.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void refreezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(50, false), "sku1", FreezeData.of(10, false)),
                1L,
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku1", "required 10", "found 0")
                .doesNotContain("sku0");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/after_multiple_refreeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeAllStocksByMultipleRequestsSuccessful() {
        sendRefreezeRequest(
                "123456",
                ImmutableMap.of("sku1", FreezeData.of(80, false)),
                1,
                status().is2xxSuccessful());
        sendRefreezeRequest(
                "12345",
                ImmutableMap.of("sku0", FreezeData.of(50, true), "sku1", FreezeData.of(10, false)),
                1,
                status().is2xxSuccessful());

        verify(skuEventAuditService, times(2)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(2)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("12345", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_orders_unfreeze_scheduled.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void skipUnfreezeIfUnfreezeAlreadyCaught() throws Exception {
        mockMvc.perform(delete(ORDER + "/12345"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    @Test
    public void unfreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = mockMvc.perform(delete(ORDER + "/12345"))
                .andExpect(status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(FreezeNotFoundException.MESSAGE);
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/unfreeze_jobs_created.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void unfreezeScheduled() throws Exception {
        mockMvc.perform(delete(ORDER + "/12345"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(delete(ORDER + "/123456"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(2)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_order_refreezed_withdot.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/unfreeze_jobs_created_with_dot.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void unfreezeScheduledWithDOtInOrderId() throws Exception {
        mockMvc.perform(delete(ORDER + "/12345"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(delete(ORDER + "/12345.6"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(freezeEventAuditService, times(2)).logUnfreezeScheduled(anyList());
        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    public void unfreezeJobExecuted() {
        UnitId unitId = new UnitId("sku0", 12L, 1);
        when(lgwClient.getStocks(Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId)), new Partner(1L)))
                .thenReturn(getSku0Stock(OffsetDateTime.now()));
        unfreezeJobExecutor.executeNextJob();

        verify(freezeEventAuditService).logUnfreezeSuccessful(any(UnfreezeJob.class));

        Stock stock = stockRepository.findByUnitIdAndType(unitId, StockType.FIT);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(99990);

        List<StockFreeze> freezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                FreezeReason.of("12345", FreezeReasonType.ORDER));
        softly
                .assertThat(freezes)
                .hasSize(0);

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);
        softly
                .assertThat(unfreezeJob.getStockFreeze().isDeleted())
                .isTrue();
        softly
                .assertThat(unfreezeJob.getExecuted())
                .isNotNull();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_delayed.xml")
    @Transactional
    public void stockWasNotUpdatedAndUnfreezeJobDelayed() {
        UnitId unitId = new UnitId("sku0", 12L, 145145);
        when(lgwClient.getStocks(Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId)),
                new Partner(145145L)))
                .thenReturn(getSku0Stock(OffsetDateTime.of(LocalDateTime.of(2018, 3, 9, 0, 0), ZoneOffset.UTC)));
        unfreezeJobExecutor.executeNextJob();

        verify(freezeEventAuditService, never()).logUnfreezeSuccessful(any(UnfreezeJob.class));

        Stock stock = stockRepository.findByUnitIdAndType(unitId, StockType.FIT);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(100000);

        List<StockFreeze> freezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                FreezeReason.of("12345", FreezeReasonType.ORDER));

        softly
                .assertThat(freezes)
                .hasSize(0);

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);

        softly
                .assertThat(unfreezeJob.getStockFreeze().isDeleted())
                .isFalse();
        softly
                .assertThat(unfreezeJob.getExecuted())
                .isNull();

        Duration duration = Duration.between(LocalDateTime.now(), unfreezeJob.getExecuteAfter());
        softly
                .assertThat(duration.toMinutes() > 0)
                .isTrue();
    }

    @Test
    @DatabaseSetup("classpath:database/states/stock_orders_unfreeze_not_delayed_because_of_zero_stock.xml")
    @Transactional
    public void stockWasUpdatedAndUnfreezeJobNotDelayedBecauseOfZeroStock() {
        UnitId unitId = new UnitId("sku0", 12L, 145145);
        when(lgwClient.getStocks(Collections.singletonList(SSEntitiesConverter.toLgwUnitId(unitId)),
                new Partner(145145L)))
                .thenReturn(getSku0Stock(OffsetDateTime.of(LocalDateTime.of(2018, 3, 9, 0, 0), ZoneOffset.UTC)));
        unfreezeJobExecutor.executeNextJob();

        verify(freezeEventAuditService).logUnfreezeSuccessful(any(UnfreezeJob.class));

        Stock stock = stockRepository.findByUnitIdAndType(unitId, StockType.FIT);
        softly
                .assertThat(stock.getAmount())
                .isEqualTo(0);
        softly
                .assertThat(stock.getFreezeAmount())
                .isEqualTo(99990);

        List<StockFreeze> freezes = stockFreezeRepository.findAllByReasonWithoutUnfreezeJobs(
                FreezeReason.of("12345", FreezeReasonType.ORDER));
        softly
                .assertThat(freezes)
                .hasSize(0);

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);
        softly
                .assertThat(unfreezeJob.getStockFreeze().isDeleted())
                .isTrue();
        softly
                .assertThat(unfreezeJob.getExecuted())
                .isNotNull();
    }

    /**
     * Проверяем, что в рамках заказа можно сделать freeze на несколько складов одновременно.
     */
    @Test
    @DatabaseSetup("classpath:database/states/freeze_multiple_wh_preset.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/freeze_multiple_wh_completed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeWithMultipleWarehouses() throws Exception {
        mockMvc.perform(post(ORDER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/orders_freeze_multiple_wh.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    /**
     * Проверяем, что в рамках заказа можно сделать refreeze на несколько складов одновременно.
     */
    @Test
    @DatabaseSetup("classpath:database/states/refreeze_multiple_wh_preset.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreeze_multiple_wh_completed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeWithMultipleWarehouses() throws Exception {
        backorderService.setBackorderAllowed(20, true);
        mockMvc.perform(post(ORDER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/orders_refreeze_multiple_wh.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    public void freezeStocksSuccessfulDespiteNoPartnersLinked() {
        when(lgwClient.getStocks(anyList(), any(Partner.class)))
                .thenThrow(new HttpTemplateException(400, "code 9404: No partners linked to warehouse: 55219"));
        mockSearchPartners(Collections.singletonList(PartnerResponse.newBuilder()
                .id(1)
                .partnerType(PartnerType.FULFILLMENT)
                .name("Warehouse Name")
                .status(PartnerStatus.INACTIVE)
                .stockSyncEnabled(true)
                .build()));

        unfreezeJobExecutor.executeNextJob();

        UnfreezeJob unfreezeJob = unfreezeJobRepository.findById(10011L).orElse(null);
        softly
                .assertThat(unfreezeJob.getStockFreeze().isDeleted())
                .isTrue();
        softly
                .assertThat(unfreezeJob.getExecuted())
                .isNotNull();
    }

    private List<ItemStocks> getSku0Stock(OffsetDateTime dateTime) {
        DateTime updated = DateTime.fromOffsetDateTime(dateTime);
        return Collections.singletonList(
                new ItemStocks(
                        new ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId(null, 12L, "sku0"),
                        resourceId("1", "1"),
                        ImmutableList.of(
                                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock(
                                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.FIT,
                                        99990, updated
                                ),
                                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock(
                                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.DEFECT,
                                        0, updated
                                ),
                                new ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock(
                                        ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType.EXPIRED,
                                        0, updated
                                )
                        )
                )
        );
    }

    private MvcResult sendFreezeRequest(String orderId, Map<String, FreezeData> items, ResultMatcher... matchers) {
        return sendFreezeRequest(orderId, items, 1, matchers);
    }

    private MvcResult sendFreezeRequest(String orderId, Map<String, FreezeData> items, int warehouseId,
                                        ResultMatcher... matchers) {
        return sendRefreezeRequest(orderId, items, 0L, warehouseId, matchers);
    }

    private MvcResult sendRefreezeRequest(String orderId, Map<String, FreezeData> items, long version,
                                          ResultMatcher... matchers) {
        return sendRefreezeRequest(orderId, items, version, 1, matchers);
    }

    private MvcResult sendRefreezeRequest(String orderId, Map<String, FreezeData> items, long version,
                                          int warehouseId, ResultMatcher... matchers) {
        return sendRefreezeRequest(orderId, items, version, warehouseId, 12, matchers);
    }

    private MvcResult sendRefreezeRequest(String orderId, Map<String, FreezeData> items, long version,
                                          int warehouseId, long vendorId, ResultMatcher... matchers) {
        try {
            List<String> itemsList = items.entrySet().stream()
                    .map(entry -> String.format(
                            "{\"item\":{\"shopSku\":\"%s\", \"vendorId\": %d, \"warehouseId\":%s},\"amount\":%s, " +
                                    "\"backorder\":%s}",
                            entry.getKey(), vendorId, warehouseId, entry.getValue().getQuantity(),
                            entry.getValue().isBackorder()
                    )).collect(Collectors.toList());

            String content = "{\"orderId\":" + orderId + ", " +
                    "\"items\":" + itemsList + ", " +
                    "\"freezeVersion\": " + version + "}";

            ResultActions resultActions = mockMvc.perform(post(ORDER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(content));
            for (ResultMatcher matcher : matchers) {
                resultActions.andExpect(matcher);
            }
            return resultActions.andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doOnceAfterCheckFreezeStockAvailable(Runnable runnable) {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Set<UnitId> unitIds = (Set<UnitId>) invocation.callRealMethod();

            // call only at once
            if (shouldDoInsideCall) {
                try {
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                    transactionTemplate.execute(action -> {
                        shouldDoInsideCall = false;
                        runnable.run();
                        return null;
                    });
                } finally {
                    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
                }
            }

            return unitIds;
        }).when(availabilityCheckingService).checkFreezeStocksAvailable(anyMap(), any());
    }
}
