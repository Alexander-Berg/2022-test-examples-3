package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.delivery.trust.client.model.request.BasketOrder;
import ru.yandex.market.delivery.trust.client.model.request.CreateBasketRequest;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CreateTrustBasketService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DisplayName("Создание корзины в Балансе")
class CreateTrustBasketServiceTest extends AbstractContextualTest {

    @Autowired
    private CreateTrustBasketService service;

    @Autowired
    private TrustClient trustClient;

    @AfterEach
    void verifyInteractions() {
        verifyNoMoreInteractions(trustClient);
    }

    @Test
    @DisplayName("Неизвестный заказ")
    @DatabaseSetup("/service/trust/before/create_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_basket.xml", assertionMode = NON_STRICT)
    void unknownOrderId() {
        long unknownOrderId = 2;
        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(unknownOrderId, 1L)))
            .hasMessage("Failed to find [ORDER] with id [2]");

        assertOrderHistoryNeverChanged(unknownOrderId);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/trust/before/create_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/create_basket.xml", assertionMode = NON_STRICT)
    void success() {
        OrderIdPayload payload = createOrderIdPayload(ORDER_ID, 1L);
        when(trustClient.createBasket(any(), any())).thenReturn("balance_basket_1");
        assertThat(service.processPayload(payload)).isEqualTo(ProcessingResult.success());
        verifyTrustCreateBasket();
        queueTaskChecker.assertQueueTaskCreated(QueueType.PAY_TRUST_BASKET, payload);
    }

    @Test
    @DisplayName("Успех из статуса BASKET_CREATION_FAILURE")
    @DatabaseSetup("/service/trust/before/create_basket_with_basket_creation_failure_status.xml")
    @ExpectedDatabase(
        value = "/service/trust/after/create_basket_with_basket_creation_failure_status.xml",
        assertionMode = NON_STRICT
    )
    void successWithBasketCreationFailureStatus() {
        OrderIdPayload payload = createOrderIdPayload(ORDER_ID, 1L);
        when(trustClient.createBasket(any(), any())).thenReturn("balance_basket_1");
        assertThat(service.processPayload(payload)).isEqualTo(ProcessingResult.success());
        verifyTrustCreateBasket();
        queueTaskChecker.assertQueueTaskCreated(QueueType.PAY_TRUST_BASKET, payload);
    }

    @Test
    @DisplayName("Предоплаченный заказ")
    @DatabaseSetup("/service/trust/before/create_basket_prepaid.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_basket_prepaid.xml", assertionMode = NON_STRICT)
    void prepaidOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 предоплачен")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ мерча в курьерскую платформу")
    @DatabaseSetup("/service/trust/before/create_basket_courier_for_shop.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_basket_courier_for_shop.xml", assertionMode = NON_STRICT)
    void courierForShopOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 является заказом мерча в курьерскую платформу")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ с невалидным статусом")
    @DatabaseSetup("/service/trust/before/create_basket_invalid.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_basket_invalid.xml", assertionMode = NON_STRICT)
    void existingBalanceOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID, 1L))).isEqualTo(
            ProcessingResult.unprocessed(
                "Неподдерживаемый статус для создания корзины в Трасте: ORDER_CREATION_FAILURE"
            )
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Заказ с пустым идентификатором платежа")
    @DatabaseSetup("/service/trust/before/create_order.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_order.xml", assertionMode = NON_STRICT)
    void orderBalancePaymentIsNull() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID, 1L))).isEqualTo(
            ProcessingResult.unprocessed(
                "Неподдерживаемый статус для создания корзины в Трасте: null"
            )
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Ошибка Баланса")
    @DatabaseSetup("/service/trust/before/create_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/create_basket.xml", assertionMode = NON_STRICT)
    void trustError() {
        doThrow(new RuntimeException("test exception")).when(trustClient).createBasket(any(), any());

        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(ORDER_ID, 1L)))
            .hasMessage("test exception");

        verifyTrustCreateBasket();
        assertOrderHistoryNeverChanged(ORDER_ID);
        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Test
    @DisplayName("Окончательная ошибка Баланса")
    @DatabaseSetup("/service/trust/before/create_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/create_basket_failure.xml", assertionMode = NON_STRICT)
    void finalTrustError() {
        service.processFinalFailure(createOrderIdPayload(1), mock(Exception.class));
    }

    private void verifyTrustCreateBasket() {
        ArgumentCaptor<CreateBasketRequest> requestCaptor = ArgumentCaptor.forClass(CreateBasketRequest.class);
        verify(trustClient).createBasket(eq("test-token"), requestCaptor.capture());

        CreateBasketRequest request = requestCaptor.getValue();
        softly.assertThat(request.getPayMethodId()).isEqualTo("cash-200600");
        softly.assertThat(request.getProductId()).isEqualTo("product-200");
        BasketOrder order = request.getOrders().get(0);
        softly.assertThat(order.getOrderId()).isEqualTo(100L);
        softly.assertThat(order.getPrice()).isEqualTo(new BigDecimal("100.5"));
    }

}
