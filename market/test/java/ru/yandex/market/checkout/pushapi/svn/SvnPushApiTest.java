package ru.yandex.market.checkout.pushapi.svn;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.helpers.SvnHelper;
import ru.yandex.market.checkout.pushapi.util.HttpResourceHelper;
import ru.yandex.market.common.report.model.FeedOfferId;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.checkout.pushapi.controller.SvnPushApiController.DATE_FORMAT;


//todo починить тесты для svn-shop'ов MBI-78417
@Disabled
public class SvnPushApiTest extends AbstractWebTestBase {

    private static final List<String> AVAILABLE_SVN_RESOURCES = List.of(
            "cart.txt",
            "global.txt",
            "inventory.txt",
            "order_status.txt",
            "order_accept.txt",
            "stocks.txt",
            "stocks_inventory.txt"
    );

    private static final CheckoutDateFormat CHECKOUT_DATE_FORMAT = new CheckoutDateFormat();

    private static final long SHOP_ID_WITH_GENERATE_DATA_ENABLED_XML = 1;

    private static final long SHOP_ID_WITH_GENERATE_DATA_DISABLED_XML = 2;

    private static final long SHOP_ID_WITH_GENERATE_DATA_ENABLED_JSON = 3;

    private static final long SHOP_ID_WITH_GENERATE_DATA_DISABLED_JSON = 4;

    private static final long SHOP_ID_WITH_GENERATE_DATA_INVENTORY = 5;

    private static final long DEFAULT_FEED_ID = 383182L;

    private static final String XML_CONTENT_BODY = xmlContentBody(new FeedOfferId("1", DEFAULT_FEED_ID));

    private static final String JSON_CONTENT_BODY = jsonContentBody(new FeedOfferId("1", DEFAULT_FEED_ID));

    private static final int DAYS_IN_WEEK = 7;

    private static final Map<Long, String> RESOURCE_DIRECTORY_BY_SHOP_ID = Map.of(
            SHOP_ID_WITH_GENERATE_DATA_ENABLED_XML, "xml-generation-on",
            SHOP_ID_WITH_GENERATE_DATA_DISABLED_XML, "xml-generation-off",
            SHOP_ID_WITH_GENERATE_DATA_ENABLED_JSON, "json-generation-on",
            SHOP_ID_WITH_GENERATE_DATA_DISABLED_JSON, "json-generation-off",
            SHOP_ID_WITH_GENERATE_DATA_INVENTORY, "json-generation-inventory"
    );

    @Autowired
    private WireMockServer svnMock;
    @Autowired
    private SvnHelper svnHelper;

    @AfterEach
    public void tearDown() {
        svnMock.resetAll();
    }

    private static String xmlContentBody(FeedOfferId feedOfferId) {
        return "<cart currency=\"" + "RUR" + "\">\n" +
                "  <items>\n" +
                "    <item feed-id=\"" + feedOfferId.getFeedId() + "\" offer-id=\"" + feedOfferId.getId() +
                "\" feed-category-id=\"{{feedcategory}}\" offer-name=\"{{offername}}\" count=\"1\"/>\n" +
                "  </items>\n" +
                "    <delivery>\n" +
                "      <region id=\"2\">\n" +
                "            <parent id=\"10174\">\n" +
                "                <parent id=\"17\">\n" +
                "                  <parent id=\"225\"/>\n" +
                "                </parent>\n" +
                "            </parent>\n" +
                "        </region>\n" +
                "  </delivery>\n" +
                "</cart>";
    }

