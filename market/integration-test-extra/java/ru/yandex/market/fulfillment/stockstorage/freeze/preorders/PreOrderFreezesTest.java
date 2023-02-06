package ru.yandex.market.fulfillment.stockstorage.freeze.preorders;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.configuration.BasicColumnsFilter;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezingMeta;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStoragePreOrderRestClient.PREORDER;

@SuppressWarnings("Duplicates")
@DatabaseSetup("classpath:database/states/system_property.xml")
public class PreOrderFreezesTest extends AbstractContextualTest {


    /**
     * Проверка:
     * Запрос на фриз /preorder успешно исполнен c корректными стоками
     * <p>
     * Результат:
     * Код 200
     * В БД создан фриз
     * У стока freeze_amount увеличен
     * Вызван аудит
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder/preorder_stocks_frozen_two_orders_new_preorder.xml",
            assertionMode = NON_STRICT_UNORDERED, columnFilters = {BasicColumnsFilter.class})
    public void freezeStocksSuccessful() throws Exception {
        sendFreezeRequest(
                "preorder3",
                ImmutableMap.of("sku0", 50),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("preorder3", FreezeReasonType.PREORDER), StockType
                                        .PREORDER, null,
                                0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз /preorder успешно исполнен c корректными стоками. При этом изменилась доступность стока
     * <p>
     * Результат:
     * Код 200
     * В БД создан фриз
     * У стока freeze_amount увеличен
     * Вызван аудит
     * В execution_queue добавлены таски для товаров, по которым изменилось availability
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder" +
                    "/preorder_stocks_frozen_two_orders_new_preorder_with_availability_change.xml",
            assertionMode = NON_STRICT_UNORDERED, columnFilters = {BasicColumnsFilter.class})
    public void freezeStocksSuccessfulWithAvailabilityChange() throws Exception {
        sendFreezeRequest(
                "preorder3",
                ImmutableMap.of("sku0", 99900),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("preorder3", FreezeReasonType.PREORDER), StockType
                                        .PREORDER, null,
                                0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз /preorder успешно исполнен c корректными стоками. При этом существует фриз с тем же
     * идентификатором в рамках заказа на FIT сток
     * <p>
     * Результат:
     * Код 200
     * В БД создан фриз
     * У стока freeze_amount увеличен
     * Вызван аудит
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/preorder/preorder_stocks_frozen_two_orders_new_preorder_as_order.xml",
            assertionMode = NON_STRICT_UNORDERED, columnFilters = {BasicColumnsFilter.class})
    public void freezeStocksSuccessfulByExistingFitOrder() throws Exception {
        sendFreezeRequest(
                "order1",
                ImmutableMap.of("sku0", 50),
                status().is2xxSuccessful())
                .getResponse()
                .getContentAsString();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("order1", FreezeReasonType.PREORDER), StockType.PREORDER,
                                null,
                                0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с пустым запросом не исполняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка с текстом не указаны обязательные поля
     */
    @Test
    public void freezeStocksFailedDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc
                .perform(post(PREORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("orderId", "items", "freezeVersion");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());
    }

