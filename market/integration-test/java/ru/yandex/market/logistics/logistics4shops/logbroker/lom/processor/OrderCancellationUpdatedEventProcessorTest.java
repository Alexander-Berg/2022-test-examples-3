package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryCancelled;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Процессинг события отмены заказа")
class OrderCancellationUpdatedEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @Test
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_cancellation_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercancelled/diff/cancellation_order_request_success.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(eventPayload.getOrderDeliveryCancelled()))
            .isEqualTo(
                OrderDeliveryCancelled.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setReason(OrderDeliveryCancelled.CancelReason.SERVICE_FAULT)
                    .build()
            );
    }

    @Test
    @DisplayName("Подтверждённая ранее заявка")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successToSuccess() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercancelled/diff/cancellation_order_request_success_to_success.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("Не FaaS заказ")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsNotFaaS() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercancelled/diff/cancellation_order_request_success.json",
                "logbroker/lom/event/common/snapshot/not_faas_order.json"
            )
        ));
    }
}
