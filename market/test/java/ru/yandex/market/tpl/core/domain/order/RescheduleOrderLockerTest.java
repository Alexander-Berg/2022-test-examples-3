package ru.yandex.market.tpl.core.domain.order;


import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.schedule.RoutingRequestWaveService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.LockerSubtask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftOrderQueryService;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RescheduleOrderLockerTest {
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final OrderManager orderManager;
    private final UserShiftOrderQueryService userShiftOrderQueryService;

    private Order lockerOrderRescheduleDateSameDay;
    private Order lockerOrderRescheduleDateOtherDay;

    private final Instant actualDeliveryDate = Instant.parse("2021-02-19T18:35:24.00Z");
    private final LocalDate rescheduleDateSameDay = LocalDate.parse("2021-02-19");
    private final LocalDate rescheduleDateOtherDay = LocalDate.parse("2021-02-21");

    @MockBean
    private RoutingRequestWaveService mockedRoutingRequestWaveService;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    private final Clock clock;

    @BeforeEach
    void init() {
        User user = testUserHelper.findOrCreateUser(1L);
        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
        PickupPoint pickupPoint = createPickupPoint();
        RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(
                user, userShiftId, actualDeliveryDate, actualDeliveryDate);

        lockerOrderRescheduleDateSameDay = createLockerOrder(rescheduleDateSameDay, pickupPoint);
        lockerOrderRescheduleDateOtherDay = createLockerOrder(rescheduleDateOtherDay, pickupPoint);

        UserShift userShift = routePoint.getUserShift();
        addLockerDeliverySubtask(userShift, lockerOrderRescheduleDateSameDay);
        addLockerDeliverySubtask(userShift, lockerOrderRescheduleDateOtherDay);
    }

    @Test
    void rescheduleOnSameDayLockerOrderTest_success_WaveStartButPickpointOrder() {
        //given
        LocalDateTime firstWaveTime = LocalDateTime.now(clock).minusHours(1L);
        doReturn(Optional.of(firstWaveTime)).when(mockedRoutingRequestWaveService).getFirstRoutingWaveTime(anyLong(),
                any());

        doReturn(true).when(configurationProviderAdapter)
                .isBooleanEnabled(ConfigurationProperties.UPDATE_ORDER_VALIDATOR_DEPENDS_ROUTING_ENABLED);

        //when
        assertDoesNotThrow(() -> orderManager.rescheduleOrdersByIds(
                List.of(lockerOrderRescheduleDateSameDay.getId()),
                rescheduleDateSameDay, null, null));
    }

    @Test
    void rescheduleOnSameDayLockerOrderTest_success() {
        DeliverySubtask actualLockerDeliveryTask = userShiftOrderQueryService
                .findDeliverySubtasksByOrder(lockerOrderRescheduleDateSameDay)
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        //Before routing starts
        LocalDateTime firstWaveTime = LocalDateTime.now(clock).plusHours(1L);
        doReturn(Optional.of(firstWaveTime)).when(mockedRoutingRequestWaveService).getFirstRoutingWaveTime(anyLong(),
                any());

        orderManager.rescheduleOrdersByIds(
                List.of(lockerOrderRescheduleDateSameDay.getId()),
                rescheduleDateSameDay, null, null);

        if (actualLockerDeliveryTask instanceof LockerSubtask) {
            LockerSubtask lockerSubtask = (LockerSubtask) actualLockerDeliveryTask;

            assertThat(lockerSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
            assertThat(actualLockerDeliveryTask.getStatus().isFailed()).isTrue();

            LocalDate deliveryDateAfterReschedule = LocalDate.ofInstant(
                    lockerOrderRescheduleDateSameDay.getDelivery().getDeliveryIntervalFrom(),
                    DateTimeUtil.DEFAULT_ZONE_ID);

            assertThat(deliveryDateAfterReschedule).isEqualTo(rescheduleDateSameDay);
        } else {
            throw new IllegalStateException("Delivery subtask must be locker subtask");
        }
    }

    @Test
    void rescheduleOtherDayLockerOrderTest() {
        DeliverySubtask actualLockerDeliveryTask = userShiftOrderQueryService
                .findDeliverySubtasksByOrder(lockerOrderRescheduleDateOtherDay)
                .findFirst()
                .orElseThrow(IllegalStateException::new);

        orderManager.rescheduleOrdersByIds(
                List.of(lockerOrderRescheduleDateOtherDay.getId()),
                rescheduleDateOtherDay, null, null);

        if (actualLockerDeliveryTask instanceof LockerSubtask) {
            LockerSubtask lockerSubtask = (LockerSubtask) actualLockerDeliveryTask;

            assertThat(lockerSubtask.getStatus()).isEqualTo(LockerDeliverySubtaskStatus.FAILED);
            assertThat(actualLockerDeliveryTask.getStatus().isFailed()).isTrue();

            LocalDate deliveryDateAfterReschedule = LocalDate.ofInstant(
                    lockerOrderRescheduleDateOtherDay.getDelivery().getDeliveryIntervalFrom(),
                    DateTimeUtil.DEFAULT_ZONE_ID);

            assertThat(deliveryDateAfterReschedule).isEqualTo(rescheduleDateOtherDay);
        } else {
            throw new IllegalStateException("Delivery subtask must be locker subtask");
        }
    }

    private Order createLockerOrder(LocalDate deliveryDate, PickupPoint pickupPoint) {
        return orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(deliveryDate)
                        .pickupPoint(pickupPoint)
                        .build()
        );
    }

    private PickupPoint createPickupPoint() {
        return pickupPointRepository.save(testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
    }

    private void addLockerDeliverySubtask(UserShift userShift, Order order) {
        userShift.addLockerDeliverySubtask(NewDeliveryRoutePointData.builder()
                        .withOrderReferenceFromOrder(order, true, false)
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .expectedDeliveryTime(actualDeliveryDate)
                        .expectedArrivalTime(actualDeliveryDate)
                        .name("asdf")
                        .address(new RoutePointAddress("asfd", GeoPointGenerator.generateLonLat()))
                        .build(),
                null,
                false
        );
    }
}

