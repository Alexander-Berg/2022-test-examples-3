package ru.yandex.market.logistics.lom.client;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.logistics.lom.model.dto.CancelOrderDto;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class OrderCancelClientTest extends AbstractClientTest {

    @Autowired
    private LomClient lomClient;

    @Test
    @DisplayName("Успешная отмена заказа")
    void cancelOrderSuccess() {
        expectOrderCancel()
            .andRespond(withSuccess(
                extractFileContent("response/order/cancel_order.json"),
                MediaType.APPLICATION_JSON
            ));

        CancellationOrderRequestDto cancellationOrderRequestDto = lomClient.cancelOrder(1L);
        softly.assertThat(cancellationOrderRequestDto).isEqualTo(
            CancellationOrderRequestDto.builder()
                .id(1L)
                .status(CancellationOrderStatus.CREATED)
                .cancellationErrorMessage(null)
                .cancellationOrderReason(null)
                .cancellationOrderRequestReasonDetails(null)
                .cancellationSegmentRequests(Collections.emptySet())
                .build()
        );
    }

    @Test
    @DisplayName("Отмена несуществующего заказа")
    void cancelNonExistOrder() throws Exception {
        expectOrderCancel()
            .andRespond(withStatus(HttpStatus.NOT_FOUND)
                .body(extractFileContent("response/order/order_not_found.json"))
                .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelOrder(1L))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage(
                "Http request exception: status <404>, response body <{\n"
                    + "   \"message\": \"Failed to find [ORDER] with id [1]\",\n"
                    + "   \"resourceType\": \"ORDER\",\n"
                    + "   \"identifier\": \"1\"\n"
                    + "}\n"
                    + ">."
            );
    }

    @Test
    @DisplayName("Отмена завершённого заказа")
    void cancelFinishedOrder() throws Exception {
        expectOrderCancel()
            .andRespond(withBadRequest()
                .body(extractFileContent("response/order/cancel/finished_cancel.json"))
                .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelOrder(1L))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage(
                "Http request exception: status <400>, response body <{\n"
                    + "  \"message\": \"Order with status FINISHED can't be cancelled\"\n"
                    + "}\n"
                    + ">."
            );
    }

    @Test
    @DisplayName("Отмена заказа, находящегося в обработке")
    void cancelProcessingOrder() throws Exception {
        expectOrderCancel()
            .andRespond(withServerError()
                .body(extractFileContent("response/order/cancel/processing_cancel.json"))
                .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelOrder(1L))
            .isInstanceOf(HttpServerErrorException.class)
            .hasMessage("500 Internal Server Error");
    }

    @Test
    @DisplayName("Заявка на отмену уже существует")
    void cancelOrderWithCancellationRequest() {
        expectOrderCancel()
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .body(extractFileContent("response/order/cancel/cancel_request_exists.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        softly.assertThatThrownBy(() -> lomClient.cancelOrder(1L))
            .isInstanceOf(HttpTemplateException.class)
            .hasMessage("Http request exception: status <409>, response body <{\n"
                + "  \"message\": \"Active cancellation request restriction. Order id 1.\"\n"
                + "}\n"
                + ">.");
    }

    @Test
    @DisplayName("Заявка на отмену уже существует с игнорированием исключения")
    void cancelOrderWithCancellationRequestIgnore() {
        expectOrderCancel()
            .andRespond(
                withStatus(HttpStatus.CONFLICT)
                    .body(extractFileContent("response/order/cancel/cancel_request_exists.json"))
                    .contentType(MediaType.APPLICATION_JSON)
            );

        lomClient.cancelOrder(1L, true);
    }

    @Test
    @DisplayName("Заявка на отмену уже существует с игнорированием исключения")
    void cancelOrderWithReason() {
        expectOrderCancelWithReason()
            .andRespond(withSuccess(
                extractFileContent("response/order/cancel_order_with_reason.json"),
                MediaType.APPLICATION_JSON
            ));

        lomClient.cancelOrder(
            1L,
            CancelOrderDto.builder()
                .reason(CancellationOrderReason.DELIVERY_SERVICE_UNDELIVERED)
                .build(),
            true
        );
    }

    @Nonnull
    private ResponseActions expectOrderCancel() {
        return mock.expect(method(HttpMethod.DELETE))
            .andExpect(requestTo(uri + "/orders/1"));
    }

    @Nonnull
    private ResponseActions expectOrderCancelWithReason() {
        return mock.expect(method(HttpMethod.POST))
            .andExpect(requestTo(uri + "/orders/1/cancel"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/order/cancel/request.json"));
    }
}
