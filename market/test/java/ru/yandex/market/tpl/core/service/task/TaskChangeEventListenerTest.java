package ru.yandex.market.tpl.core.service.task;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.history.TaskStatusHistoryEvent;
import ru.yandex.market.tpl.core.domain.task.history.TaskStatusHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderPickupTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.util.AssertExt;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiredArgsConstructor
class TaskChangeEventListenerTest extends TplAbstractTest {

    private final TaskStatusHistoryEventRepository taskStatusHistoryEventRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final RoutePointRepository routePointRepository;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final Clock clock;

    @Test
    @Disabled("https://st.yandex-team.ru/MARKETTPL-7095")
    void addHistoryEvent() {
        UserShift userShift = createUserShiftAndEnqueue(35236L);
        long pickupRoutePointId = userShift.getCurrentRoutePoint().getId();
        RoutePoint pickupRoutePoint = routePointRepository.findByIdOrThrow(pickupRoutePointId);
        OrderPickupTask orderPickupTask = pickupRoutePoint.streamPickupTasks().findFirst().orElseThrow();

        AssertExt.awaitAssert(() -> {
            List<TaskStatusHistoryEvent> events = taskStatusHistoryEventRepository.findAll();
            assertThat(events.size()).isEqualTo(1);
            TaskStatusHistoryEvent event = events.get(0);
            assertThat(event.getTaskId()).isEqualTo(orderPickupTask.getId());
            assertThat(event.getStatusFrom()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED.name());
            assertThat(event.getStatusTo()).isEqualTo(orderPickupTask.getStatus().name());
        }, 2);
    }

    private UserShift createUserShiftAndEnqueue(long uin) {
        User user = userHelper.findOrCreateUser(uin);
        long userShiftId = createUserShift(user);

        commandService.switchActiveUserShift(user, userShiftId);
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));

        UserShift userShift = userShiftRepository.findById(userShiftId).get();

        commandService.arriveAtRoutePoint(
                user,
                new UserShiftCommand.ArriveAtRoutePoint(
                        userShiftId, userShift.getCurrentRoutePoint().getId(),
                        new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShiftId)
                )
        );

        commandService.enqueue(
                user,
                new UserShiftCommand.Enqueue(
                        userShiftId,
                        userShift.getCurrentRoutePoint().getId(),
                        userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().get().getId()
                )
        );

        return userShift;
    }

    private long createUserShift(User user) {
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
        return commandService.createUserShift(createCommand);
    }
}
