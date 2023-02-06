package ru.yandex.market.tpl.core.domain.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.partner.DeliveryServiceFactory;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.DEFAULT_ZONE_ID;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class SaveCorrectDeliveryIntervalAfterRescheduleTest {
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftRepository userShiftRepository;
    private final DeliveryServiceFactory deliveryServiceFactory;

    private final Clock clock;
    private LocalDate now;
    private Order order;
    private User user;

    @BeforeEach
    void init() {
        now = LocalDate.now(clock);
        user = userHelper.findOrCreateUser(35338L, now);

        order = orderGenerateService.createOrder();
    }

    @Test
    void testOnCorrectIntervalAfterReopenTask() {
        deliveryServiceFactory.createScheduledIntervalsForDeliveryServiceTestMatchInterval();

        UserShift us = prepareShift(now, order);
        RoutePoint routePointOrderDeliveryTask = us.getCurrentRoutePoint();
        OrderDeliveryTask task = routePointOrderDeliveryTask.streamOrderDeliveryTasks().findFirst().orElseThrow();

        rescheduleWithNewIntervalByCourier(2, us, OrderDeliveryRescheduleReasonType.CLIENT_REQUEST);
        reopenTaskByCourier(us, routePointOrderDeliveryTask, task);

        assertThat(order.getDelivery().getInterval()).isEqualTo(
                new Interval(
                        Instant.parse("1990-01-01T09:00:00.0Z").minus(3, ChronoUnit.HOURS),
                        Instant.parse("1990-01-01T22:00:00.0Z").minus(3, ChronoUnit.HOURS)
                )
        );
    }

    private UserShift prepareShift(LocalDate date, Order order) {
        Shift shift = userHelper.findOrCreateOpenShift(date);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskUnpaid(
                        "addr1", order.getDelivery().getDeliveryDate(DEFAULT_ZONE_ID), 12,
                        order.getId(), false))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        UserShift userShift = userShiftRepository.findById(
                userShiftCommandService.createUserShift(createCommand)).orElseThrow();

        userShiftCommandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        userShiftCommandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);

        return userShift;
    }

    private void rescheduleWithNewIntervalByCourier(int days, UserShift us,
                                                    OrderDeliveryRescheduleReasonType rescheduleReasonType) {
        Interval deliveryInterval = new Interval(
                DateTimeUtil.todayAtHour(20, clock).plus(days, ChronoUnit.DAYS),
                DateTimeUtil.todayAtHour(22, clock).plus(days, ChronoUnit.DAYS));
        RoutePoint rp = us.getCurrentRoutePoint();

        userShiftCommandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                us.getId(), rp.getId(), rp.streamDeliveryTasks().findFirst().orElseThrow().getId(),
                DeliveryReschedule.fromCourier(user, deliveryInterval.getStart(),
                        deliveryInterval.getEnd(),
                        rescheduleReasonType), todayAtHour(9, clock), DEFAULT_ZONE_ID));
    }

    private void reopenTaskByCourier(UserShift userShift, RoutePoint routePoint, OrderDeliveryTask task) {
        userShiftCommandService.reopenDeliveryTask(user,
                new UserShiftCommand.ReopenOrderDeliveryTask(userShift.getId(),
                        routePoint.getId(), task.getId(), Source.COURIER)
        );
    }
}