    private static String jsonContentBody(FeedOfferId feedOfferId) {
        return "{\n" +
                "  \"cart\": {\n" +
                "    \"items\": [\n" +
                "      {\n" +
                "        \"feedId\": " + feedOfferId.getFeedId() + ",\n" +
                "        \"offerId\": \"" + feedOfferId.getId() + "\",\n" +
                "        \"offerName\": \"Чайник электрический 100W\",\n" +
                "        \"count\": 2\n" +
                "      }\n" +
                "    ],\n" +
                "    \"delivery\": {\n" +
                "      \"region\": {\n" +
                "        \"id\": 213,\n" +
                "        \"parent\": {\n" +
                "          \"id\": 1\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    @Test
    public void returnsItemsFromRequestWhenGenerateDataEnabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_XML;

        mockSvn(svnMock, shopId);

        String today = CHECKOUT_DATE_FORMAT.formatShort(new Date());
        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(XML_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().xml(
                        "<cart shop-admin=\"false\" tax-system=\"OSN\">\n" +
                                "   <items>\n" +
                                "      <item feed-id=\"383182\" offer-id=\"1\" offer-name=\"{{offername}}\" " +
                                "price=\"250\" " +
                                "            subsidy=\"0\" count=\"1\" />\n" +
                                "   </items>\n" +
                                "   <delivery-options>\n" +
                                "      <delivery type=\"PICKUP\" price=\"0\" service-name=\"Почта России PICKUP\" " +
                                "payment-allow=\"true\">\n" +
                                "         <dates from-date=\"" + today + "\" to-date=\"" + today + "\" />\n" +
                                "         <outlets>\n" +
                                "            <outlet code=\"1\" />\n" +
                                "         </outlets>\n" +
                                "      </delivery>\n" +
                                "      <delivery type=\"POST\" price=\"250\" service-name=\"Почта России POST\" " +
                                "payment-allow=\"true\">\n" +
                                "         <dates from-date=\"" + today + "\" to-date=\"" + today + "\" />\n" +
                                "      </delivery>\n" +
                                "      <delivery type=\"DELIVERY\" price=\"350\" " +
                                "                service-name=\"Почта России DELIVERY\" payment-allow=\"true\">\n" +
                                "         <dates from-date=\"" + today + "\" to-date=\"" + today + "\" />\n" +
                                "      </delivery>\n" +
                                "   </delivery-options>\n" +
                                "   <payment-methods>\n" +
                                "      <payment-method>CASH_ON_DELIVERY</payment-method>\n" +
                                "      <payment-method>CARD_ON_DELIVERY</payment-method>\n" +
                                "      <payment-method>SHOP_PREPAID</payment-method>\n" +
                                "      <payment-method>YANDEX</payment-method>\n" +
                                "   </payment-methods>\n" +
                                "</cart>"
                ));
    }

    @Test
    public void returnsStocksWhenGenerateDataEnabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_XML;

        mockSvn(svnMock, shopId);

        StocksRequest stocksRequest = new StocksRequest();
        stocksRequest.setWarehouseId(1234L);
        stocksRequest.setSkus(Collections.singletonList("asdasd"));

        svnHelper.performQueryStocksXml(shopId, stocksRequest)
                .andExpect(xpath("//stocksResponse/stocks/stock/@sku").string("asdasd"))
                .andExpect(xpath("//stocksResponse/stocks/stock/@warehouseId").string("1234"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@count").string("10"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@type").string("FIT"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@updatedAt").exists())
        ;
    }

    @Test
    public void returnStocksWhenGenerateDataDisabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_XML;

        mockSvn(svnMock, shopId);

        StocksRequest stocksRequest = new StocksRequest();
        stocksRequest.setWarehouseId(1234L);
        stocksRequest.setSkus(Collections.singletonList("asdasd"));

        svnHelper.performQueryStocksXml(shopId, stocksRequest, "EB00000195533612")
                .andExpect(xpath("//stocksResponse/stocks/stock/@sku").string("asdasd"))
                .andExpect(xpath("//stocksResponse/stocks/stock/@warehouseId").string("1234"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@count").string("10"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@type").string("FIT"))
                .andExpect(xpath("//stocksResponse/stocks/stock/items/item/@updatedAt").exists());
    }

    @Test
    public void returnStocksJsonWhenGenerateDataEnabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_JSON;

        mockSvn(svnMock, shopId);

        StocksRequest stocksRequest = new StocksRequest();
        stocksRequest.setWarehouseId(1234L);
        stocksRequest.setSkus(Collections.singletonList("asdasd"));

        svnHelper.performQueryStocksJson(shopId, stocksRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skus[0].sku").value("asdasd"))
                .andExpect(jsonPath("$.skus[0].warehouseId").value("1234"))
                .andExpect(jsonPath("$.skus[0].items[0].type").value("FIT"))
                .andExpect(jsonPath("$.skus[0].items[0].count").value("10"))
                .andExpect(jsonPath("$.skus[0].items[0].updatedAt").exists());
    }

    @Test
    public void returnStocksJsonWheneGenerateDataInventory() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_INVENTORY;

        mockSvn(svnMock, shopId);

        StocksRequest stocksRequest = new StocksRequest();
        stocksRequest.setWarehouseId(1234L);
        stocksRequest.setSkus(Arrays.asList("asdasd", "dsadsa"));

