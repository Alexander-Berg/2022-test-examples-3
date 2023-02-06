package ru.yandex.market.fulfillment.stockstorage;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.Sku;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.repository.SkuRepository;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:database/states/system_property.xml")
class EnableStockTest extends AbstractContextualTest {

    private static final String STOCK_ENABLE_URL = "/support/stock-enable";
    @Autowired
    private SkuRepository skuRepository;

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/expected/enable_stocks/disabled_stocks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void disableStocks() throws Exception {
        mockMvc.perform(post(STOCK_ENABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/disable_stocks.json")))
                .andExpect(status().is2xxSuccessful());

        verify(skuEventAuditService).logSkuEnableChanged(anyList());
        List<Sku> skus = skuRepository.findAllByUnitIdIn(Collections.singletonList(new UnitId("sku0", 12L, 1)));
        verify(skuEventAuditService).logSkuEnableChanged(eq(skus));
        verify(stockEventsHandler, times(1)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/expected/enable_stocks/enabled_stocks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void enableStocks() throws Exception {
        mockMvc.perform(post(STOCK_ENABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/enable_stocks.json")))
                .andExpect(status().is2xxSuccessful());

        verify(skuEventAuditService).logSkuEnableChanged(anyList());
        List<Sku> skus = skuRepository.findAllByUnitIdIn(Collections.singletonList(new UnitId("sku1", 12L, 1)));
        verify(skuEventAuditService).logSkuEnableChanged(eq(skus));
        verify(stockEventsHandler, times(1)).handle(anyList());

    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(value = "classpath:database/expected/enable_stocks/stocks_unchanged.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void enableStocksInvalidSkus() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCK_ENABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/enable_stocks_invalid_skus.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/errors/enable_stocks_failed.json")));

        verify(skuEventAuditService, never()).logSkuEnableChanged(anyList());
        verify(stockEventsHandler, never()).handle(anyList());

    }

    @Test
    void onInvalidCommentPattern() throws Exception {
        var contentAsString = mockMvc.perform(post(STOCK_ENABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/enable_stocks_with_invalid_comment_pattern.json")))
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(contentAsString).is(
                jsonMatching("{\"errors\":{\"comment\":\"must match \\\"^.*?MARKETFF-.*?$\\\"\"}}")
        );

        verify(skuEventAuditService, never()).logSkuEnableChanged(anyList());
        verify(stockEventsHandler, never()).handle(anyList());
    }
}
