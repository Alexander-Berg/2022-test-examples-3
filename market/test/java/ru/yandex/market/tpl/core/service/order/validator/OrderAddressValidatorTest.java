package ru.yandex.market.tpl.core.service.order.validator;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.region.actualization.TplRegionBorderGisDao;
import ru.yandex.market.tpl.core.util.TplDbConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAddressValidatorTest {

    @Mock
    private TplRegionBorderGisDao tplRegionBorderGisDao;
    @Mock
    private SortingCenterService sortingCenterService;
    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @Mock
    private OrderRepository orderRepository;
    @InjectMocks
    private OrderAddressValidator orderAddressValidator;

    @Test
    void isGeoValid_Fail() {
        //given
        long orderId = 1L;
        when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Collections.singleton(orderId)),
                any(), anyInt())).thenReturn(List.of());

        //when
        boolean isGeoValid = orderAddressValidator.isGeoValid(orderId);

        //then
        assertFalse(isGeoValid);
    }

    @Test
    void isGeoValid_Success() {
        //given
        long orderId = 1L;
        when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Collections.singleton(orderId)),
                any(), anyInt())).thenReturn(List.of(TplRegionBorderGisDao.OrderRegion.of(orderId, 2L)));

        //when
        boolean isGeoValid = orderAddressValidator.isGeoValid(orderId);

        //then
        assertTrue(isGeoValid);
    }

    @Test
    void isGeoValidBulk_Fail() {
        //given
        long orderId1 = 1L;
        long orderId2 = 2L;

        Mockito.when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Set.of(orderId1, orderId2)),
                any(), anyInt())).thenReturn(List.of());

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(Set.of(orderId1, orderId2));

        //then
        assertFalse(geoValidBulkResult.get(orderId1));
        assertFalse(geoValidBulkResult.get(orderId2));
    }

    @Test
    void isGeoValidBulk_Success_whenAllPickup() {
        //given
        long orderId1 = 1L;
        long orderId2 = 2L;

        List<Order> pickups = List.of(buildPickupOrder(orderId1), buildPickupOrder(orderId2));
        when(orderRepository.findAllById(argThat(
                arg -> listArgumentMatcher(arg, List.of(orderId1, orderId2))
        ))).thenReturn(pickups);

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(Set.of(orderId1, orderId2));

        //then
        verify(tplRegionBorderGisDao, never()).findDsRegionWithinMargin(any(), any(), anyInt());

        assertTrue(geoValidBulkResult.get(orderId1));
        assertTrue(geoValidBulkResult.get(orderId2));
    }

    @Test
    void isGeoValidBulk_Success_whenSomePickup() {
        //given
        long orderId1 = 1L;
        long orderId2 = 2L;

        List<Order> pickups = List.of(buildPickupOrder(orderId1));
        when(orderRepository.findAllById(argThat(
                arg -> listArgumentMatcher(arg, List.of(orderId1, orderId2))
        ))).thenReturn(pickups);

        when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Set.of(orderId2)),
                any(), anyInt())).thenReturn(List.of(
                TplRegionBorderGisDao.OrderRegion.of(orderId2, 4L)
        ));

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(Set.of(orderId1, orderId2));

        //then
        assertTrue(geoValidBulkResult.get(orderId1));
        assertTrue(geoValidBulkResult.get(orderId2));
    }

    @Test
    void isGeoValidBulk_Success_whenNonePickups() {
        //given
        long orderId1 = 1L;
        long orderId2 = 2L;

        when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Set.of(orderId1, orderId2)),
                any(), anyInt())).thenReturn(List.of(
                        TplRegionBorderGisDao.OrderRegion.of(orderId1, 3L),
                        TplRegionBorderGisDao.OrderRegion.of(orderId2, 4L)
                ));

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(Set.of(orderId1, orderId2));

        //then
        assertTrue(geoValidBulkResult.get(orderId1));
        assertTrue(geoValidBulkResult.get(orderId2));
    }

    @Test
    void isGeoValidBulk_Success_Batch() {
        //given
        long orderId1 = 1L;
        long orderId2 = TplDbConstants.MAX_IDS_IN_ONE_DB_QUERY + 1L;

        Set<Long> hugeOrderIdsSet = Stream
                .iterate(1L, n -> (n + 1))
                .limit(TplDbConstants.MAX_IDS_IN_ONE_DB_QUERY + 1)
                .collect(Collectors.toSet());

        ArgumentCaptor<Set<Long>> requestOrdersCaptor =
                ArgumentCaptor.forClass(Set.class);

        when(tplRegionBorderGisDao.findDsRegionWithinMargin(requestOrdersCaptor.capture(),
                any(), anyInt())).thenReturn(
                        List.of(TplRegionBorderGisDao.OrderRegion.of(orderId1, 3L)),
                        List.of(TplRegionBorderGisDao.OrderRegion.of(orderId2, 4L)
        ));

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(hugeOrderIdsSet);

        //then
        assertThat(hugeOrderIdsSet).isEqualTo(requestOrdersCaptor
                .getAllValues()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
        );

        assertTrue(geoValidBulkResult.get(orderId1));
        assertTrue(geoValidBulkResult.get(orderId2));
    }


    @Test
    void isGeoValidBulk_Merge() {
        //given
        long orderId1 = 1L;
        long orderId2 = 2L;
        long orderId4 = 4L;

        when(tplRegionBorderGisDao.findDsRegionWithinMargin(eq(Set.of(orderId1, orderId2)),
                any(), anyInt())).thenReturn(List.of(
                TplRegionBorderGisDao.OrderRegion.of(orderId1, 3L)
        ));

        List<Order> pickups = List.of(buildPickupOrder(orderId4));
        when(orderRepository.findAllById(argThat(
                arg -> listArgumentMatcher(arg, List.of(orderId1, orderId2, orderId4))
        ))).thenReturn(pickups);

        //when
        Map<Long, Boolean> geoValidBulkResult = orderAddressValidator.isGeoValidBulk(Set.of(orderId1, orderId2, orderId4));

        //then
        assertTrue(geoValidBulkResult.get(orderId1));
        assertFalse(geoValidBulkResult.get(orderId2));
        assertTrue(geoValidBulkResult.get(orderId4));
    }

    private boolean listArgumentMatcher(Iterable<Long> argument, List<Long> expected) {
        return argument instanceof List &&
                expected.containsAll((List<Long>)argument) &&
                ((List<Long>)argument).containsAll(expected);
    }

    @Test
    void testIsGeoValid_Fail() {
        //given
        BigDecimal lan = BigDecimal.ONE;
        BigDecimal lon = BigDecimal.TEN;
        long dsId = 1L;

        when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(dsId), any(), eq(lon), eq(lan), anyInt(), any())).thenReturn(List.of());

        //when
        boolean isGeoValid = orderAddressValidator.isGeoValid(lon, lan, dsId);

        //then
        assertFalse(isGeoValid);
    }

    @Test
    void testIsGeoValid_Success() {
        //given
        BigDecimal lan = BigDecimal.ONE;
        BigDecimal lon = BigDecimal.TEN;
        long dsId = 1L;
        long foundedRegionId = 2L;

        when(tplRegionBorderGisDao.findDsRegionIdsWithinMarginFromPoint(
                eq(dsId), any(), eq(lon), eq(lan), anyInt(), any())).thenReturn(List.of(foundedRegionId));

        //when
        boolean isGeoValid = orderAddressValidator.isGeoValid(lon, lan, dsId);

        //then
        assertTrue(isGeoValid);
    }

    @Test
    void testIsEnabled_Success() {
        //given
        long dsId = 1L;
        long scIdForDs = 2L;
        when(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(1L, 2L));
        SortingCenter scForDs = new SortingCenter();
        scForDs.setId(scIdForDs);
        when(sortingCenterService.findSortCenterForDs(dsId))
                .thenReturn(scForDs);

        //when
        boolean isEnabled = orderAddressValidator.isEnabledForDS(dsId);

        //then
        assertTrue(isEnabled);
    }

    @Test
    void testIsEnabled_Fail() {
        //given
        long dsId = 1L;
        long scIdForDs = 2L;
        when(configurationProviderAdapter.getValueAsLongs(ConfigurationProperties.VALIDATE_ADDRESS_SC_IDS))
                .thenReturn(Set.of(3L));
        SortingCenter scForDs = new SortingCenter();
        scForDs.setId(scIdForDs);
        when(sortingCenterService.findSortCenterForDs(dsId))
                .thenReturn(scForDs);

        //when
        boolean isEnabled = orderAddressValidator.isEnabledForDS(dsId);

        //then
        assertFalse(isEnabled);
    }

    private Order buildPickupOrder(Long orderId) {
        Order mockedOrder = mock(Order.class);
        when(mockedOrder.isPickup()).thenReturn(true);
        when(mockedOrder.getId()).thenReturn(orderId);
        return mockedOrder;
    }
}
