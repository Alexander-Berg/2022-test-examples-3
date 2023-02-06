package ru.yandex.market.tpl.api.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus;
import ru.yandex.market.tpl.api.test.TplApiAbstractTest;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayLockerDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayOrderDeliveryTaskProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayRoutePointMapper;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayRoutePointProjection;
import ru.yandex.market.tpl.core.domain.usershift.projection.UserTodayShiftProjection;
import ru.yandex.market.tpl.core.service.order.OrderFeaturesResolver;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserTodayRoutePointMapperTest extends TplApiAbstractTest {
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final OrderGenerateService orderGenerateService;
    private final UserTodayRoutePointMapper userTodayRoutePointMapper;
    private final Clock clock;
    private final OrderRepository orderRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandService commandService;
    private final OrderFeaturesResolver orderFeaturesResolver;

    @Test
    @Transactional
    void mapProjection() {
        User user = userHelper.findOrCreateUser(345763985L);
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        PickupPoint pickupPoint = PickupPointGenerator.generatePickupPoint(3463476346L);
        pickupPointRepository.save(pickupPoint);


        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        LockerDeliveryTask lockerTask1 = userHelper.addLockerDeliveryTaskToShift(user,
                userShift, order1);

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var clientReturn2 = clientReturnGenerator.generateReturnFromClient();
        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        var tod2 = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn2.getId(), deliveryTime
                )
        );

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());
        OrderDeliveryTask orderTask1 =
                (OrderDeliveryTask) userHelper.addDeliveryTaskToShift(user,
                        userShift, order2);
        userHelper.addCallTask(
                UserShiftCommand.CreateCallToRecipientTask.builder()
                        .userShiftId(userShift.getId())
                        .orderDeliveryTaskId(orderTask1.getId())
                        .expectedCallTime(Instant.now(clock))
                        .build(),
                userShift,
                orderTask1,
                CallToRecipientTaskStatus.NOT_CALLED
        );

        var result = userTodayRoutePointMapper.map(user.getId());
        checkUserShift(result, userShift);
    }

    private void checkUserShift(UserTodayShiftProjection userTodayShiftProjection, UserShift userShift) {
        userShift.streamRoutePoints().forEach(rp ->
                assertRoutePointAndTasks(userTodayShiftProjection, rp)
        );
    }

    private void assertRoutePointAndTasks(
            UserTodayShiftProjection userTodayShiftProjection,
            RoutePoint routePoint
    ) {
        var rp =
                StreamEx.of(userTodayShiftProjection.getRoutePointProjections()).filter(rpProj -> Objects.equals(rpProj.getId(),
                        routePoint.getId())).findFirst().orElseThrow();
        assertRoutePoint(rp, routePoint);

        ZoneOffset zoneOffset = routePoint.getSortingCenter().getZoneOffset();

        routePoint.streamOrderDeliveryTasks().forEach(task -> {
            var taskProjRes =
                    StreamEx.of(rp.getUserTodayOrderDeliveryTaskProjections()).filter(taskProj -> Objects.equals(taskProj.getId(), task.getId())).findFirst().orElseThrow();
            assertTask(taskProjRes, task, zoneOffset);
        });
        routePoint.streamLockerDeliveryTasks().forEach(task -> {
            var taskProjRes =
                    StreamEx.of(rp.getUserTodayLockerDeliveryTaskProjections()).filter(taskProj -> Objects.equals(taskProj.getId(), task.getId())).findFirst().orElseThrow();
            assertTask(taskProjRes, task, rp);
        });
    }

    private void assertRoutePoint(UserTodayRoutePointProjection rpProjection, RoutePoint routePoint) {
        if (rpProjection.getRoutePointType() != RoutePointType.ORDER_PICKUP &&
                rpProjection.getRoutePointType() != RoutePointType.ORDER_RETURN) {
            assertThat(!routePoint.getStatus().isTerminal()).isEqualTo(rpProjection.isActive());
            assertThat(routePoint.getType()).isEqualTo(rpProjection.getRoutePointType());
            assertThat(rpProjection.getId()).isEqualTo(routePoint.getId());
            assertThat(routePoint.getAddressString()).isEqualTo(rpProjection.getAddressString());
            assertThat(routePoint.getExpectedDateTime()).isEqualTo(rpProjection.getExpectedDateTime());
            assertThat(rpProjection.getUserTodayLockerDeliveryTaskProjections().size()).isEqualTo(routePoint.streamLockerDeliveryTasks().count());
            assertThat(rpProjection.getUserTodayOrderDeliveryTaskProjections().size()).isEqualTo(routePoint.streamOrderDeliveryTasks().count());
        }
    }

    private void assertTask(UserTodayLockerDeliveryTaskProjection lockerProjection, LockerDeliveryTask task,
                            UserTodayRoutePointProjection rpProjection) {
        assertThat(lockerProjection.getPickupPointId()).isEqualTo(task.getPickupPointId());
        PickupPoint pickupPoint = pickupPointRepository.getById(task.getPickupPointId());
        assertThat(pickupPoint.getPartnerSubType()).isEqualTo(rpProjection.getLockerDeliveryType());
        assertThat(lockerProjection.getId()).isEqualTo(task.getId());
        assertThat(lockerProjection.getLockerDeliveryTaskStatus()).isEqualTo(task.getStatus());
        assertThat(lockerProjection.getOrdinalNumber()).isEqualTo(task.getOrdinalNumber());
    }

    private void assertTask(
            UserTodayOrderDeliveryTaskProjection orderProjection,
            OrderDeliveryTask task,
            ZoneOffset zoneOffset
    ) {
        assertThat(orderProjection.isClientReturn()).isEqualTo(task.isClientReturn());
        assertThat(orderProjection.getMultiOrderId()).isEqualTo(task.getMultiOrderId());
        assertThat(orderProjection.getId()).isEqualTo(task.getId());
        assertThat(orderProjection.getOrderDeliveryTaskStatus()).isEqualTo(task.getStatus());
        assertThat(orderProjection.getOrdinalNumber()).isEqualTo(task.getOrdinalNumber());
        if (task.getOrderId() != null) {
            Order order = orderRepository.getById(task.getOrderId());
            assertThat(orderProjection.isFashion()).isEqualTo(orderFeaturesResolver.isFashion(order));
            assertThat(orderProjection.getRecipientFio()).isEqualTo(order.getDelivery().getRecipientFio());
            assertThat(order.getOrderFlowStatus()).isEqualTo(orderProjection.getOrderFlowStatus());
            assertThat(orderProjection.getIntervalFrom()).isEqualTo(
                    LocalDateTime.ofInstant(order.getDelivery().getDeliveryIntervalFrom(), zoneOffset)
            );
            assertThat(orderProjection.getIntervalTo()).isEqualTo(
                    LocalDateTime.ofInstant(order.getDelivery().getDeliveryIntervalTo(), zoneOffset)
            );
        } else {
            ClientReturn clientReturn = clientReturnRepository.getById(task.getClientReturnId());
            assertThat(orderProjection.isFashion()).isEqualTo(false);
            assertThat(orderProjection.getRecipientFio()).isEqualTo(clientReturn.getClient().getClientData().getFullName());
            assertThat(orderProjection.getIntervalFrom()).isEqualTo(clientReturn.getArriveIntervalFrom());
            assertThat(orderProjection.getIntervalTo()).isEqualTo(clientReturn.getArriveIntervalTo());
        }
    }
}
