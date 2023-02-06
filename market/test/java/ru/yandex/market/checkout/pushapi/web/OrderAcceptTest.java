package ru.yandex.market.checkout.pushapi.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderAcceptHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.pushapi.service.shop.CheckouterShipmentDateCalculationRuleServiceTest;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;

public class OrderAcceptTest extends AbstractWebTestBase {

    private static final Logger logger = LoggerFactory.getLogger(OrderAcceptTest.class);

    @Autowired
    private PushApiOrderAcceptHelper orderAcceptHelper;
    @Autowired
    private ShopApiConfigurer shopApiConfigurer;
    @Autowired
    private WireMockServer shopadminStubMock;
    @Autowired
    private WireMockServer checkouterMock;

    @Test
    public void testOrderAccept() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();

        RequestContextHolder.createNewContext();

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();

        String body = request.getBodyAsString();
        logger.info("body: {}", body);

        byte[] bodyBytes = request.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        var order = objectMapper.readValue(bodyBytes, Map.class);

        Assertions.assertNull(JsonUtil.getByPath(order, "/order/buyer"));
        Assertions.assertNull(JsonUtil.getByPath(order, "/order/rgb"));
    }

    @Test
    public void testOrderAcceptRgb() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        parameters.setPartnerInterface(true);

        RequestContextHolder.createNewContext();

        String requestBody = IOUtils.readInputStream(CheckouterShipmentDateCalculationRuleServiceTest.class
                .getResourceAsStream("/files/shipmentDateCalcRules.json"));

        checkouterMock.givenThat(get(urlPathEqualTo("/shops/242103/shipment/date-calculation-rule"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(requestBody)
                        .withHeader("Content-Type", "application/json")));

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(0));
    }

    @Test
    public void testOrderPrerorder() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        parameters.getOrder().setPreorder(true);
        parameters.setPartnerInterface(true);

        RequestContextHolder.createNewContext();
        String requestBody = IOUtils.readInputStream(CheckouterShipmentDateCalculationRuleServiceTest.class
                .getResourceAsStream("/files/shipmentDateCalcRules.json"));

        checkouterMock.givenThat(get(urlPathEqualTo("/shops/242103/shipment/date-calculation-rule"))
                .willReturn(new ResponseDefinitionBuilder()
                        .withBody(requestBody)
                        .withHeader("Content-Type", "application/json")));

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(0));
    }

    @Test
    public void testEdaOrderAccept() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        Address address = AddressProvider.getAddress(addr -> {
            addr.setGps("11.111111111,22.222222222");
            addr.setType(AddressType.SHOP);
        });
        Delivery delivery = parameters.getOrder().getDelivery();
        delivery.setShopAddress(address);
        parameters.setDataType(DataType.JSON);

        RequestContextHolder.createNewContext();

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();

        String body = request.getBodyAsString();
        logger.info("body: {}", body);

        JsonPathUtils.jpath("$.order.delivery.address.lon")
                .assertValue(body, 11.111111);
        JsonPathUtils.jpath("$.order.delivery.address.lat")
                .assertValue(body, 22.222222);
        JsonPathUtils.jpath("$.order.items[0].warehouseId")
                .assertValue(body, MOCK_SORTING_CENTER_HARDCODED);
        JsonPathUtils.jpath("$.order.items[0].partnerWarehouseId")
                .assertValue(body, "super-sklad");
    }

    @ParameterizedTest
    @MethodSource("guidsTestData")
    public void testPrescriptionGuids(DataType dataType) throws Exception {
        final Set<String> PRESCRIPTION_GUIDS = Set.of("guid_1", "guid_2");

        PushApiOrderParameters parameters = new PushApiOrderParameters();
        parameters.setDataType(dataType);
        parameters.getOrder().getItems().forEach(item -> item.setPrescriptionGuids(PRESCRIPTION_GUIDS));

        RequestContextHolder.createNewContext();

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();

        verifyPrescriptionGuids(request, dataType, PRESCRIPTION_GUIDS);
    }

    public static Stream<Arguments> guidsTestData() {
        return Stream.of(DataType.JSON).map(Arguments::of);
    }

    private void verifyPrescriptionGuids(LoggedRequest request, DataType dataType, Set<String> expectedGuids)
            throws Exception {
        String body = request.getBodyAsString();
        logger.info("body: {}", body);

        switch (dataType) {
            case XML:
                byte[] bodyBytes = request.getBody();
                XpathUtils.xpath("/order/items/item[1]/guids/guid")
                        .assertNodeCount(bodyBytes, StandardCharsets.UTF_8.name(), 2);
                break;
            case JSON:
                net.minidev.json.JSONArray jsonArr =
                        (net.minidev.json.JSONArray) JsonPathUtils.jpath("$.order.items[0].guids")
                                .evaluateJsonPath(body);
                Assertions.assertNotNull(jsonArr);
                Set<String> foundGuids = new HashSet<>();

                for (Object value : jsonArr) {
                    @SuppressWarnings("unchecked")
                    String guid = new ArrayList<>(((LinkedHashMap<String, String>) value).values()).get(0);
                    foundGuids.add(guid);
                }
                Assertions.assertEquals(foundGuids, expectedGuids);
                break;
            default:
                Assertions.fail("Uncheckable DataType");
        }
    }

    @Test
    public void testOrderWithNotesAccept() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();
        parameters.getOrder().setNotes("Комментарий к заказу");
        parameters.getOrder().setContext(Context.SANDBOX);
        parameters.setDataType(DataType.JSON);

        RequestContextHolder.createNewContext();

        shopApiConfigurer.mockOrderResponse(parameters);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        LoggedRequest request = extractRequest();

        String body = request.getBodyAsString();
        logger.info("body: {}", body);

        JsonPathUtils.jpath("$.order.notes")
                .assertValue(body, "Комментарий к заказу");
    }

    @Test
    public void testOrderAcceptWithIncorrectContentTypeXML() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();

        RequestContextHolder.createNewContext();

        parameters.setDataType(DataType.XML);
        shopApiConfigurer.mockOrderResponse(parameters);

        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters, 422);
    }

    @Test
    public void testOrderAcceptWithIncorrectContentTypeJSON() throws Exception {
        PushApiOrderParameters parameters = new PushApiOrderParameters();

        RequestContextHolder.createNewContext();

        parameters.setDataType(DataType.UNKNOWN);
        shopApiConfigurer.mockOrderResponse(parameters);

        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);
    }

    private LoggedRequest extractRequest() {
        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(serveEvents);
        return event.getRequest();
    }
}
