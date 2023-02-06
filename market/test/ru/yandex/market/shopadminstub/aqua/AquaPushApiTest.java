package ru.yandex.market.shopadminstub.aqua;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.yandex.market.providers.CartRequestProvider;
import ru.yandex.market.providers.InventoryProvider;
import ru.yandex.market.providers.ItemProvider;
import ru.yandex.market.providers.OrderAcceptRequestProvider;
import ru.yandex.market.providers.PushApiShopConfigProvider;
import ru.yandex.market.shopadminstub.application.AbstractTestBase;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.GenerateDataMode;
import ru.yandex.market.shopadminstub.model.OrderAcceptRequest;
import ru.yandex.market.shopadminstub.model.PushApiShopConfig;
import ru.yandex.market.shopadminstub.model.inventory.Inventory;
import ru.yandex.market.shopadminstub.util.HttpResourceHelper;
import ru.yandex.market.util.TestSerializationService;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOption;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkDeliveryOptionsCount;
import static ru.yandex.market.shopadminstub.stub.StubPushApiTestUtils.checkItem;

public class AquaPushApiTest extends AbstractTestBase {
    private static final long DEFAULT_SHOP_ID = 13371337L;
    private static final long ANOTHER_SHOP_ID = 19411945;
    private static final String AQUA_URL_PREFIX = "/storage/get/ru/yandex/autotests/market/push/api/shop/";

    @Autowired
    private WireMockServer aquaMock;
    @Autowired
    private TestSerializationService testSerializationService;

