package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.trust.PayTrustBasketService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DisplayName("Оплата корзины в Балансе")
class PayTrustBasketServiceTest extends AbstractContextualTest {

    @Autowired
    private TrustClient trustClient;

    @Autowired
    private PayTrustBasketService service;

    @AfterEach
    void verifyInteractions() {
        verifyNoMoreInteractions(trustClient);
    }

    @Test
    @DisplayName("Неизвестный заказ")
    @DatabaseSetup("/service/trust/before/pay_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/pay_basket.xml", assertionMode = NON_STRICT)
    void unknownOrderId() {
        long unknownOrderId = 2;
        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(unknownOrderId)))
            .hasMessage("Failed to find [ORDER] with id [2]");

        assertOrderHistoryNeverChanged(unknownOrderId);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/trust/before/pay_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/pay_basket.xml", assertionMode = NON_STRICT)
    void success() {
        when(trustClient.createBasket(any(), any())).thenReturn("balance_basket_1");

        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(ProcessingResult.success());

        verifyTrustPayBasket();
    }

    @Test
    @DisplayName("Успех из статуса BASKET_PAYMENT_FAILURE")
    @DatabaseSetup("/service/trust/before/pay_basket_with_basket_payment_failure_status.xml")
    @ExpectedDatabase(
        value = "/service/trust/after/pay_basket_with_basket_payment_failure_status.xml",
        assertionMode = NON_STRICT
    )
    void successWithBasketPaymentFailureStatus() {
        when(trustClient.createBasket(any(), any())).thenReturn("balance_basket_1");

        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(ProcessingResult.success());

        verifyTrustPayBasket();
    }

    @Test
    @DisplayName("Предоплаченный заказ")
    @DatabaseSetup("/service/trust/before/pay_basket_prepaid.xml")
    @ExpectedDatabase(value = "/service/trust/before/pay_basket_prepaid.xml", assertionMode = NON_STRICT)
    void prepaidOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 предоплачен")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ мерча в курьерскую платформу")
    @DatabaseSetup("/service/trust/before/pay_basket_courier_for_shop.xml")
    @ExpectedDatabase(value = "/service/trust/before/pay_basket_courier_for_shop.xml", assertionMode = NON_STRICT)
    void courierForShopOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 является заказом мерча в курьерскую платформу")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ с невалидным статусом")
    @DatabaseSetup("/service/trust/before/pay_basket_invalid.xml")
    @ExpectedDatabase(value = "/service/trust/before/pay_basket_invalid.xml", assertionMode = NON_STRICT)
    void existingBalanceOrder() {
        assertThat(service.processPayload(createOrderIdPayload(ORDER_ID))).isEqualTo(
            ProcessingResult.unprocessed("Неподдерживаемый статус для оплаты корзины в Трасте: BASKET_CREATION_FAILURE")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Ошибка Баланса")
    @DatabaseSetup("/service/trust/before/pay_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/pay_basket.xml", assertionMode = NON_STRICT)
    void trustError() {
        doThrow(new RuntimeException("test exception")).when(trustClient).payBasket(any(), any());

        assertThatThrownBy(() -> service.processPayload(createOrderIdPayload(ORDER_ID)))
            .hasMessage("test exception");

        verifyTrustPayBasket();
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Окончательная ошибка Баланса")
    @DatabaseSetup("/service/trust/before/pay_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/pay_basket_failure.xml", assertionMode = NON_STRICT)
    void finalTrustError() {
        service.processFinalFailure(createOrderIdPayload(1), mock(Exception.class));
    }

    private void verifyTrustPayBasket() {
        verify(trustClient).payBasket("test-token", "balance_basket_1");
    }

}
