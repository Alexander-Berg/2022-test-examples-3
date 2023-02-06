package ru.yandex.market.logistics.lom.controller.order.processing;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderSegmentRequestPayload;
import ru.yandex.market.logistics.lom.jobs.processor.ChangeOrderToOnDemandRequestStatusUpdateProcessor;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Обновление статуса заявки преобразование заказа в заказ с доставкой по клику")
@DatabaseSetup("/controller/order/change_order_to_on_demand/setup.xml")
@DatabaseSetup("/controller/order/change_order_to_on_demand/status_update/before/setup.xml")
class ChangeOrderToOnDemandRequestStatusUpdateProcessorTest extends AbstractContextualTest {

    private static final ChangeOrderSegmentRequestPayload PAYLOAD =
        PayloadFactory.createChangeOrderSegmentRequestPayload(1L, "1", 1L);

    @Autowired
    private ChangeOrderToOnDemandRequestStatusUpdateProcessor processor;

    @Test
    @DisplayName("Статус SUCCESS")
    @DatabaseSetup("/controller/order/change_order_to_on_demand/status_update/before/success.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_order_to_on_demand/status_update/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testProcessSuccessStatus() {
        processor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Статус FAIL")
    @DatabaseSetup("/controller/order/change_order_to_on_demand/status_update/before/fail.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_order_to_on_demand/status_update/after/fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testProcessFailStatus() {
        processor.processPayload(PAYLOAD);
    }
}
