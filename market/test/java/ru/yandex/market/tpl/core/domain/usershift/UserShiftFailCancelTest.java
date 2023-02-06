package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.Nullable;

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
import ru.yandex.market.tpl.core.exception.TplInvalidStateException;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftFailCancelTest {

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftFailCancelHelper userShiftFailCancelHelper;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;

    private UserShift userShift;
    private User user;


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
        userHelper.finishPickupAtStartOfTheDayAndSelectNext(userShift);
        RoutePoint rp = userShiftFailCancelHelper.currentRoutePoint(userShift);
        userHelper.finishDelivery(rp, true);
    }

    @Test
    void cannotCancelTaskInFinished() {
        RoutePoint rp = userShift.getRoutePoints().get(1);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThatThrownBy(() -> userShiftFailCancelHelper.failTask(user, userShift, rp))
                .hasRootCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    void canFailTaskInTransit() {
        RoutePoint rp = userShiftFailCancelHelper.currentRoutePoint(userShift);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);

        userShiftFailCancelHelper.failTask(user, userShift, rp);
    }

    @Test
    void canFailTaskInNotStarted() {
        RoutePoint rp = userShift.getRoutePoints().get(3);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);

        userShiftFailCancelHelper.failTask(user, userShift, rp);
    }

    @Test
    void cannotFailTaskInFinished() {
        RoutePoint rp = userShift.getRoutePoints().get(1);

        assumeThat(rp.getStatus()).isEqualTo(RoutePointStatus.FINISHED);

        assertThatThrownBy(() -> userShiftFailCancelHelper.failTask(user, userShift, rp))
                .hasRootCauseInstanceOf(TplInvalidStateException.class);
    }

    @Test
    void canFailTaskInProgress() {
        RoutePoint rp = userShiftFailCancelHelper.currentRoutePoint(userShift);

        userShiftFailCancelHelper.arriveAtCurrentRoutePoint(user, userShift);

        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);

        userShiftFailCancelHelper.failTask(user, userShift, rp);
    }

    @Test
    void shouldSwitchRoutePointAfterFail() {
        checkRoutePointSwitch(rp -> userShiftFailCancelHelper.failTask(user, userShift, rp)
                , userShift.getRoutePoints().get(2), userShift.getRoutePoints().get(3));
    }

    /**
     * Проверяет, что после совершения действия с точкой она завершается и происходит переключение на следующую.
     *
     * @param action   действие с точкой
     * @param rp       текущая точка
     * @param expected точка, которая должна стать текущей после действия; если не задана, значит смена должна закрыться
     */
    void checkRoutePointSwitch(Consumer<RoutePoint> action, RoutePoint rp, @Nullable RoutePoint expected) {
        assertThat(userShift.getCurrentRoutePoint()).isEqualTo(rp);

        action.accept(rp);

        assertThat(rp.getStatus())
                .describedAs("RoutePoint is finished if all tasks are done")
                .isEqualTo(RoutePointStatus.FINISHED);
        assertThat(userShift.getCurrentRoutePoint())
                .describedAs("Current RoutePoint is switched if all tasks are done")
                .isEqualTo(expected);
    }

    void assertThatShiftIsClosed() {
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        assertThat(userShift.getCurrentRoutePoint()).isNull();
        assertThat(userShift.getRoutePoints()).extracting(RoutePoint::getStatus).containsOnly(RoutePointStatus.FINISHED);
        assertThat(userShift.getClosedAt()).isNotNull();

    }
}
