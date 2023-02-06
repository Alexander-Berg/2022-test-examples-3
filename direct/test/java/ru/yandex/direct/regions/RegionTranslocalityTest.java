package ru.yandex.direct.regions;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@RunWith(Parameterized.class)
public class RegionTranslocalityTest {
    private static final long RYAZAN_REGION_ID = 11L;
    private static final long RYAZAN_PROVINCE_REGION_ID = 10776L;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
// TODO сделать эти тесты там, где есть сконфигурированный реальными данными GeoTreeFactory в spring-контексте
//                {"Крым принадлежит России (российский клиент)", CRIMEA_REGION_ID, RUSSIA_REGION_ID, RUSSIA_REGION_ID},
//
//                {"Крым принадлежит Украине (украинский клиент)", CRIMEA_REGION_ID, UKRAINE_REGION_ID,
//                        UKRAINE_REGION_ID},
//                {"Крым принадлежит Украине (крымский клиент)", CRIMEA_REGION_ID, UKRAINE_REGION_ID, CRIMEA_REGION_ID},
//                {"Крым принадлежит Украине (др. клиенты, не из России)", CRIMEA_REGION_ID, UKRAINE_REGION_ID,
//                        GLOBAL_REGION_ID},

                {"Рязань принадлежит Рязанской области (российский клиент)", RYAZAN_REGION_ID,
                        RYAZAN_PROVINCE_REGION_ID, RUSSIA_REGION_ID},
                {"Рязань принадлежит Рязанской области (др. клиент)", RYAZAN_REGION_ID, RYAZAN_PROVINCE_REGION_ID,
                        GLOBAL_REGION_ID}
        });
    }

    @BeforeClass
    public static void beforeClass() throws IOException {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    private static GeoTree geoTree;

    private long regionId;
    private long parentRegionId;
    private long clientCountryId;

    public RegionTranslocalityTest(String testName, long regionId, long parentRegionId, long clientCountryId) {
        this.regionId = regionId;
        this.parentRegionId = parentRegionId;
        this.clientCountryId = clientCountryId;
    }

    @Test
    public void testParametrized() {
        Region region = geoTree.getRegion(regionId);

        Region parent = region.getParent();
        assertThat(parent.getId(), is(parentRegionId));
    }
}
