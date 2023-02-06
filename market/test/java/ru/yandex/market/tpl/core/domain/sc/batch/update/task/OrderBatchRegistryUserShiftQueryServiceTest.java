package ru.yandex.market.tpl.core.domain.sc.batch.update.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.task.projection.OrderProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.PickupPointDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.shift.task.projection.UserShiftWLockerOrdersProjection;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class OrderBatchRegistryUserShiftQueryServiceTest extends TplAbstractTest {

    public static final LocalDate SHIFT_DATE = LocalDate.now();
    private final TestUserHelper testUserHelper;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final OrderGenerateService orderGenerateService;
    private final OrderBatchRegistryUserShiftQueryService orderBatchRegistryUserShiftQueryService;

    @Test
    void fetchUserShiftWithOrdersForBatches() {
        User user = testUserHelper.findOrCreateUser(1L);
        UserShift userShift = testUserHelper.createEmptyShift(user, SHIFT_DATE);

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
        var clientDeliveryOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        var order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order2);

        List<UserShiftWLockerOrdersProjection> userShiftProjections =
                orderBatchRegistryUserShiftQueryService.fetchUserShiftWithOrdersForBatches(
                        Map.of(user.getId(), List.of(order.getId(), order2.getId())),
                        SHIFT_DATE
                );

        assertThat(userShiftProjections).hasSize(1);
        UserShiftWLockerOrdersProjection userShiftProjection = userShiftProjections.get(0);
        List<PickupPointDeliveryTaskProjection> lockerDeliveryTasks = userShiftProjection.getLockerDeliveryTasks();
        assertThat(lockerDeliveryTasks).hasSize(1);
        var orderExtIds = StreamEx.of(lockerDeliveryTasks.get(0).getOrders())
                .map(OrderProjection::getExternalId)
                .toSet();
        assertThat(orderExtIds).containsExactlyInAnyOrder(
                order.getExternalOrderId(),
                order2.getExternalOrderId()
        );
    }
}
