package ru.yandex.market.delivery.tracker.service.tracking;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.InboundDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.InboundOldDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.MovementDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OutboundDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.OutboundOldDeliveryCheckpointStatus;
import ru.yandex.market.delivery.tracker.domain.enums.TransferDeliveryCheckpointStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.ATTEMPT_FAIL;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.DELIVERED;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.EXCEPTION;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.EXPIRED;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.INFO_RECEIVED;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.IN_TRANSIT;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.OUT_FOR_DELIVERY;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.PENDING;
import static ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.UNKNOWN;

class CheckpointMapperTest {

    @Test
    void testAllCheckpointsHaveMapping() {
        assertThat(
            OrderDeliveryCheckpointMapper.MAP.size() +
                MovementDeliveryCheckpointMapper.MAP.size() +
                InboundDeliveryCheckpointMapper.MAP.size() +
                OutboundDeliveryCheckpointMapper.MAP.size() +
                TransferDeliveryCheckpointMapper.MAP.size() +
                InboundOldDeliveryCheckpointMapper.MAP.size() +
                OutboundOldDeliveryCheckpointMapper.MAP.size()
        )
            .as("Every delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(
                OrderDeliveryCheckpointStatus.values().length +
                    MovementDeliveryCheckpointStatus.values().length +
                    InboundDeliveryCheckpointStatus.values().length +
                    OutboundDeliveryCheckpointStatus.values().length +
                    TransferDeliveryCheckpointStatus.values().length +
                    InboundOldDeliveryCheckpointStatus.values().length +
                    OutboundOldDeliveryCheckpointStatus.values().length
            );
    }

