package ru.yandex.market.tpl.core.domain.usershift;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UnloadedOrder;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.tomorrowAtHour;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_SHIFT_CLOSED_CRON_UPDATED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class UserShiftWithLockerTasksCancelByCronTest {

    private final OrderRepository orderRepository;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftManager userShiftManager;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TestDataFactory testDataFactory;
    private final OrderManager orderManager;
    private final PickupPointRepository pickupPointRepository;
    private User user;
    private Order order;
    private Order order2;
    private Shift shift;
    private UserShift userShift;
    private long userShiftId;
    private RoutePoint routePoint;
    private LockerDeliveryTask lockerDeliveryTask;

    @MockBean
    private Clock clock;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @BeforeEach
    void createShifts() {
        when(configurationProviderAdapter.isBooleanEnabled(IS_SHIFT_CLOSED_CRON_UPDATED)).thenReturn(true);
        ClockUtil.initFixed(clock, LocalDateTime.now().minusHours(4).minusMinutes(40));
        user = userHelper.findOrCreateUser(1L);
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        PickupPoint pickupPoint = pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER
                , 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        order = getPickupOrder(pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED);
        order2 = getPickupOrder(pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED);

        userShiftReassignManager.assign(userShift, order);
        userShiftReassignManager.assign(userShift, order2);

        userHelper.checkinAndFinishPickup(userShift);

        routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();
        lockerDeliveryTask = (LockerDeliveryTask) routePoint.streamDeliveryTasks().findFirst().orElseThrow();
        assertThat(lockerDeliveryTask.getSubtasks())
                .hasSize(2);

        when(configurationProviderAdapter.isBooleanEnabled(UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED))
                .thenReturn(true);
    }

    @Test
    void shouldCloseUserShiftAfterFinishLockerTasks() {
        assertThat(getAllShiftsToClose()).isEmpty();

        userHelper.arriveAtRoutePoint(routePoint);
        Long returnRoutePointId = userShift.streamReturnRoutePoints()
                .findFirst().get().getId();

        commandService.finishLoadingLocker(user,
                new UserShiftCommand.FinishLoadingLocker(routePoint.getUserShift().getId(), routePoint.getId(),
                        lockerDeliveryTask.getId(), null, ScanRequest.builder()
                        .successfullyScannedOrders(List.of(order.getId(), order2.getId()))
                        .build()));
        commandService.finishUnloadingLocker(user,
                new UserShiftCommand.FinishUnloadingLocker(
                        routePoint.getUserShift().getId(),
                        routePoint.getId(),
                        lockerDeliveryTask.getId(),
                        Set.of(
                                new UnloadedOrder(order.getExternalOrderId(), null, List.of()),
                                new UnloadedOrder(order2.getExternalOrderId(), null, List.of()))));

        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(250, ChronoUnit.MINUTES)),
                returnRoutePointId);
        orderRepository.findCurrentUserOrders(userShift.getUser().getId());
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(1);
    }


    @Test
    void shouldNoCloseUserShiftAfterFailLockerTasks() {
        assertThat(getAllShiftsToClose()).isEmpty();

        userHelper.arriveAtRoutePoint(routePoint);
        Long returnRoutePointId = userShift.streamReturnRoutePoints()
                .findFirst().get().getId();
        for (Order o : List.of(order, order2)) {
            orderManager.rescheduleOrder(o, new Interval(tomorrowAtHour(18, clock), tomorrowAtHour(20, clock)),
                    Source.DELIVERY);
        }

        List<UserShift> userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);

        jdbcTemplate.update("UPDATE route_point SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.now(clock).minus(250, ChronoUnit.MINUTES)),
                returnRoutePointId);
        orderRepository.findCurrentUserOrders(userShift.getUser().getId());
        userShifts = getAllShiftsToClose();
        assertThat(userShifts).hasSize(0);
    }

    private List<UserShift> getAllShiftsToClose() {
        return userShiftManager.findShiftsToClose(shift.getShiftDate(),
                Instant.now(clock).plusSeconds(1));
    }

    private Order getPickupOrder(PickupPoint pickupPoint, GeoPoint geoPoint, OrderFlowStatus orderFlowStatus) {
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(LocalDate.now(clock))
                        .pickupPoint(pickupPoint)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .flowStatus(orderFlowStatus)
                        .build());
    }
}
