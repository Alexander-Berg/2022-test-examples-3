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

@DisplayName("Обработка события перехода заказа в доставку")
@ParametersAreNonnullByDefault
public class InDeliveryEventProcessorTest extends AbstractIntegrationTest {
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
        String snapshotFilePath
    ) {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                diffFilePath,
                snapshotFilePath
            )
        ));
        logisticEventUtil.assertEventPayload(
            1L,
            LogisticEvent::getOrderDeliveryNewStatus,
            OrderDeliveryNewStatus.newBuilder()
                .setOrderId(2L)
                .setShopId(101L)
                .setStatus(OrderDeliveryNewStatus.DeliveryStatus.IN_DELIVERY)
                .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(Instant.parse("2022-04-12T14:15:52Z")))
                .build(),
            softly
        );
    }

    @Test
    @DisplayName("Новый статус - не IN")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void statusIsNotIn() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_transit_pickup.json",
                "logbroker/lom/event/indelivery/snapshot/market_with_movement_segment.json"
            )
        ));
    }

    @Test
    @DisplayName("Получен статус IN для FBS Express заказа")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void taxiExpressStatusIn() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/market_with_taxi_express_segment.json"
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
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/yandex_go_with_movement_segment.json"
            )
        ));
    }

    @Nonnull
    private static Stream<Arguments> successProcessing() {
        return Stream.of(
            Arguments.of(
                "FBS/FBY - есть MOVEMENT сегмент",
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/market_with_movement_segment.json"
            ),
            Arguments.of(
                "FBS/FBY - нет MOVEMENT сегмента",
                "logbroker/lom/event/indelivery/diff/last_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/market_without_movement_segment.json"
            ),
            Arguments.of(
                "DBS - есть MOVEMENT сегмент",
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/dbs_with_movement_segment.json"
            ),
            Arguments.of(
                "FaaS - есть MOVEMENT сегмент",
                "logbroker/lom/event/indelivery/diff/middle_mile_segment_status_in.json",
                "logbroker/lom/event/indelivery/snapshot/faas_with_movement_segment.json"
            )
        );
    }
}
