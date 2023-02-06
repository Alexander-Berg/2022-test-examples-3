package ru.yandex.market.tpl.core.service.tracking;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.ShiftManager;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.SHIFT_CREATED;
import static ru.yandex.market.tpl.api.model.tracking.SmsStatus.NEVER_SENT;
import static ru.yandex.market.tpl.api.model.tracking.SmsStatus.SENT;

/**
 * @author aostrikov
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CoreTest
class TrackingSmsSendingTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final TrackingRepository trackingRepository;
    private final ShiftManager shiftManager;
    private final Clock clock;

    private ru.yandex.market.tpl.core.domain.order.Order order;

    private Shift shift;
    private Shift nextDayShift;

    private User user;
    private User user2;


    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(98459L, LocalDate.now(clock));
        user2 = userHelper.findOrCreateUser(2342L, LocalDate.now(clock));
        order = orderGenerateService.createOrder();

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        nextDayShift = userHelper.findOrCreateOpenShift(LocalDate.now(clock).plusDays(1));

    }

    @Test
    void shouldNotSendAfterShiftStart() {
        UserShift userShift = createShift(user);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);
    }

    @Test
    void shouldSendAfterShiftStart() {
        UserShift userShift = createShift(user);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);

        openShiftFinishPickup(user, userShift);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);
    }

    @Test
    void shouldReassignAfterShiftCreation() {
        UserShift userShift = createShift(user);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);

        UserShift userShift2 = createShift(user2);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);
    }

    @Test
    void shouldReassignAndSendAfterShiftStart() {
        UserShift userShift = createShift(user);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);

        UserShift userShift2 = createShift(user2);
        openShiftFinishPickup(user2, userShift2);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);
    }

    @Test
    void shouldReassignAfterShiftStart() {
        UserShift userShift = createShift(user);
        openShiftFinishPickup(user, userShift);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);

        // reassign to user2
        var userShift2 = userHelper.createEmptyShift(user2, shift);
        var task = tracking().getOrderDeliveryTask();

        shiftManager.assignOrderIfNeeded(userShift.getShift().getShiftDate(),
                task.getOrderId(), user2.getId(),
                task.getExpectedDeliveryTime(), task.getExpectedDeliveryTime()
        );

        commandService.checkin(user2, new UserShiftCommand.CheckIn(userShift2.getId()));
        commandService.startShift(user2, new UserShiftCommand.Start(userShift2.getId()));

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);
    }

    @Test
    void shouldMarkNotSendOnNextDay() {
        UserShift userShift = createShift(user);
        openShiftFinishPickup(user, userShift);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);

        UserShift userShift2 = createNextDayShift(user2);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);
    }

    @Test
    void shouldRepeatSendingNextDay() {
        UserShift userShift = createShift(user);
        openShiftFinishPickup(user, userShift);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift);
        userHelper.finishDelivery(tracking().getOrderDeliveryTask().getRoutePoint(), true);
        RoutePoint returnRoutPoint = userShift.getCurrentRoutePoint();
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        commandService.finishReturnTask(user, new UserShiftCommand.FinishReturnTask(userShift.getId(),
                returnRoutPoint.getId(), returnRoutPoint.streamReturnTasks().findFirst().orElseThrow().getId()));
        userHelper.finishUserShift(userShift);

        UserShift userShift2 = createNextDayShift(user2);

        assertThat(tracking().getSmsStatus()).isEqualTo(NEVER_SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);

        openShiftFinishPickup(user2, userShift2);

        assertThat(tracking().getSmsStatus()).isEqualTo(SENT);
        assertThat(tracking().getUserShift()).isEqualTo(userShift2);
    }

    private UserShift createShift(User user) {
        return userHelper.createShiftWithDeliveryTask(user, SHIFT_CREATED, shift, order);
    }

    private UserShift createNextDayShift(User user) {
        return userHelper.createShiftWithDeliveryTask(user, SHIFT_CREATED, nextDayShift, order);
    }

    private void openShiftFinishPickup(User user, UserShift userShift) {
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
    }

    private Tracking tracking() {
        return trackingRepository.findByOrderId(order.getId()).orElseThrow();
    }

}
