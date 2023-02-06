package ru.yandex.market.tpl.api.facade;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSwitchAndArriveDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSwitchDto;
import ru.yandex.market.tpl.api.model.task.RoutePointSwitchReasonType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShiftFacadeTest extends BaseApiTest {
    private final TestUserHelper userHelper;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final ShiftFacade shiftFacade;
    private final RoutePointRepository routePointRepository;

    @Test
    @Transactional
    void switchAndArriveRoutePoint() {
        User user = userHelper.findOrCreateUser(34576398519L);
        UserShift userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        userShift.setActive(true);
        userShiftRepository.save(userShift);

        Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());
        OrderDeliveryTask orderTask1 =
                (OrderDeliveryTask) userHelper.addDeliveryTaskToShift(user,
                        userShift, order1);
        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());
        OrderDeliveryTask orderTask2 =
                (OrderDeliveryTask) userHelper.addDeliveryTaskToShift(user,
                        userShift, order2);

        userHelper.checkinAndFinishPickup(userShift);
        userShift = userShiftRepository.getById(userShift.getId());
        assertThat(userShift.getCurrentRoutePoint()).isNotNull();
        Long nextRoutePointId = Objects.equals(userShift.getCurrentRoutePoint().getId(),
                orderTask1.getRoutePoint().getId()) ? orderTask2.getRoutePoint().getId() :
                orderTask1.getRoutePoint().getId();
        var request = RoutePointSwitchAndArriveDto
                .builder()
                .id(nextRoutePointId)
                .arrive(true)
                .location(LocationDto
                        .builder()
                        .longitude(BigDecimal.ONE)
                        .latitude(BigDecimal.ONE)
                        .deviceId("1")
                        .userShiftId(userShift.getId())
                        .build())
                .routePointSwitch(RoutePointSwitchDto
                        .builder()
                        .reason(RoutePointSwitchReasonType.OTHER)
                        .comment("Так получилось")
                        .build()
                )
                .build();

        var result = shiftFacade.switchAndArriveRoutePoint(request, user);
        var nextRoutePoint = routePointRepository.getById(nextRoutePointId);
        userShift = userShiftRepository.getById(userShift.getId());
        assertThat(result.getCurrentRoutePointId()).isEqualTo(nextRoutePointId);
        assertThat(userShift.getCurrentRoutePoint()).isNotNull();
        assertThat(userShift.getCurrentRoutePoint().getId()).isEqualTo(nextRoutePointId);
        assertThat(nextRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }
}
