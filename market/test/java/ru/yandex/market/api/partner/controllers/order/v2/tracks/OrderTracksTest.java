package ru.yandex.market.api.partner.controllers.order.v2.tracks;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Тесты для ручек управления трек-кодами
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class OrderTracksTest extends OrderControllerV2TestTemplate {

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для не DBS заказа (XML)")
    void testUpdateTrackCodeWithNoDbsPartnerXml() {
        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>CODE</track-code>" +
                "    <delivery-service-id>124</delivery-service-id>" +
                "</track>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.XML,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"CAMPAIGN_TYPE_NOT_SUPPORTED\" " +
                "           message=\"Campaign type is not allowed\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для не DBS заказа (JSON)")
    void testUpdateTrackCodeWithNoDbsPartnerJson() {
        //language=json
        String requestBody = "{ \"trackCode\": \"CODE\",  \"deliveryServiceId\": 124 }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/10668/orders/1/delivery/track.json",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"CAMPAIGN_TYPE_NOT_SUPPORTED\", " +
                "    \"message\":\"Campaign type is not allowed\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для не существующего DBS заказа (XML)")
    void testUpdateTrackCodeForNotExistentDbsPartnerXml() {
        prepareNotFoundCheckouterMock(1, 2001);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>CODE</track-code>" +
                "    <delivery-service-id>124</delivery-service-id>" +
                "</track>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.XML,
                        requestBody));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"NOT_FOUND\" " +
                "           message=\"Order not found: 1\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для не существующего DBS заказа (JSON)")
    void testUpdateTrackCodeForNotExistentDbsPartnerJson() {
        prepareNotFoundCheckouterMock(1, 2001);

        //language=json
        String requestBody = "{ \"trackCode\": \"CODE\",  \"deliveryServiceId\": 124 }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"NOT_FOUND\", " +
                "    \"message\":\"Order not found: 1\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для DBS заказа без шипментов (XML)")
    void testUpdateTrackCodeForOrderWithoutParcelsXml() {
        prepareCheckouterMock("mocks/checkouter/get_order_without_parcels.json", 1, 2001);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>CODE</track-code>" +
                "    <delivery-service-id>124</delivery-service-id>" +
                "</track>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.XML,
                        requestBody));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"NOT_FOUND\" " +
                "           message=\"Order has no parcels: 1\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для DBS заказа, неизвестная СД")
    void testDeliveryServiceNotFound() {
        prepareCheckouterMock("mocks/checkouter/get_order_without_parcels.json", 1, 2001);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.XML,
                        resourceAsString("requests/updateTrack.dsNotFound.request.json")
                ));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getRawStatusCode());
        JsonTestUtil.assertEquals(resourceAsString("expected/updateTrack.dsNotFound.response.json"),
                exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для DBS заказа без шипментов (JSON)")
    void testUpdateTrackCodeForOrderWithoutParcelsJson() {
        prepareCheckouterMock("mocks/checkouter/get_order_without_parcels.json", 1, 2001);

        //language=json
        String requestBody = "{ \"trackCode\": \"CODE\",  \"deliveryServiceId\": 124 }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.json",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"NOT_FOUND\", " +
                "    \"message\":\"Order has no parcels: 1\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для DBS заказа с ранее исползованным кодом (JSON)")
    void testUpdateTrackCodeForOrderWhitDuplicateTrackCodeJson() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;
        String trackCode = "CODE";
        long deliveryServiceId = 124;

        prepareDuplicateCodeCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        String requestBody =
                "{ \"trackCode\": \"" + trackCode + "\",  \"deliveryServiceId\": " + deliveryServiceId + " }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.json",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"TRACK_CODE_ALREADY_USED\", " +
                "    \"message\":\"Track code CODE has been already used for delivery service 124\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода для DBS заказа с ранее исползованным кодом (XML)")
    void testUpdateTrackCodeForOrderWhitDuplicateTrackCodeXml() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;
        String trackCode = "CODE";
        long deliveryServiceId = 124;

        prepareDuplicateCodeCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>" + trackCode + "</track-code>" +
                "    <delivery-service-id>" + deliveryServiceId + "</delivery-service-id>" +
                "</track>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"TRACK_CODE_ALREADY_USED\" " +
                "           message=\"Track code CODE has been already used for delivery service 124\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода, если не передана СД (XML)")
    void testUpdateTrackCodeWithoutDeliveryServiceXml() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;

        prepareUpdateTrackCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>CODE</track-code>" +
                "</track>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                        HttpMethod.POST,
                        Format.XML,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"BAD_REQUEST\" " +
                "           message=\"Delivery service is not specified\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @Test
    @DisplayName("Эксепшен при попытке обновления трек-кода, если не передана СД (JSON)")
    void testUpdateTrackCodeWithoutDeliveryServiceJson() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;

        prepareUpdateTrackCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        //language=json
        String requestBody = "{ \"trackCode\": \"CODE\" }";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.json",
                        HttpMethod.POST,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"BAD_REQUEST\", " +
                "    \"message\":\"Delivery service is not specified\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @Test
    @DisplayName("Обновление трек-кода для DBS заказа (XML)")
    void testUpdateTrackCodeXml() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;
        String trackCode = "CODE";
        long deliveryServiceId = 124;

        prepareUpdateTrackCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<track>" +
                "    <track-code>" + trackCode + "</track-code>" +
                "    <delivery-service-id>" + deliveryServiceId + "</delivery-service-id>" +
                "</track>";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.xml",
                HttpMethod.POST,
                Format.XML,
                requestBody);

        ArgumentCaptor<List<Track>> captor = forClass(List.class);
        verify(checkouterAPI, times(1)).updateDeliveryTracks(eq(orderId), eq(parcelId), captor.capture(),
                eq(ClientRole.SHOP), eq(partnerId));

        List<Track> tracks = captor.getValue();
        assertNotNull(tracks);
        assertEquals(1, tracks.size());

        Track track = tracks.get(0);
        assertEquals(trackCode, track.getTrackCode());
        assertEquals(deliveryServiceId, track.getDeliveryServiceId());
    }

    @Test
    @DisplayName("Обновление трек-кода для DBS заказа (JSON)")
    void testUpdateTrackCodeJson() {
        long orderId = 1L;
        long parcelId = 665364L;
        long partnerId = 2001L;
        String trackCode = "CODE";
        long deliveryServiceId = 124;

        prepareUpdateTrackCheckouterMock("mocks/checkouter/get_order_update_track_code.json", orderId, parcelId,
                partnerId);

        String requestBody = "{\"trackCode\": \"" + trackCode + "\",  \"deliveryServiceId\": " + deliveryServiceId +
                "}";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/track.json",
                HttpMethod.POST,
                Format.JSON,
                requestBody);

        ArgumentCaptor<List<Track>> captor = forClass(List.class);
        verify(checkouterAPI, times(1)).updateDeliveryTracks(eq(orderId), eq(parcelId), captor.capture(),
                eq(ClientRole.SHOP), eq(partnerId));

        List<Track> tracks = captor.getValue();
        assertNotNull(tracks);
        assertEquals(1, tracks.size());

        Track track = tracks.get(0);
        assertEquals(trackCode, track.getTrackCode());
        assertEquals(deliveryServiceId, track.getDeliveryServiceId());
    }

    private void prepareDuplicateCodeCheckouterMock(String getOrderBodyPath, long orderId, long parcelId,
                                                    long clientId) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockGetOrder(server, orderId, clientId)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(getOrderBodyPath)));

        checkouterMockHelper.mockUpdateDeliveryTracks(server, orderId, parcelId, clientId,
                withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                //language=json
                                "{\n" +
                                        "  \"message\": \"There are tracks with the same trackCode and " +
                                        "deliveryServiceId in " +
                                        "orders: [" + orderId + "]\",\n" +
                                        "  \"code\": \"DUPlCATE_KEY\",\n" +
                                        "  \"status\": 400\n" +
                                        "}")
        );
    }

    private void prepareUpdateTrackCheckouterMock(String getOrderBodyPath, long orderId, long parcelId, long clientId) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockGetOrder(server, orderId, clientId)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resourceAsString(getOrderBodyPath)));

        checkouterMockHelper.mockUpdateDeliveryTracks(server, orderId, parcelId, clientId)
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"tracks\":[]}"));
    }
}
