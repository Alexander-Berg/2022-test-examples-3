package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.IN_PROGRESS;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.IN_TRANSIT;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.NOT_STARTED;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.UNFINISHED;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.ON_TASK;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_CREATED;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_OPEN;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_REFUSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CoreTest
class UserShiftDeliveryRescheduleTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;

    private final Clock clock;

    private Shift shift;
    private User user;

    @BeforeAll
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в NOT_STARTED c одной таской")
    void sameDayOneTaskNotStarted() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в IN_TRANSIT c одной таской, она текущая")
    void sameDayOneTaskInTransit() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, IN_TRANSIT);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в IN_PROGRESS c одной таской, она текущая")
    void sameDayOneTaskInProgress() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, IN_PROGRESS);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в UNFINISHED c одной таской")
    void sameDayOneTaskUnfinished() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, UNFINISHED);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим на след день из точки c одной таской")
    void nextDayOneTask() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        testUserHelper.rescheduleNextDay(routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(routePoint.getTasks().get(0).getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим на след день из точки c одной таской, она текущая")
    void nextDayOneTaskCurrent() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, IN_TRANSIT);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        testUserHelper.rescheduleNextDay(routePoint);
        testUserHelper.finishCallTasksAtRoutePoint(routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(routePoint.getTasks().get(0).getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("НЕ переносим из точки в FINISHED")
    void notMoveInFinishedPoint() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, FINISHED);

        assertThatThrownBy(() -> testUserHelper.rescheduleNextDay(routePoint))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @DisplayName("НЕ переносим когда userShift в SHIFT_CREATED")
    void notMoveInCreatedShift() {
        UserShift userShift = shiftInStatus(SHIFT_CREATED);
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);

        assertThatThrownBy(() -> testUserHelper.rescheduleNextDay(routePoint))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @DisplayName("НЕ переносим когда userShift в SHIFT_CLOSED")
    void notMoveInClosedShift() {
        UserShift userShift = singlePointShift();
        RoutePoint routePoint = routePointInStatus(userShift, FINISHED);

        assertThatThrownBy(() -> testUserHelper.rescheduleNextDay(routePoint))
                .isInstanceOf(CommandFailedException.class)
                .hasCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в NOT_STARTED c несколькими тасками, остаются завершенные")
    void multipleTasksMoveInNotStartedShiftNothingLeft() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED, true);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в IN_TRANSIT c несколькими тасками, остаются завершенные, " +
            "точка текущая")
    void multipleTasksMoveInTransitShift() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, IN_TRANSIT, true, true);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в UNFINISHED c несколькими тасками, остаются завершенные")
    void multipleTasksToUnfinishedMove() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, UNFINISHED, true);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в NOT_STARTED c несколькими тасками, остаются НЕзавершенные")
    void multipleTasksMoveInNotStartedShift() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(NOT_STARTED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в UNFINISHED c несколькими тасками, остаются НЕзавершенные")
    void multipleTasksMoveInUnfinishedShift() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, UNFINISHED);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(UNFINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в IN_TRANSIT c несколькими тасками, остаются НЕзавершенные, " +
            "точка текущая")
    void name16() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, IN_TRANSIT);

        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();
        Task<?> movedTask = routePoint.getTasks().get(0);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(IN_TRANSIT);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);

        validateTaskMove(userShift, routePoint, movedTask);
    }

    @Test
    @DisplayName("Переносим в рамках дня из точки в UNFINISHED c одной таской, она текущая")
    void sameDayOneTaskUnfinishedNotCurrent() {
        UserShift userShift = shift();
        RoutePoint routePoint = routePointInStatus(userShift, UNFINISHED, true, false);

        reschedule(userShift, routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(userShift.getCurrentRoutePoint()).isNotEqualTo(routePoint);
    }

    @Test
    @DisplayName("Переносим на след день из точки в NOT_STARTED c несколькими тасками, остаются завершенные")
    void multipleTasksToNotStartedMoveFinished() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED, true);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        testUserHelper.rescheduleNextDay(routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);
        assertThat(routePoint.getTasks().get(0).getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);
    }

    @Test
    @DisplayName("Переносим на след день из точки в NOT_STARTED c несколькими тасками, остаются НЕзавершенные")
    void multipleTasksToNotStartedMoveUnfinished() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);
        RoutePoint oldCurrent = userShift.getCurrentRoutePoint();

        testUserHelper.rescheduleNextDay(routePoint);

        assertThat(routePoint.getStatus()).isEqualTo(NOT_STARTED);
        assertThat(routePoint.getTasks().get(0).getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(oldCurrent);
    }

    @Test
    void failRescheduleWithWrongInterval() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);

        Interval interval = LocalTimeInterval.valueOf("01:00-04:00").toInterval(clock.instant(), clock.getZone());

        assertThatThrownBy(
                () -> commandService.rescheduleDeliveryTask(user,
                        new UserShiftCommand.RescheduleOrderDeliveryTask(
                                userShift.getId(), routePoint.getId(), routePoint.getTasks().get(0).getId(),
                                DeliveryReschedule.fromCourier(user,
                                        interval.getStart(),
                                        interval.getEnd(),
                                        OrderDeliveryRescheduleReasonType.CLIENT_REQUEST),
                                todayAtHour(9, clock),
                                userShift.getZoneId()
                        )))
                .isInstanceOf(TplInvalidParameterException.class)
                .hasMessageContaining("Interval is not available");
    }

    @DisplayName("Не валидировать интервалы при домаршрутизации")
    @Test
    public void whenRescheduleAfterExternalRescheduleDoseNotValidateInterval() {
        UserShift userShift = multitaskShift();
        RoutePoint routePoint = routePointInStatus(userShift, NOT_STARTED);
        int dayInSecond = 24 * 60 * 60;
        int hourInSecond = 60 * 60;
        Instant instant = clock.instant().plusSeconds(dayInSecond);
        Interval interval = LocalTimeInterval.valueOf("09:00-15:00").toInterval(instant, clock.getZone());

        assertDoesNotThrow(
                () -> commandService.rescheduleDeliveryTask(user,
                        new UserShiftCommand.RescheduleOrderDeliveryTask(
                                userShift.getId(), routePoint.getId(), routePoint.getTasks().get(0).getId(),
                                DeliveryReschedule.fromCourier(user,
                                        interval.getStart(),
                                        interval.getEnd(),
                                        OrderDeliveryRescheduleReasonType.EXTERNAL_RESCHEDULE),
                                instant.plusSeconds(hourInSecond),
                                userShift.getZoneId()
                        )));
    }

    private void validateTaskMove(UserShift userShift, RoutePoint routePoint, Task<?> movedTask) {
        assertThat(routePoint.getTasks().size()).isEqualTo(1);
        assertThat(routePoint.getTasks().get(0)).isNotEqualTo(movedTask);

        assertThat(userShift.getRoutePoints().get(3).getStatus()).isEqualTo(NOT_STARTED);
        assertThat(userShift.getRoutePoints().get(3).getTasks().size()).isEqualTo(1);
        assertThat(userShift.getRoutePoints().get(3).getTasks().get(0)).isEqualTo(movedTask);
    }

    private void reschedule(UserShift userShift, RoutePoint routePoint) {
        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), routePoint.getId(), routePoint.getTasks().get(0).getId(),
                DeliveryReschedule.fromCourier(user, todayAtHour(20, clock), todayAtHour(22, clock),
                        OrderDeliveryRescheduleReasonType.CLIENT_REQUEST), todayAtHour(9, clock),
                userShift.getZoneId()
        ));
        testUserHelper.finishCallTasksAtRoutePoint(routePoint);
    }

    private RoutePoint routePointInStatus(UserShift userShift, RoutePointStatus status) {
        return routePointInStatus(userShift, status, false, false);
    }

    private RoutePoint routePointInStatus(UserShift userShift, RoutePointStatus status, boolean lastTaskFinished) {
        return routePointInStatus(userShift, status, false, lastTaskFinished);
    }

    private RoutePoint routePointInStatus(UserShift userShift, RoutePointStatus status, boolean isCurrent,
                                          boolean lastTaskFinished) {
        if (status == IN_PROGRESS) {
            RoutePoint routePoint = getRoutePointWithStatus(userShift, IN_TRANSIT);
            arrive(userShift, routePoint);

        } else if (status == UNFINISHED && !isCurrent) {
            RoutePoint routePoint = getRoutePointWithStatus(userShift, IN_TRANSIT);
            arrive(userShift, routePoint);
            switchTo(userShift, getRoutePointWithStatus(userShift, NOT_STARTED));

        } else if (status == UNFINISHED) {
            RoutePoint routePoint = getRoutePointWithStatus(userShift, IN_TRANSIT);
            arrive(userShift, routePoint);
            RoutePoint notStartedRp = getRoutePointWithStatus(userShift, NOT_STARTED);
            switchTo(userShift, notStartedRp);
            arrive(userShift, notStartedRp);
            switchTo(userShift, getRoutePointWithStatus(userShift, UNFINISHED));

        } else if (status == FINISHED) {
            RoutePoint routePoint = getRoutePointWithStatus(userShift, IN_TRANSIT);
            failLastTask(userShift, routePoint);
            userHelper.finishCallTasksAtRoutePoint(routePoint);
        }

        RoutePoint result = getRoutePointWithStatus(userShift, status);

        assertThat(result.getStatus()).isEqualTo(status);
        if (isCurrent) {
            assertThat(userShift.getCurrentRoutePoint()).isEqualTo(result);
        }

        if (lastTaskFinished && result.tasks.size() > 1 && result.getTasks().get(1).getStatus() != DELIVERY_FAILED) {
            failLastTask(userShift, result);
        }

        return result;
    }

    private RoutePoint getRoutePointWithStatus(UserShift userShift, RoutePointStatus status) {
        return userShift.streamDeliveryRoutePoints()
                .filter(rp -> rp.getStatus() == status)
                .findFirst().orElseThrow();
    }

    private void arrive(UserShift userShift, RoutePoint routePoint) {
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), routePoint.getId(),
                helper.getLocationDto(userShift.getId())
        ));
    }

    private void addTask(UserShift userShift, String address, int hour, SimpleStrategies strategy) {
        commandService.addDeliveryTask(user, new UserShiftCommand.AddDeliveryTask(
                userShift.getId(),
                helper.taskPrepaid(address, hour, orderGenerateService.createOrder().getId()),
                strategy
        ));
    }

    private void failLastTask(UserShift userShift, RoutePoint routePoint) {
        List<Task<?>> tasks = routePoint.getTasks();
        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(),
                routePoint.getId(),
                tasks.get(tasks.size() - 1).getId(), new OrderDeliveryFailReason(CLIENT_REFUSED, "")
        ));
    }

    private void switchTo(UserShift userShift, RoutePoint newRoutePoint) {
        commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShift.getId(), newRoutePoint.getId()
        ));
    }

    private UserShift multitaskShift() {
        return shiftInStatus(ON_TASK, false, true);
    }

    private UserShift shift() {
        return shiftInStatus(ON_TASK, false, false);
    }

    private UserShift singlePointShift() {
        return shiftInStatus(ON_TASK, true, false);
    }

    private UserShift shiftInStatus(UserShiftStatus status) {
        return shiftInStatus(status, false, false);
    }

    private UserShift shiftInStatus(UserShiftStatus status, boolean singlePoint, boolean multitask) {
        UserShiftCommand.Create command = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build();

        UserShift userShift = repository.findById(commandService.createUserShift(command)).orElseThrow();

        addTask(userShift, "addr1", 12, SimpleStrategies.NO_MERGE);
        if (!singlePoint) {
            addTask(userShift, "addr2", 13, SimpleStrategies.NO_MERGE);
        }

        if (multitask) {
            addTask(userShift, "addr1", 12, SimpleStrategies.BY_DATE_MERGE);
            if (!singlePoint) {
                addTask(userShift, "addr2", 13, SimpleStrategies.BY_DATE_MERGE);
            }
        }

        if (status == SHIFT_CREATED) {
            return userShift;
        }

        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));

        if (status == SHIFT_OPEN) {
            return userShift;
        }

        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        assertThat(userShift.getStatus()).isEqualTo(status);

        if (singlePoint) {
            assertThat(userShift.getRoutePoints().size()).isEqualTo(3);
        } else {
            assertThat(userShift.getRoutePoints().size()).isEqualTo(4);
        }

        userHelper.finishPickupAtStartOfTheDay(userShift);

        return userShift;
    }

}
