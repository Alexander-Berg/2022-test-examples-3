package ru.yandex.market.logistics.lom.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.processor.trust.SaveTrustOrderService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createTrustOrderIdPayload;

@DisplayName("Сохранение трастового заказа")
class SaveTrustOrderServiceTest extends AbstractContextualTest {

    private static final long TRUST_ORDER_ID = 1001L;

    @Autowired
    private SaveTrustOrderService saveTrustOrderService;

    @Test
    @DisplayName("Неизвестный заказ")
    @DatabaseSetup("/service/trust/before/save_order.xml")
    @ExpectedDatabase(value = "/service/trust/before/save_order.xml", assertionMode = NON_STRICT)
    void unknownOrderId() {
        long unknownOrderId = 2;
        assertThatThrownBy(
            () -> saveTrustOrderService.processPayload(createTrustOrderIdPayload(unknownOrderId, TRUST_ORDER_ID))
        ).hasMessage("Failed to find [ORDER] with id [2]");
        assertOrderHistoryNeverChanged(unknownOrderId);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/service/trust/before/save_order.xml")
    @ExpectedDatabase(value = "/service/trust/after/save_order.xml", assertionMode = NON_STRICT)
    void success() {
        assertThat(saveTrustOrderService.processPayload(createTrustOrderIdPayload(ORDER_ID, TRUST_ORDER_ID)))
            .isEqualTo(ProcessingResult.success());
    }

    @Test
    @DisplayName("Предоплаченный заказ")
    @DatabaseSetup("/service/trust/before/save_order_prepaid.xml")
    @ExpectedDatabase(value = "/service/trust/before/save_order_prepaid.xml", assertionMode = NON_STRICT)
    void prepaidOrder() {
        assertThat(saveTrustOrderService.processPayload(createTrustOrderIdPayload(ORDER_ID, TRUST_ORDER_ID)))
            .isEqualTo(ProcessingResult.unprocessed("Заказ 1 предоплачен"));
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

    @Test
    @DisplayName("Заказ мерча в курьерскую платформу")
    @DatabaseSetup("/service/trust/before/save_order_courier_for_shop.xml")
    @ExpectedDatabase(value = "/service/trust/before/save_order_courier_for_shop.xml", assertionMode = NON_STRICT)
    void courierForShopOrder() {
        assertThat(saveTrustOrderService.processPayload(createTrustOrderIdPayload(ORDER_ID, TRUST_ORDER_ID)))
            .isEqualTo(ProcessingResult.unprocessed("Заказ 1 является заказом мерча в курьерскую платформу"));
        assertOrderHistoryNeverChanged(ORDER_ID);
    }

}
