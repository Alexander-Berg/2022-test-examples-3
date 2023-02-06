package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RoutingListDataQueryServiceTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandDataHelper helper;
    private final PickupPointRepository pickupPointRepository;

    private final UserShiftOrderQueryService userShiftOrderQueryService;

    private RoutePoint orderDeliveryRoutePoint;
    private Order regularOrder;
    private Order lockerOrder;
    private OrderDeliveryTask orderDeliveryTask;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void init() {
        var user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        orderDeliveryRoutePoint = testDataFactory.createEmptyRoutePoint(user, userShiftId);
        regularOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .build());
        orderDeliveryRoutePoint.addDeliveryTask(
                helper.taskUnpaid("asdf", 3, regularOrder.getId()).getOrderReference(),
                Instant.now(),
                true
        );
        orderDeliveryTask = orderDeliveryRoutePoint.streamOrderDeliveryTasks()
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        UserShift userShift = orderDeliveryRoutePoint.getUserShift();
        lockerOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now())
                        .pickupPoint(pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)))
                        .build()
        );
        userShift.addLockerDeliverySubtask(NewDeliveryRoutePointData.builder()
                        .withOrderReferenceFromOrder(lockerOrder, true, false)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .expectedArrivalTime(Instant.now())
                        .name("asdf")
                        .address(new RoutePointAddress("asfd", GeoPointGenerator.generateLonLat()))
                        .build(),
                null,
                false
        );

        lockerDeliveryTask = userShift.streamDeliveryTasks()
                .select(LockerDeliveryTask.class)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }


    @Test
    void getDeliverySubtasksByOrder() {
        DeliverySubtask actualOrderDeliveryTask = userShiftOrderQueryService.findDeliverySubtasksByOrder(regularOrder)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(actualOrderDeliveryTask).isEqualTo(orderDeliveryTask);
        DeliverySubtask actualLockerDeliveryTask = userShiftOrderQueryService.findDeliverySubtasksByOrder(lockerOrder)
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(actualLockerDeliveryTask).isEqualTo(lockerDeliveryTask.getSubtasks().get(0));
    }
}