        svnHelper.performQueryStocksJson(shopId, stocksRequest, "57000001CE5625A6")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skus").value(hasSize(2)))
                .andExpect(jsonPath("$.skus[0].sku").value("asdasd"))
                .andExpect(jsonPath("$.skus[0].warehouseId").value("1234"))
                .andExpect(jsonPath("$.skus[0].items[0].type").value("FIT"))
                .andExpect(jsonPath("$.skus[0].items[0].count").value("10"))
                .andExpect(jsonPath("$.skus[0].items[0].updatedAt").exists())
                .andExpect(jsonPath("$.skus[1].sku").value("dsadsa"))
                .andExpect(jsonPath("$.skus[1].warehouseId").value("1234"))
                .andExpect(jsonPath("$.skus[1].items").isEmpty());
    }

    @Test
    public void returnStocksJsonWhenGenerateDataDisabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_JSON;

        mockSvn(svnMock, shopId);

        StocksRequest stocksRequest = new StocksRequest();
        stocksRequest.setWarehouseId(1234L);
        stocksRequest.setSkus(Collections.singletonList("asdasd"));

        svnHelper.performQueryStocksJson(shopId, stocksRequest, "57000001CE5625A6")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skus[0].sku").value("asdasd"))
                .andExpect(jsonPath("$.skus[0].warehouseId").value("1234"))
                .andExpect(jsonPath("$.skus[0].items[0].type").value("FIT"))
                .andExpect(jsonPath("$.skus[0].items[0].count").value("10"))
                .andExpect(jsonPath("$.skus[0].items[0].updatedAt").exists());
    }


    @Test
    public void returnsItemsFromRequestWhenGenerateDataEnabledJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_JSON;

        mockSvn(svnMock, shopId);

        String deliveryFromDate = CHECKOUT_DATE_FORMAT.formatShort(new Date());
        String deliveryToDate = CHECKOUT_DATE_FORMAT.formatShort(DateUtils.addDays(new Date(), DAYS_IN_WEEK));
        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(JSON_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\n" +
                                "  \"cart\": {\n" +
                                "    \"items\": [\n" +
                                "      {\n" +
                                "        \"feedId\": 383182,\n" +
                                "        \"offerId\": \"1\",\n" +
                                "        \"offerName\": \"Чайник электрический 100W\",\n" +
                                "        \"count\": 2,\n" +
                                "        \"price\": 250,\n" +
                                "        \"delivery\": true\n" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"deliveryOptions\": [\n" +
                                "      {\n" +
                                "        \"type\": \"PICKUP\",\n" +
                                "        \"serviceName\": \"Почта России PICKUP\",\n" +
                                "        \"price\": 0,\n" +
                                "        \"dates\": {\n" +
                                "          \"fromDate\": \"" + deliveryFromDate + "\",\n" +
                                "          \"toDate\": \"" + deliveryToDate + "\"\n" +
                                "        }\n," +
                                "        \"paymentMethods\": [],\n" +
                                "        \"outlets\": [\n" +
                                "          {\n" +
                                "            \"id\": 1\n" +
                                "          }\n" +
                                "        ],\n" +
                                "        \"validFeatures\": [\n" +
                                "          \"PLAINCPA\"\n" +
                                "        ]" +
                                "      },\n" +
                                "      {\n" +
                                "        \"type\": \"POST\",\n" +
                                "        \"serviceName\": \"Почта России POST\",\n" +
                                "        \"price\": 250,\n" +
                                "        \"dates\": {\n" +
                                "          \"fromDate\": \"" + deliveryFromDate + "\",\n" +
                                "          \"toDate\": \"" + deliveryToDate + "\"\n" +
                                "        },\n" +
                                "        \"paymentMethods\": [],\n" +
                                "        \"validFeatures\": [\n" +
                                "          \"PLAINCPA\"\n" +
                                "        ]\n" +
                                "      },\n" +
                                "      {\n" +
                                "        \"type\": \"DELIVERY\",\n" +
                                "        \"serviceName\": \"Почта России DELIVERY\",\n" +
                                "        \"price\": 350,\n" +
                                "        \"dates\": {\n" +
                                "          \"fromDate\": \"" + deliveryFromDate + "\",\n" +
                                "          \"toDate\": \"" + deliveryToDate + "\"\n" +
                                "        }\n," +
                                "        \"paymentMethods\": [],\n" +
                                "        \"validFeatures\": [\n" +
                                "          \"PLAINCPA\"\n" +
                                "        ]" +
                                "      }\n" +
                                "    ],\n" +
                                "    \"paymentMethods\": [\n" +
                                "      \"CASH_ON_DELIVERY\",\n" +
                                "      \"CARD_ON_DELIVERY\",\n" +
                                "      \"SHOP_PREPAID\",\n" +
                                "      \"YANDEX\"\n" +
                                "    ]\n," +
                                "  \"taxSystem\": \"OSN\"" +
                                "  }\n" +
                                "}", true
                ));
    }

    @Test
    public void returnsContentOfCartFileWhenGenerateDataDisabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_XML;

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(XML_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_XML)
                                .param("auth-token", "EB00000195533612"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().xml(getResourceContent(shopId, "cart.txt")));
    }

    @Test
    public void returnsContentOfCartFileWhenGenerateDataDisabledJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_JSON;

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(JSON_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("auth-token", "57000001CE5625A6"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json(getResourceContent(shopId, "cart.txt")));
    }

    @Test
    public void returnsZeroCountOfItemsWhenNoSuchItemsInInventoryWhenGenerateDataIsInventoryJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_INVENTORY;

        mockSvn(svnMock, shopId);
        String today = CHECKOUT_DATE_FORMAT.formatShort(new Date());
        String tomorrow = CHECKOUT_DATE_FORMAT.formatShort(DateUtils.addDays(new Date(), 1));
        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(jsonContentBody(new FeedOfferId("3", DEFAULT_FEED_ID)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("auth-token", "3A000001F854DDF2"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\n" +
                                "   \"cart\":{\n" +
                                "      \"items\":[\n" +
                                "         {\n" +
                                "            \"feedId\":383182,\n" +
                                "            \"offerId\":\"3\",\n" +
                                "            \"offerName\":\"Чайник электрический 100W\",\n" +
                                "            \"count\":0,\n" +
                                "            \"price\":250,\n" +
                                "            \"delivery\":true\n" +
                                "         }\n" +
                                "      ],\n" +
                                "      \"deliveryOptions\":[\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"VIP курьер\",\n" +
                                "            \"price\":250,\n" +
                                "            \"vat\":\"VAT_10\",\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ]\n," +
                                "            \"paymentMethods\":[]\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"Premium курьер\",\n" +
                                "            \"price\":200,\n" +
                                "            \"vat\":\"VAT_10\",\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + tomorrow + "\",\n" +
                                "               \"toDate\":\"" + tomorrow + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ]\n," +
                                "            \"paymentMethods\":[]\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"Курьер\",\n" +
                                "            \"price\":150,\n" +
                                "            \"vat\":\"VAT_10\",\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ]\n," +
                                "            \"paymentMethods\":[]\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"POST\",\n" +
                                "            \"serviceName\":\"Почта\",\n" +
                                "            \"price\":100,\n" +
                                "            \"vat\":\"VAT_10\",\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ]\n," +
                                "            \"paymentMethods\":[]\n" +
                                "         }\n" +
                                "      ],\n" +
                                "      \"paymentMethods\":[\n" +
                                "         \"YANDEX\",\n" +
                                "         \"CASH_ON_DELIVERY\",\n" +
                                "         \"CARD_ON_DELIVERY\"\n" +
                                "      ]\n," +
                                "      \"taxSystem\": \"OSN\"" +
                                "   }\n" +
                                "}", true
                ));
    }


    @Test
    public void returnsCountOfItemsWhenHasItemsInInventoryFileWhenGenerateDataIsInventoryJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_INVENTORY;

        mockSvn(svnMock, shopId);

        String today = CHECKOUT_DATE_FORMAT.formatShort(new Date());
        String tomorrow = CHECKOUT_DATE_FORMAT.formatShort(DateUtils.addDays(new Date(), 1));
        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/cart", shopId)
                                .content(JSON_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("auth-token", "3A000001F854DDF2"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\n" +
                                "   \"cart\":{\n" +
                                "      \"items\":[\n" +
                                "         {\n" +
                                "            \"feedId\":383182,\n" +
                                "            \"offerId\":\"1\",\n" +
                                "            \"offerName\":\"Чайник электрический 100W\",\n" +
                                "            \"count\":2,\n" +
                                "            \"price\":250,\n" +
                                "            \"delivery\":true\n" +
                                "         }\n" +
                                "      ],\n" +
                                "      \"deliveryOptions\":[\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"VIP курьер\",\n" +
                                "            \"price\":250,\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ],\n" +
                                "            \"vat\": \"VAT_10\",\n" +
                                "            \"paymentMethods\": []\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"Premium курьер\",\n" +
                                "            \"price\":200,\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + tomorrow + "\",\n" +
                                "               \"toDate\":\"" + tomorrow + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ],\n" +
                                "            \"vat\": \"VAT_10\",\n" +
                                "            \"paymentMethods\": []\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"DELIVERY\",\n" +
                                "            \"serviceName\":\"Курьер\",\n" +
                                "            \"price\":150,\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ],\n" +
                                "            \"vat\": \"VAT_10\",\n" +
                                "            \"paymentMethods\": []\n" +
                                "         },\n" +
                                "         {\n" +
                                "            \"type\":\"POST\",\n" +
                                "            \"serviceName\":\"Почта\",\n" +
                                "            \"price\":100,\n" +
                                "            \"dates\":{\n" +
                                "               \"fromDate\":\"" + today + "\",\n" +
                                "               \"toDate\":\"" + today + "\"\n" +
                                "            },\n" +
                                "            \"validFeatures\":[\n" +
                                "               \"PLAINCPA\"\n" +
                                "            ],\n" +
                                "            \"vat\": \"VAT_10\",\n" +
                                "            \"paymentMethods\": []\n" +
                                "         }\n" +
                                "      ],\n" +
                                "      \"paymentMethods\":[\n" +
                                "         \"YANDEX\",\n" +
                                "         \"CASH_ON_DELIVERY\",\n" +
                                "         \"CARD_ON_DELIVERY\"\n" +
                                "      ],\n" +
                                "      \"taxSystem\": \"OSN\"" +
                                "   }\n" +
                                "}", true
                ));
    }

    @Test
    public void acceptsOrderWhenGenerateDataAndRandomOrderIdAreDisabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_XML;
        String currentDate = DATE_FORMAT.format(LocalDate.now());

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/order/accept", shopId)
                                .content(XML_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().xml("<order id=\"1234\" accepted=\"true\" shipment-date=" +
                        "\"" + currentDate + "\"/>"));
    }


    @Test
    public void acceptsOrderWhenGenerateDataDisabled() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_XML;

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/order/accept", shopId)
                                .content(XML_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_XML)
                                .param("auth-token", "EB00000195533612"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().xml("<order accepted=\"true\" id=\"73738\"/>"));
    }

    @Test
    public void acceptsOrderWhenGenerateDataAndRandomOrderIdAreDisabledJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_ENABLED_JSON;
        String currentDate = DATE_FORMAT.format(LocalDate.now());

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/order/accept", shopId)
                                .content(JSON_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n" +
                        "    \"order\": {\n" +
                        "        \"accepted\": true,\n" +
                        "        \"id\" : \"1234\",\n" +
                        "        \"shipmentDate\" : \"" +
                        currentDate +
                        "\"}\n" +
                        "}"));
    }


    @Test
    public void acceptsOrderWhenGenerateDataDisabledJson() throws Exception {
        long shopId = SHOP_ID_WITH_GENERATE_DATA_DISABLED_JSON;

        mockSvn(svnMock, shopId);

        var result = mockMvc.perform(
                        post("/svn-shop/{shopId}/order/accept", shopId)
                                .content(JSON_CONTENT_BODY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("auth-token", "57000001CE5625A6"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json("{\n" +
                        "    \"order\": {\n" +
                        "        \"accepted\": true,\n" +
                        "\t\t\"id\" : \"1234\"\n" +
                        "    }\n" +
                        "}"));
    }


    private static void mockSvn(Stubbing svnMock, long shopId) throws IOException {
        for (String resourceName : AVAILABLE_SVN_RESOURCES) {
            URL resourceUrl = getResourceUrl(shopId, resourceName);
            mockSvn(svnMock, resourceUrl, shopId, resourceName);
        }
    }

    private static void mockSvn(Stubbing svnMock, URL resource, long shopId, String resourceName) throws IOException {
        if (resource == null) {
            return;
        }
        svnMock.stubFor(get(urlPathEqualTo(String.format("/market/shops/test-util/%s/%s", shopId, resourceName)))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(IOUtils.toByteArray(resource))
                        .withHeader("Content-Type", "text/plain; charset=utf-8")));
    }

    private String getResourceContent(long shopId, String resourceName) throws IOException {
        String resourceContent = IOUtils.toString(
                getResourceUrl(shopId, resourceName),
                StandardCharsets.UTF_8);
        Properties resourceProperties = HttpResourceHelper.parseResourceWithBody(resourceContent);
        return resourceProperties.getProperty(HttpResourceHelper.BODY);
    }

    private static URL getResourceUrl(long shopId, String resourceName) {
        String localResourceName = String.format("/files/svn/%s/%s",
                RESOURCE_DIRECTORY_BY_SHOP_ID.get(shopId),
                resourceName);
        return SvnPushApiTest.class.getResource(localResourceName);
    }
}