    @Test
    public void postOrderStatus() throws Exception {
        mockMvc.perform(post("/aqua-shop/{shopId}/order/status", DEFAULT_SHOP_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void postOrderStatusNoGeneration() throws Exception {
        String orderStatusResponse = IOUtils.toString(
                getClass().getResourceAsStream("/files/svn/xml-generation-off/order_status.txt"),
                StandardCharsets.UTF_8
        );
        String body = orderStatusResponse.split(HttpResourceHelper.BODY + "=")[1];


        PushApiShopConfig config = PushApiShopConfigProvider.buildNoGenerationXML();
        mockGlobal(DEFAULT_SHOP_ID, config);
        mockOrderStatus(DEFAULT_SHOP_ID, orderStatusResponse);

        mockMvc.perform(post("/aqua-shop/{shopId}/order/shipment/status", DEFAULT_SHOP_ID)
                .param("auth-token", config.getToken()))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().xml(body));
    }

    @Test
    public void postOrderShipmentStatus() throws Exception {
        mockGlobal(DEFAULT_SHOP_ID, PushApiShopConfigProvider.buildGenerateXML());

        mockMvc.perform(post("/aqua-shop/{shopId}/order/shipment/status", DEFAULT_SHOP_ID))
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void postCart() throws Exception {
        mockGlobal(DEFAULT_SHOP_ID, PushApiShopConfigProvider.buildGenerateXML());

        CartRequest cartRequest = CartRequestProvider.buildCartRequest();

        ResultActions resultActions = mockMvc.perform(post("/aqua-shop/{shopId}/cart", DEFAULT_SHOP_ID)
                .content(testSerializationService.serializeXml(cartRequest))
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk());

        LocalDate now = LocalDate.now();

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);

        checkDeliveryOptionsCount(resultActions, 3);
        checkDeliveryOption(resultActions, now, 1, Collections.emptyList(), "Почта России PICKUP", "PICKUP", "0", 0, 7);
        checkDeliveryOption(resultActions, now, 2, Collections.emptyList(), "Почта России POST", "POST", "250", 0, 7);
        checkDeliveryOption(resultActions, now, 3, Collections.emptyList(), "Почта России DELIVERY", "DELIVERY", "350", 0, 7);
    }

    @Test
    public void postCartNoGeneration() throws Exception {
        String cartResponse = IOUtils.toString(
                getClass().getResourceAsStream("/files/svn/xml-generation-off/cart.txt"),
                StandardCharsets.UTF_8);
        String body = cartResponse.split(HttpResourceHelper.BODY + "=")[1];

        PushApiShopConfig config = PushApiShopConfigProvider.buildNoGenerationXML();
        mockGlobal(DEFAULT_SHOP_ID, config);
        mockCartResponse(DEFAULT_SHOP_ID, cartResponse);

        mockMvc.perform(post("/aqua-shop/{shopId}/cart", DEFAULT_SHOP_ID)
                .param("auth-token", config.getToken())
                .content(cartResponse)
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().xml(body));

    }

    @Test
    public void postCartInventory() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();

        PushApiShopConfig config = PushApiShopConfigProvider.buildInventoryXML();
        mockGlobal(DEFAULT_SHOP_ID, config);
        mockInventory(DEFAULT_SHOP_ID, InventoryProvider.buildInventory(cartRequest));

        ResultActions resultActions = mockMvc.perform(post("/aqua-shop/{shopId}/cart", DEFAULT_SHOP_ID)
                .param("auth-token", config.getToken())
                .content(testSerializationService.serializeXml(cartRequest))
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk());

        checkItem(resultActions, 1, ItemProvider.DEFAULT_FEED_ID, ItemProvider.DEFAULT_OFFER_ID);

        LocalDate today = LocalDate.now();
        checkDeliveryOptionsCount(resultActions, 4);
        checkDeliveryOption(resultActions, today, 1, Collections.emptyList(), "Шнырь-курьер", "DELIVERY", "250", 0, 0);
        checkDeliveryOption(resultActions, today, 2, Collections.emptyList(), "Premium курьер", "DELIVERY", "200", 1, 1);
        checkDeliveryOption(resultActions, today, 3, Collections.emptyList(), "Курьер", "DELIVERY", "150", 0, 0);
        checkDeliveryOption(resultActions, today, 4, Collections.emptyList(), "Почта", "POST", "100", 0, 0);
    }

    // TODO: раскомментить, когда починим ru.yandex.market.shopadminstub.controller.errors.ErrorAwareController.methodNotFoundHandler()
    @Disabled
    @Test
    public void shouldReturn404IfInventoryNotFound() throws Exception {
        CartRequest cartRequest = CartRequestProvider.buildCartRequest();

        PushApiShopConfig config = PushApiShopConfigProvider.buildInventoryXML();
        mockGlobal(ANOTHER_SHOP_ID, config);

        mockMvc.perform(post("/aqua-shop/{shopId}/cart", DEFAULT_SHOP_ID)
                .param("auth-token", config.getToken())
                .content(testSerializationService.serializeXml(cartRequest))
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isNotFound());
    }

    @Test
    public void postOrderAccept() throws Exception {
        mockGlobal(DEFAULT_SHOP_ID, PushApiShopConfigProvider.buildGenerateXML());

        OrderAcceptRequest orderAcceptRequest = OrderAcceptRequestProvider.buildOrderAcceptRequest();

        mockMvc.perform(post("/aqua-shop/{shopId}/order/accept", DEFAULT_SHOP_ID)
                .content(testSerializationService.serializeXml(orderAcceptRequest))
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(xpath("/order/@accepted").booleanValue(true))
                .andExpect(xpath("/order/@id").exists());
    }

    @Test
    public void postOrderAcceptNoGeneration() throws Exception {
        String orderAcceptResponse = IOUtils.toString(
                getClass().getResourceAsStream("/files/svn/xml-generation-off/order_accept.txt"),
                StandardCharsets.UTF_8);
        String body = orderAcceptResponse.split(HttpResourceHelper.BODY + "=")[1];

        PushApiShopConfig config = PushApiShopConfigProvider.buildNoGenerationXML();
        mockGlobal(DEFAULT_SHOP_ID, config);
        mockOrderAccept(DEFAULT_SHOP_ID, orderAcceptResponse);

        OrderAcceptRequest orderAcceptRequest = OrderAcceptRequestProvider.buildOrderAcceptRequest();

        mockMvc.perform(post("/aqua-shop/{shopId}/order/accept", DEFAULT_SHOP_ID)
                .param("auth-token", config.getToken())
                .content(testSerializationService.serializeXml(orderAcceptRequest))
                .contentType(MediaType.APPLICATION_XML))
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(content().xml(body));
    }


    private void mockGlobal(long shopId, PushApiShopConfig config) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("generate-data", toKey(config.getGenerateDataMode()));
        properties.setProperty("random-order-id", config.isRandomOrder() ? "true" : "");
        properties.setProperty("timeout", String.valueOf(config.getTimeout()));
        properties.setProperty("token", config.getToken());
        properties.setProperty("data-type", config.getDataType().name());
        properties.setProperty("auth-type", config.getAuthType().name());
        properties.setProperty("price", config.getPrice());
        properties.setProperty("shop-id", config.getShopId() == null ? "" : String.valueOf(config.getShopId()));
        properties.setProperty("fastdelivery", config.isFastDelivery() ? "true" : "");

        StringWriter stringWriter = new StringWriter();
        properties.store(stringWriter, null);

        aquaMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(AQUA_URL_PREFIX + shopId + "/config"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(stringWriter.toString())));
    }

    private void mockCartResponse(long shopId, String cartResponse) {
        mockAquaResponse(shopId, cartResponse, "/cart");
    }

    private void mockOrderAccept(long shopId, String orderAcceptResponse) {
        mockAquaResponse(shopId, orderAcceptResponse, "/order/accept");
    }

    private void mockOrderStatus(long shopId, String orderStatusResponse) {
        mockAquaResponse(shopId, orderStatusResponse, "/order/status");
    }

    private void mockAquaResponse(long shopId, String cartResponse, String resource) {
        aquaMock.stubFor(WireMock.get(WireMock.urlPathEqualTo(AQUA_URL_PREFIX + shopId + resource))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(cartResponse)
                        .withHeaders(new HttpHeaders(new ContentTypeHeader(MediaType.APPLICATION_XML_VALUE)))));
    }

    private void mockInventory(long shopId, Inventory inventory) {
        mockAquaResponse(shopId, testSerializationService.serializeJson(inventory), "/inventory");
    }

    private static String toKey(GenerateDataMode mode) {
        switch (mode) {
            case OFF:
                return "off";
            case AUTO:
                return "ok";
            case INVENTORY:
                return "inventory";
            default:
                throw new IllegalArgumentException("mode: " + mode);
        }
    }
}
