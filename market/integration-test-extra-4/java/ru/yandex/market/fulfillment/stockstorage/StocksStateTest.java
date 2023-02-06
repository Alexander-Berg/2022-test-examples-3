package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.service.warehouse.group.StocksWarehouseGroupCache;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.FREEZES;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.GET_AVAILABLE;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderRestClient.ORDER;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundRestClient.OUTBOUND;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStoragePreOrderRestClient.PREORDER;

public class StocksStateTest extends AbstractContextualTest {

    private static final String CHECK_AVAILABLE_URL = "/stocks/checkAvailable";
    private static final String ORDERS_CHECK_AVAILABLE_URL = ORDER + GET_AVAILABLE;
    private static final String ORDERS_GET_FREEZE = ORDER + "/%s" + FREEZES;
    private static final String OUTBOUNDS_GET_FREEZE = OUTBOUND + "/%s" + FREEZES;
    private static final String PREORDERS_CHECK_AVAILABLE_URL = PREORDER + GET_AVAILABLE;
    private static final String ALL_BY_VENDORS_URL = "/stocks/allByVendors";
    private static final String BY_VENDORS_URL = "/stocks/byVendors";

    @Autowired
    private StocksWarehouseGroupCache stocksWarehouseGroupCache;

    @BeforeEach
    void loadCache() {
        stocksWarehouseGroupCache.reload();
    }

