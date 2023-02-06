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
import ru.yandex.market.logistics.logistics4shops.event.model.ImportWarehouseType;
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent;
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryNewStatus;
import ru.yandex.market.logistics.logistics4shops.event.model.ShipmentType;
import ru.yandex.market.logistics.logistics4shops.event.model.SortedAtScData;
import ru.yandex.market.logistics.logistics4shops.logbroker.LomEventMessageHandler;
import ru.yandex.market.logistics.logistics4shops.utils.LogisticEventUtil;
import ru.yandex.market.logistics.logistics4shops.utils.LomEventFactory;
import ru.yandex.market.logistics.logistics4shops.utils.ProtobufAssertionsUtils;

@DisplayName("Обработка события сортировки на СЦ")
@ParametersAreNonnullByDefault
public class SortedAtScEventProcessorTest extends AbstractIntegrationTest {
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
        String displayName,
        String snapshotFilePath,
        OrderDeliveryNewStatus orderDeliveryNewStatus
    ) {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordersorted/diff/segment_1_in_status.json",
                snapshotFilePath
            )
        ));
        logisticEventUtil.assertEventPayload(
            1L,
            LogisticEvent::getOrderDeliveryNewStatus,
            orderDeliveryNewStatus,
            softly
        );
    }

    @Test
    @DisplayName("Партнёр первой мили - Фулфилмент")
    @ExpectedDatabase(
        value = "/logbroker/lom/event/common/db/after/event_not_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void firstSegmentIsFfPartner() {
        lomEventMessageHandler.handle(List.of(
            LomEventFactory.eventDto(
                "logbroker/lom/event/ordersorted/diff/segment_1_in_status.json",
                "logbroker/lom/event/ordersorted/snapshot/ff_partner.json"
            )
        ));
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
                "logbroker/lom/event/ordersorted/diff/segment_1_out_status.json",
                "logbroker/lom/event/ordersorted/snapshot/sc_segment_after_dropship.json"
            )
        ));
    }

    @Nonnull
    private static Stream<Arguments> successProcessing() {
        return Stream.of(
            Arguments.of(
                "Самопривоз в СЦ",
                "logbroker/lom/event/ordersorted/snapshot/sc_segment_after_dropship.json",
                buildEventPayload(ShipmentType.IMPORT, ImportWarehouseType.SORTING_CENTER)
            ),
            Arguments.of(
                "Самопривоз в дропофф",
                "logbroker/lom/event/ordersorted/snapshot/dropoff_segment_after_dropship.json",
                buildEventPayload(ShipmentType.IMPORT, ImportWarehouseType.DROPOFF)
            ),
            Arguments.of(
                "Забор в СЦ",
                "logbroker/lom/event/ordersorted/snapshot/sc_segment_after_dropship_withdraw.json",
                buildEventPayload(ShipmentType.WITHDRAW, ImportWarehouseType.SORTING_CENTER)
            )
        );
    }

    @Nonnull
    private static OrderDeliveryNewStatus buildEventPayload(
        ShipmentType shipmentType,
        ImportWarehouseType warehouseType
    ) {
        return OrderDeliveryNewStatus.newBuilder()
            .setOrderId(2L)
            .setShopId(101L)
            .setStatus(OrderDeliveryNewStatus.DeliveryStatus.SORTED_AT_SC)
            .setStatusOriginalDate(ProtobufAssertionsUtils.toTimestamp(Instant.parse("2022-04-12T14:15:52Z")))
            .setSortedAtScData(SortedAtScData.newBuilder()
                .setShipmentType(shipmentType)
                .setImportWarehouseType(warehouseType)
                .build()
            )
            .build();
    }
}