    /**
     * Проверка:
     * Запрос на фриз с запросом не содержащим товаров не исполняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка с текстом не указаны обязательные поля item
     */
    @Test
    public void freezeStocksFailedDueToRequestWithEmptyStocks() throws Exception {
        String contentAsString = mockMvc
                .perform(post(PREORDER)
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

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с запросом содержащим пустой item не исполняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка с текстом не указаны обязательные поля
     */
    @Test
    public void freezeStocksFailedDueToRequestWithEmptyItemAmountData() throws Exception {
        String contentAsString = mockMvc
                .perform(post(PREORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\":12345, \"items\":[{}]}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("item", "amount");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с запросом содержащим дублирующий item не исполняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка с текстом о дублирующихся айтемах
     */
    @Test
    public void freezeStocksFailedDueToRequestWithDuplicatedStocks() throws Exception {
        String contentAsString = mockMvc
                .perform(post(PREORDER)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\":12345, " +
                                "\"items\":[" +
                                "    {\"item\":{\"shopSku\":\"sku0\", \"vendorId\": 12, \"warehouseId\":1}," +
                                "\"amount\":123}," +
                                "    {\"item\":{\"shopSku\":\"sku0\", \"vendorId\": 12, \"warehouseId\":1}," +
                                "\"amount\":123}" +
                                "], " +
                                "\"freezeVersion\": 0}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate items found for id", "sku0", "12");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с уже существующим идентификатором и типом фриза не исполняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка о дублирующихся фризах
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToRequestWithDuplicatedFreeze() throws Exception {
        String contentAsString = sendFreezeRequest(
                "preorder1",
                ImmutableMap.of("sku0", 123),
                status().is(StockStorageErrorStatusCode.DUPLICATE_FREEZE.getCode()))
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate freeze: preorder1 type: PREORDER")
                .contains("409_BAD_REQUEST");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());
    }

    /**
     * Проверка:
     * Запрос на фриз с запросом на отстутствующий sku не ислолняется, при отключенной
     * {@link SystemPropertyKey#SHOULD_CREATE_SKU_ON_BACKORDERED_FREEZE}.
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @DatabaseSetup("classpath:database/states/system_property_with_disabled_creating_sku_on_freeze.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToNoStocksFound() throws Exception {
        String contentAsString = sendFreezeRequest(
                "failPreorder1",
                ImmutableMap.of("sku02", 123),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku02:1}");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с запросом на sku у которого нет PREORDER стока не ислолняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/on_sku_without_preorder_stocks.xml")
    @ExpectedDatabase(value = "classpath:database/states/preorder/on_sku_without_preorder_stocks.xml",
            assertionMode =
                    NON_STRICT_UNORDERED)
    public void notEnoughOnNoPreorderStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku3", 50),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku3:1}");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с запросом на выключенный sku не ислолняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_pushed_disable_one_sku.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void freezeDisabledStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", 50, "sku1", 50),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}")
                .doesNotContain("{12:sku0:1}");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на фриз с запросом на sku e которого недостаточно PREORDER стока не ислолняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка о недостаточности стока с указанием item
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", 100001, "sku1", 50),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku0", "required 100001", "found 99900")
                .doesNotContain("sku1");

        contentAsString = sendFreezeRequest(
                "123",
                ImmutableMap.of("sku0", 100, "sku1", 100051),
                status().is4xxClientError())
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku1", "required 100051", "found 100000")
                .doesNotContain("sku0");

        verify(skuEventAuditService, never())
                .logStockFreeze(anyList());
        verify(freezeEventAuditService, never())
                .logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never())
                .handle(anyList());

    }

    /**
     * Проверка:
     * Запрос на рефриз(такой фриз уже есть, но с версий меньше чем в запросе) не ислолняется
     * <p>
     * Результат:
     * Код 400
     * Ошибка о дубликате фриза
     */
    @Test
    @DatabaseSetup("classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/states/preorder/preorder_stocks_frozen_two_orders.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testRefreezeIsNotAllowed() throws Exception {
        String contentAsString = sendRefreezeRequest(
                "preorder1",
                ImmutableMap.of("sku0", 50, "sku1", 10),
                1,
                status().is(StockStorageErrorStatusCode.DUPLICATE_FREEZE.getCode()))
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate freeze: preorder1 type: PREORDER")
                .contains("409_BAD_REQUEST");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    /**
     * Проверяем, что в рамках предзаказа можно сделать freeze на несколько складов одновременно.
     */
    @Test
    @DatabaseSetup({
            "classpath:database/states/preorder_stocks.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/preorder_stocks_frozen.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeWithMultipleWarehouses() throws Exception {
        mockMvc.perform(post(PREORDER)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/preorders_freeze_multiple_wh.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123", FreezeReasonType.PREORDER), StockType.PREORDER,
                                null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    private MvcResult sendFreezeRequest(String orderId,
                                        Map<String, Integer> items,
                                        ResultMatcher... matchers) {
        return sendRefreezeRequest(orderId, items, 0L, matchers);
    }

    private MvcResult sendRefreezeRequest(String orderId,
                                          Map<String, Integer> items,
                                          long version,
                                          ResultMatcher... matchers) {
        try {

            List<String> itemsList = items.entrySet().stream()
                    .map(entry -> String.format(
                            "{\"item\":{\"shopSku\":\"%s\", \"vendorId\": 12, \"warehouseId\":1},\"amount\":%s}",
                            entry.getKey(), entry.getValue()
                    )).collect(Collectors.toList());

            ResultActions resultActions = mockMvc.perform(post(PREORDER)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("{\"orderId\":\"" + orderId + "\", " +
                            "\"items\":" + itemsList + ", " +
                            "\"freezeVersion\": " + version + "}"));
            for (ResultMatcher matcher : matchers) {
                resultActions.andExpect(matcher);
            }
            return resultActions.andReturn();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
