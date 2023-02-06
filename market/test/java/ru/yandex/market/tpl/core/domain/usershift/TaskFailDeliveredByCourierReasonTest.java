package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.listener.OnActionReroute;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus.CANCELLED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.NO_PASSPORT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class TaskFailDeliveredByCourierReasonTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private User user;
    private Order order;
    private Shift shift;
    private UserShift userShift;

    @MockBean
    private OnActionReroute onActionReroute;

    @BeforeEach
    void createShifts() {
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .build());

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Instant arrivalTime = Instant.now(clock);
        userShift = repository.findById(
                commandService.createUserShift(
                        UserShiftCommand.Create.builder()
                                .userId(user.getId())
                                .shiftId(shift.getId())
                                .routePoint(helper.taskPrepaid("addr1", order.getId(), arrivalTime, false))
                                .build()
                )).orElseThrow();

        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDayAndSelectNext(userShift);
        RoutePoint rp = userShift.getCurrentRoutePoint();
        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), rp.getId(),
                helper.getLocationDto(userShift.getId())
        ));
    }

    @Test
    void shouldCancelTaskIfOrderIsDamaged() {
        RoutePoint rp = userShift.getRoutePoints().get(1);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(),
                rp.getId(),
                rp.getTasks().get(0).getId(),
                new OrderDeliveryFailReason(ORDER_IS_DAMAGED, "")
        ));

        assertThat(order.getDeliveryStatus()).isEqualTo(CANCELLED);

    }

    @Test
    void shouldCancelTaskIfOrderIsNoPassport() {
        RoutePoint rp = userShift.getRoutePoints().get(1);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(),
                rp.getId(),
                rp.getTasks().get(0).getId(),
                new OrderDeliveryFailReason(NO_PASSPORT, "")
        ));
        assertThat(order.getDeliveryStatus()).isEqualTo(CANCELLED);
    }


}
