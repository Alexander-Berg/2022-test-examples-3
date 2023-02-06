package ru.yandex.market.api.partner.controllers.order.v2.misc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.model.DeliverDigitalItemDTO;
import ru.yandex.market.api.partner.controllers.order.model.DeliverDigitalItemRequestDTO;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ConfirmationReason;
import ru.yandex.market.checkout.checkouter.order.digital.DeliverDigitalItemRequest;
import ru.yandex.market.checkout.checkouter.order.eda.EdaOrderChangePriceRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Тесты для ручек, не подходящих под другие категории
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
public class MiscOrderControllerTest extends OrderControllerV2TestTemplate {

    @Test
    void deliverDigitalGoodsJson() throws JsonProcessingException {
        doNothing().when(checkouterAPI).deliverDigitalGoods(anyLong(), any());

        DeliverDigitalItemRequestDTO request = new DeliverDigitalItemRequestDTO(getDigitalItems());

        ObjectMapper objectMapper = new ApiObjectMapperFactory().createJsonMapper();
        String requestJson = objectMapper.writeValueAsString(request);

        FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/deliverDigitalGoods.json",
                HttpMethod.POST,
                requestJson,
                String.class,
                MediaType.APPLICATION_JSON
        );

        ArgumentCaptor<DeliverDigitalItemRequest> requestCaptor = forClass(DeliverDigitalItemRequest.class);
        verify(checkouterAPI, times(1)).deliverDigitalGoods(anyLong(), requestCaptor.capture());

