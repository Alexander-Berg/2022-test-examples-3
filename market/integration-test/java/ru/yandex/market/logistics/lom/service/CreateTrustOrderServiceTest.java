package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.delivery.trust.client.model.request.CreateOrderRequest;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CreateTrustOrderService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DisplayName("Создание заказа в Балансе")
class CreateTrustOrderServiceTest extends AbstractContextualTest {

    @Autowired
    private CreateTrustOrderService service;

    @Autowired
    private TrustClient trustClient;

    @AfterEach
    void verifyInteractions() {
        verifyNoMoreInteractions(trustClient);
    }

    @Test
    @DisplayName("Неизвестный заказ")
    @DatabaseSetup("/service/trust/before/create_order.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order.xml", assertionMode = NON_STRICT)
    void unknownOrderId() {
        long unknownOrderId = 2;
        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(unknownOrderId)))
            .hasMessage("Failed to find [ORDER] with id [2]");

        assertOrderHistoryNeverChanged(unknownOrderId);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/trust/before/create_order.xml")
    @ExpectedDatabase(value = "/service/trust/after/create_order.xml", assertionMode = NON_STRICT)
    void success() {
        doReturn(100L).when(trustClient).createOrder(any(), any());

        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(ProcessingResult.success());
        verifyTrustCreateOrder();
    }

    @Test
    @DisplayName("Успех из статуса ORDER_CREATION_FAILURE")
    @DatabaseSetup("/service/trust/before/create_order_with_order_creation_failure_status.xml")
    @ExpectedDatabase(value = "/service/trust/after/create_order.xml", assertionMode = NON_STRICT)
    void successWithOrderCreationFailureStatus() {
        doReturn(100L).when(trustClient).createOrder(any(), any());

        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(ProcessingResult.success());
        verifyTrustCreateOrder();
    }

    @Test
    @DisplayName("Предоплаченный заказ")
    @DatabaseSetup("/service/trust/before/create_order_prepaid.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order_prepaid.xml", assertionMode = NON_STRICT)
    void prepaidOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 предоплачен")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ мерча в курьерскую платформу")
    @DatabaseSetup("/service/trust/before/create_order_courier_for_shop.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order_courier_for_shop.xml", assertionMode = NON_STRICT)
    void courierForShopOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 является заказом мерча в курьерскую платформу")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ с данными баланса")
    @DatabaseSetup("/service/trust/before/create_order_existing.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order_existing.xml", assertionMode = NON_STRICT)
    void existingBalanceOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Неподдерживаемый статус для создания заказа в Трасте: ORDER_CREATED"
            ));
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Ошибка Баланса")
    @DatabaseSetup("/service/trust/before/create_order.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order.xml", assertionMode = NON_STRICT)
    void trustError() {
        doThrow(new RuntimeException("test exception")).when(trustClient).createOrder(any(), any());

        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(ORDER_ID)))
            .hasMessage("test exception");

        verifyTrustCreateOrder();
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Окончательная ошибка Баланса")
    @DatabaseSetup("/service/trust/before/create_order.xml")
    @ExpectedDatabase(value = "/service/trust/after/create_order_failure.xml", assertionMode = NON_STRICT)
    void finalTrustError() {
        service.processFinalFailure(createOrderIdPayload(ORDER_ID), mock(Exception.class));
    }

    private void verifyTrustCreateOrder() {
        ArgumentCaptor<CreateOrderRequest> requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(trustClient).createOrder(eq("test-token"), requestCaptor.capture());

        CreateOrderRequest request = requestCaptor.getValue();
        softly.assertThat(request.getProductId()).isEqualTo("product-200");
        softly.assertThat(request.getCommission()).isEqualTo(170);
    }

}
