package ru.yandex.market.fulfillment.stockstorage.freeze.orders;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezingMeta;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;

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

@DatabaseSetup("classpath:database/states/system_property.xml")
public class OldOrderFreezesTest extends AbstractContextualTest {

    private static final String FREEZE_URL = "/stocks/freeze";

    @Test
    public void freezeStocksFailedDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("orderId", "items");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    @Test
    public void freezeStocksFailedDueToRequestWithEmptyStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\":123, \"items\":[]}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("items")
                .doesNotContain("orderId");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_orders_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_unfreeze_scheduled_nothing_is_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToRequestWithDuplicatedFreeze() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\":12345, \"items\":[{\"warehouseId\": 1}]}"))
                .andExpect(status().is(StockStorageErrorStatusCode.DUPLICATE_FREEZE.getCode()))
                .andReturn().getResponse()
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
    public void freezeStocksFailedDueToRequestWithDuplicatedStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/duplicate_stocks.json")))
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
    public void freezeStocksFailedDueToRequestWithIncorrectStocksAmount() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/negative_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Required stock.quantity negative", "sku0", "12");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedDueToRequestWithStocksNonPositiveAmount() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/no_positive_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Required stocks has no positive counts");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedDueToNoStocksFound() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/normal_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "{12:sku0:1}", "{12:sku1:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stoc" +
            "ks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/stocks_are_frozen_by_one_order.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void freezeStocksSuccessful() throws Exception {
        executeFreeze("requests/freeze/normal_stocks.json");

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_pushed_disable_one_sku.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void freezeDisabledStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/normal_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "{12:sku1:1}")
                .doesNotContain("{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/no_fit_stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/states/no_fit_stocks_korobytes_pushed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void notEnoughOnNoFitStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/normal_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Failed to freeze stocks. Not enough available items", "{12:sku1:1}", "{12:sku0:1}");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/korobytes/stocks_korobytes_pushed_nothing_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/to_many_sku0_stocks_required.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku0", "required 100001", "found 100000")
                .doesNotContain("sku1");

        contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/to_many_sku1_stocks_required.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
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
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/sku0_partial_stock_freeze_1.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/sku0_partial_stock_freeze_2.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/sku0_partial_stock_freeze_3.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/sku0_partial_stock_freeze_4.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/sku0_partial_stock_freeze_5.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        //and try to freeze when all stocks are already frozen
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/normal_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Not enough available items", "sku0", "required 50", "found 0")
                .doesNotContain("sku1");

        verify(skuEventAuditService, times(5)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(5)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, times(10)).handle(anyList());

        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1231", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1232", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1233", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1234", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1235", FreezeReasonType.ORDER), StockType.FIT, null, 0)),
                        anyMap()
                );
    }


    /**
     * Проверяем, что если в запросе на freeze не был указан идентификатор склада,
     * то для него возвращается 400 BAD REQUEST
     */
    @Test
    public void freezeWithNullWarehouseIdBeingNotReplaced() throws Exception {
        String contentAsString = mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/freeze_with_null_warehouse.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("warehouseId");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    /**
     * Проверяем, что запрос на фриз айтемов с разных складов (id = 20 и id = 30) успешно выполняется.
     */
    @Test
    @DatabaseSetup("classpath:database/states/freeze_multiple_wh_preset.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/freeze_multiple_wh_completed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeOnMultipleWarehouses() throws Exception {
        executeFreeze("requests/freeze/freeze_multiple_wh.json");
    }

    private void executeFreeze(String requestPath) throws Exception {
        mockMvc.perform(post(FREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent(requestPath)))
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
}
