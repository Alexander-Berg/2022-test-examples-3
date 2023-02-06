package ru.yandex.market.tpl.core.domain.usershift.location;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.ds.DeliveryServiceRegionRepository;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.region.TplRegionService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.service.user.UserDtoMapper;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.tpl.core.domain.order.address.AddressGenerator.AddressGenerateParam.DEFAULT_REGION_ID;

public class DeliveryRegionInfoServiceUnitTest {

    public static final long SC_WITH_EMPTY_COORDS_ID = 111L;
    public static final long SC_WITH_EMPTY_DS = 777L;

    private final SortingCenterService sortingCenterService = mock(SortingCenterService.class);
    private final Clock clock = mock(Clock.class);
    private final TplRegionService tplRegionService = mock(TplRegionService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);

    private final DeliveryRegionInfoService deliveryRegionInfoService =
            new DeliveryRegionInfoService(
                    clock,
                    mock(RegionBordersService.class),
                    mock(DsRepository.class),
                    mock(UserRepository.class),
                    mock(UserDtoMapper.class),
                    mock(MovementRepository.class),
                    mock(ConfigurationProviderAdapter.class),
                    tplRegionService,
                    mock(DeliveryServiceRegionRepository.class),
                    sortingCenterService,
                    mock(OrderRepository.class));

    @Test
    void filterRegionsByDetailing() {
        var root = new Region(1, "1", RegionType.SETTLEMENT, null);
        var nonCitiesSubTree = new Region(2, "2", RegionType.VILLAGE, root);
        var nonCitiesSubTreeChild = new Region(3, "3", RegionType.VILLAGE, nonCitiesSubTree);
        var withCitiesSubTree = new Region(4, "4", RegionType.CITY, root);
        var withCitiesSubTreeChild1 = new Region(5, "5", RegionType.CITY_DISTRICT, withCitiesSubTree);
        var withCitiesSubTreeChild2 = new Region(6, "6", RegionType.CITY_DISTRICT, withCitiesSubTree);
        var withOnlyStationTreeChild = new Region(7, "7", RegionType.CITY, root);
        var station = new Region(8, "8", RegionType.METRO_STATION, withOnlyStationTreeChild);
        var secondaryTypeDistrict = new Region(9, "9", RegionType.SECONDARY_DISTRICT, withCitiesSubTreeChild1);


        Set<Region> res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withOnlyStationTreeChild),
                DetailingEnum.SECONDARY_DISTRICT_REGION_VILLAGE, Set.of(7));
        assertEquals(res.size(), 1);

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withOnlyStationTreeChild),
                DetailingEnum.CITY_DISTRICT_RURAL, Set.of(7));
        assertEquals(res.size(), 1);
        assertEquals(res.stream().findFirst().get().getId(), withOnlyStationTreeChild.getId());

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withCitiesSubTree),
                DetailingEnum.CITY_DISTRICT_RURAL, Set.of(5, 7, 9));
        assertEquals(res.size(), 1);
        assertEquals(res.stream().findFirst().get().getId(), withCitiesSubTreeChild1.getId());

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withCitiesSubTree),
                DetailingEnum.CITY_FEDERATION_DISTRICT, Set.of(5, 7, 9));
        assertEquals(res.size(), 1);
        assertEquals(res.stream().findFirst().get().getId(), withCitiesSubTree.getId());

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(nonCitiesSubTree),
                DetailingEnum.CITY_DISTRICT_RURAL, Set.of(2, 5, 7, 9));
        assertEquals(res.size(), 1);
        assertEquals(res.stream().findFirst().get().getId(), nonCitiesSubTreeChild.getId());

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withCitiesSubTree),
                DetailingEnum.CITY_DISTRICT_RURAL, Set.of(2, 5, 7, 9));
        assertEquals(res.size(), 1);
        assertEquals(res.stream().findFirst().get().getId(), withCitiesSubTreeChild1.getId());

        res = deliveryRegionInfoService.filterRegionsByDetailing(List.of(withCitiesSubTree),
                DetailingEnum.CITY_DISTRICT_RURAL, Set.of(2, 5, 6, 7, 9));
        assertEquals(res.size(), 2);
        assertEquals(res.contains(withCitiesSubTreeChild1), true);
        assertEquals(res.contains(withCitiesSubTreeChild2), true);
    }

    @Test
    void throwsException_whenScHasNullCoordinates() {
        //given
        SortingCenter sc = new SortingCenter();
        sc.setId(SC_WITH_EMPTY_COORDS_ID);
        doReturn(sc).when(sortingCenterService).findById(eq(SC_WITH_EMPTY_COORDS_ID));

        doReturn(Collections.emptyList()).when(sortingCenterService).findDsForSortingCenter(eq(SC_WITH_EMPTY_COORDS_ID));
        RegionTree mockedTree = mock(RegionTree.class);
        doReturn(mockedTree).when(tplRegionService).getOrBuildRegionTree();
        Region mockedRegion = mock(Region.class);
        doReturn(mockedRegion).when(mockedTree).getRegion(anyInt());
        doReturn(RegionType.CITY).when(mockedRegion).getType();

        ClockUtil.initFixed(clock);

        //then
        assertThrows(TplInvalidParameterException.class,
                () -> deliveryRegionInfoService.getRegionLayers(DetailingEnum.SECONDARY_DISTRICT_REGION_VILLAGE,
                        LocalDate.now(), LocalDate.now(), SC_WITH_EMPTY_COORDS_ID));
    }

    @Test
    void throwsException_whenScHasEmptyDs() {
        //given
        doReturn(Collections.emptyList()).when(sortingCenterService).findDsForSortingCenter(eq(SC_WITH_EMPTY_DS));

        //then
        assertThrows(TplInvalidParameterException.class,
                () -> deliveryRegionInfoService.getRegionInfoWithStatistics(DEFAULT_REGION_ID, null,
                        null, SC_WITH_EMPTY_DS));
    }
}
