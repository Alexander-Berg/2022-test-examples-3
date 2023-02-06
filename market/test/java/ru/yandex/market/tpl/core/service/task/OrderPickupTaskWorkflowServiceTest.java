package ru.yandex.market.tpl.core.service.task;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
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
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class OrderPickupTaskWorkflowServiceTest extends TplAbstractTest {

    private final OrderPickupTaskWorkflowService subject;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TransactionTemplate transactionTemplate;

    @Test
    void finishOrderPickupTasks_fromNotStarted() {
        UserShift userShift = transactionTemplate.execute(
                status -> createUserShiftAndArriveAtFirstRoutePoint(true)
        );
        subject.finishOrderPickupTasks(userShift.getId());

        transactionTemplate.execute(status -> {
            UserShift result = userShiftRepository.findByIdOrThrow(userShift.getId());
            OrderPickupTask orderPickupTask = result.streamPickupTasks().findFirst().get();
            assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.PARTIALLY_FINISHED);

            dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 0);
            return null;
        });
    }

    @Test
    void finishOrderPickupTasks_fromMissedArrival() {
        UserShift userShift = transactionTemplate.execute(status -> {
            var us = createUserShiftAndArriveAtFirstRoutePoint(true);
            pushToMissedArrival(us);
            dbQueueTestUtil.clear(QueueType.REGISTER_COURIER_IN_YARD);
            return us;
        });
        subject.finishOrderPickupTasks(userShift.getId());

        transactionTemplate.execute(status -> {
            UserShift result = userShiftRepository.findByIdOrThrow(userShift.getId());
            OrderPickupTask orderPickupTask = result.streamPickupTasks().findFirst().get();
            assertThat(orderPickupTask.getStatus()).isEqualTo(OrderPickupTaskStatus.PARTIALLY_FINISHED);

            dbQueueTestUtil.assertQueueHasSize(QueueType.REGISTER_COURIER_IN_YARD, 0);
            return null;
        });
    }

    @Test
    void finishOrderPickupTasks_noOrderPickup() {
        UserShift userShift = transactionTemplate.execute(
                status -> createUserShiftAndArriveAtFirstRoutePoint(false)
        );

        subject.finishOrderPickupTasks(userShift.getId());
    }

    private UserShift createUserShiftAndArriveAtFirstRoutePoint(boolean addPickup) {
        User user = userHelper.findOrCreateUser(1L);
        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        Order order = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build());

        UserShiftCommand.Create.CreateBuilder createCommandBuilder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId());
        if (addPickup) {
            createCommandBuilder = createCommandBuilder
                    .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()));
        }
        createCommandBuilder = createCommandBuilder
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE);

        var createCommand = createCommandBuilder.build();
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

    private void pushToMissedArrival(UserShift userShift) {
        RoutePoint routePoint = userShift.getCurrentRoutePoint();
        OrderPickupTask orderPickupTask = userShift.streamPickupTasks().findFirst().get();
        User user = userShift.getUser();

        commandService.enqueue(
                user,
                new UserShiftCommand.Enqueue(
                        userShift.getId(),
                        routePoint.getId(),
                        orderPickupTask.getId()
                )
        );

        commandService.inviteToLoading(
                user,
                new UserShiftCommand.InviteToLoading(
                        userShift.getId(),
                        routePoint.getId(),
                        orderPickupTask.getId(),
                        Instant.now(clock).minus(15, ChronoUnit.MINUTES)
                )
        );

        commandService.missArrival(
                user,
                new UserShiftCommand.MissArrival(
                        userShift.getId(),
                        routePoint.getId(),
                        orderPickupTask.getId(),
                        false,
                        Source.SYSTEM
                )
        );
    }

}