        DeliverDigitalItemRequest captured = requestCaptor.getValue();
        assertEquals(captured.getItems().get(1L).get(0).getCode(), "code");
        assertEquals(captured.getItems().get(1L).get(0).getSlip(), "slip");
        assertNotNull(captured.getItems().get(1L).get(0).getActivateTill());
        assertEquals(captured.getItems().get(2L).size(), 2);
    }

    @Test
    void deliverDigitalGoodsXML() throws JsonProcessingException {
        doNothing().when(checkouterAPI).deliverDigitalGoods(anyLong(), any());

        DeliverDigitalItemRequestDTO request = new DeliverDigitalItemRequestDTO(getDigitalItems());

        ObjectMapper xmlMapper = new ApiObjectMapperFactory().createXmlMapper();
        String requestXML = xmlMapper.writeValueAsString(request);

        FunctionalTestHelper.makeRequestWithContentType(
                urlBasePrefix + "/campaigns/10668/orders/1/deliverDigitalGoods.xml",
                HttpMethod.POST,
                requestXML,
                String.class,
                MediaType.APPLICATION_XML
        );

        ArgumentCaptor<DeliverDigitalItemRequest> requestCaptor = forClass(DeliverDigitalItemRequest.class);
        verify(checkouterAPI, times(1)).deliverDigitalGoods(anyLong(), requestCaptor.capture());

        DeliverDigitalItemRequest captured = requestCaptor.getValue();
        assertEquals(captured.getItems().get(1L).get(0).getCode(), "code");
        assertEquals(captured.getItems().get(1L).get(0).getSlip(), "slip");
        assertNotNull(captured.getItems().get(1L).get(0).getActivateTill());
        assertEquals(captured.getItems().get(2L).size(), 2);
    }

    /**
     * Проверяет обновление цены заказа.
     */
    @Test
    void testUpdateOrderPrice() {
        doNothing().when(checkouterEdaApi).changeEdaOrderPrice(any(), anyLong(), any());

        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/20001/orders/1/price",
                HttpMethod.PATCH, Format.JSON, "{\"price\": \"1.23\"}");

        org.assertj.core.api.Assertions.assertThat(response)
                .extracting(ResponseEntity::getStatusCode)
                .isEqualTo(HttpStatus.OK);

        ArgumentCaptor<RequestClientInfo> clientInfoCaptor = forClass(RequestClientInfo.class);
        verify(checkouterEdaApi).changeEdaOrderPrice(clientInfoCaptor.capture(), eq(1L),
                eq(new EdaOrderChangePriceRequest(new BigDecimal("1.23"))));
        verifyNoMoreInteractions(checkouterEdaApi);

        org.assertj.core.api.Assertions.assertThat(clientInfoCaptor.getValue().getClientId())
                .isEqualTo(2001);
    }

    /**
     * Проверяет, что возвращаем 400 для не DBS партнера при попытке обновить цену заказа.
     */
    @Test
    void testUpdateOrderPriceForNonDbs() {
        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/10774/orders/1/price",
                        HttpMethod.PATCH, Format.JSON, "{\"price\": \"1.23\"}"))
                .satisfies(
                        badRequest -> org.assertj.core.api.Assertions.assertThat(badRequest.getResponseBodyAsString())
                                .isEqualTo("{\"status\":\"ERROR\"," +
                                        "\"errors\":[{\"code\":\"CAMPAIGN_TYPE_NOT_SUPPORTED\"," +
                                        "\"message\":\"Campaign type is not allowed\"}]}"));
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getAcceptCancellationRequestSuccessfulArguments")
    @DisplayName("Успешные случаи подтверждения магазином отмены заказа")
    void testAcceptCancellationRequest(String description,
                                       Format format,
                                       Long orderId,
                                       Long cancellationRequestId,
                                       String checkouterResponseMockPath,
                                       String requestBodyPath,
                                       ChangeRequestStatus expectedNewStatus,
                                       ConfirmationReason expectedReason) {
        ArgumentCaptor<ChangeRequestPatchRequest> updateRequestCaptor =
                forClass(ChangeRequestPatchRequest.class);

        /*
        Given
        Подготовка моков чекаутера
         */
        MockRestServiceServer checkouterMock = checkouterMockHelper.getServer();
        checkouterMockHelper.mockGetOrderWithChangeRequestReturnsBody(checkouterMock,
                orderId, 669,
                resourceAsString(checkouterResponseMockPath));
        checkouterMockHelper.mockChangeRequestStatusUpdate(checkouterMock,
                orderId, 669, cancellationRequestId);

        /*
         When
         */
        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/10669/orders/" + orderId + "/cancellation/accept.json",
                HttpMethod.PUT,
                format,
                resourceAsString(requestBodyPath));

        /*
         * Then
         */
        //null передается, когда ничего апдейтить не надо
        if (expectedNewStatus != null) {
            verify(checkouterAPI).updateChangeRequestStatus(eq(orderId), eq(cancellationRequestId),
                    eq(ClientRole.SHOP), eq(669L), updateRequestCaptor.capture());

            assertNotNull(updateRequestCaptor.getValue());
            assertEquals(expectedNewStatus, updateRequestCaptor.getValue().getStatus());

            if (expectedNewStatus == ChangeRequestStatus.REJECTED) {
                AbstractChangeRequestPayload payload = updateRequestCaptor
                        .getValue()
                        .getPayload();

                assertNotNull(payload);
                assertTrue(payload instanceof CancellationRequestPayload);
                assertEquals(expectedReason, ((CancellationRequestPayload) payload).getConfirmationReason());
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getAcceptCancellationRequestNegativeArguments")
    @DisplayName("Неуспешные случаи подтверждения магазином отмены заказа")
    void testAcceptCancellationRequest_negativeCases(String description,
                                                     Long orderId,
                                                     String checkouterResponseMockPath,
                                                     String requestBodyPath,
                                                     HttpStatus expectedHttpStatus,
                                                     String expectedResponsePath) {
        /*
        Given
        Подготовка моков чекаутера
         */
        MockRestServiceServer checkouterMock = checkouterMockHelper.getServer();
        checkouterMockHelper.mockGetOrderWithChangeRequestReturnsBody(checkouterMock,
                orderId, 669,
                resourceAsString(checkouterResponseMockPath));
        /*
         When
         */
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/10669/orders/" + orderId + "/cancellation/accept.json",
                        HttpMethod.PUT,
                        Format.JSON,
                        resourceAsString(requestBodyPath)));
        /*
        Then
         */
        assertEquals(expectedHttpStatus.value(), exception.getRawStatusCode());
        JsonTestUtil.assertEquals(resourceAsString(expectedResponsePath), exception.getResponseBodyAsString());
    }

    private Set<DeliverDigitalItemDTO> getDigitalItems() {
        Date activateTill = Date.from(Instant.now().plus(10, ChronoUnit.DAYS));
        return Set.of(
                new DeliverDigitalItemDTO(1L, "code", "slip", activateTill),
                new DeliverDigitalItemDTO(2L, "code2", "slip2", activateTill),
                new DeliverDigitalItemDTO(2L, "code3", "slip3", activateTill)
        );
    }

    private static Stream<Arguments> getAcceptCancellationRequestSuccessfulArguments() {
        return Stream.of(
                Arguments.of("Успешное подтверждение отмены (JSON)",
                        Format.JSON,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_cancellation.json",
                        "requests/approveOrderCancellationRequest.json",
                        ChangeRequestStatus.APPLIED,
                        null
                ),
                Arguments.of("Успешное подтверждение отмены (XML)",
                        Format.XML,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_cancellation.json",
                        "requests/approveOrderCancellationRequest.xml",
                        ChangeRequestStatus.APPLIED,
                        null
                ),
                Arguments.of("Успешное отклонение отмены (JSON)",
                        Format.JSON,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_cancellation.json",
                        "requests/rejectOrderCancellationRequest.json",
                        ChangeRequestStatus.REJECTED,
                        ConfirmationReason.DELIVERED
                ),
                Arguments.of("Успешное отклонение отмены (XML)",
                        Format.XML,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_cancellation.json",
                        "requests/rejectOrderCancellationRequest.xml",
                        ChangeRequestStatus.REJECTED,
                        ConfirmationReason.DELIVERY
                ),
                Arguments.of("Попытка повторного подтверждения (XML)",
                        Format.XML,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_approved_cancellation.json",
                        "requests/approveOrderCancellationRequest.xml",
                        null,
                        null
                ),
                Arguments.of("Попытка повторной отмены (XML)",
                        Format.XML,
                        2827258L,
                        123L,
                        "mocks/checkouter/get_order_with_rejected_cancellation.json",
                        "requests/rejectOrderCancellationRequest.xml",
                        null,
                        null
                )
        );
    }

    private static Stream<Arguments> getAcceptCancellationRequestNegativeArguments() {
        return Stream.of(
                Arguments.of("Попытка заапрувить отклоненную заявку на отмену заказа",
                        2827258L,
                        "mocks/checkouter/get_order_with_rejected_cancellation.json",
                        "requests/approveOrderCancellationRequest.json",
                        HttpStatus.BAD_REQUEST,
                        "expected/invalidChangeRequestStatus.response.json"
                ),
                Arguments.of("Попытка отклонить одобренную заявку на отмену заказа",
                        2827258L,
                        "mocks/checkouter/get_order_with_approved_cancellation.json",
                        "requests/rejectOrderCancellationRequest.json",
                        HttpStatus.BAD_REQUEST,
                        "expected/invalidChangeRequestStatus.response.json"
                ),
                Arguments.of("Нет заявок на отмену у заказа, инициированных покупателем",
                        2827258L,
                        "mocks/checkouter/get_order_without_cancellation.json",
                        "requests/rejectOrderCancellationRequest.json",
                        HttpStatus.NOT_FOUND,
                        "expected/invalidChangeRequestClient.response.json"
                ),
                Arguments.of("Реджэкт без указания причины",
                        2827258L,
                        "mocks/checkouter/get_order_without_cancellation.json",
                        "requests/rejectOrderCancellationRequestWithoutReason.json",
                        HttpStatus.NOT_FOUND,
                        "expected/invalidChangeRequestNoReason.response.json"
                )
        );
    }
}
