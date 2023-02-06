package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.time.Instant;
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

@DisplayName("Процессинг события доставки заказа")
class OrderDeliveredEventProcessorTest extends AbstractIntegrationTest {
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
                "logbroker/lom/event/orderdelivered/diff/last_mile_status_out.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
        LogisticEvent eventPayload = logisticEventUtil.getEventPayload(1L);
        ProtobufAssertionsUtils.prepareProtobufAssertion(softly.assertThat(eventPayload.getOrderDeliveryNewStatus()))
            .isEqualTo(
                OrderDeliveryNewStatus.newBuilder()
                    .setOrderId(2L)
                    .setShopId(101L)
                    .setStatus(OrderDeliveryNewStatus.DeliveryStatus.DELIVERED)
                    .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(Instant.parse("2022-04-12T14:15:52Z")))
                    .build()
            );
    }

    @Test
    @DisplayName("Событие не является получением статуса OUT")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void not50Checkpoint() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/orderdelivered/diff/last_mile_status_info_received.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("Статус OUT получила не первая миля")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outFromAnotherSegment() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/common/diff/return_segment_status_out.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }

    @Test
    @DisplayName("У заказа нет последней мили")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void orderHasNoFirstMile() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/orderdelivered/diff/last_mile_status_out.json",
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
                "logbroker/lom/event/orderdelivered/diff/last_mile_status_out.json",
                "logbroker/lom/event/common/snapshot/not_faas_order.json"
            )
        ));
    }

    @Test
    @DisplayName("Невалидный статус сегмента")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidSegmentStatus() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/orderdelivered/diff/last_mile_invalid_status.json",
                "logbroker/lom/event/common/snapshot/all_segments_are_created.json"
            )
        ));
    }
}
