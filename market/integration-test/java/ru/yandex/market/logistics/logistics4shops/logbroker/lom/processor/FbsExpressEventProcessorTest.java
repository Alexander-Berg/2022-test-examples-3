package ru.yandex.market.logistics.logistics4shops.logbroker.lom.processor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

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

@DisplayName("Обработка событий прямого потока FbsExpress")
@ParametersAreNonnullByDefault
public class FbsExpressEventProcessorTest extends AbstractIntegrationTest {
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
        String diffFilePath,
        OrderDeliveryNewStatus.DeliveryStatus status
    ) {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                diffFilePath,
                "logbroker/lom/event/fbsexpress/snapshot/middle_mile_taxi_express_segment.json"
            )
        ));
        logisticEventUtil.assertEventPayload(
            1L,
            LogisticEvent::getOrderDeliveryNewStatus,
            OrderDeliveryNewStatus.newBuilder()
                .setOrderId(2L)
                .setShopId(101L)
                .setStatus(status)
                .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(Instant.parse("2022-04-12T14:15:52Z")))
                .build(),
            softly
        );
    }

    @Test
    @DisplayName("Новый статус не подлежит обработке")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusIsIn() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/fbsexpress/diff/in_status_received.json",
                "logbroker/lom/event/fbsexpress/snapshot/middle_mile_taxi_express_segment.json"
            )
        ));
    }

    @Nonnull
    private static Stream<Arguments> successProcessing() {
        return Stream.of(
            Arguments.of(
                "TRANSIT_COURIER_SEARCH от TAXI_EXPRESS сегмента",
                "logbroker/lom/event/fbsexpress/diff/transit_courier_search_status_received.json",
                OrderDeliveryNewStatus.DeliveryStatus.COURIER_SEARCH
            ),
            Arguments.of(
                "TRANSIT_COURIER_FOUND от TAXI_EXPRESS сегмента",
                "logbroker/lom/event/fbsexpress/diff/transit_courier_found_status_received.json",
                OrderDeliveryNewStatus.DeliveryStatus.COURIER_FOUND
            ),
            Arguments.of(
                "TRANSIT_COURIER_ARRIVED_TO_SENDER от TAXI_EXPRESS сегмента",
                "logbroker/lom/event/fbsexpress/diff/transit_courier_arrived_to_sender_status_received.json",
                OrderDeliveryNewStatus.DeliveryStatus.COURIER_ARRIVED
            ),
            Arguments.of(
                "TRANSIT_COURIER_SEARCH от TAXI_EXPRESS сегмента",
                "logbroker/lom/event/fbsexpress/diff/transit_transportation_recipient_status_received.json",
                OrderDeliveryNewStatus.DeliveryStatus.IN_DELIVERY
            )
        );
    }
}
