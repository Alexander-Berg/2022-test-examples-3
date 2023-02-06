package ru.yandex.market.tpl.core.service.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;

import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ArriveAtRoutePointServiceBeforeFinishingPickupTest {

    private final ArriveAtRoutePointService arriveAtRoutePointService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final RoutePointRepository routePointRepository;
    private final UserShiftCommandService commandService;


    private User user;
    private User user2;
    private UserShift userShift;
    private UserShift userShift2;

    @BeforeEach
    void init() {
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(824125L, date);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .build());
        userShift = testUserHelper.createOpenedShift(user, order, date);

        user2 = testUserHelper.findOrCreateUser(824126L, date);
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .build());
        userShift2 = testUserHelper.createOpenedShift(user2, order, date);
    }

    @Test
    void shouldNotSwitchOpenRoutePointWhenPickupIsNotFinished() {
        LocationDto locationDto = new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId());
        thenThrownBy(() -> arriveAtRoutePointService.arrivedAtRoutePoint(userShift.streamOrderDeliveryTasks()
                        .map(e -> e.getRoutePoint().getId()).findFirst().get(),
                locationDto, user)).isInstanceOf(TplIllegalArgumentException.class);
        arriveAtRoutePointService.arrivedAtRoutePoint(userShift.getCurrentRoutePoint().getId(),
                locationDto, user);

        thenThrownBy(() -> arriveAtRoutePointService.arrivedAtRoutePoint(userShift.streamOrderDeliveryTasks()
                        .map(e -> e.getRoutePoint().getId()).findFirst().get(),
                locationDto, user)).isInstanceOf(TplIllegalArgumentException.class);

        testUserHelper.finishPickupAtStartOfTheDay(userShift2);

    }

}
