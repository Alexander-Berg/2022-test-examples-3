package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assumptions.assumeThat;


@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class UserShiftFailCancelOnFinalTaskTest {
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftFailCancelHelper userShiftFailCancelHelper;
    private final UserShiftRepository repository;
    private final Clock clock;

    private UserShift userShift;
    private User user;
    private RoutePoint unfinished;

    @BeforeEach
    void createShiftAndPassOneRoutePoint() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        Order prepaidOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        Order prepaidOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        Order unpaidOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CARD)
                .build());

        Order unpaidOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CARD)
                .build());


        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr1", 12, unpaidOrder1.getId()))
                .routePoint(helper.taskPrepaid("addr3", 14, prepaidOrder1.getId()))
                .routePoint(helper.taskPrepaid("addrPaid", 13, prepaidOrder2.getId()))
                .routePoint(helper.taskUnpaid("addrPaid", 16, unpaidOrder2.getId()))
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        RoutePoint rp = userShiftFailCancelHelper.currentRoutePoint(userShift);
        userHelper.finishDelivery(rp, false);

        unfinished = userShiftFailCancelHelper.currentRoutePoint(userShift);
        userShiftFailCancelHelper.arriveAtCurrentRoutePoint(user, userShift);
        commandService.switchOpenRoutePoint(user, new UserShiftCommand.SwitchOpenRoutePoint(
                userShift.getId(), userShift.getRoutePoints().get(3).getId()
        ));

        assumeThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assumeThat(userShiftFailCancelHelper.currentRoutePoint(userShift).getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assumeThat(unfinished.getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
    }

    @Test
    void canFailTaskInUnfinished() {
        userShiftFailCancelHelper.failTask(user, userShift, unfinished);
    }

}
