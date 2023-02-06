package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryNewStatus;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Обработка события прибытия заказа в ПВЗ")
public class PickupReadyEventProcessorTest extends AbstractIntegrationTest {
    @Autowired
    private LomEventMessageHandler lomEventMessageHandler;

    @Autowired
    private LogisticEventUtil logisticEventUtil;

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Успешная обработка события")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successProcessing(
        @SuppressWarnings("unused") String displayName,
        String snapshotFilePath
    ) {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/pickupready/diff/last_segment_transit_pickup_status.json",
                snapshotFilePath
            )
        ));
        logisticEventUtil.assertEventPayload(
            1L,
            LogisticEvent::getOrderDeliveryNewStatus,
            OrderDeliveryNewStatus.newBuilder()
                .setOrderId(2L)
                .setShopId(101L)
                .setStatus(OrderDeliveryNewStatus.DeliveryStatus.PICKUP_READY)
                .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(Instant.parse("2022-04-12T14:15:52Z")))
                .build(),
            softly
        );
    }

    @Test
    @DisplayName("Партнёр последней мили - курьер")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void lastSegmentIsCourierPartner() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/pickupready/diff/last_segment_transit_pickup_status.json",
                "logbroker/lom/event/pickupready/snapshot/last_segment_courier_segment.json"
            )
        ));
    }

    @Test
    @DisplayName("Новый статус - не TRANSIT_PICKUP")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusIsNotTransitPickup() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/pickupready/diff/last_segment_out_status.json",
                "logbroker/lom/event/pickupready/snapshot/market_last_mile_pickup_segment.json"
            )
        ));
    }

    @Test
    @DisplayName("Заказ YANDEX_GO - обрабатывать не нужно")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void yandexGoOrder() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/pickupready/diff/last_segment_transit_pickup_status.json",
                "logbroker/lom/event/pickupready/snapshot/yandex_go_last_mile_pickup_segment.json"
            )
        ));
    }

    @Nonnull
    private static Stream<Arguments> successProcessing() {
        return Stream.of(
            Arguments.of(
                "FBS/FBY, Последняя миля - ПВЗ",
                "logbroker/lom/event/pickupready/snapshot/market_last_mile_pickup_segment.json"
            ),
            Arguments.of(
                "FBS/FBY, Последняя миля - Почта",
                "logbroker/lom/event/pickupready/snapshot/market_last_mile_post_segment.json"
            ),
            Arguments.of(
                "FBS/FBY, Последняя миля - Го платформа",
                "logbroker/lom/event/pickupready/snapshot/market_last_mile_go_platform_segment.json"
            ),
            Arguments.of(
                "DBS Последняя миля - ПВЗ",
                "logbroker/lom/event/pickupready/snapshot/dbs_last_mile_pickup_segment.json"
            ),
            Arguments.of(
                "FaaS Последняя миля - ПВЗ",
                "logbroker/lom/event/pickupready/snapshot/faas_last_mile_pickup_segment.json"
            )
        );
    }
}
