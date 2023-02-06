package ru.yandex.market.fulfillment.stockstorage.freeze.outbounds;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
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
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.OUTBOUND;

public class OutboundFreezeTest extends AbstractContextualTest {

    @Test
    public void failToCheckAvailableDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("must not be null")
                .contains("freezeVersion", "warehouseId", "stockType", "outboundId", "items");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedDueToRequestWithEmptyStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("items");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedEmptyOutboundItem() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":40,\"items\":[{}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("must not be null")
                .contains("vendorId", "shopSku", "amount");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedEmptySku() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":40,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"\",\"amount\":2}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("shopSku");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedOnRestrictedStockTypes() throws Exception {
        String quarantineResponse = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":40,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku\",\"amount\":2}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(quarantineResponse)
                .contains("Can't freeze stock with type = QUARANTINE");

        String preorderResponse = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":60,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku\",\"amount\":2}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(preorderResponse)
                .contains("Can't freeze stock with type = PREORDER");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedOnZeroAmount() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku\",\"amount\":0}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Required stocks has no positive amounts");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedOnNegativeAmount() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku1\",\"amount\":-1}],\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("amount", "must be greater than or equal to 0");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void freezeStocksFailedDueToRequestWithDuplicatedStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku1\",\"amount\":10},{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}]," +
                        "\"freezeVersion\":5}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Duplicate items found for units", "sku1", "12");

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    public void notEnoughStocksWhenSkuIsAbsent() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123," +
                        "\"warehouseId\":1," +
                        "\"stockType\":50,\"items\":[" +
                        "{\"vendorId\":12,\"shopSku\":\"sku0\",\"amount\":10}," +
                        "{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}" +
                        "],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123," +
                        "\"notEnoughToFreeze\":[" +
                        "{\"sku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":10}," +
                        "{\"sku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":100}" +
                        "]}"));

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/stocks_are_frozen_by_one_outbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksSuccessful() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":10},{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}]," +
                        "\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/surplus_stock.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/surplus_stock_is_frozen.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void freezeStocksSuccessfulWithSurplusStockType() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":70,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku1\",\"amount\":100}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_korobytes_pushed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void notEnoughStocksWhenStockIsAbsent() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123," +
                        "\"warehouseId\":1," +
                        "\"stockType\":30,\"items\":[" + //EXPIRED stock is absent, but rest are present
                        "{\"vendorId\":12,\"shopSku\":\"sku0\",\"amount\":10}," +
                        "{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}" +
                        "],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123," +
                        "\"notEnoughToFreeze\":[" +
                        "{\"sku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":10}," +
                        "{\"sku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":100}" +
                        "]}"));

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_pushed_disable_one_sku.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void freezeDisabledStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123," +
                        "\"warehouseId\":1," +
                        "\"stockType\":50,\"items\":[" +
                        "{\"vendorId\":12,\"shopSku\":\"sku0\",\"amount\":10}," +
                        "{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}" +
                        "],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123," +
                        "\"notEnoughToFreeze\":[" +
                        "{\"sku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":100}" +
                        "]}"));

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/korobytes/stocks_korobytes_pushed_nothing_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void freezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":100001},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":100}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123,\"notEnoughToFreeze\":[{\"sku\":\"sku0\",\"vendorId\":12," +
                        "\"warehouseId\":1,\"quantity\":100000,\"quantityToFreeze\":100001}]}"));

        contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":100},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":100001}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123,\"notEnoughToFreeze\":[{\"sku\":\"sku1\",\"vendorId\":12," +
                        "\"warehouseId\":1,\"quantity\":100000,\"quantityToFreeze\":100001}]}"));

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/sku0_stocks_are_frozen.xml", assertionMode =
            NON_STRICT_UNORDERED)
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    public void freezeAllStocksByTwoRequestsSuccessful() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":1231,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":50000}],\"freezeVersion\":0}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":1232,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":50000}],\"freezeVersion\":0}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        //and try to freeze when all stocks are already frozen
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":10},{\"vendorId\":12,\"shopSku\":\"sku1\",\"amount\":100}]," +
                        "\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":123,\"notEnoughToFreeze\":[{\"sku\":\"sku0\",\"vendorId\":12," +
                        "\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":10}]}"));

        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku1\",\"amount\":50000}],\"freezeVersion\":0}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, times(3)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(3)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1231", FreezeReasonType.OUTBOUND),
                                StockType.DEFECT,
                                null,
                                0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("1232", FreezeReasonType.OUTBOUND),
                                StockType.DEFECT,
                                null,
                                0)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("12345", FreezeReasonType.OUTBOUND), StockType.DEFECT,
                                null, 0)),
                        anyMap()
                );

        verify(stockEventsHandler, times(6)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_single_outbound.xml")
    @ExpectedDatabase(value = "classpath:database/states/stocks_frozen_single_outbound.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void skipFreezeIfAlreadyExist() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":50000},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":50000}],\"freezeVersion\":0}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/outbound_after_refreeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeStock() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":1}],\"freezeVersion\":12345}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService).logStockFreeze(anyList());
        verify(freezeEventAuditService).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("12345", FreezeReasonType.OUTBOUND), StockType.DEFECT,
                                null, 12345)),
                        anyMap()
                );
        verify(stockEventsHandler, times(3)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml")
    @ExpectedDatabase(value = "classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void refreezeStocksFailedDueToNotEnoughStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":100},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":100001}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":12345,\"notEnoughToFreeze\":[{\"sku\":\"sku1\",\"vendorId\":12," +
                        "\"warehouseId\":1,\"quantity\":50000,\"quantityToFreeze\":50001}]}"));

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbound_refreezed.xml")
    @ExpectedDatabase(value = "classpath:database/states/outbounds/stocks_outbound_refreezed.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void outdatedRefreezeSkipped() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123456,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":1}],\"freezeVersion\":1}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml")
    @ExpectedDatabase(value = "classpath:database/states/outbounds/stocks_outbounds_unfreeze_scheduled.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void skipRefreezeIfUnfreezeAlreadyCaught() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123456,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":1}],\"freezeVersion\":1}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, never()).logStockFreeze(anyList());
        verify(freezeEventAuditService, never()).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml")
    @ExpectedDatabase(value = "classpath:database/expected/outbounds/outbound_after_two_refreeze.xml", assertionMode
            = NON_STRICT_UNORDERED)
    public void refreezeAllStocksByMultipleRequestsSuccessful() throws Exception {
        String contentAsString = mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":99999},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":100000}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"outboundId\":12345,\"notEnoughToFreeze\":[{\"sku\":\"sku0\",\"vendorId\":12," +
                        "\"warehouseId\":1,\"quantity\":0,\"quantityToFreeze\":49999}]}"));

        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":123456,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":1}],\"freezeVersion\":1}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"outboundId\":12345,\"warehouseId\":1,\"stockType\":50,\"items\":[{\"vendorId\":12," +
                        "\"shopSku\":\"sku0\",\"amount\":99999},{\"vendorId\":12,\"shopSku\":\"sku1\"," +
                        "\"amount\":100000}],\"freezeVersion\":5}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        verify(skuEventAuditService, times(2)).logStockFreeze(anyList());
        verify(freezeEventAuditService, times(2)).logFreezeSuccessful(any(FreezingMeta.class), anyMap());
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("123456", FreezeReasonType.OUTBOUND), StockType.DEFECT,
                                null, 1)),
                        anyMap()
                );
        verify(freezeEventAuditService)
                .logFreezeSuccessful(
                        eq(FreezingMeta.of(FreezeReason.of("12345", FreezeReasonType.OUTBOUND), StockType.DEFECT,
                                null, 5)),
                        anyMap()
                );

        verify(stockEventsHandler, times(4)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/1001_defect_stocks.xml")
    @ExpectedDatabase(value = "classpath:database/expected/freeze/1001_defect_stocks_freeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void moreThanThousandSkuFreezeSuccessful() throws Exception {
        mockMvc.perform(post(OUTBOUND)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/freeze/1001_outbound_freeze.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }
}
