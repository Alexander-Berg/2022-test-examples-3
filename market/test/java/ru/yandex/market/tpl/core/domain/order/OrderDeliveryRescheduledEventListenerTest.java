package ru.yandex.market.tpl.core.domain.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import ru.yandex.market.tpl.common.util.datetime.Interval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.events.OrderDeliveryRescheduledEvent;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderDeliveryRescheduledEventListenerTest {

    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private SortingCenterPropertyService sortingCenterPropertyService;
    @Mock
    private SortingCenterService sortingCenterService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderManager orderManager;
    @Mock
    private DsZoneOffsetCachingService dsZoneOffsetCachingService;

    @InjectMocks
    private OrderDeliveryRescheduledEventListener unit;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCancelOrderAfterReschedulingLimitIsReached() {
        long orderId = 12L;
        Integer daysWithDeliveryTask = 3;
        Integer maxReschedules = 3;
        long deliveryServiceId = 1L;
        Long sortingCenterId = 111L;
        ZoneOffset offset = ZoneOffset.UTC;
        Interval interval1 = new Interval(
                Instant.now().minus(Period.ofDays(2)),
                Instant.now().minus(Period.ofDays(1))
        );
        Interval interval2 = new Interval(
                Instant.now().minus(Period.ofDays(1)),
                Instant.now()
        );

        Order mockOrder = mock(Order.class);
        when(mockOrder.getDeliveryServiceId()).thenReturn(deliveryServiceId);
        when(mockOrder.getId()).thenReturn(orderId);

        DeliveryReschedule mockReschedule = mock(DeliveryReschedule.class);
        when(mockReschedule.getInterval()).thenReturn(interval1);

        OrderDeliveryRescheduledEvent mockEvent = mock(OrderDeliveryRescheduledEvent.class);
        when(mockEvent.getAggregate()).thenReturn(mockOrder);
        when(mockEvent.getIntervalBefore()).thenReturn(interval2);
        when(mockEvent.getDeliveryReschedule()).thenReturn(mockReschedule);

        SortingCenter mockSortingCenter = mock(SortingCenter.class);
        when(mockSortingCenter.getId()).thenReturn(sortingCenterId);

        when(dsZoneOffsetCachingService.getOffsetForDs(deliveryServiceId)).thenReturn(offset);
        when(sortingCenterService.findSortCenterForDs(deliveryServiceId)).thenReturn(mockSortingCenter);

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                eq(sortingCenterId))
        ).thenReturn(true);

        when(orderRepository.getDaysWithDeliveryTaskCount(orderId)).thenReturn(daysWithDeliveryTask);
        when(configurationProviderAdapter.getValueAsInteger(ConfigurationProperties.FAILED_DELIVERY_COUNT_FOR_AUTO_CANCEL))
                .thenReturn(Optional.of(maxReschedules));

        unit.cancelOrderIfExtraRescheduling(mockEvent);

        verify(orderManager).cancelOrder(eq(mockOrder), any(OrderDeliveryFailReason.class));
    }

    @Test
    void shouldNotCancelOrderIfReschedulingLimitIsNotReached() {
        long orderId = 12L;
        Integer daysWithDeliveryTask = 2;
        Integer maxReschedules = 3;
        long deliveryServiceId = 1L;
        Long sortingCenterId = 111L;
        ZoneOffset offset = ZoneOffset.UTC;
        Interval interval1 = new Interval(
                Instant.now().minus(Period.ofDays(2)),
                Instant.now().minus(Period.ofDays(1))
        );
        Interval interval2 = new Interval(
                Instant.now().minus(Period.ofDays(1)),
                Instant.now()
        );

        Order mockOrder = mock(Order.class);
        when(mockOrder.getDeliveryServiceId()).thenReturn(deliveryServiceId);
        when(mockOrder.getId()).thenReturn(orderId);

        DeliveryReschedule mockReschedule = mock(DeliveryReschedule.class);
        when(mockReschedule.getInterval()).thenReturn(interval1);

        OrderDeliveryRescheduledEvent mockEvent = mock(OrderDeliveryRescheduledEvent.class);
        when(mockEvent.getAggregate()).thenReturn(mockOrder);
        when(mockEvent.getIntervalBefore()).thenReturn(interval2);
        when(mockEvent.getDeliveryReschedule()).thenReturn(mockReschedule);

        SortingCenter mockSortingCenter = mock(SortingCenter.class);
        when(mockSortingCenter.getId()).thenReturn(sortingCenterId);

        when(dsZoneOffsetCachingService.getOffsetForDs(deliveryServiceId)).thenReturn(offset);
        when(sortingCenterService.findSortCenterForDs(deliveryServiceId)).thenReturn(mockSortingCenter);

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                eq(sortingCenterId))
        ).thenReturn(true);

        when(orderRepository.getDaysWithDeliveryTaskCount(orderId)).thenReturn(daysWithDeliveryTask);
        when(configurationProviderAdapter.getValueAsInteger(ConfigurationProperties.FAILED_DELIVERY_COUNT_FOR_AUTO_CANCEL))
                .thenReturn(Optional.of(maxReschedules));

        unit.cancelOrderIfExtraRescheduling(mockEvent);

        verify(orderManager, never()).cancelOrder(eq(mockOrder), any(OrderDeliveryFailReason.class));
    }

    @Test
    void shouldNotCancelOrderForSameDayInterval() {
        long orderId = 12L;
        long deliveryServiceId = 1L;
        Long sortingCenterId = 111L;
        ZoneOffset offset = ZoneOffset.UTC;
        Interval interval1 = new Interval(
                Instant.now().minus(Period.ofDays(2)),
                Instant.now().minus(Period.ofDays(1))
        );

        Order mockOrder = mock(Order.class);
        when(mockOrder.getDeliveryServiceId()).thenReturn(deliveryServiceId);
        when(mockOrder.getId()).thenReturn(orderId);

        DeliveryReschedule mockReschedule = mock(DeliveryReschedule.class);
        when(mockReschedule.getInterval()).thenReturn(interval1);

        OrderDeliveryRescheduledEvent mockEvent = mock(OrderDeliveryRescheduledEvent.class);
        when(mockEvent.getAggregate()).thenReturn(mockOrder);
        when(mockEvent.getIntervalBefore()).thenReturn(interval1);
        when(mockEvent.getDeliveryReschedule()).thenReturn(mockReschedule);

        SortingCenter mockSortingCenter = mock(SortingCenter.class);
        when(mockSortingCenter.getId()).thenReturn(sortingCenterId);

        when(dsZoneOffsetCachingService.getOffsetForDs(deliveryServiceId)).thenReturn(offset);
        when(sortingCenterService.findSortCenterForDs(deliveryServiceId)).thenReturn(mockSortingCenter);

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                eq(sortingCenterId))
        ).thenReturn(true);

        unit.cancelOrderIfExtraRescheduling(mockEvent);

        verify(orderManager, never()).cancelOrder(eq(mockOrder), any(OrderDeliveryFailReason.class));
        verify(configurationProviderAdapter, never()).getValueAsInteger(ConfigurationProperties.FAILED_DELIVERY_COUNT_FOR_AUTO_CANCEL);
        verify(orderRepository, never()).getDaysWithDeliveryTaskCount(orderId);
    }

    @Test
    void shouldNotCancelOrderIfReschedulingIsNotLimitedForSc() {
        long orderId = 12L;
        long deliveryServiceId = 1L;
        Long sortingCenterId = 111L;
        ZoneOffset offset = ZoneOffset.UTC;
        Interval interval1 = new Interval(
                Instant.now().minus(Period.ofDays(2)),
                Instant.now().minus(Period.ofDays(1))
        );
        Interval interval2 = new Interval(
                Instant.now().minus(Period.ofDays(1)),
                Instant.now()
        );

        Order mockOrder = mock(Order.class);
        when(mockOrder.getDeliveryServiceId()).thenReturn(deliveryServiceId);
        when(mockOrder.getId()).thenReturn(orderId);

        DeliveryReschedule mockReschedule = mock(DeliveryReschedule.class);
        when(mockReschedule.getInterval()).thenReturn(interval1);

        OrderDeliveryRescheduledEvent mockEvent = mock(OrderDeliveryRescheduledEvent.class);
        when(mockEvent.getAggregate()).thenReturn(mockOrder);
        when(mockEvent.getIntervalBefore()).thenReturn(interval2);
        when(mockEvent.getDeliveryReschedule()).thenReturn(mockReschedule);

        SortingCenter mockSortingCenter = mock(SortingCenter.class);
        when(mockSortingCenter.getId()).thenReturn(sortingCenterId);

        when(dsZoneOffsetCachingService.getOffsetForDs(deliveryServiceId)).thenReturn(offset);
        when(sortingCenterService.findSortCenterForDs(deliveryServiceId)).thenReturn(mockSortingCenter);

        when(sortingCenterPropertyService.findPropertyValueForSortingCenterOrDefault(
                eq(SortingCenterProperties.IS_RESCHEDULING_LIMITED),
                eq(sortingCenterId))
        ).thenReturn(false);

        unit.cancelOrderIfExtraRescheduling(mockEvent);

        verify(orderManager, never()).cancelOrder(eq(mockOrder), any(OrderDeliveryFailReason.class));
        verify(configurationProviderAdapter, never()).getValueAsInteger(ConfigurationProperties.FAILED_DELIVERY_COUNT_FOR_AUTO_CANCEL);
        verify(orderRepository, never()).getDaysWithDeliveryTaskCount(orderId);
    }
}