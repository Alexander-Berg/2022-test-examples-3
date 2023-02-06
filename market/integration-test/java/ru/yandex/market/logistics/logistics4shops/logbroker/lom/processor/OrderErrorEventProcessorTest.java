package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.util.List;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryNewStatus;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Процессинг события перехода заказа в ошибочный статус")
class OrderErrorEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @Test
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordererror/diff/order_received_validation_error_status.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(eventPayload.getOrderDeliveryNewStatus()))
            .isEqualTo(
                OrderDeliveryNewStatus.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setStatus(OrderDeliveryNewStatus.DeliveryStatus.ERROR)
                    .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(LomEventFactory.CREATED_TIMESTAMP))
                    .build()
            );
    }

    @Test
    @DisplayName("Событие не является событием перехода в ошибочный статус")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusChangedToProcessing() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordererror/diff/order_received_processing_status.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("Событие является событием перехода в несуществующий статус")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusChangedToInvalidState() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordererror/diff/order_received_invalid_status.json",
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
                "logbroker/lom/event/ordererror/diff/order_received_validation_error_status.json",
                "logbroker/lom/event/common/snapshot/not_faas_order.json"
            )
        ));
    }
}
