package ru.yandex.market.tpl.internal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderCoordsUpdaterTest {
    private static final Long USER_ID = 824129L;
    private static final Double LATITUDE = 55.;
    private static final Double LONGITUDE = 37.;

    private final OrderCoordsUpdater orderCoordsUpdater;
    private final OrderRepository orderRepository;
    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final OrderCommandService orderCommandService;
    private final Clock clock;
    private final TplRegionBorderGisDao tplRegionBorderGisDao;
    private final ConfigurationProviderAdapter configurationProvider;
    private final SortingCenterService sortingCenterService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private Order order;

    @MockBean
    private DeliveryServiceRegionRepository deliveryServiceRegionRepository;

    private final HttpGeobase httpGeobase;

    @BeforeEach
    void setUp() {
        var user = userHelper.findOrCreateUser(USER_ID);
        this.order = orderGenerateService.createOrder();
        userHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_OPEN, order);
    }

    @Test
    void updateOrderDeliveryCoords_validationEnabled_Failure() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(1));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of(1L));

        when(configurationProvider.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId()).getId()));

        when(tplRegionBorderGisDao.
                        findDsRegionIdsWithinMarginFromPoint(any(),any(),any(),any(),anyInt(), any()))
                .thenReturn(List.of());

        //when
        assertThrows(TplInvalidParameterException.class,
                () -> orderCoordsUpdater.updateById(order.getId(), LATITUDE, LONGITUDE));
    }

    @Test
    void updateOrderDeliveryCoords_validationEnabled_Success() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(1));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of(1L));

        when(configurationProvider.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId()).getId()));

        when(tplRegionBorderGisDao.
                findDsRegionIdsWithinMarginFromPoint(any(),any(),any(),any(),anyInt(), any()))
                .thenReturn(List.of(123L));

        //when
        orderCoordsUpdater.updateById(order.getId(), LATITUDE, LONGITUDE);

        //then
        Order order = orderRepository.findById(this.order.getId()).orElseThrow();
        OrderDelivery orderDelivery = order.getDelivery();
        assertEquals(LATITUDE, orderDelivery.getDeliveryAddress().getLatitude().longValue());
        assertEquals(LONGITUDE, orderDelivery.getDeliveryAddress().getLongitude().longValue());
        assertEquals(LONGITUDE, orderDelivery.getDeliveryAddress().getLongitude().longValue());
        assertTrue(order.getIsAddressValid());
        assertNotNull(order.getAddressValidatedAt());
    }

    @Test
    void updateOrderDeliveryCoords_validationDisabled() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(1));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of(1L));

        when(configurationProvider.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of());

        //when
        orderCoordsUpdater.updateById(order.getId(), LATITUDE, LONGITUDE);

        //then
        Order order = orderRepository.findById(this.order.getId()).orElseThrow();
        OrderDelivery orderDelivery = order.getDelivery();
        assertEquals(LATITUDE, orderDelivery.getDeliveryAddress().getLatitude().longValue());
        assertEquals(LONGITUDE, orderDelivery.getDeliveryAddress().getLongitude().longValue());
    }

    @Test
    void updateOrderDeliveryCoordsByExternalOrderIdWithReschedule() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(1));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of(1L));

        //when
        orderCoordsUpdater.updateByExternalOrderId(order.getExternalOrderId(), LATITUDE, LONGITUDE, true);


        //then
        Order order = orderRepository.findById(this.order.getId()).orElseThrow();
        OrderDelivery orderDelivery = order.getDelivery();
        assertEquals(LATITUDE, orderDelivery.getDeliveryAddress().getLatitude().longValue());
        assertEquals(LONGITUDE, orderDelivery.getDeliveryAddress().getLongitude().longValue());

        assertThat(order.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID))
                .isEqualTo(LocalDate.now(clock).plusDays(1));
        var dsApiCheckpoints = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId())
                .stream()
                .map(OrderFlowStatusHistory::getDsApiCheckpoint)
                .collect(Collectors.toList());
        assertThat(dsApiCheckpoints).contains(44);
    }

    @Test
    void updateOrderDeliveryCoordsByExternalNotInDeliveryZone() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(2));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of());

        Mockito.when(configurationProvider.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(1L));

        //when
        assertThrows(Exception.class, () -> orderCoordsUpdater.updateByExternalOrderId(order.getExternalOrderId(),
                LATITUDE, LONGITUDE, true));
    }

    @Test
    void updateOrderDeliveryCoordsByExternalOrderIdWithoutReschedule() {
        //given
        when(httpGeobase.getRegionId(LATITUDE, LONGITUDE)).thenReturn(1);
        Map<Long, Set<Integer>> map = new HashMap<>();
        map.put(order.getDeliveryServiceId(), Set.of(1));
        when(deliveryServiceRegionRepository.getRegionsByDeliveryService(Set.of(order.getDeliveryServiceId()), 1))
                .thenReturn(map);

        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        Mockito.when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(order.getDeliveryServiceId()), any(), eq(geoPoint.getLongitude()),
                eq(geoPoint.getLatitude()), anyInt(), any()))
                .thenReturn(List.of(1L));

        //when
        orderCoordsUpdater.updateByExternalOrderId(order.getExternalOrderId(), LATITUDE, LONGITUDE, false);

        //then
        Order order = orderRepository.findById(this.order.getId()).orElseThrow();
        OrderDelivery orderDelivery = order.getDelivery();
        assertEquals(LATITUDE, orderDelivery.getDeliveryAddress().getLatitude().longValue());
        assertEquals(LONGITUDE, orderDelivery.getDeliveryAddress().getLongitude().longValue());

        assertThat(order.getDelivery().getDeliveryDate(DateTimeUtil.DEFAULT_ZONE_ID))
                .isEqualTo(LocalDate.now(clock));
    }

    @Test
    void mixedUpLatAndLon() {
        //given
        Mockito.when(configurationProvider.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(1L));

        //then
        assertThrows(Exception.class, () -> orderCoordsUpdater.updateById(order.getId(), LONGITUDE, LATITUDE));

    }

    @Test
    void addChangeHistoryOfCoordinates() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        OrderCommand.UpdateCoordinates command = new OrderCommand.UpdateCoordinates(order.getId(), geoPoint);
        orderCommandService.updateCoordinates(command);
        Optional<OrderHistoryEvent> historyEvent = orderHistoryEventRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        assertThat(historyEvent).isPresent();
        assertThat(historyEvent.get().getType()).isEqualTo(OrderEventType.COORDINATES_UPDATED);
        assertThat(historyEvent.get().getSource()).isEqualTo(Source.SYSTEM);
    }

    @Test
    void sendLesEventOnCoordinatesUpdate() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(LATITUDE, LONGITUDE);
        OrderCommand.UpdateCoordinates command = new OrderCommand.UpdateCoordinates(order.getId(), geoPoint);
        orderCommandService.updateCoordinates(command);
        dbQueueTestUtil.assertQueueHasSize(QueueType.ORDER_DELIVERY_ADDRESS_UPDATE_SEND_TO_SQS, 1);
    }
}
