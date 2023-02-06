package ru.yandex.market.tpl.core.service.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ARRIVED_DISTANCE_FILTER_ENABLED;
import static ru.yandex.market.tpl.core.service.usershift.ArriveAtRoutePointService.DEFAULT_DISTANCE;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@Slf4j
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ArriveAtRoutePointServiceTest {

    public static final String WRONG_LOCATION_COMMENT = "comment";

    private final ArriveAtRoutePointService arriveAtRoutePointService;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserPropertyService userPropertyService;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private RoutePointRepository routePointRepository;

    private User user;
    private UserShift userShift;

    @AfterEach
    void cleanUp() {
        reset(configurationProviderAdapter);
        reset(routePointRepository);
    }

    @BeforeEach
    void init() {
        when(configurationProviderAdapter.isBooleanEnabled(ARRIVED_DISTANCE_FILTER_ENABLED)).thenReturn(true);
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(824125L, date);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .flowStatus(TRANSPORTATION_RECIPIENT)
                .build());
        userShift = testUserHelper.createOpenedShift(user, order, date);
        var routePoint = mock(RoutePoint.class);
        var id = userShift.getCurrentRoutePoint().getId();
        when(routePoint.getId()).thenReturn(id);
        when(routePoint.getType()).thenReturn(RoutePointType.ORDER_PICKUP);
        when(routePoint.isOrderListEmpty()).thenReturn(false);
        when(routePoint.getUserShift()).thenReturn(userShift);
        when(routePointRepository.findById(id))
                .thenReturn(userShift.findRoutePoint(id));
        when(routePointRepository.findByIdOrThrow(id))
                .thenReturn(userShift.findRoutePoint(id).orElseThrow());
    }

    @Test
    void testNoDistanceFilterNotEnabled() {
        when(configurationProviderAdapter.isBooleanEnabled(ARRIVED_DISTANCE_FILTER_ENABLED)).thenReturn(false);
        LocationDto locationDto = new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId());
        assertThat(arriveAtRoutePointService.arrivedAtRoutePoint(userShift.getCurrentRoutePoint().getId(),
                locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(DEFAULT_DISTANCE);
    }

    @Test
    void testNoDistanceFilterNotEnabledForUser() {
        LocationDto locationDto = new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId());
        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, false);

        assertThat(arriveAtRoutePointService.arrivedAtRoutePoint(userShift.getCurrentRoutePoint().getId(),
                locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(DEFAULT_DISTANCE);
    }

    @Test
    void testDistanceIsAcceptable() {
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(0.0));

        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        LocationDto locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId());
        assertThat(
                arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(), locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(new RoutePointDto.DistanceToRoutePoint(true, new BigDecimal("0.0"),
                        RoutePointDto.CheckType.DISTANCE_CHECK));
    }

    @Test
    void testDistanceIsNotAcceptable() {
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(9000.0));

        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        LocationDto locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId());
        assertThat(
                arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(), locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(new RoutePointDto.DistanceToRoutePoint(false, new BigDecimal("9000.0"),
                        RoutePointDto.CheckType.DISTANCE_CHECK));
    }

    @Test
    void shouldAcceptWithWrongLocationIfCommentExists() {
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(9000.0));
        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        LocationDto locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId(),
                WRONG_LOCATION_COMMENT, null);
        RoutePointDto routePointDto = arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(),
                locationDto, user);
        assertThat(routePointDto.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }

    @Test
    void shouldNoThrowExceptionIfRoutePointIsAlreadyInProgress() {
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(0.0));
        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        RoutePoint currentRoutePoint = userShift.getCurrentRoutePoint();
        LocationDto locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId(),
                null);
        when(routePointRepository.findById(currentRoutePoint.getId()))
                .thenReturn(Optional.of(currentRoutePoint));
        RoutePointDto routePointDto = arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(),
                locationDto, user);
        assertThat(routePointDto.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        routePointDto = arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(),
                locationDto, user);
        assertThat(routePointDto.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }

    @Test
    void electronicQueue_farFromSc() {
        var acceptableDistanceToSc = 5000L;
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getSortingCenter(), SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED, true);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getSortingCenter(), SortingCenterProperties.ACCEPTABLE_DISTANCE_TO_SC_FOR_EQUEUE,
                 acceptableDistanceToSc);

        var distanceToSc = (double) acceptableDistanceToSc + 1;
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(distanceToSc));
        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        var currentRoutePoint = userShift.getCurrentRoutePoint();
        var locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId());
        assertThat(
                arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(), locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(new RoutePointDto.DistanceToRoutePoint(false, BigDecimal.valueOf(distanceToSc),
                        RoutePointDto.CheckType.DISTANCE_CHECK));
    }

    @Test
    void electronicQueue_distanceToScIsAcceptable() {
        var acceptableDistanceToSc = 5000L;
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getSortingCenter(), SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED, true);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getSortingCenter(), SortingCenterProperties.ACCEPTABLE_DISTANCE_TO_SC_FOR_EQUEUE,
                acceptableDistanceToSc);

        var distanceToSc = (double) acceptableDistanceToSc - 1;
        when(routePointRepository.getDistanceMetersToRoutePointOrders(anyLong(), any(), any()))
                .thenReturn(List.of(distanceToSc));
        userPropertyService.addPropertyToUser(user, UserProperties.ARRIVED_TO_RP_DISTANCE_FILTER_ENABLED, true);

        var currentRoutePoint = userShift.getCurrentRoutePoint();
        var locationDto = new LocationDto(currentRoutePoint.getGeoPoint().getLongitude(),
                currentRoutePoint.getGeoPoint().getLatitude(), "myDevice", userShift.getId());
        assertThat(
                arriveAtRoutePointService.arrivedAtRoutePoint(currentRoutePoint.getId(), locationDto, user))
                .extracting(RoutePointDto::getDistanceToRoutePoint)
                .isEqualTo(new RoutePointDto.DistanceToRoutePoint(true, BigDecimal.valueOf(distanceToSc),
                        RoutePointDto.CheckType.DISTANCE_CHECK));
    }

}
