package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.shift.task.projection.OrderProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.UserShiftWLockerOrdersProjection;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBatchTaskFilterServiceTest {

    @Test
    void withEligibleOrdersForBatches() {
        var taskWithMultiPlaceOrder = createPickupPointDeliveryTask(PartnerSubType.PVZ, List.of(List.of("1", "2")));
        var taskToLocker = createPickupPointDeliveryTask(PartnerSubType.LOCKER, List.of(List.of("3")));
        var taskToPvzWith2SinglePlaceOrders = createPickupPointDeliveryTask(PartnerSubType.PVZ, List.of(List.of("4")
                , List.of("5")));
        var taskToPvzWith1SinglePlaceAnd1MultiPlaceOrder =
                createPickupPointDeliveryTask(PartnerSubType.PVZ, List.of(List.of("6"), List.of("7", "8")));
        var userShiftIn = UserShiftWLockerOrdersProjection.builder()
                .lockerDeliveryTasks(List.of(
                        taskWithMultiPlaceOrder,
                        taskToLocker,
                        taskToPvzWith2SinglePlaceOrders,
                        taskToPvzWith1SinglePlaceAnd1MultiPlaceOrder
                ))
                .build();
        var userShiftOut = new OrderBatchTaskFilterService().withEligibleOrdersForBatches(userShiftIn);
        assertThat(userShiftOut).isEqualTo(UserShiftWLockerOrdersProjection.builder()
                .lockerDeliveryTasks(List.of(
                        taskToPvzWith2SinglePlaceOrders,
                        createPickupPointDeliveryTask(PartnerSubType.PVZ, List.of(List.of("6")))
                ))
                .build());
    }

    private PickupPointDeliveryTaskProjection createPickupPointDeliveryTask(PartnerSubType partnerSubType,
                                                                            List<List<String>> places) {
        List<OrderProjection> orders = StreamEx.of(places)
                .map(p -> OrderProjection.builder()
                        .orderPlaceBarcodes(p)
                        .build())
                .toList();
        return PickupPointDeliveryTaskProjection.builder()
                .pickupPoint(PickupPointProjection.builder()
                        .partnerSubType(partnerSubType)
                        .build())
                .orders(orders)
                .build();
    }
}
