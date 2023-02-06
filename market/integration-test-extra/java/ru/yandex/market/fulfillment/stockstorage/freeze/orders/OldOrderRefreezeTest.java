package ru.yandex.market.fulfillment.stockstorage.freeze.orders;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.StockStorageErrorStatusCode;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.FreezingMeta;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReason;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.FreezeReasonType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.StockType;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FreezeNotFoundException;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.backorder.BackorderService;

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
public class OldOrderRefreezeTest extends AbstractContextualTest {

    private static final String REFREEZE_URL = "/stocks/refreeze";

    @Autowired
    protected BackorderService backorderService;

    @Test
    public void refreezeStocksFailedDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("orderId", "items", "refreezeVersion");
    }

    @Test
    public void freezeStocksFailedDueToRequestWithEmptyStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\":123, \"items\":[], \"refreezeVersion\": 123}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("items")
                .doesNotContain("orderId")
                .doesNotContain("refreezeVersion");
    }

    @Test
    public void freezeStocksFailedDueToNonPositiveRefreezeVersion() throws Exception {
        String contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\":123, \"items\":[{\"warehouseId\": 1}], \"refreezeVersion\": -123}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("refreezeVersion")
                .doesNotContain("orderId")
                .doesNotContain("items");

        contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"orderId\":123, \"items\":[{\"warehouseId\": 1}], \"refreezeVersion\": 0}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("refreezeVersion")
                .doesNotContain("orderId")
                .doesNotContain("items");
    }

    @Test
    public void refreezeStocksFailedDueToNoFreezesFound() throws Exception {
        String contentAsString = mockMvc.perform(
                post(REFREEZE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"orderId\": \"12345\",\"refreezeVersion\":1,\"items\":[{\"warehouseId\": 1}]}"))
                .andExpect(status().is(StockStorageErrorStatusCode.FREEZE_NOT_FOUND.getCode()))
                .andReturn().getResponse()
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
    public void skipRefreezeIfOutdated() throws Exception {
        mockMvc.perform(
                post(REFREEZE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(extractFileContent("requests/freeze/refreeze_second_order.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreezed_by_second_order.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeStocksSuccessful() throws Exception {
        executeRefreeze("requests/freeze/refreeze_second_order.json", 3);
    }

    @Test
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_orders_unfreeze_scheduled.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/refreeze_maked_despite_unfreeze_initialized.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void refreezeSuccessfulWhenUnfreezeScheduled() throws Exception {
        mockMvc.perform(
                post(REFREEZE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(extractFileContent("requests/freeze/refreeze_first_order_backorder.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

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
    @DatabaseSetup({
            "classpath:database/states/backorder/warehouses.xml",
            "classpath:database/states/stocks_frozen_two_orders.xml"})
    @ExpectedDatabase(value = "classpath:database/expected/freeze/failed_to_refreeze_first_order.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void refreezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/refreeze_first_order.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
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
    public void refreezeAllStocksByMultipleRequestsSuccessful() throws Exception {
        mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/refreeze_second_order.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/refreeze_first_order.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
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

        verify(stockEventsHandler, times(5)).handle(anyList());
    }

    /**
     * Проверяем, что если в запросе на refreeze не был указан идентификатор склада,
     * то для него возвразается 400 BAD REQUEST
     */
    @Test
    public void refreezeWithNullWarehouseId() throws Exception {
        String contentAsString = mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/refreeze_with_null_warehouse.json")))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("warehouseId");
        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());
    }

    private void executeRefreeze(String requestPath, int handlerTimes) throws Exception {
        mockMvc.perform(post(REFREEZE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent(requestPath)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.ORDER), StockType.FIT, null, 1)),
                        anyMap()
                );

        verify(stockEventsHandler, times(handlerTimes)).handle(anyList());
    }
}
