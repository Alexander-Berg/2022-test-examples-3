package ru.yandex.market.fulfillment.stockstorage.freeze.outbounds;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.GET_AVAILABLE;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.GET_AVAILABLE_ITEMS;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.OUTBOUND;

public class OutboundsTest extends AbstractContextualTest {

    private static final String CHECK_AVAILABLE_URL = OUTBOUND + GET_AVAILABLE;
    private static final String CHECK_AVAILABLE_ITEM_URL = OUTBOUND + GET_AVAILABLE_ITEMS;

    @Test
    public void failToCheckAvailableDueToRequestWithNulls() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("vendorId", "warehouseId", "stockType");
    }

    @Test
    public void failToCheckAvailableDueToIllegalStockType() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12,\"warehouseId\":1,\"stockType\":73}"))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("Can't handle stock with type");
    }

    @Test
    public void checkAvailableReturnsEmptyResponseOnEmptyDatabase() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12,\"warehouseId\":1,\"stockType\":10}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"stockType\":10,\"stocks\":[]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/single_fit_stock_two_sku.xml")
    public void checkAvailableWithSkuListReturnsSingleStoredStock() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":10,\"shopSkuList\":[\"sku0\",\"sku1\"]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/single_fit_stock.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/single_fit_stock_two_sku.xml")
    public void checkAvailableByItemListReturnsSingleStoredStock() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"stockType\":10, \"items\": [{\"shopSku\": \"sku0\",\"vendorId\": 12,\"warehouseId\": 1}," +
                        "{\"shopSku\": \"sku1\",\"vendorId\": 12,\"warehouseId\": 1}]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/single_fit_stock.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_freeze.xml")
    public void checkAvailableReturnsBothStocksWithFreeze() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":10}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/two_stocks.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_freeze.xml")
    public void checkAvailableWithListOfAllStoredSkuReturnsAllStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":10,\"shopSkuList\":[\"sku0\",\"sku1\"]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/two_stocks.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_freeze_by_different_vendors.xml")
    public void checkAvailableWithListOfAllStoredItemsReturnsAllStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"stockType\":10, \"items\": [{\"shopSku\": \"sku0\",\"vendorId\": 11,\"warehouseId\": 1}," +
                        "{\"shopSku\": \"sku1\",\"vendorId\": 12,\"warehouseId\": 1}]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder(
                        extractFileContent("response/outbounds/two_stocks_by_different_vendors.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_freeze.xml")
    public void checkAvailableWithFilterByOneSkuReturnsSingleSku() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":10,\"shopSkuList\":[\"sku0\"]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/single_fit_stock.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_freeze.xml")
    public void checkAvailableByOneItemReturnsSingleItem() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_ITEM_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"stockType\":10, \"items\": [{\"shopSku\": \"sku0\",\"vendorId\": 12,\"warehouseId\": " +
                        "1}]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/single_fit_stock.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/two_fit_stocks_one_defect.xml")
    public void checkAvailableDefectReturnsOneDefectStock() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":50}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/outbounds/single_defect_stock.json")));
    }

    /**
     * Проверяем, что warehouseId 1 не будет трансформирован в 145 при обращении,
     * и в ответ данные так же вернутся с warehouseId 1.
     */
    @Test
    @DatabaseSetup("classpath:database/states/outbounds/single_fit_stock.xml")
    public void checkReplacementForRealFulfillmentId() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":" + 1 + ",\"stockType\":10}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .is(jsonMatching(
                        extractFileContent("response/outbounds/real_warehouse_id_without_replacement_1.json")));
    }

    /**
     * Проверяем, что если в запросе warehouseId = 1,
     * то в ответ так же будет возвращена 1 (не произойдет замены на 145).
     */
    @Test
    @DatabaseSetup("classpath:database/states/outbounds/single_fit_stock.xml")
    public void checkNoReplacementForFakeWarehouseId() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"vendorId\":12, \"warehouseId\":1,\"stockType\":10}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent(
                        "response/outbounds/real_warehouse_id_replacement_2.json")));
    }


}
