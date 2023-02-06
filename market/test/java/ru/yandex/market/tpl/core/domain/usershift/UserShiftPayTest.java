package ru.yandex.market.tpl.core.domain.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class UserShiftPayTest {

    public static final String WRONG_LOCATION_COMMENT = "Попросили завести в соседний дом";
    private final Clock clock;

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;

    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final RoutePointRepository routePointRepository;
    private User user;
    private UserShift userShift;
    private Order order;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(35236L);

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .build());
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
    }

    @Test
    void shouldPay() {
        OrderPaymentType paymentType = OrderPaymentType.CASH;
        arriveAndPay(paymentType);

        assertThat(order.getPaymentType()).isEqualTo(paymentType);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Test
    void shouldPayCancelAndReopen() {
        shouldPay();

        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED, "my client refused!")
        ));
        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), rp.getId(), task.getId(), Source.COURIER
        ));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(task.getFailReason()).isNull();
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.UNFINISHED);
    }

    @Test
    void shouldPayWithAnotherType() {
        arriveAndPay(OrderPaymentType.CASH);

        OrderPaymentType paymentType = OrderPaymentType.CARD;
        pay(paymentType);
        assertThat(order.getPaymentType()).isEqualTo(paymentType);
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    }

    @Test
    void shouldUnPayWhenReturn() {
        arriveAndPay(OrderPaymentType.CASH);
        userShift.setStatus(UserShiftStatus.SHIFT_CLOSED);
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        assertThat(order.getPaymentStatus()).isEqualTo(OrderPaymentStatus.UNPAID);
    }

    private void arriveAndPay(OrderPaymentType paymentType) {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);
        assumeThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);

        commandService.arriveAtRoutePoint(user,
                new UserShiftCommand.ArriveAtRoutePoint(userShift.getId(), rp.getId(),
                        getLocationDto(userShift.getId())));

        assertThat(routePointRepository.findById(rp.getId()).orElseThrow().getWrongLocationComment())
                .isEqualTo(WRONG_LOCATION_COMMENT);
        pay(paymentType);
    }

    private void pay(OrderPaymentType paymentType) {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        OrderDeliveryTask task = (OrderDeliveryTask) rp.getTasks().get(0);

        commandService.payOrder(user,
                new UserShiftCommand.PayOrder(userShift.getId(), rp.getId(), task.getId(), paymentType, null));

        assertThat(task.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(rp.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }

    private LocationDto getLocationDto(Long userShiftId) {
        LocationDto location = new LocationDto();
        location.setLatitude(new BigDecimal(0));
        location.setLongitude(new BigDecimal(0));
        location.setUserShiftId(userShiftId);
        location.setWrongLocationComment(WRONG_LOCATION_COMMENT);
        return location;
    }

}
