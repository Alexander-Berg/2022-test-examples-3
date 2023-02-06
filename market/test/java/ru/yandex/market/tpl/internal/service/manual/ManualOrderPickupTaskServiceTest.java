package ru.yandex.market.tpl.internal.service.manual;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.service.usershift.ArriveAtRoutePointService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualOrderPickupTaskServiceTest {

    private final ManualOrderPickupTaskService subject;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final ArriveAtRoutePointService arriveAtRoutePointService;

    @Test
    void pushActionWithUnsupportedStatus() {
        UserShift userShift = createUserShiftAndArriveAtPickup(9990L);
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);

        assertThrows(TplInvalidParameterException.class, () -> {
            subject.pushAction(orderPickupTask.getId(), OrderPickupTaskStatus.PARTIALLY_FINISHED);
        });
    }

    @Test
    void pushActionToInQueue() {
        UserShift userShift = createUserShiftAndArriveAtPickup(9990L);
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);

        subject.pushAction(orderPickupTask.getId(), OrderPickupTaskStatus.IN_QUEUE);

        var result = userShiftRepository.findById(userShift.getId()).get();
        assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.IN_QUEUE);
    }

    @Test
    void pushActionToWaitingArrival() {
        UserShift userShift = createUserShiftAndArriveAtPickup(9990L);
        arriveAtRoutePoint(userShift);
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);

        subject.pushAction(orderPickupTask.getId(), OrderPickupTaskStatus.WAITING_ARRIVAL);

        var result = userShiftRepository.findById(userShift.getId()).get();
        assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.WAITING_ARRIVAL);
    }

    @Test
    void pushActionToMissedArrival() {
        UserShift userShift = createUserShiftAndArriveAtPickup(9990L);
        arriveAtRoutePoint(userShift);
        inviteToLoading(userShift);
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);

        subject.pushAction(orderPickupTask.getId(), OrderPickupTaskStatus.MISSED_ARRIVAL);

        var result = userShiftRepository.findById(userShift.getId()).get();
        assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.MISSED_ARRIVAL);
    }

    @Test
    void pushActionToReadyToScan() {
        UserShift userShift = createUserShiftAndArriveAtPickup(9990L);
        arriveAtRoutePoint(userShift);
        inviteToLoading(userShift);
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);

        subject.pushAction(orderPickupTask.getId(), OrderPickupTaskStatus.READY_TO_SCAN);

        var result = userShiftRepository.findById(userShift.getId()).get();
        assertThat(getOrderPickupTask(result).getStatus()).isEqualTo(OrderPickupTaskStatus.READY_TO_SCAN);
    }

    private OrderPickupTask getOrderPickupTask(UserShift userShift) {
        return userShift.streamPickupTasks().findFirst()
                .orElseThrow(() -> new RuntimeException("OrderPickupTask not found"));
    }

    private UserShift createUserShiftAndArriveAtPickup(long uin) {
        User user = userHelper.findOrCreateUser(uin);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);

        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                true
        );

        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        RoutePoint routePoint = userShift.getCurrentRoutePoint();
        Long routePointId = routePoint.getId();

        commandService.arriveAtRoutePoint(
                user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId, routePointId,
                        new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
                )
        );

        return userShift;
    }

    private void arriveAtRoutePoint(UserShift userShift) {
        arriveAtRoutePointService.arrivedAtRoutePoint(
                userShift.getCurrentRoutePoint().getId(),
                new LocationDto(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "myDevice",
                        userShift.getId()
                ),
                userShift.getUser()
        );
    }

    private void inviteToLoading(UserShift userShift) {
        OrderPickupTask orderPickupTask = getOrderPickupTask(userShift);
        commandService.inviteToLoading(
                userShift.getUser(),
                new UserShiftCommand.InviteToLoading(
                        userShift.getId(),
                        orderPickupTask.getRoutePoint().getId(),
                        orderPickupTask.getId(),
                        Instant.now(clock).plus(5, ChronoUnit.MINUTES)
                )
        );
    }

}
