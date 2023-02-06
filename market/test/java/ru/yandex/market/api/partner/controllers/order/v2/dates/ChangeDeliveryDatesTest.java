package ru.yandex.market.api.partner.controllers.order.v2.dates;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.model.DbsDeliveryDateUpdateReason;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.common.retrofit.CommonRetrofitHttpExecutionException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.orderservice.client.model.ActorType;
import ru.yandex.market.orderservice.client.model.ApiError;
import ru.yandex.market.orderservice.client.model.ChangeDeliveryDatesResponse;
import ru.yandex.market.orderservice.client.model.ChangeDeliveryDatesResponseDto;
import ru.yandex.market.orderservice.client.model.ChangeRequestStatusType;
import ru.yandex.market.orderservice.client.model.CommonApiResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.api.partner.controllers.order.config.OrderControllerV2Config.ENV_ROUTE_REQS_TO_ORDER_SERVICE;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.jsonEquals;

/**
 * Тесты ручек переноса дат доставки (orders/{orderId}/delivery/date)
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class ChangeDeliveryDatesTest extends OrderControllerV2TestTemplate {

    @ParameterizedTest(name = "reason = {0}")
    @EnumSource(DbsDeliveryDateUpdateReason.class)
    @DisplayName("Обновление даты доставки для DBS заказа (XML)")
    void testUpdateDeliveryDateXml(DbsDeliveryDateUpdateReason reason) {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate toDate = LocalDate.now().plusDays(2);

        prepareEditOrderCheckouterMock(orderId, partnerId);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<dateChangeRequest>" +
                "    <dates>" +
                "        <to-date>" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "</to-date>" +
                "    </dates>" +
                "    <reason>" + reason + "</reason>" +
                "</dateChangeRequest>";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.xml",
                HttpMethod.PUT,
                Format.XML,
                requestBody);

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), eq(ClientRole.SHOP), eq(partnerId), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        DeliveryEditRequest deliveryEditRequest = orderEditRequest.getDeliveryEditRequest();
        assertNotNull(deliveryEditRequest);
        assertEquals(toDate, deliveryEditRequest.getToDate());
        assertEquals(toDate, deliveryEditRequest.getFromDate());
        assertEquals(reason.name(), HistoryEventReason.USER_MOVED_DELIVERY_DATES.equals(deliveryEditRequest.getReason())
                ? DbsDeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES.name()
                : DbsDeliveryDateUpdateReason.PARTNER_MOVED_DELIVERY_DATES.name());
    }

    @ParameterizedTest(name = "reason = {0}")
    @EnumSource(DbsDeliveryDateUpdateReason.class)
    @DisplayName("Обновление даты доставки для DBS заказа (XML) через order-service")
    void testUpdateDeliveryDateXmlViaOs(DbsDeliveryDateUpdateReason reason) {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate fromDate = LocalDate.now().plusDays(1);
        LocalDate toDate = LocalDate.now().plusDays(2);

        prepareChangeDeliveryDateOsMock(partnerId, orderId);
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "true");

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<dateChangeRequest>" +
                "    <dates>" +
                "        <from-date>" + fromDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "</from-date>" +
                "        <to-date>" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "</to-date>" +
                "    </dates>" +
                "    <reason>" + reason + "</reason>" +
                "</dateChangeRequest>";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.xml",
                HttpMethod.PUT,
                Format.XML,
                requestBody);

        verifyNoInteractions(checkouterAPI);
        verify(papiOrderServiceClient, times(1)).postChangeDeliveryDates(
                eq(partnerId),
                eq(orderId),
                eq(toDate),
                eq(toDate),
                any(),
                eq(ActorType.API)
        );

        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "false");
    }

    @ParameterizedTest(name = "reason = {0}")
    @EnumSource(DbsDeliveryDateUpdateReason.class)
    @DisplayName("Обновление даты доставки для DBS заказа (JSON)")
    void testUpdateDeliveryDateJson(DbsDeliveryDateUpdateReason reason) {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate toDate = LocalDate.now().plusDays(2);

        prepareEditOrderCheckouterMock(orderId, partnerId);

        //language=json
        String requestBody =
                "{\"dates\": {\"toDate\": \"" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\"}, " +
                        "\"reason\":" +
                        " \"" + reason + "\"}";

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.json",
                HttpMethod.PUT,
                Format.JSON,
                requestBody);

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), eq(ClientRole.SHOP), eq(partnerId), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        DeliveryEditRequest deliveryEditRequest = orderEditRequest.getDeliveryEditRequest();
        assertNotNull(deliveryEditRequest);
        assertEquals(toDate, deliveryEditRequest.getToDate());
        assertEquals(toDate, deliveryEditRequest.getFromDate());
        assertEquals(reason.name(), HistoryEventReason.USER_MOVED_DELIVERY_DATES.equals(deliveryEditRequest.getReason())
                ? DbsDeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES.name()
                : DbsDeliveryDateUpdateReason.PARTNER_MOVED_DELIVERY_DATES.name());
    }

    @ParameterizedTest(name = "reason = {0}")
    @CsvSource({
            "edit_count_exceeded",
            "days_count_exceeded"
    })
    @DisplayName("Некорректное обновление даты доставки для DBS заказа (XML)")
    void testUpdateDeliveryDateErrorXml(String errorCode) {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate toDate = LocalDate.now().plusDays(2);
        DbsDeliveryDateUpdateReason reason = DbsDeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES;

        prepareEditOrderWithErrorCheckouterMock(orderId, partnerId, errorCode);

        //language=xml
        String requestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<dateChangeRequest>" +
                "    <dates>" +
                "        <to-date>" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "</to-date>" +
                "    </dates>" +
                "    <reason>" + reason + "</reason>" +
                "</dateChangeRequest>";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.xml",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody));

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), eq(ClientRole.SHOP), eq(partnerId), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        DeliveryEditRequest deliveryEditRequest = orderEditRequest.getDeliveryEditRequest();
        assertNotNull(deliveryEditRequest);
        assertEquals(toDate, deliveryEditRequest.getToDate());
        assertEquals(toDate, deliveryEditRequest.getFromDate());
        assertEquals(reason.name(), deliveryEditRequest.getReason().name());

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=xml
        String expected = "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code=\"" + errorCode.toUpperCase() + "\" " +
                "           message=\"message\"/>" +
                "  </errors>" +
                "</response>";
        MbiAsserts.assertXmlEquals(expected,
                exception.getResponseBodyAsString(),
                MbiAsserts.IGNORE_ORDER
        );
    }

    @ParameterizedTest(name = "reason = {0}")
    @CsvSource({
            "edit_count_exceeded",
            "days_count_exceeded"
    })
    @DisplayName("Некорректное обновление даты доставки для DBS заказа (JSON)")
    void testUpdateDeliveryDateErrorJson(String errorCode) {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate toDate = LocalDate.now().plusDays(2);
        DbsDeliveryDateUpdateReason reason = DbsDeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES;

        prepareEditOrderWithErrorCheckouterMock(orderId, partnerId, errorCode);

        //language=json
        String requestBody =
                "{\"dates\": {\"toDate\": \"" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\"}, " +
                        "\"reason\":" +
                        " \"" + reason + "\"}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.json",
                        HttpMethod.PUT,
                        Format.XML,
                        requestBody));

        ArgumentCaptor<OrderEditRequest> captor = forClass(OrderEditRequest.class);
        verify(checkouterAPI, times(1)).editOrder(eq(orderId), eq(ClientRole.SHOP), eq(partnerId), anyList(),
                captor.capture());

        OrderEditRequest orderEditRequest = captor.getValue();
        assertNotNull(orderEditRequest);

        DeliveryEditRequest deliveryEditRequest = orderEditRequest.getDeliveryEditRequest();
        assertNotNull(deliveryEditRequest);
        assertEquals(toDate, deliveryEditRequest.getToDate());
        assertEquals(toDate, deliveryEditRequest.getFromDate());
        assertEquals(reason.name(), deliveryEditRequest.getReason().name());

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"" + errorCode.toUpperCase() + "\", " +
                "    \"message\":\"message\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
    }

    @ParameterizedTest(name = "reason = {0}")
    @CsvSource({
            "edit_count_exceeded",
            "days_count_exceeded"
    })
    @DisplayName("Некорректное обновление даты доставки для DBS заказа (JSON) через order-service")
    void testUpdateDeliveryDateErrorJsonViaOs(String errorCode) throws JsonProcessingException {
        long orderId = 1L;
        long partnerId = 2001L;
        LocalDate toDate = LocalDate.now().plusDays(2);
        DbsDeliveryDateUpdateReason reason = DbsDeliveryDateUpdateReason.USER_MOVED_DELIVERY_DATES;

        prepareChangeDeliveryDateOsErrorMock(partnerId, orderId, errorCode);
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "true");

        //language=json
        String requestBody =
                "{\"dates\": {\"toDate\": \"" + toDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\"}, " +
                        "\"reason\":" +
                        " \"" + reason + "\"}";

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        requestBody));

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getRawStatusCode());

        //language=json
        String expected = "" +
                "{" +
                "  \"status\":\"ERROR\", " +
                "  \"errors\":[{" +
                "    \"code\":\"" + errorCode.toUpperCase() + "\", " +
                "    \"message\":\"message\"" +
                "  }]" +
                "}";
        JsonTestUtil.assertEquals(
                expected,
                exception.getResponseBodyAsString()
        );
        environmentService.setValue(ENV_ROUTE_REQS_TO_ORDER_SERVICE, "false");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentsForUpdateDeliveryDateInvalidRequest")
    void testUpdateDeliveryDateInvalidRequest(String testName, String requestFilePath, String responseFilePath) {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/20001/orders/1/delivery/date.json",
                        HttpMethod.PUT,
                        Format.XML,
                        resourceAsString(requestFilePath)));

        assertThat(
                exception,
                Matchers.allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(jsonEquals(resourceAsString(responseFilePath)))
                )
        );
    }

    private void prepareEditOrderCheckouterMock(long orderId, long clientId) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockEditOrder(server, orderId, clientId, "WHITE")
                .andRespond(withSuccess()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("[]"));
    }

    private void prepareChangeDeliveryDateOsMock(long partnerId, long orderId) {
        var result = new ChangeDeliveryDatesResponseDto();
        result.setOrderId(orderId);
        result.setPartnerId(partnerId);
        result.setChangeRequestStatus(ChangeRequestStatusType.PROCESSING);
        var response = new ChangeDeliveryDatesResponse();
        response.setResult(result);

        when(papiOrderServiceClient.postChangeDeliveryDates(
                eq(partnerId),
                eq(orderId),
                any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private void prepareEditOrderWithErrorCheckouterMock(long orderId, long clientId, String errorCode) {
        MockRestServiceServer server = checkouterMockHelper.getServer();

        checkouterMockHelper.mockEditOrder(server, orderId, clientId, "WHITE")
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(
                                //language=json
                                "{\n" +
                                        "  \"message\": \"message\"," +
                                        "  \"code\": \"" + errorCode + "\"," +
                                        "  \"status\": 403" +
                                        "}")
                );
    }

    private void prepareChangeDeliveryDateOsErrorMock(long partnerId, long orderId, String errorCode)
            throws JsonProcessingException {
        var result = new CommonApiResponse();
        var error = new ApiError();
        error.setCode(ApiError.CodeEnum.ORDER_EDIT_ERROR);
        error.setDetails(Map.of("checkouterErrorCode", errorCode.toUpperCase()));
        error.setMessage("message");
        result.setErrors(List.of(error));

        when(papiOrderServiceClient.postChangeDeliveryDates(
                eq(partnerId),
                eq(orderId),
                any(), any(), any(), any()))
                .thenReturn(CompletableFuture.failedFuture(
                                new CompletionException(
                                        new CommonRetrofitHttpExecutionException(
                                                "some message",
                                                400,
                                                null,
                                                new ObjectMapper().writeValueAsString(result)
                                        )
                                )
                        )
                );
    }

    private static Stream<Arguments> getArgumentsForUpdateDeliveryDateInvalidRequest() {
        return Stream.of(
                Arguments.of(
                        "Null в дате",
                        "requests/changeDeliveryDate.invalid1.request.json",
                        "expected/changeDeliveryDate.invalid1.response.json"
                ),
                Arguments.of(
                        "Null в reason",
                        "requests/changeDeliveryDate.invalid2.request.json",
                        "expected/changeDeliveryDate.invalid2.response.json"
                )
        );
    }
}
