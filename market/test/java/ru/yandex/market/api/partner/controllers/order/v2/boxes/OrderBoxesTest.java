package ru.yandex.market.api.partner.controllers.order.v2.boxes;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.logistics.logistics4shops.client.exception.Logistics4ShopsClientException;
import ru.yandex.market.logistics4shops.client.api.OrderBoxApi;
import ru.yandex.market.logistics4shops.client.model.OrderBox;
import ru.yandex.market.logistics4shops.client.model.OrderBoxItem;
import ru.yandex.market.logistics4shops.client.model.OrderBoxesDto;
import ru.yandex.market.logistics4shops.client.model.OrderBoxesRequestDto;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тесты для "/campaigns/{campaignId}/orders/{orderId}/delivery/parcels/{parceId}/boxes"
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class OrderBoxesTest extends OrderControllerV2TestTemplate {

    private static final String PACKAGING_ORDER_PATH = "mocks/checkouter/processing-packaging-order.json";
    private static final String ANY_VALID_ORDER_PATH = PACKAGING_ORDER_PATH;
    private static final String READY_TO_SHIP_ORDER_PATH = "mocks/checkouter/processing-ready.to.ship-order.json";
    private static final String SHIPPED_ORDER_PATH = "mocks/checkouter/processing-shipped-order.json";
    private static final String USER_RECEIVED_ORDER_PATH = "mocks/checkouter/delivery-user.received-order.json";
    private static final String DELIVERED_USER_RECEIVED_ORDER_PATH =
            "mocks/checkouter/delivered-user.received-order.json";

    @Autowired
    protected OrderBoxApi logistics4shops;

    @Test
    void putParcelBoxesJson() {
        prepareCheckouterMock();
        String requestBody = putBoxesRequest();

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        String expected = putBoxesExpectedResponse();

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertJsonEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putZeroParcelBoxesJson() {
        HttpClientErrorException.BadRequest badRequest = Assertions.assertThrows(
                HttpClientErrorException.BadRequest.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                        HttpMethod.PUT, "{\"boxes\":[]}", String.class, MediaType.APPLICATION_JSON
                )
        );
        MbiAsserts.assertJsonEquals(
                "{" +
                        "\"status\":\"ERROR\"," +
                        "\"errors\":[" +
                        "{\"code\":\"BAD_REQUEST\",\"message\":\"boxes must not be empty (rejected value: [])\"}" +
                        "]" +
                        "}",
                badRequest.getResponseBodyAsString()
        );
    }

    @DisplayName("Сохранение коробок в logistics4shops")
    @Test
    void putParcelBoxesJsonToL4s() {
        String requestBody = putBoxesRequest();

        prepareGetOrderMock(668L, ANY_VALID_ORDER_PATH);
        prepareL4sMock();
        environmentService.setValue("partner-api.send.boxes.to.l4s", "true");

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        String expected = putBoxesExpectedResponse();

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertJsonEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).getOrder(any(), any());
        verify(checkouterAPI, never()).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
        environmentService.setValue("partner-api.send.boxes.to.l4s", "false");
    }

    @ParameterizedTest
    @DisplayName("Обработка ошибок из logistics4shops при сохранении коробок JSON")
    @MethodSource("errorsFromL4S")
    void putParcelBoxesJsonToL4SErrorHandling(int statusCode, String errorMessage, String expectedCode,
                                              String expectedMessage) {
        String requestBody = putBoxesRequest();

        prepareGetOrderMock(668L, ANY_VALID_ORDER_PATH);
        prepareL4SMockWithError(statusCode, errorMessage);
        environmentService.setValue("partner-api.send.boxes.to.l4s", "true");

        HttpStatusCodeException exception = Assertions.assertThrows(
                HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON));

        HttpStatusCodeException shipmentException = Assertions.assertThrows(
                HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON));

        //language=json
        String expected = "{\n" +
                "  \"status\":\"ERROR\",\n" +
                "  \"error\":{\n" +
                "    \"code\":" + statusCode + ",\n" +
                "    \"message\":\"" + expectedMessage + "\"\n" +
                "  },\n" +
                "  \"errors\":[\n" +
                "    {\n" +
                "      \"code\":\"" + expectedCode + "\",\n" +
                "      \"message\":\"" + expectedMessage + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertThat(exception, hasErrorCode(HttpStatus.valueOf(expectedCode)));
        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());

        assertThat(shipmentException, hasErrorCode(HttpStatus.valueOf(expectedCode)));
        MbiAsserts.assertJsonEquals(expected, shipmentException.getResponseBodyAsString());

        verify(checkouterAPI, times(2)).getOrder(any(), any());
        verify(checkouterAPI, never()).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
        environmentService.setValue("partner-api.send.boxes.to.l4s", "false");
    }

    @ParameterizedTest
    @DisplayName("Обработка ошибок из logistics4shops при сохранении коробок XML")
    @MethodSource("errorsFromL4S")
    void putParcelBoxesXmlToL4SErrorHandling(int statusCode, String errorMessage, String expectedCode,
                                             String expectedMessage) {
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"id1\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" fulfilment-id=\"id2\">\n" +
                "            <items>\n" +
                "                <item id=\"2\" count=\"3\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        prepareGetOrderMock(668L, ANY_VALID_ORDER_PATH);
        prepareL4SMockWithError(statusCode, errorMessage);
        environmentService.setValue("partner-api.send.boxes.to.l4s", "true");

        HttpStatusCodeException exception = Assertions.assertThrows(
                HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML));

        HttpStatusCodeException shipmentException = Assertions.assertThrows(
                HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML));

        //language=xml
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<response>\n" +
                "    <status>ERROR</status>\n" +
                "    <error code=\"" + statusCode + "\">\n" +
                "        <message>" + expectedMessage + "</message>\n" +
                "    </error>\n" +
                "    <errors>\n" +
                "        <error code=\"" + expectedCode + "\" message=\"" + expectedMessage + "\"/>\n" +
                "    </errors>\n" +
                "</response>";

        assertThat(exception, hasErrorCode(HttpStatus.valueOf(expectedCode)));
        MbiAsserts.assertXmlEquals(expected, exception.getResponseBodyAsString(), MbiAsserts.IGNORE_ORDER);

        assertThat(shipmentException, hasErrorCode(HttpStatus.valueOf(expectedCode)));
        MbiAsserts.assertXmlEquals(expected, shipmentException.getResponseBodyAsString(), MbiAsserts.IGNORE_ORDER);

        verify(checkouterAPI, times(2)).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
        verify(checkouterAPI, never()).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
        environmentService.setValue("partner-api.send.boxes.to.l4s", "false");
    }

    @Test
    void putParcelBoxesXML() {
        prepareCheckouterMock();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"id1\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" fulfilment-id=\"id2\">\n" +
                "            <items>\n" +
                "                <item id=\"2\" count=\"3\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        //language=xml
        String expected = "<response>\n" +
                "    <status>OK</status>\n" +
                "    <result>\n" +
                "        <boxes>\n" +
                "            <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" " +
                "fulfilment-id=\"id1\">\n" +
                "                <items>\n" +
                "                    <item id=\"1\" count=\"2\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "            <box id=\"2\" weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" " +
                "fulfilment-id=\"id2\">\n" +
                "                <items>\n" +
                "                    <item id=\"2\" count=\"3\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "        </boxes>\n" +
                "    </result>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertXmlEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @ParameterizedTest
    @MethodSource("invalidOrders")
    void rejectPutParcelBoxesForOrderInInvalidState(long campaignId, long clientId, String orderPath,
                                                    String errorMessage) {
        prepareGetOrderMock(clientId, orderPath);

        String requestBody = putBoxesRequest();

        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/" + campaignId + "/orders/1/delivery/parcels/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON))
                .satisfies(
                        badRequest -> org.assertj.core.api.Assertions.assertThat(badRequest.getResponseBodyAsString())
                                .isEqualTo("{\"status\":\"ERROR\"," +
                                        "\"errors\":[{\"code\":\"BAD_REQUEST\"," +
                                        "\"message\":\"" + errorMessage + "\"}]}"));

        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/" + campaignId + "/orders/1/delivery/shipments/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON))
                .satisfies(
                        badRequest -> org.assertj.core.api.Assertions.assertThat(badRequest.getResponseBodyAsString())
                                .isEqualTo("{\"status\":\"ERROR\"," +
                                        "\"errors\":[{\"code\":\"BAD_REQUEST\"," +
                                        "\"message\":\"" + errorMessage + "\"}]}"));

        verify(checkouterAPI, times(2)).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
        verify(checkouterAPI, never()).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @ParameterizedTest
    @MethodSource("validOrders")
    void acceptPutParcelBoxesForOrderInValidState(long campaignId, long clientId, String orderPath) {
        prepareCheckouterMock(
                checkouterMockRequest(),
                checkouterBoxesResponse(),
                clientId,
                orderPath
        );

        String requestBody = putBoxesRequest();

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/" + campaignId + "/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/" + campaignId + "/orders/1/delivery/shipments/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        //language=json
        String expected = putBoxesExpectedResponse();

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertJsonEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putEmptyParcel() {
        //language=json
        String requestBody = "{}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON));

        HttpClientErrorException shipmentException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), shipmentException.getRawStatusCode());

        JsonTestUtil.assertEquals(
                exception.getResponseBodyAsString(),
                "{\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"BAD_REQUEST\",\n" +
                        "      \"message\": \"boxes must not be empty (rejected value: null)\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        );

        JsonTestUtil.assertEquals(
                shipmentException.getResponseBodyAsString(),
                "{\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"BAD_REQUEST\",\n" +
                        "      \"message\": \"boxes must not be empty (rejected value: null)\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        );

        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    void putParcelBoxesExpectedNoFilledFulfilmentId() {
        prepareCheckouterMockWithoutFFIdResponse();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"id1\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" fulfilment-id=\"id2\">\n" +
                "            <items>\n" +
                "                <item id=\"2\" count=\"3\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML));

        //language=xml
        String expected = "<response>\n" +
                "    <error code=\"400\">\n" +
                "        <message>Expected filled fulfilmentId when more than one box</message>\n" +
                "    </error>\n" +
                "    <errors>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"Expected filled fulfilmentId when more than one " +
                "box\"/>\n" +
                "    </errors>\n" +
                "    <status>ERROR</status>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER);
    }

    @Test
    @DisplayName("Проверка передачи информации о грузовых местах без ВГХ")
    void putParcelBoxesWithoutWeightDimensions() {
        //language=json
        String requestBody = "{" +
                "  \"boxes\": [" +
                "    {" +
                "      \"fulfilmentId\": \"id1\"," +
                "      \"items\": [" +
                "        {" +
                "          \"id\": 1," +
                "          \"count\": 2" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";
        //language=json
        String expected = "{" +
                "  \"status\": \"OK\"," +
                "  \"result\": {" +
                "    \"boxes\": [" +
                "      {" +
                "        \"id\": 1," +
                "        \"fulfilmentId\": \"id1\"," +
                "        \"items\": [" +
                "          {" +
                "            \"id\": 1," +
                "            \"count\": 2" +
                "          }" +
                "        ]" +
                "      }" +
                "    ]" +
                "  }" +
                "}";
        //language=json
        prepareCheckouterMock("" +
                        "{" +
                        "  \"boxes\": [" +
                        "    {" +
                        "      \"fulfilmentId\": \"id1\"," +
                        "      \"items\": [" +
                        "        {" +
                        "          \"itemId\": 1," +
                        "          \"count\": 2" +
                        "        }" +
                        "      ]" +
                        "    }" +
                        "  ]" +
                        "}", "" +
                        "{" +
                        "  \"boxes\": [" +
                        "    {" +
                        "     \"id\": 1," +
                        "      \"fulfilmentId\": \"id1\"," +
                        "      \"items\": [" +
                        "        {" +
                        "          \"itemId\": 1," +
                        "          \"count\": 2" +
                        "        }" +
                        "      ]" +
                        "    }" +
                        "  ]" +
                        "}",
                668);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );
    }

    @Test
    @DisplayName("Проверка передачи информации о грузовых местах без ВГХ (DBS)")
    void putParcelBoxesWithoutWeightDimensionsDBS() {
        //language=json
        String requestBody = "{" +
                "  \"boxes\": [" +
                "    {" +
                "      \"fulfilmentId\": \"id1\"," +
                "      \"items\": [" +
                "        {" +
                "          \"id\": 1," +
                "          \"count\": 2" +
                "        }" +
                "      ]" +
                "    }" +
                "  ]" +
                "}";
        //language=json
        String expected = "{" +
                "  \"status\": \"OK\"," +
                "  \"result\": {" +
                "    \"boxes\": [" +
                "      {" +
                "        \"id\": 1," +
                "        \"fulfilmentId\": \"id1\"," +
                "        \"items\": [" +
                "          {" +
                "            \"id\": 1," +
                "            \"count\": 2" +
                "          }" +
                "        ]" +
                "      }" +
                "    ]" +
                "  }" +
                "}";
        //language=json
        prepareCheckouterMock("" +
                        "{" +
                        "  \"boxes\": [" +
                        "    {" +
                        "      \"fulfilmentId\": \"id1\"," +
                        "      \"items\": [" +
                        "        {" +
                        "          \"itemId\": 1," +
                        "          \"count\": 2" +
                        "        }" +
                        "      ]" +
                        "    }" +
                        "  ]" +
                        "}", "" +
                        "{" +
                        "  \"boxes\": [" +
                        "    {" +
                        "     \"id\": 1," +
                        "      \"fulfilmentId\": \"id1\"," +
                        "      \"items\": [" +
                        "        {" +
                        "          \"itemId\": 1," +
                        "          \"count\": 2" +
                        "        }" +
                        "      ]" +
                        "    }" +
                        "  ]" +
                        "}",
                2001);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );
    }

    @Test
    void putParcelBoxesXMLInvalid() {
        prepareCheckouterMock();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"-1000\" width=\"-11\" height=\"-12\" depth=\"-13\">\n" +
                "            <items>\n" +
                "                <item id=\"-1\" count=\"-2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML));

        HttpClientErrorException shipmentException = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML));

        //language=xml
        String expected = "<response>\n" +
                "    <status>ERROR</status>\n" +
                "    <errors>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].weight must be greater than 0 (rejected " +
                "value: -1000)\"/>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].width must be greater than 0 (rejected value:" +
                " -11)\"/>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].height must be greater than 0 (rejected " +
                "value: -12)\"/>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].depth must be greater than 0 (rejected value:" +
                " -13)\"/>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].items[0].id must be greater than 0 (rejected " +
                "value: -1)\"/>\n" +
                "        <error code=\"BAD_REQUEST\" message=\"boxes[0].items[0].count must be greater than 0 " +
                "(rejected value: -2)\"/>\n" +
                "    </errors>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER);

        MbiAsserts.assertXmlEquals(expected,
                shipmentException.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER);

        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    void testNullFulfilmentIdWithOneBoxGenerateFulfilmentId() {
        prepareCheckouterMockOneBox();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"1\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);
        //language=xml
        String expected = "<response>\n" +
                "    <status>OK</status>\n" +
                "    <result>\n" +
                "        <boxes>\n" +
                "            <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" " +
                "fulfilment-id=\"1-1\">\n" +
                "                <items>\n" +
                "                    <item id=\"1\" count=\"1\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "        </boxes>\n" +
                "    </result>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertXmlEquals(
                expected,
                shipmentResponse.getBody()
        );
        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void testNullFulfilmentIdWithSeveralBoxes() {
        prepareCheckouterMock();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\">\n" +
                "            <items>\n" +
                "                <item id=\"2\" count=\"3\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        Assertions.assertThrows(HttpServerErrorException.class, () -> FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML));

        verify(checkouterAPI, times(1)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void testEmptyParcelItemsJson() {
        prepareCheckouterMockEmptyItems();
        //language=json
        String requestBody =
                "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"id1\"" +
                        "    },\n" +
                        "    {\n" +
                        "      \"weight\": 1001,\n" +
                        "      \"width\": 21,\n" +
                        "      \"height\": 22,\n" +
                        "      \"depth\": 23,\n" +
                        "      \"fulfilmentId\": \"id2\"," +
                        "      \"items\": []\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        ResponseEntity<String> response =
                FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON);

        //language=json
        String expected =
                "{\n" +
                        "    \"status\": \"OK\",\n" +
                        "    \"result\":\n" +
                        "    {\n" +
                        "        \"boxes\": [\n" +
                        "        {\n" +
                        "            \"id\": 1,\n" +
                        "            \"weight\": 1000,\n" +
                        "            \"width\": 11,\n" +
                        "            \"height\": 12,\n" +
                        "            \"depth\": 13,\n" +
                        "            \"fulfilmentId\": \"id1\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"id\": 2,\n" +
                        "            \"weight\": 1001,\n" +
                        "            \"width\": 21,\n" +
                        "            \"height\": 22,\n" +
                        "            \"depth\": 23,\n" +
                        "            \"fulfilmentId\": \"id2\"\n" +
                        "        }]\n" +
                        "    }\n" +
                        "}";

        MbiAsserts.assertJsonEquals(expected, response.getBody());

        verify(checkouterAPI, times(1)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void testEmptyParcelItemsXml() {
        prepareCheckouterMockEmptyItems();
        //language=xml
        String requestBody =
                "<parcel>\n" +
                        "    <boxes>\n" +
                        "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" " +
                        "fulfilment-id=\"id1\">\n" +
                        "            <items/>\n" +
                        "        </box>\n" +
                        "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" " +
                        "fulfilment-id=\"id2\">\n" +
                        "            <items/>\n" +
                        "        </box>\n" +
                        "    </boxes>\n" +
                        "</parcel>";

        ResponseEntity<String> response =
                FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML);

        //language=xml
        String expected =
                "<response>\n" +
                        "    <status>OK</status>\n" +
                        "    <result>\n" +
                        "        <boxes>\n" +
                        "            <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" " +
                        "fulfilment-id=\"id1\" />\n" +
                        "            <box id=\"2\" weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" " +
                        "fulfilment-id=\"id2\" />\n" +
                        "        </boxes>\n" +
                        "    </result>\n" +
                        "</response>";

        MbiAsserts.assertXmlEquals(expected, response.getBody(), MbiAsserts.IGNORE_ORDER);

        verify(checkouterAPI, times(1)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putParcelBoxesJsonShouldCompleteIfFulfilmentIdContainsNonBreakingSpacesOrQuotationMarks() {
        prepareCheckouterMock();
        //language=json
        String requestBody = "{\n" +
                "  \"boxes\": [\n" +
                "    {\n" +
                "      \"weight\": 1000,\n" +
                "      \"width\": 11,\n" +
                "      \"height\": 12,\n" +
                "      \"depth\": 13,\n" +
                "      \"fulfilmentId\": \"i\\\"d\u00A01\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"id\": 1,\n" +
                "          \"count\": 2\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"weight\": 1001,\n" +
                "      \"width\": 21,\n" +
                "      \"height\": 22,\n" +
                "      \"depth\": 23,\n" +
                "      \"fulfilmentId\": \"\u2007id\u20072\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"id\": 2,\n" +
                "          \"count\": 3\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_JSON);

        //language=json
        String expected = "{\n" +
                "  \"status\": \"OK\",\n" +
                "  \"result\": {\n" +
                "    \"boxes\": [\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"weight\": 1000,\n" +
                "        \"width\": 11,\n" +
                "        \"height\": 12,\n" +
                "        \"depth\": 13,\n" +
                "        \"fulfilmentId\": \"id1\"," +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": 1,\n" +
                "            \"count\": 2\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"weight\": 1001,\n" +
                "        \"width\": 21,\n" +
                "        \"height\": 22,\n" +
                "        \"depth\": 23,\n" +
                "        \"fulfilmentId\": \"id2\"," +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": 2,\n" +
                "            \"count\": 3\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertJsonEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putParcelBoxesXMLShouldCompleteIfFulfilmentIdContainsNonBreakingSpacesOrQuotationMarks() {
        prepareCheckouterMock();
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"&quot;" +
                "id\u00A01\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "        <box weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" " +
                "fulfilment-id=\"\u2007id\u20072\">\n" +
                "            <items>\n" +
                "                <item id=\"2\" count=\"3\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        //language=xml
        String expected = "<response>\n" +
                "    <status>OK</status>\n" +
                "    <result>\n" +
                "        <boxes>\n" +
                "            <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" " +
                "fulfilment-id=\"id1\">\n" +
                "                <items>\n" +
                "                    <item id=\"1\" count=\"2\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "            <box id=\"2\" weight=\"1001\" width=\"21\" height=\"22\" depth=\"23\" " +
                "fulfilment-id=\"id2\">\n" +
                "                <items>\n" +
                "                    <item id=\"2\" count=\"3\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "        </boxes>\n" +
                "    </result>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertXmlEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putParcelBoxesXMLShouldCompleteIfFulfilmentIdContainsSpaceAndDash() {
        //language=xml
        String requestBody = "<parcel>\n" +
                "    <boxes>\n" +
                "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"it is id-1\">\n" +
                "            <items>\n" +
                "                <item id=\"1\" count=\"2\"/>\n" +
                "            </items>\n" +
                "        </box>\n" +
                "    </boxes>\n" +
                "</parcel>";

        prepareCheckouterMock("" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"it is id-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"itemId\": 1,\n" +
                        "          \"count\": 2\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n", "" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"it is id-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"count\": 2,\n" +
                        "          \"itemId\": 1\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                668);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        //language=xml
        String expected = "<response>\n" +
                "    <status>OK</status>\n" +
                "    <result>\n" +
                "        <boxes>\n" +
                "            <box id=\"1\" weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"it" +
                " is id-1\">\n" +
                "                <items>\n" +
                "                    <item id=\"1\" count=\"2\"/>\n" +
                "                </items>\n" +
                "            </box>\n" +
                "        </boxes>\n" +
                "    </result>\n" +
                "</response>";

        MbiAsserts.assertXmlEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertXmlEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void putParcelBoxesJsonShouldCompleteIfFulfilmentIdContainsSpaceAndDash() {
        //language=json
        String requestBody = "{\n" +
                "  \"boxes\": [\n" +
                "    {\n" +
                "      \"weight\": 1000,\n" +
                "      \"width\": 11,\n" +
                "      \"height\": 12,\n" +
                "      \"depth\": 13,\n" +
                "      \"fulfilmentId\": \"it is id-1\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"id\": 1,\n" +
                "          \"count\": 2\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
        prepareCheckouterMock("" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"it is id-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"itemId\": 1,\n" +
                        "          \"count\": 2\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n", "" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"it is id-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"count\": 2,\n" +
                        "          \"itemId\": 1\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                668);

        ResponseEntity<String> response = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/parcels/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        ResponseEntity<String> shipmentResponse = FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                HttpMethod.PUT,
                requestBody,
                String.class,
                MediaType.APPLICATION_XML);

        //language=json
        String expected = "{\n" +
                "  \"status\": \"OK\",\n" +
                "  \"result\": {\n" +
                "    \"boxes\": [\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"weight\": 1000,\n" +
                "        \"width\": 11,\n" +
                "        \"height\": 12,\n" +
                "        \"depth\": 13,\n" +
                "        \"fulfilmentId\": \"it is id-1\"," +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": 1,\n" +
                "            \"count\": 2\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        MbiAsserts.assertJsonEquals(
                expected,
                response.getBody()
        );

        MbiAsserts.assertJsonEquals(
                expected,
                shipmentResponse.getBody()
        );

        verify(checkouterAPI, times(2)).putParcelBoxes(anyLong(), anyLong(), any(), any(RequestClientInfo.class));
    }

    @Test
    void testFulfilmentIdPatternJsonException() {
        //language=json
        String requestBody =
                "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"id1 упс\"," +
                        "        \"items\": [\n" +
                        "          {\n" +
                        "            \"id\": 1,\n" +
                        "            \"count\": 2\n" +
                        "          }\n" +
                        "        ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.json",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_JSON)
        );

        //language=json
        String expected =
                "{" +
                        "\"status\":\"ERROR\"," +
                        "\"errors\":[" +
                        "   {" +
                        "     \"code\":\"BAD_REQUEST\"," +
                        "     \"message\":\"boxes[0].fulfilmentId must match \\\"^[\\\\p{Alnum}- ]*$\\\" (rejected " +
                        "value: id1 упс)\"" +
                        "   }" +
                        "  ]" +
                        "}";

        MbiAsserts.assertJsonEquals(expected, exception.getResponseBodyAsString());
    }

    @Test
    void testFulfilmentIdPatternXmlException() {
        //language=xml
        String requestBody =
                "<parcel>\n" +
                        "    <boxes>\n" +
                        "        <box weight=\"1000\" width=\"11\" height=\"12\" depth=\"13\" fulfilment-id=\"id1 " +
                        "упс\">\n" +
                        "                <items>\n" +
                        "                    <item id=\"1\" count=\"2\"/>\n" +
                        "                </items>\n" +
                        "        </box>\n" +
                        "    </boxes>\n" +
                        "</parcel>";

        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequestWithContentType(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/shipments/1/boxes.xml",
                        HttpMethod.PUT,
                        requestBody,
                        String.class,
                        MediaType.APPLICATION_XML)
        );

        //language=xml
        String expected =
                "<response>\n" +
                        "    <status>ERROR</status>\n" +
                        "    <errors>\n" +
                        "        <error code=\"BAD_REQUEST\" message=\"boxes[0].fulfilmentId must match &quot;" +
                        "^[\\p{Alnum}- ]*$&quot; (rejected value: id1 упс)\"/>\n" +
                        "    </errors>\n" +
                        "</response>";

        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    static Stream<Arguments> errorsFromL4S() {
        return Stream.of(
                Arguments.of(400, "Invalid request. Validations: [...]", "BAD_REQUEST",
                        "Invalid request. Validations: [...]"),
                Arguments.of(404, "Failed to find [ORDER] with id [1]", "NOT_FOUND",
                        "Failed to find [ORDER] with id [1]"),
                Arguments.of(500, "java.lang.NullPointerException", "INTERNAL_SERVER_ERROR",
                        "Internal server error")
        );
    }

    static Stream<Arguments> invalidOrders() {
        return Stream.of(
                //FBS
                Arguments.of(10668, 668, SHIPPED_ORDER_PATH,
                        "You could not change boxes for order 1 in this status PROCESSING (SHIPPED)"),
                Arguments.of(10668, 668, USER_RECEIVED_ORDER_PATH,
                        "You could not change boxes for order 1 in this status DELIVERY (USER_RECEIVED)"),
                Arguments.of(10668, 668, DELIVERED_USER_RECEIVED_ORDER_PATH,
                        "You could not change boxes for order 1 in this status DELIVERED (DELIVERED_USER_RECEIVED)"),
                //DBS
                Arguments.of(20001, 2001, DELIVERED_USER_RECEIVED_ORDER_PATH,
                        "You could not change boxes for order 1 in this status DELIVERED (DELIVERED_USER_RECEIVED)")
        );
    }

    static Stream<Arguments> validOrders() {
        return Stream.of(
                //FBS
                Arguments.of(10668, 668, PACKAGING_ORDER_PATH),
                Arguments.of(10668, 668, READY_TO_SHIP_ORDER_PATH),
                //DBS
                Arguments.of(20001, 2001, PACKAGING_ORDER_PATH),
                Arguments.of(20001, 2001, READY_TO_SHIP_ORDER_PATH),
                Arguments.of(20001, 2001, SHIPPED_ORDER_PATH),
                Arguments.of(20001, 2001, USER_RECEIVED_ORDER_PATH)
        );
    }

    private void prepareCheckouterMock() {
        prepareCheckouterMock(
                checkouterMockRequest(),
                checkouterBoxesResponse(),
                668
        );
    }

    private void prepareCheckouterMockWithoutFFIdResponse() {
        //language=json
        prepareCheckouterMock(
                checkouterMockRequest(),
                "" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"id1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"count\": 2,\n" +
                        "          \"itemId\": 1\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": 2,\n" +
                        "      \"weight\": 1001,\n" +
                        "      \"width\": 21,\n" +
                        "      \"height\": 22,\n" +
                        "      \"depth\": 23,\n" +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"count\": 3,\n" +
                        "          \"itemId\": 2\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                668
        );
    }

    private String checkouterMockRequest() {
        //language=json
        return "" +
                "{\n" +
                "  \"boxes\": [\n" +
                "    {\n" +
                "      \"weight\": 1000,\n" +
                "      \"width\": 11,\n" +
                "      \"height\": 12,\n" +
                "      \"depth\": 13,\n" +
                "      \"fulfilmentId\": \"id1\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"itemId\": 1,\n" +
                "          \"count\": 2\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"weight\": 1001,\n" +
                "      \"width\": 21,\n" +
                "      \"height\": 22,\n" +
                "      \"depth\": 23,\n" +
                "      \"fulfilmentId\": \"id2\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"itemId\": 2,\n" +
                "          \"count\": 3\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }

    private String checkouterBoxesResponse() {
        //language=json
        return "{\n" +
                "  \"boxes\": [\n" +
                "    {\n" +
                "      \"id\": 1,\n" +
                "      \"weight\": 1000,\n" +
                "      \"width\": 11,\n" +
                "      \"height\": 12,\n" +
                "      \"depth\": 13,\n" +
                "      \"fulfilmentId\": \"id1\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"count\": 2,\n" +
                "          \"itemId\": 1\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": 2,\n" +
                "      \"weight\": 1001,\n" +
                "      \"width\": 21,\n" +
                "      \"height\": 22,\n" +
                "      \"depth\": 23,\n" +
                "      \"fulfilmentId\": \"id2\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"count\": 3,\n" +
                "          \"itemId\": 2\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }

    private String putBoxesRequest() {
        //language=json
        return "{\n" +
                "  \"boxes\": [\n" +
                "    {\n" +
                "      \"weight\": 1000,\n" +
                "      \"width\": 11,\n" +
                "      \"height\": 12,\n" +
                "      \"depth\": 13,\n" +
                "      \"fulfilmentId\": \"id1\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"id\": 1,\n" +
                "          \"count\": 2\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"weight\": 1001,\n" +
                "      \"width\": 21,\n" +
                "      \"height\": 22,\n" +
                "      \"depth\": 23,\n" +
                "      \"fulfilmentId\": \"id2\"," +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"id\": 2,\n" +
                "          \"count\": 3\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }

    private String putBoxesExpectedResponse() {
        //language=json
        return "{\n" +
                "  \"status\": \"OK\",\n" +
                "  \"result\": {\n" +
                "    \"boxes\": [\n" +
                "      {\n" +
                "        \"id\": 1,\n" +
                "        \"weight\": 1000,\n" +
                "        \"width\": 11,\n" +
                "        \"height\": 12,\n" +
                "        \"depth\": 13,\n" +
                "        \"fulfilmentId\": \"id1\"," +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": 1,\n" +
                "            \"count\": 2\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      {\n" +
                "        \"id\": 2,\n" +
                "        \"weight\": 1001,\n" +
                "        \"width\": 21,\n" +
                "        \"height\": 22,\n" +
                "        \"depth\": 23,\n" +
                "        \"fulfilmentId\": \"id2\"," +
                "        \"items\": [\n" +
                "          {\n" +
                "            \"id\": 2,\n" +
                "            \"count\": 3\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
    }

    private void prepareCheckouterMockOneBox() {
        //language=json
        prepareCheckouterMock(
                "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"1-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"itemId\": 1,\n" +
                        "          \"count\": 1\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }" +
                        "  ]\n" +
                        "}\n",
                "" +
                        "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"1-1\"," +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"count\": 1,\n" +
                        "          \"itemId\": 1\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }]\n" +
                        "}\n",
                668
        );
    }

    private void prepareCheckouterMockEmptyItems() {
        //language=json
        prepareCheckouterMock(
                "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"id1\"" +
                        "    },\n" +
                        "    {\n" +
                        "      \"weight\": 1001,\n" +
                        "      \"width\": 21,\n" +
                        "      \"height\": 22,\n" +
                        "      \"depth\": 23,\n" +
                        "      \"fulfilmentId\": \"id2\"" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "{\n" +
                        "  \"boxes\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"weight\": 1000,\n" +
                        "      \"width\": 11,\n" +
                        "      \"height\": 12,\n" +
                        "      \"depth\": 13,\n" +
                        "      \"fulfilmentId\": \"id1\"" +
                        "    },\n" +
                        "    {\n" +
                        "      \"id\": 2,\n" +
                        "      \"weight\": 1001,\n" +
                        "      \"width\": 21,\n" +
                        "      \"height\": 22,\n" +
                        "      \"depth\": 23,\n" +
                        "      \"fulfilmentId\": \"id2\"" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                668
        );
    }

    private void prepareCheckouterMock(String request, String response, long clientId) {
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        prepareCheckouterMock(server, request, response, clientId);
    }

    private void prepareCheckouterMock(MockRestServiceServer server, String request, String response, long clientId) {
        prepareCheckouterMock(server, request, response, clientId, ANY_VALID_ORDER_PATH);
    }

    private void prepareCheckouterMock(String request, String response, long clientId, String orderPath) {
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        prepareCheckouterMock(server, request, response, clientId, orderPath);
    }

    private void prepareCheckouterMock(MockRestServiceServer server, String request, String response, long clientId,
                                       String orderPath) {
        prepareGetOrderMock(server, clientId, orderPath);
        String urlParcel = checkouterUrl + "/orders/1/delivery/parcels/1/boxes?clientRole=SHOP&clientId=" +
                clientId + "&shopId=";
        String urlShipment = checkouterUrl + "/orders/1/delivery/shipments/1/boxes?clientRole=SHOP&clientId=" +
                clientId + "&shopId=";
        server.expect(ExpectedCount.twice(), req -> {
                    URI uri = req.getURI();
                    assertTrue(uri.toString().equals(urlParcel) || uri.toString().equals(urlShipment));
                })
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(request))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response));
    }

    private void prepareGetOrderMock(long clientId, String orderPath) {
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        prepareGetOrderMock(server, clientId, orderPath);
    }

    private void prepareGetOrderMock(MockRestServiceServer server, long clientId, String orderPath) {
        String getOrderUrl = checkouterUrl + "/orders/1?clientRole=SHOP&clientId=" + clientId + "&shopId=&archived" +
                "=false";
        server.expect(ExpectedCount.twice(), req -> {
                    URI uri = req.getURI();
                    assertEquals(getOrderUrl, uri.toString());
                }).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(orderPath)));
    }

    private void prepareL4sMock() {
        when(logistics4shops.putOrderBoxes(anyString(), any(OrderBoxesRequestDto.class), eq(668L), isNull()))
                .thenReturn(new OrderBoxesDto().orderId("12345").boxes(List.of(
                        new OrderBox()
                                .id(1L)
                                .barcode("id1")
                                .weight(1000L)
                                .width(11L)
                                .height(12L)
                                .length(13L)
                                .items(List.of(new OrderBoxItem().id(1L).count(2))),
                        new OrderBox()
                                .id(2L)
                                .barcode("id2")
                                .weight(1001L)
                                .width(21L)
                                .height(22L)
                                .length(23L)
                                .items(List.of(new OrderBoxItem().id(2L).count(3)))
                )));
    }

    private void prepareL4SMockWithError(int statusCode, String errorMessage) {
        when(logistics4shops.putOrderBoxes(anyString(), any(OrderBoxesRequestDto.class), eq(668L), isNull()))
                .thenThrow(new Logistics4ShopsClientException(statusCode, errorMessage));
    }
}
