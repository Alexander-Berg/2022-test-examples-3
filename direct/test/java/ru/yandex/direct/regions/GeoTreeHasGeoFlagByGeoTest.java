package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class GeoTreeHasGeoFlagByGeoTest {
    private static final Long REGION0_ID = 0L;
    private static final Long REGION1_WITH_GEOFLAG_ID = 1L;
    private static final Long REGION2_WITH_GEOFLAG_ID = 2L;
    private static final Long REGION3_WITHOUT_GEOFLAG_ID = 3L;

    private static GeoTree geoTree;

    @Parameterized.Parameter
    public Collection<Long> geo;

    @Parameterized.Parameter(1)
    public boolean expectedGeoFlag;

    private static Region createTestRegion(Long id, boolean geoFlag) {
        String name = "Reg" + id.toString();
        Region region = new Region(
                id, 0, name, name, name, name, geoFlag);
        region.setParent(region);
        return region;
    }

    @BeforeClass
    public static void setUpClass() {
        geoTree = new GeoTree(
                ImmutableMap.of(
                        REGION0_ID,
                        createTestRegion(REGION0_ID, false),
                        REGION1_WITH_GEOFLAG_ID,
                        createTestRegion(REGION1_WITH_GEOFLAG_ID, true),
                        REGION2_WITH_GEOFLAG_ID,
                        createTestRegion(REGION2_WITH_GEOFLAG_ID, true),
                        REGION3_WITHOUT_GEOFLAG_ID,
                        createTestRegion(REGION3_WITHOUT_GEOFLAG_ID, false)),
                emptyMap(),
                GeoTreeType.GLOBAL);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{null, false},
                new Object[]{emptyList(), false},
                new Object[]{singletonList(REGION1_WITH_GEOFLAG_ID), true},
                new Object[]{singletonList(-REGION1_WITH_GEOFLAG_ID), false},
                new Object[]{Lists.newArrayList(REGION1_WITH_GEOFLAG_ID, REGION2_WITH_GEOFLAG_ID), true},
                new Object[]{Lists.newArrayList(REGION1_WITH_GEOFLAG_ID, REGION3_WITHOUT_GEOFLAG_ID), false},
                new Object[]{Lists.newArrayList(REGION1_WITH_GEOFLAG_ID, -REGION3_WITHOUT_GEOFLAG_ID), true});
    }

    @Test
    public void test() {
        boolean actualGeoFlag = geoTree.hasGeoFlagByGeo(geo);
        assertThat(
                "Ожидаемый geoflag не совпадает с фактическим",
                actualGeoFlag,
                equalTo(expectedGeoFlag));
    }
}