    @Test
    void testAllOrderCheckpointsHaveMapping() {
        assertThat(OrderDeliveryCheckpointMapper.MAP.size())
            .as("Every order delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(OrderDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllMovementCheckpointsHaveMapping() {
        assertThat(MovementDeliveryCheckpointMapper.MAP.size())
            .as("Every movement delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(MovementDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllInboundCheckpointsHaveMapping() {
        assertThat(InboundDeliveryCheckpointMapper.MAP.size())
            .as("Every inbound delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(InboundDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllOutboundCheckpointsHaveMapping() {
        assertThat(OutboundDeliveryCheckpointMapper.MAP.size())
            .as("Every outbound delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(OutboundDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllTransferCheckpointsHaveMapping() {
        assertThat(TransferDeliveryCheckpointMapper.MAP.size())
            .as("Every transfer delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(TransferDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllInboundOldCheckpointsHaveMapping() {
        assertThat(InboundOldDeliveryCheckpointMapper.MAP.size())
            .as("Every old API inbound delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(InboundOldDeliveryCheckpointStatus.values().length);
    }

    @Test
    void testAllOutboundOldCheckpointsHaveMapping() {
        assertThat(OutboundOldDeliveryCheckpointMapper.MAP.size())
            .as("Every old API outbound delivery checkpoint has to have mapping to CheckpointStatus")
            .isEqualTo(OutboundOldDeliveryCheckpointStatus.values().length);
    }

    @ParameterizedTest
    @MethodSource("checkpointStatusesMapping")
    void testCheckpointStatusesMapping(DeliveryCheckpointStatus deliveryCheckpointStatus,
                                       CheckpointStatus expectedCheckpointStatus) {
        CheckpointStatus checkpointStatus = DeliveryCheckpointMapper.mapDeliveryStatus(deliveryCheckpointStatus);
        assertThat(checkpointStatus).isEqualTo(expectedCheckpointStatus);
    }

    private static Stream<Arguments> checkpointStatusesMapping() {
        return Stream.of(
            Arguments.of(OrderDeliveryCheckpointStatus.SENDER_SENT, PENDING),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_LOADED, INFO_RECEIVED),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_CUSTOMS_ARRIVED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_CUSTOMS_CLEARED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_AT_START_SORT, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_RECIPIENT, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_DELIVERY, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_UPDATED_BY_SHOP, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXTENDED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_STORAGE_PERIOD_EXPIRED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT, OUT_FOR_DELIVERY),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT, OUT_FOR_DELIVERY),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT, OUT_FOR_DELIVERY),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_ATTEMPT_FAILED, ATTEMPT_FAIL),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_CAN_NOT_BE_COMPLETED, ATTEMPT_FAIL),
            Arguments.of(OrderDeliveryCheckpointStatus.DELIVERY_DELIVERED, DELIVERED),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_PREPARING, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_ARRIVED_DELIVERY, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.RETURN_TRANSMITTED_FULFILMENT, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_CREATED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_LOADED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_ERROR, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_CANCELED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_AT_START, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_OUT_OF_STOCK, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_AUTOMATICALLY_REMOVED_ITEMS, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_AWAITING_CLARIFICATION, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_PLACES_CHANGED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_PREPARED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_TRANSMITTED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ARRIVED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_PREPARING_SENDER, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_TRANSFERRED, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RETURNED, IN_TRANSIT),
            Arguments.of(
                OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_ORDER_PARTIALLY_RECEIPT_AT_SECONDARY_RECEPTION,
                IN_TRANSIT
            ),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RFF_PREPARING_FULFILLMENT, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RFF_TRANSMITTED_FULFILLMENT, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.SORTING_CENTER_RETURN_RFF_ARRIVED_FULFILLMENT, IN_TRANSIT),
            Arguments.of(OrderDeliveryCheckpointStatus.LOST, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.ERROR_NOT_FOUND, EXPIRED),
            Arguments.of(OrderDeliveryCheckpointStatus.CANCELED, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.ERROR, EXCEPTION),
            Arguments.of(OrderDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(MovementDeliveryCheckpointStatus.PENDING, PENDING),
            Arguments.of(MovementDeliveryCheckpointStatus.CREATED, INFO_RECEIVED),
            Arguments.of(MovementDeliveryCheckpointStatus.ERROR, EXCEPTION),
            Arguments.of(MovementDeliveryCheckpointStatus.CANCELLED, EXCEPTION),
            Arguments.of(MovementDeliveryCheckpointStatus.CONFIRMED, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.CANCELLED_BY_PARTNER, EXCEPTION),
            Arguments.of(MovementDeliveryCheckpointStatus.COURIER_FOUND, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.OUTBOUND_WAREHOUSE_REACHED, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.HANDED_OVER, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.DELIVERING, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.INBOUND_WAREHOUSE_REACHED, IN_TRANSIT),
            Arguments.of(MovementDeliveryCheckpointStatus.DELIVERED, DELIVERED),
            Arguments.of(MovementDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(InboundDeliveryCheckpointStatus.PENDING, PENDING),
            Arguments.of(InboundDeliveryCheckpointStatus.CREATED, INFO_RECEIVED),
            Arguments.of(InboundDeliveryCheckpointStatus.ARRIVED, IN_TRANSIT),
            Arguments.of(InboundDeliveryCheckpointStatus.ACCEPTANCE, IN_TRANSIT),
            Arguments.of(InboundDeliveryCheckpointStatus.ACCEPTED, IN_TRANSIT),
            Arguments.of(InboundDeliveryCheckpointStatus.SHIPPED, DELIVERED),
            Arguments.of(InboundDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(OutboundDeliveryCheckpointStatus.PENDING, PENDING),
            Arguments.of(OutboundDeliveryCheckpointStatus.CREATED, INFO_RECEIVED),
            Arguments.of(OutboundDeliveryCheckpointStatus.CANCELLED, EXCEPTION),
            Arguments.of(OutboundDeliveryCheckpointStatus.ASSEMBLING, IN_TRANSIT),
            Arguments.of(OutboundDeliveryCheckpointStatus.ASSEMBLED, IN_TRANSIT),
            Arguments.of(OutboundDeliveryCheckpointStatus.TRANSFERRED, DELIVERED),
            Arguments.of(OutboundDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(TransferDeliveryCheckpointStatus.NEW, INFO_RECEIVED),
            Arguments.of(TransferDeliveryCheckpointStatus.PROCESSING, IN_TRANSIT),
            Arguments.of(TransferDeliveryCheckpointStatus.ACCEPTED, IN_TRANSIT),
            Arguments.of(TransferDeliveryCheckpointStatus.COMPLETED, DELIVERED),
            Arguments.of(TransferDeliveryCheckpointStatus.ERROR, EXCEPTION),
            Arguments.of(TransferDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(InboundOldDeliveryCheckpointStatus.PENDING, PENDING),
            Arguments.of(InboundOldDeliveryCheckpointStatus.CREATED, INFO_RECEIVED),
            Arguments.of(InboundOldDeliveryCheckpointStatus.ARRIVED, IN_TRANSIT),
            Arguments.of(InboundOldDeliveryCheckpointStatus.ACCEPTANCE, IN_TRANSIT),
            Arguments.of(InboundOldDeliveryCheckpointStatus.ACCEPTED, IN_TRANSIT),
            Arguments.of(InboundOldDeliveryCheckpointStatus.SHIPPED, DELIVERED),
            Arguments.of(InboundOldDeliveryCheckpointStatus.UNKNOWN, UNKNOWN),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.PENDING, PENDING),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.CREATED, INFO_RECEIVED),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.ERROR, EXCEPTION),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.CANCELLED, EXCEPTION),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.ASSEMBLING, IN_TRANSIT),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.ASSEMBLED, IN_TRANSIT),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.TRANSFERRED, DELIVERED),
            Arguments.of(OutboundOldDeliveryCheckpointStatus.UNKNOWN, UNKNOWN)
        );
    }
}
