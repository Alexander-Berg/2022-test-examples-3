package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.trust.client.TrustClient;
import ru.yandex.market.delivery.trust.client.model.response.BasketStatus;
import ru.yandex.market.delivery.trust.client.model.response.GetBasketResponse;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.trust.CheckTrustBasketService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

@DisplayName("Проверка статуса корзины в Балансе")
class CheckTrustBasketServiceTest extends AbstractContextualTest {

    private static final String SERVICE_TOKEN = "test-token";
    private static final String BASKET_ID = "balance_basket_1";

    @Autowired
    private TrustClient trustClient;

    @Autowired
    private CheckTrustBasketService service;

    @Test
    @DisplayName("Неизвестный заказ")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/check_basket.xml", assertionMode = NON_STRICT)
    void unknownOrder() {
        assertThatThrownBy(() -> checkBasket(2L))
            .hasMessage("Failed to find [ORDER] with id [2]");

        verifyZeroInteractions(trustClient);
    }

    @Test
    @DisplayName("Платеж в неподдерживаемом статусе")
    @DatabaseSetup("/service/trust/before/check_basket_wrong_status.xml")
    @ExpectedDatabase(
        value = "/service/trust/before/check_basket_wrong_status.xml",
        assertionMode = NON_STRICT
    )
    void wrongPaymentStatus() {
        assertThat(checkBasket(1L)).isEqualTo(
            ProcessingResult.unprocessed("Неподдерживаемый статус для проверки корзины в Трасте: BASKET_CREATED")
        );

        verifyZeroInteractions(trustClient);
    }

    @Test
    @DisplayName("Статус в Балансе не изменился")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/check_basket.xml", assertionMode = NON_STRICT)
    void noUpdate() {
        mockBasketStatus(BasketStatus.STARTED);

        assertThat(checkBasket(1L)).isEqualTo(ProcessingResult.success());

        verify(trustClient).getBasket(SERVICE_TOKEN, BASKET_ID);
    }

    @Test
    @DisplayName("Платеж отменен в Балансе")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/check_basket_error.xml", assertionMode = NON_STRICT)
    void cancelledPayment() {
        mockBasketStatus(BasketStatus.CANCELLED);

        assertThat(checkBasket(1L)).isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Платеж не подтвержден в Балансе")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/check_basket_error.xml", assertionMode = NON_STRICT)
    void notAuthorizedPayment() {
        mockBasketStatus(BasketStatus.NOT_AUTHORIZED);

        assertThat(checkBasket(1L)).isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Ошибка Баланса")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/before/check_basket.xml", assertionMode = NON_STRICT)
    void failure() {
        doThrow(new RuntimeException("test exception")).when(trustClient).getBasket(SERVICE_TOKEN, BASKET_ID);

        assertThatThrownBy(() -> checkBasket(1L)).hasMessage("test exception");
    }

    @Test
    @DisplayName("Окончательная ошибка Баланса")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/check_basket_failure.xml", assertionMode = NON_STRICT)
    void finalFailure() {
        service.processFinalFailure(createOrderIdPayload(1L), mock(Exception.class));
    }

    @Test
    @DisplayName("Успешное обновление")
    @DatabaseSetup("/service/trust/before/check_basket.xml")
    @ExpectedDatabase(value = "/service/trust/after/check_basket_complete.xml", assertionMode = NON_STRICT)
    void success() {
        mockBasketStatus(BasketStatus.CLEARED);

        assertThat(checkBasket(1L)).isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Предоплаченный заказ")
    @DatabaseSetup("/service/trust/before/check_basket_prepaid.xml")
    @ExpectedDatabase(value = "/service/trust/before/check_basket_prepaid.xml", assertionMode = NON_STRICT)
    void prepaidOrder() {
        mockBasketStatus(BasketStatus.CLEARED);

        assertThat(checkBasket(1L)).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 предоплачен")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ мерча в курьерскую платформу")
    @DatabaseSetup("/service/trust/before/check_basket_courier_for_shop.xml")
    @ExpectedDatabase(value = "/service/trust/before/check_basket_courier_for_shop.xml", assertionMode = NON_STRICT)
    void courierForShopOrder() {
        mockBasketStatus(BasketStatus.CLEARED);

        assertThat(checkBasket(1L)).isEqualTo(
            ProcessingResult.unprocessed("Заказ 1 является заказом мерча в курьерскую платформу")
        );
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    private ProcessingResult checkBasket(Long orderId) {
        return service.processPayload(createOrderIdPayload(orderId));
    }

    private void mockBasketStatus(BasketStatus status) {
        when(trustClient.getBasket(SERVICE_TOKEN, BASKET_ID))
            .thenReturn(
                GetBasketResponse.builder()
                    .basketId(BASKET_ID)
                    .status(status)
                    .build()
            );
    }
}
