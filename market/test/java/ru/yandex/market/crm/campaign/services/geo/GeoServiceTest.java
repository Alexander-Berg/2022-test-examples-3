package ru.yandex.market.crm.campaign.services.geo;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.services.geo.GeoService;
import ru.yandex.market.crm.core.services.geo.domain.GeoData;
import ru.yandex.market.crm.core.services.geo.domain.GeoRegion;
import ru.yandex.market.crm.core.suppliers.GeoDataSupplier;
import ru.yandex.market.crm.external.contentapi.ContentApiClient;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GeoServiceTest {

    @Mock
    private GeoDataSupplier geoDataSupplier;

    @Mock
    private ContentApiClient contentApiClient;

    private GeoRegion russia;
    private GeoRegion kazakhstan;
    private GeoRegion uralFO;
    private GeoRegion sverdlObl;
    private GeoRegion ekb;

    private GeoService geoService;


    @Before
    public void before() {
        Int2ObjectMap<GeoRegion> regionIndex = new Int2ObjectOpenHashMap<>();

        russia = new GeoRegion(225, -1, "", "");
        regionIndex.put(russia.getId(), russia);

        kazakhstan = new GeoRegion(159, -1, "", "");
        regionIndex.put(kazakhstan.getId(), kazakhstan);

        uralFO = new GeoRegion(52, russia.getId(), "", "");
        regionIndex.put(uralFO.getId(), uralFO);

        sverdlObl = new GeoRegion(11162, uralFO.getId(), "", "");

        ekb = new GeoRegion(54, sverdlObl.getId(), "", "");

        Int2ObjectMap<IntList> children = new Int2ObjectOpenHashMap<>();
        children.put(russia.getId(), new IntArrayList(singletonList(uralFO.getId())));
        children.put(uralFO.getId(), new IntArrayList(singletonList(sverdlObl.getId())));
        children.put(sverdlObl.getId(), new IntArrayList(singletonList(ekb.getId())));

        GeoData geoData = new GeoData(regionIndex, children);

        when(geoDataSupplier.get()).thenReturn(geoData);
        geoService = new GeoService(geoDataSupplier, contentApiClient);
    }

    @Test
    public void isRegionInRegionTest() {
        assertTrue(geoService.isRegionInRegion(ekb.getId(), ekb.getId()));
        assertTrue(geoService.isRegionInRegion(ekb.getId(), sverdlObl.getId()));
        assertTrue(geoService.isRegionInRegion(ekb.getId(), uralFO.getId()));
        assertTrue(geoService.isRegionInRegion(ekb.getId(), russia.getId()));

        assertFalse(geoService.isRegionInRegion(ekb.getId(), kazakhstan.getId()));
        assertFalse(geoService.isRegionInRegion(russia.getId(), kazakhstan.getId()));
        assertFalse(geoService.isRegionInRegion(uralFO.getId(), sverdlObl.getId()));
    }
}
