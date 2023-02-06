package ru.yandex.market.checkout.pushapi.service.shop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.pushapi.client.entity.Region;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeoRegionServiceTest {

    private RegionService regionService = mock(RegionService.class);
    private ru.yandex.market.checkout.pushapi.service.GeoRegionService geoRegionService = new ru.yandex.market.checkout.pushapi.service.GeoRegionService();

    @BeforeEach
    public void setUp() throws Exception {
        geoRegionService.setRegionService(regionService);
    }

    @Test
    public void testGetRegion() throws Exception {
        final ru.yandex.common.util.region.Region region3 = new ru.yandex.common.util.region.Region(
                3, "region3", RegionType.CITY,
                new ru.yandex.common.util.region.Region(
                        2, "region2", RegionType.CITY,
                        new ru.yandex.common.util.region.Region(
                                1, "region1", RegionType.CITY, null
                        )
                )
        );

        final RegionTree regionTree = mock(RegionTree.class);
        when(regionService.getRegionTree()).thenReturn(regionTree);
        when(regionTree.getRegion(3)).thenReturn(region3);

        final Region region = geoRegionService.getRegion(3);
        assertEquals(3, region.getId());
        assertEquals("region3", region.getName());
        final Region region2 = region.getParent();
        assertNotNull(region2);
        assertEquals(2, region2.getId());
        assertEquals("region2", region2.getName());
        final Region region1 = region2.getParent();
        assertNotNull(region1);
        assertEquals(1, region1.getId());
        assertEquals("region1", region1.getName());
        assertNull(region1.getParent());
    }
}
