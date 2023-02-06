package ru.yandex.market.tpl.core.domain.usershift.commands;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.DELIVERY_DELAY;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.NO_PASSPORT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.OTHER;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;
import static ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand.RescheduleOrderDeliveryTask.MAX_RESCHEDULE_DAYS;

class OrderDeliveryRescheduleCommandTest {

    private final Clock clock = ClockUtil.initFixed(mock(Clock.class));
    private final User user = UserUtil.createUserWithoutSchedule(1L);

    @Test
    void shouldRescheduleToFutureDay() {
        rescheduleTo(hourInTheFuture(3, 10), hourInTheFuture(3, 12),
                todayAtHour(5)).validate();
        rescheduleTo(hourInTheFuture(MAX_RESCHEDULE_DAYS, 10), hourInTheFuture(MAX_RESCHEDULE_DAYS, 12),
                todayAtHour(5)).validate();
    }

    @Test
    void shouldRescheduleToCurrentDay() {
        rescheduleTo(todayAtHour(10), todayAtHour(12), todayAtHour(5)).validate();
        rescheduleTo(todayAtHour(10), todayAtHour(12), todayAtHour(10)).validate();
        rescheduleTo(todayAtHour(10), todayAtHour(12), todayAtHour(12)).validate();
    }

    @Test
    void shouldRescheduleToAllDayInterval() {
        rescheduleTo(todayAtHour(10), todayAtHour(18), todayAtHour(5)).validate();
    }

    @Test
    void shouldNotRescheduleToPastDays() {
        shouldFail(rescheduleTo(yesterdayAtHour(10), yesterdayAtHour(12), todayAtHour(5))::validate);
    }

    @Test
    void shouldNotRescheduleFromClientToToday() {
        shouldFail(rescheduleFromClientTo(todayAtHour(10), todayAtHour(15), todayAtHour(5))::validate);
    }

    @Test
    void shouldRescheduleFromClientToNextDay() {
        rescheduleFromClientTo(tomorrowAtHour(10), tomorrowAtHour(12), todayAtHour(5)).validate();
    }

    @Test
    void shouldNotRescheduleToPastTimeToday() {
        shouldFail(rescheduleTo(todayAtHour(12), todayAtHour(14), todayAtHour(15))::validate);
    }

    @Test
    void shouldNotHaveBoundariesOnDifferentDays() {
        shouldFail(rescheduleTo(todayAtHour(10), tomorrowAtHour(12), todayAtHour(5))::validate);
        shouldFail(rescheduleTo(hourInTheFuture(3, 10),
                hourInTheFuture(4, 12), todayAtHour(5))::validate);
    }

    @Test
    void shouldNotRescheduleToMoreThanOneWeekForward() {
        shouldFail(rescheduleTo(hourInTheFuture(MAX_RESCHEDULE_DAYS + 1, 10),
                hourInTheFuture(MAX_RESCHEDULE_DAYS + 1, 12), todayAtHour(5))::validate);
    }

    @Test
    void shouldNotRescheduleWithNullDescriptionOnOtherReason() {
        DeliveryReschedule rescheduleDto = DeliveryReschedule
                .fromCourier(user, new DeliveryRescheduleDto(todayAtHour(12), todayAtHour(14), OTHER, null));
        shouldFail(new UserShiftCommand.RescheduleOrderDeliveryTask(1, 1, 1,
                rescheduleDto, todayAtHour(5), DEFAULT_ZONE_ID)::validate);
    }

    @Test
    void shouldNotRescheduleWithEmptyDescriptionOnOtherReason() {
        DeliveryReschedule rescheduleDto = DeliveryReschedule.
                fromCourier(user, new DeliveryRescheduleDto(todayAtHour(12), todayAtHour(14), OTHER, ""));
        shouldFail(new UserShiftCommand.RescheduleOrderDeliveryTask(1, 1, 1,
                rescheduleDto, todayAtHour(5), DEFAULT_ZONE_ID)::validate);
    }

    @Test
    void shouldRescheduleWithNoPassportReason() {
        DeliveryReschedule rescheduleDto = DeliveryReschedule.
                fromCourier(user, new DeliveryRescheduleDto(todayAtHour(12), todayAtHour(14), NO_PASSPORT, ""));
        assertDoesNotThrow(new UserShiftCommand.RescheduleOrderDeliveryTask(1, 1, 1,
                rescheduleDto, todayAtHour(5), DEFAULT_ZONE_ID)::validate);
    }

    private void shouldFail(Executable ex) {
        assertThrows(TplInvalidParameterException.class, ex);
    }

    private UserShiftCommand.RescheduleOrderDeliveryTask rescheduleFromClientTo(Instant from, Instant to,
                                                                                Instant current) {
        return new UserShiftCommand.RescheduleOrderDeliveryTask(1, 1, 1,
                DeliveryReschedule.fromClient(from, to), current, DEFAULT_ZONE_ID);
    }

    private UserShiftCommand.RescheduleOrderDeliveryTask rescheduleTo(
            Instant from, Instant to, Instant current) {
        return new UserShiftCommand.RescheduleOrderDeliveryTask(1, 1, 1,
                DeliveryReschedule.fromCourier(user, from, to, DELIVERY_DELAY), current, DEFAULT_ZONE_ID);
    }

    private Instant yesterdayAtHour(int val) {
        return todayAtHour(val).minus(1, ChronoUnit.DAYS);
    }

    private Instant tomorrowAtHour(int val) {
        return todayAtHour(val).plus(1, ChronoUnit.DAYS);
    }

    private Instant hourInTheFuture(int todayPlusDays, int hour) {
        return todayAtHour(hour).plus(todayPlusDays, ChronoUnit.DAYS);
    }

    private Instant todayAtHour(int hour) {
        return DateTimeUtil.todayAtHour(hour, clock);
    }

}
