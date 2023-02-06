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

@DisplayName("Процессинг создания заказа на первой миле")
class OrderCreatedEventProcessorProcessorTest extends AbstractIntegrationTest {
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
                "logbroker/lom/event/ordercreated/diff/first_segment_external_id_changed.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(eventPayload.getOrderDeliveryNewStatus()))
            .isEqualTo(
                OrderDeliveryNewStatus.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(LomEventFactory.CREATED_TIMESTAMP))
                    .setStatus(OrderDeliveryNewStatus.DeliveryStatus.CREATED)
                    .build()
            );
    }

    @Test
    @DisplayName("Событие не является событием создания заказа")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onlyStatusChanged() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercreated/diff/first_mile_status_changed.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("Создание заказа произошло не на сегменте прямого потока")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnFlowSegmentCreated() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercreated/diff/return_flow_segment_created.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("Заказ создан не на всех сегментах")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderIsNotCreatedOnAllSegments() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercreated/diff/first_segment_external_id_changed.json",
                "logbroker/lom/event/ordercreated/snapshot/last_mile_segment_is_not_created.json"
            )
        ));
    }

    @Test
    @DisplayName("У заказа нет последней мили")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderHasNoLastMile() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordercreated/diff/first_segment_external_id_changed.json",
                "logbroker/lom/event/common/snapshot/no_last_mile.json"
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
                "logbroker/lom/event/ordercreated/diff/first_segment_external_id_changed.json",
                "logbroker/lom/event/common/snapshot/not_faas_order.json"
            )
        ));
    }
}
