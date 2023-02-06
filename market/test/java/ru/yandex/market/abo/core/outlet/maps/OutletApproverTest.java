package ru.yandex.market.abo.core.outlet.maps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.common.util.region.CustomRegionAttribute;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.abo.core.outlet.maps.model.MapsOutlet;
import ru.yandex.market.abo.core.outlet.maps.model.OutletTolokaDetails;
import ru.yandex.market.abo.core.outlet.model.OutletCheck;
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckStatus;
import ru.yandex.market.abo.core.outlet.model.status.OutletCheckSubStatus;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.CoordinatesDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.GeoInfoDTO;
import ru.yandex.market.mbi.api.client.entity.pagedOutletsDTO.OutletInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 26.04.18.
 */
public class OutletApproverTest {
    private static final long ABO_ID = 0;
    private static final long MBI_ID = 1L;
    private static final long MAPS_ID = 2L;

    private static final int REGION_MSK = Regions.MOSCOW;

    private static final OutletType OUTLET_TYPE = OutletType.MIXED;
    private static final OutletStatus OUTLET_STATUS = OutletStatus.AT_MODERATION;
    private static final String OUTLET_NAME = "meow";

    private static final double LATITUDE = 11;
    private static final double LONGITUDE = 1;

    private static final Map<Long, String> RUBRICS_MAP = new HashMap<Long, String>() {{
        put(1L, "Ready Player One");
        put(2L, "Ready Player Two");
    }};

    @InjectMocks
    private OutletApprover outletApprover;
    @Mock
    private OutletYtService outletYtService;
    @Mock
    private RegionService regionService;
    @Mock
    private RegionTree regionTree;
    @Mock
    private Region region;
    @Mock
    private OutletParamsComparator paramsComparator;
    @Mock
    private OutletInfoDTO mbiOutletDTO;
    @Mock
    private OutletInfo mbiOutlet;
    @Mock
    private MapsOutlet mapsOutlet;
    @Mock
    private OutletCheck outletCheck;
    @Mock
    private GeoInfo geoInfo;
    @Mock
    private Coordinates coordinates;
    @Mock
    private ShopInfoService shopInfoService;

    private List<OutletCheck> aboChecks = new ArrayList<>();
    private List<OutletInfoDTO> mbiOutlets = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        Stream.of(aboChecks, mbiOutlets).forEach(List::clear);
        aboChecks.add(outletCheck);
        mbiOutlets.add(mbiOutletDTO);

        when(mbiOutletDTO.toOutletInfo()).thenReturn(mbiOutlet);
        when(mbiOutletDTO.getId()).thenReturn(MBI_ID);
        when(mbiOutletDTO.getGeoInfo()).thenReturn(new GeoInfoDTO(new CoordinatesDTO(LONGITUDE, LATITUDE), (long) REGION_MSK));
        when(mbiOutlet.getId()).thenReturn(MBI_ID);
        when(mbiOutlet.getAddress()).thenReturn(new Address.Builder().setCity("msk").build());
        when(mbiOutlet.getType()).thenReturn(OUTLET_TYPE);
        when(mbiOutlet.getStatus()).thenReturn(OUTLET_STATUS);
        when(mbiOutlet.getName()).thenReturn(OUTLET_NAME);

        when(mbiOutlet.getPhones()).thenReturn(
                List.of(new PhoneNumber("Russia", "Moscow", "81234567890", "", "", PhoneType.PHONE))
        );
        when(mbiOutlet.getSchedule()).thenReturn(
                new Schedule(1, List.of(new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 5, 0, 30)))
        );

        when(mbiOutlet.getGeoInfo()).thenReturn(geoInfo);
        when(geoInfo.getRegionId()).thenReturn((long) REGION_MSK);
        when(geoInfo.getGpsCoordinates()).thenReturn(coordinates);
        when(coordinates.getLat()).thenReturn(LATITUDE);
        when(coordinates.getLon()).thenReturn(LONGITUDE);

        when(regionService.getRegionTree()).thenReturn(regionTree);
        when(regionTree.getRegion(REGION_MSK)).thenReturn(region);
        when(region.getCustomAttributeValue(CustomRegionAttribute.TIMEZONE_OFFSET)).thenReturn("3");

        when(outletYtService.fetchMapsOutletsFromYT(anyCollection()))
                .thenReturn(Collections.singletonList(mapsOutlet));
        when(mapsOutlet.getMbiId()).thenReturn(MBI_ID);
        when(mapsOutlet.getPermalink()).thenReturn(MAPS_ID);
        when(mapsOutlet.getRubrics()).thenReturn(RUBRICS_MAP);

        when(outletCheck.getMbiOutletId()).thenReturn(MBI_ID);
        when(outletCheck.getId()).thenReturn(ABO_ID);

        when(shopInfoService.getShopInfo(anyLong())).thenReturn(new ShopInfo());
    }

    @Test
    public void approveOutletsPositive() {
        when(paramsComparator.outletsEqual(any(), any())).thenReturn(true);
        var tolokaChecks = outletApprover.approveOutlets(aboChecks, mbiOutlets);
        verify(outletCheck).setStatus(OutletCheckStatus.APPROVED);
        verify(outletCheck).setSubStatus(OutletCheckSubStatus.SPRAV_DB_FOUND_SAME);
        assertTrue(tolokaChecks.isEmpty());
    }

    @Test
    public void approveOutletsNegative() {
        when(paramsComparator.outletsEqual(any(), any())).thenReturn(false);

        var tolokaChecks = outletApprover.approveOutlets(aboChecks, mbiOutlets);
        verify(outletCheck).setStatus(OutletCheckStatus.NEED_TOLOKA_CHECK);
        assertEquals(1, tolokaChecks.size());

        var tolokaCheck = tolokaChecks.get(0);
        assertEquals(ABO_ID, tolokaCheck.getId());
        assertEquals("RU", tolokaCheck.getCountry());
        assertEquals("RU", tolokaCheck.getLang());

        OutletTolokaDetails checkDetails = tolokaCheck.getCheckDetails();
        assertEquals(MAPS_ID, checkDetails.getPermaLink());
        assertEquals(OUTLET_NAME, checkDetails.getOutletName());
        assertEquals(LATITUDE + "," + LONGITUDE, checkDetails.getGpsCoordinates());
        assertNotNull(checkDetails.getAddress());
        assertEquals("1;2", checkDetails.getRubricIdsCsv());
        assertEquals("Ready Player One;Ready Player Two", checkDetails.getRubricNamesCsv());
    }

    @Test
    public void notFoundInMbi() {
        var tolokaChecks = outletApprover.approveOutlets(aboChecks, Collections.emptyList());
        verify(outletCheck).setStatus(OutletCheckStatus.CANCELED);
        assertTrue(tolokaChecks.isEmpty());
    }

    @Test
    public void notFoundInMaps() {
        when(outletYtService.fetchMapsOutletsFromYT(anyCollection())).thenReturn(Collections.emptyList());
        var tolokaChecks = outletApprover.approveOutlets(aboChecks, mbiOutlets);
        verify(outletCheck).setStatus(OutletCheckStatus.NOT_FOUND);
        assertTrue(tolokaChecks.isEmpty());
    }
}
