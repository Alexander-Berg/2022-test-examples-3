package ru.yandex.market.logistics.logistics4go.controller.order;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.logistics4go.client.model.ApiError;
import ru.yandex.market.logistics.logistics4go.client.model.CancelOrderResponse;
import ru.yandex.market.logistics.logistics4go.client.model.CancellationRequestReason;
import ru.yandex.market.logistics.logistics4go.client.model.CancellationRequestStatus;
import ru.yandex.market.logistics.logistics4go.client.model.ErrorType;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.shouldBeCode;
import static ru.yandex.market.logistics.logistics4go.client.ResponseSpecBuilders.validatedWith;

@DisplayName("Тесты ручки создания заявки на отмену заказа")
class CancelOrderTest extends AbstractOrderTest {

    @Test
    @DisplayName("Заявка успешно создана")
    void success() {
        mockSearchLomOrder(1L, new OrderDto());

        doReturn(
            CancellationOrderRequestDto.builder()
                .id(10L)
                .status(CancellationOrderStatus.CREATED)
                .cancellationOrderReason(CancellationOrderReason.SHOP_CANCELLED)
                .build()
        )
            .when(lomClient)
            .cancelOrder(1L);

        CancelOrderResponse cancellationRequest = apiClient
            .orders()
            .cancelOrder()
            .orderIdPath(1L)
            .executeAs(validatedWith(shouldBeCode(SC_OK)));

        softly.assertThat(cancellationRequest).usingRecursiveComparison().isEqualTo(expectedResponse());

        verifySearchLomOrder(1L);
        verify(lomClient).cancelOrder(1L);
    }

    @Test
    @DisplayName("Несуществующий заказ")
    void orderDoesNotExist() {
        mockSearchLomOrder(1L, List.of());

        apiClient
            .orders()
            .cancelOrder()
            .orderIdPath(1L)
            .execute(validatedWith(shouldBeCode(SC_NOT_FOUND)));

        verifySearchLomOrder(1L);
    }

    @Test
    @DisplayName("Ошибка при создании заявки в LOM")
    void lomClientCancellationError() {
        mockSearchLomOrder(1L, new OrderDto());

        when(lomClient.cancelOrder(1L))
            .thenThrow(new HttpTemplateException(SC_BAD_REQUEST, "{\"message\": \"terrible request\"}"));

        ApiError response = apiClient
            .orders()
            .cancelOrder()
            .orderIdPath(1L)
            .execute(validatedWith(shouldBeCode(SC_INTERNAL_SERVER_ERROR)))
            .as(ApiError.class);

        softly.assertThat(response.getCode()).isEqualTo(ErrorType.UNKNOWN);
        softly.assertThat(response.getMessage()).isEqualTo(
            "Http request exception: status <400>, response body <{\"message\": \"terrible request\"}>."
        );

        verifySearchLomOrder(1L);
        verify(lomClient).cancelOrder(1L);
    }

    @Test
    @DisplayName("Ошибка 422 при создании заявки в LOM")
    void lomClientCancellationUnprocessableEntityError() {
        mockSearchLomOrder(1L, new OrderDto());

        when(lomClient.cancelOrder(1L))
            .thenThrow(new HttpTemplateException(
                SC_UNPROCESSABLE_ENTITY,
                "{\"message\": \"Order has already been delivered\"}"
            ));

        ApiError response = apiClient
            .orders()
            .cancelOrder()
            .orderIdPath(1L)
            .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY)))
            .as(ApiError.class);

        softly.assertThat(response.getCode()).isEqualTo(ErrorType.INVALID_RESOURCE_STATE);
        softly.assertThat(response.getMessage()).isEqualTo("Order has already been delivered");

        verifySearchLomOrder(1L);
        verify(lomClient).cancelOrder(1L);
    }

    @Test
    @DisplayName("Ошибка 409 при создании заявки в LOM")
    void lomClientCancellationConflictError() {
        mockSearchLomOrder(1L, new OrderDto());

        when(lomClient.cancelOrder(1L))
            .thenThrow(new HttpTemplateException(
                SC_UNPROCESSABLE_ENTITY,
                "{\"message\": \"Order has active cancellation requests\"}"
            ));

        ApiError response = apiClient
            .orders()
            .cancelOrder()
            .orderIdPath(1L)
            .execute(validatedWith(shouldBeCode(SC_UNPROCESSABLE_ENTITY)))
            .as(ApiError.class);

        softly.assertThat(response.getCode()).isEqualTo(ErrorType.INVALID_RESOURCE_STATE);
        softly.assertThat(response.getMessage()).isEqualTo("Order has active cancellation requests");

        verifySearchLomOrder(1L);
        verify(lomClient).cancelOrder(1L);
    }

    private CancelOrderResponse expectedResponse() {
        return new CancelOrderResponse()
            .id(10L)
            .status(CancellationRequestStatus.CREATED)
            .reason(CancellationRequestReason.SHOP_CANCELLED);
    }
}
