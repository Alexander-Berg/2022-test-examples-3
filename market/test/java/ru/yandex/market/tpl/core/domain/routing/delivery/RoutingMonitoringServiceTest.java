package ru.yandex.market.tpl.core.domain.routing.delivery;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocation;
import ru.yandex.market.tpl.core.domain.usershift.location.UserLocationRepository;
import ru.yandex.market.tpl.core.external.routing.delivery.client.RoutingClient;
import ru.yandex.market.tpl.core.external.routing.delivery.model.OrderBatch;
import ru.yandex.market.tpl.core.external.routing.delivery.model.PositionsDto;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_ROUTING_MONITORING_ENABLED;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RoutingMonitoringServiceTest {
    private final RoutingMonitoringService routingDeliveryService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserRepository userRepository;
    private final UserShiftRepository userShiftRepository;
    private final UserLocationRepository userLocationRepository;
    private final TestDataFactory testDataFactory;
    private final PickupPointRepository pickupPointRepository;
    private final UserCommandService userCommandService;
    @MockBean
    private RoutingClient routingClient;
    @MockBean
    private ConfigurationServiceAdapter configurationServiceAdapter;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;


    @Captor
    private ArgumentCaptor<PositionsDto> positionsDtoArgumentCaptor;
    private User user;
    private UserShift userShift;

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
        userCommandService.updateRoutingClientId(UserCommand.UpdateRoutingCourierId.builder().userId(user.getId()).routingCourierId(123L).build());
        userRepository.save(user);
        userShift.setRouteId(1L);
        userShiftRepository.save(userShift);
        when(configurationProviderAdapter.isBooleanEnabled(same(IS_ROUTING_MONITORING_ENABLED))).thenReturn(true);
    }

    @Test
    public void testSendUserLocationFirstTime() {
        when(configurationProviderAdapter.getValueAsLong(any())).thenReturn(Optional.empty());
        var userLocation = createUserLocation();

        routingDeliveryService.processLocations();

        verify(routingClient).pushPositions(any(), any(), positionsDtoArgumentCaptor.capture());
        verify(configurationServiceAdapter).mergeValue(eq(ConfigurationProperties.USER_LOCATION_LAST_PUSHED_ID),
                eq(userLocation.getId()));
        var positionsDto = positionsDtoArgumentCaptor.getValue();
        assertThat(positionsDto.getPositions()).hasSize(1);
        var positionDto = positionsDto.getPositions().iterator().next();
        assertThat(positionDto.getLatitude()).isEqualTo(userLocation.getLatitude());
        assertThat(positionDto.getLongitude()).isEqualTo(userLocation.getLongitude());
        assertThat(positionDto.getTime()).isEqualTo(userLocation.getCreatedAt().atZone(DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @Test
    public void testSendUserLocationAfterSavedId() {
        when(configurationProviderAdapter.getValueAsLong(any())).thenReturn(Optional.of(0L));
        var userLocation = createUserLocation();

        routingDeliveryService.processLocations();

        verify(routingClient).pushPositions(any(), any(), positionsDtoArgumentCaptor.capture());
        verify(configurationServiceAdapter).mergeValue(eq(ConfigurationProperties.USER_LOCATION_LAST_PUSHED_ID),
                eq(userLocation.getId()));
        var positionsDto = positionsDtoArgumentCaptor.getValue();
        assertThat(positionsDto.getPositions()).hasSize(1);
        var positionDto = positionsDto.getPositions().iterator().next();
        assertThat(positionDto.getLatitude()).isEqualTo(userLocation.getLatitude());
        assertThat(positionDto.getLongitude()).isEqualTo(userLocation.getLongitude());
        assertThat(positionDto.getTime()).isEqualTo(userLocation.getCreatedAt().atZone(DateTimeUtil.DEFAULT_ZONE_ID));
    }

    @Test
    public void testSendUserLocationSavedIdIsGreater() {
        when(configurationProviderAdapter.getValueAsLong(any())).thenReturn(Optional.of(1000L));

        routingDeliveryService.processLocations();

        verify(routingClient, times(0)).pushPositions(any(), any(), any());
    }

    @Test
    public void testMapFinishedTasks() {
        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .pickupPoint(pickupPoint)
                .build());
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        var result = routingDeliveryService.createOrderBatchForShift(userShift.getId());
        result.stream()
                .map(OrderBatch::getStatus)
                .forEach(status -> assertThat(status).isEqualTo("new"));

        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        userShift.streamDeliveryRoutePoints()
                .forEach(rp -> testUserHelper.finishDelivery(rp, false));
        result = routingDeliveryService.createOrderBatchForShift(userShift.getId());
        result.stream()
                .map(OrderBatch::getStatus)
                .forEach(status -> assertThat(status).isEqualTo("finished"));
    }

    @Test
    public void testMapFailedTasks() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .pickupPoint(pickupPoint)
                .build());

        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        var result = routingDeliveryService.createOrderBatchForShift(userShift.getId());
        result.stream()
                .map(OrderBatch::getStatus)
                .forEach(status -> assertThat(status).isEqualTo("new"));

        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        userShift.streamDeliveryRoutePoints()
                .forEach(rp -> testUserHelper.finishDelivery(rp, true));
        result = routingDeliveryService.createOrderBatchForShift(userShift.getId());
        result.stream()
                .map(OrderBatch::getStatus)
                .forEach(status -> assertThat(status).isEqualTo("cancelled"));
    }

    private UserLocation createUserLocation() {
        UserLocation userLocation = new UserLocation();
        userLocation.setLatitude(new BigDecimal("1.0"));
        userLocation.setLongitude(new BigDecimal("1.0"));
        userLocation.setUserShiftId(userShift.getId());
        userLocation.setDeviceId("deviceId");
        userLocation.setUserId(user.getId());
        return userLocationRepository.save(userLocation);
    }
}