    @Test
    public void checkAvailableOnEmptyDatabaseOldOrders() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/check_available.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"stocks\":[]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    public void checkAvailablePartIsNonexistentOldOrders() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/check_available_nonexistent.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/stocks/checkAvailable.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    public void checkAvailableFullMatchOldOrders() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/check_available.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/stocks/checkAvailable.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    public void checkAvailableWithDisabledSkuOldOrders() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/check_available.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/check_available_disabled_sku.json")));
    }

    @Test
    public void checkAvailableOnEmptyDatabase() throws Exception {
        perfomCheckAvailableOnEmptyDatabase(ORDERS_CHECK_AVAILABLE_URL);
    }

    @Test
    public void preorderCheckAvailableOnEmptyDatabase() throws Exception {
        perfomCheckAvailableOnEmptyDatabase(PREORDERS_CHECK_AVAILABLE_URL);
    }

    private void perfomCheckAvailableOnEmptyDatabase(String ordersCheckAvailableUrl) throws Exception {
        String contentAsString = mockMvc.perform(post(ordersCheckAvailableUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("{\"items\":[]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    public void checkAvailablePartIsNonexistent() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku2\",\"vendorId\":12,\"warehouseId\":1}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    public void checkAvailableFullMatch() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    public void checkAvailableWithDisabledSku() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":1}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    public void getOrdersFreezeOnEmptyDatabase() throws Exception {
        mockMvc.perform(get(String.format(ORDERS_GET_FREEZE, "12345")))
                .andExpect(content().json(extractFileContent("response/freezes/empty_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    public void getOrdersFreezeWhenItIsNotExist() throws Exception {
        mockMvc.perform(get(String.format(ORDERS_GET_FREEZE, "1234567")))
                .andExpect(content().json(extractFileContent("response/freezes/empty_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_two_orders.xml")
    public void getOrdersFreezeWhenItExistsWithoutUnfreezeJobs() throws Exception {
        mockMvc.perform(get(String.format(ORDERS_GET_FREEZE, "123456")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(extractFileContent("response/freezes/existing_order_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_deleted_orders_and_different_stocks.xml")
    public void getOrdersFreezeWhenItExistsWithUnfreezeJobs() throws Exception {
        mockMvc.perform(get(String.format(ORDERS_GET_FREEZE, "123456")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(extractFileContent("response/freezes/existing_order_freezes_with_quarantine.json")));
    }

    @Test
    public void getOutboundsFreezeOnEmptyDatabase() throws Exception {
        mockMvc.perform(get(String.format(OUTBOUNDS_GET_FREEZE, "12345")))
                .andExpect(content().json(extractFileContent("response/freezes/empty_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml")
    public void getOutboundsFreezeWhenItIsNotExist() throws Exception {
        mockMvc.perform(get(String.format(OUTBOUNDS_GET_FREEZE, "1234567")))
                .andExpect(content().json(extractFileContent("response/freezes/empty_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/outbounds/stocks_frozen_two_outbounds.xml")
    public void getOutboundsFreezeWhenItExistsWithoutUnfreezeJobs() throws Exception {
        mockMvc.perform(get(String.format(OUTBOUNDS_GET_FREEZE, "12345")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(extractFileContent("response/freezes/existing_outbounds_freezes.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_frozen_deleted_outbounds_and_different_stocks.xml")
    public void getOutboundsFreezeWhenItExistsWithUnfreezeJobs() throws Exception {
        mockMvc.perform(get(String.format(OUTBOUNDS_GET_FREEZE, "123456")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(extractFileContent("response/freezes/existing_outbound_freezes_with_quarantine.json")));
    }

    @Test
    public void getFitStocksAllByVendorsOnEmptyDatabase() throws Exception {
        String contentAsString = mockMvc.perform(post(ALL_BY_VENDORS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/all_by_vendors.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/empty_by_vendors.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    public void getFitStocksAllByVendors() throws Exception {
        String contentAsString = mockMvc.perform(post(ALL_BY_VENDORS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/all_by_vendors.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/all_by_vendors.json")));
    }

    @Test
    public void getFitStocksByVendorsOnEmptyDatabase() throws Exception {
        String contentAsString = mockMvc.perform(post(BY_VENDORS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/by_vendors.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/empty_by_vendors_paged.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    public void getFitStocksByVendors() throws Exception {
        String contentAsString = mockMvc.perform(post(BY_VENDORS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/by_vendors.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/by_vendors.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_korobytes_pushed.xml")
    public void getFitStocksByVendorsPaged() throws Exception {
        String contentAsString = mockMvc.perform(post(BY_VENDORS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/stocks/by_vendors_paged.json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching(extractFileContent("response/stocks/by_vendors_paged.json")));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_with_warehouse_replacement.xml")
    public void checkAvailableFullMatchWithoutWarehouseReplacement() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"stocks\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":66}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":66}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .is(jsonMatchingWithoutOrder("" +
                        "{\"stocks\":[" +
                        "    {\"sku\":\"sku0\",\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":66, \"quantity\":" +
                        " 100000}," +
                        "    {\"sku\":\"sku1\",\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":66, \"quantity\":" +
                        " 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_multiple_warehouses.xml")
    public void checkAvailableFromDifferentWarehouses() throws Exception {
        String contentAsString = mockMvc.perform(post(CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"stocks\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"stocks\":[" +
                        "    {\"sku\":\"sku0\",\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1, \"quantity\": " +
                        "100000}," +
                        "    {\"sku\":\"sku1\",\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2, \"quantity\": " +
                        "100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_multiple_warehouses.xml")
    public void ordersCheckAvailableFullMatchWithoutWarehouseReplacement() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}, \"amount\": 100000}" +
                        "]}"));
    }

    /**
     * Проверяет, что при запросе по главному и второстепенному складу из группы возвращаются остатки главного склада.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_shared_stocks.xml")
    public void ordersGetAvailableSharedStocks() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("{\"items\":[" +
                                "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                                "    {\"shopSku\":\"sku0\",\"vendorId\":13,\"warehouseId\":2}" +
                                "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":13,\"warehouseId\":2}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_multiple_warehouses.xml")
    public void ordersCheckAvailableFromDifferentWarehouses() throws Exception {
        String contentAsString = mockMvc.perform(post(ORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/preorders_pushed_multiple_warehouses.xml")
    public void preordersCheckAvailableFullMatchWithoutWarehouseReplacement() throws Exception {
        String contentAsString = mockMvc.perform(post(PREORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}, \"amount\": 100000}" +
                        "]}"));
    }

    @Test
    @DatabaseSetup("classpath:database/states/preorders_pushed_multiple_warehouses.xml")
    public void preordersCheckAvailableFromDifferentWarehouses() throws Exception {
        String contentAsString = mockMvc.perform(post(PREORDERS_CHECK_AVAILABLE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{\"items\":[" +
                        "    {\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}," +
                        "    {\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}" +
                        "]}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .is(jsonMatching("" +
                        "{\"items\":[" +
                        "    {\"item\":{\"shopSku\":\"sku0\",\"vendorId\":12,\"warehouseId\":1}, \"amount\": 100000}," +
                        "    {\"item\":{\"shopSku\":\"sku1\",\"vendorId\":12,\"warehouseId\":2}, \"amount\": 100000}" +
                        "]}"));
    }
}
