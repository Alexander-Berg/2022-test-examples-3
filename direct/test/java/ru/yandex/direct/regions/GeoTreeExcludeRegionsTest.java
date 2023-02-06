package ru.yandex.direct.regions;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@RunWith(Parameterized.class)
public class GeoTreeExcludeRegionsTest {
    private static final long RUSSIA = RUSSIA_REGION_ID;
    private static final long SIBERIA = 59L;
    private static final long MOSCOW = MOSCOW_REGION_ID;
    private static final long MOSCOW_AND_MOSCOW_REGION = MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
    private static final long SAINT_PETERSBURG = 2L;
    private static final long CENTRAL_FEDERAL_DISTRICT = 3L;
    private static final long NORTHWESTERN_FEDERAL_DISTRICT = 17L;
    private static final long SAINT_PETERSBURG_AND_LENINGRAD_REGION = 10174L;
    private static final long AFRICA = 241L;
    private static final long UKRAINE = UKRAINE_REGION_ID;
    private static final long KIEV = 143L;
    private static final long CIS = 166L;
    private static final long CRIMEA = CRIMEA_REGION_ID;

    @Parameterized.Parameter
    public GeoTree geoTree;

    @Parameterized.Parameter(1)
    public List<Long> geoTargeting;

    @Parameterized.Parameter(2)
    public List<Long> regionsToExclude;

    @Parameterized.Parameter(3)
    public List<Long> effectiveGeo;

    @Parameterized.Parameters(name = "geoTree, geoTargeting = {1}, regionsToExclude = {2}, effectiveGeo = {3}")
    public static Collection getParameters() throws IOException {
        GeoTree globalGeoTree = TestGeoTrees.loadGlobalTree();
        GeoTree apiGeoTree = TestGeoTrees.loadApiTree();
        GeoTree ruGeoTree = TestGeoTrees.loadRussianTree();

        return asList(new Object[][]{
                {globalGeoTree, singletonList(GLOBAL_REGION_ID), emptyList(), singletonList(GLOBAL_REGION_ID)},
                {globalGeoTree, asList(MOSCOW, KIEV), singletonList(UKRAINE), singletonList(MOSCOW)},
                {globalGeoTree, asList(MOSCOW, KIEV), asList(RUSSIA, UKRAINE), emptyList()},
                {globalGeoTree, asList(RUSSIA, UKRAINE), singletonList(RUSSIA), singletonList(UKRAINE)},
                {globalGeoTree, singletonList(CIS), singletonList(UKRAINE), asList(CIS, -UKRAINE)},
                {globalGeoTree, singletonList(RUSSIA), singletonList(MOSCOW), asList(RUSSIA, -MOSCOW)},
                {globalGeoTree, singletonList(RUSSIA), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(RUSSIA, -MOSCOW, -SAINT_PETERSBURG)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT), singletonList(MOSCOW),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG)},
                {globalGeoTree, emptyList(), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(GLOBAL_REGION_ID, -MOSCOW, -SAINT_PETERSBURG)},
                {globalGeoTree, singletonList(SIBERIA), asList(MOSCOW, SAINT_PETERSBURG), singletonList(SIBERIA)},
                {globalGeoTree, singletonList(CENTRAL_FEDERAL_DISTRICT), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(CENTRAL_FEDERAL_DISTRICT, -MOSCOW)},
                {globalGeoTree, singletonList(NORTHWESTERN_FEDERAL_DISTRICT), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW, SAINT_PETERSBURG),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG)},
                {globalGeoTree, singletonList(MOSCOW_AND_MOSCOW_REGION), asList(MOSCOW, SAINT_PETERSBURG),
                        asList(MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},
                {globalGeoTree, singletonList(RUSSIA), singletonList(MOSCOW_AND_MOSCOW_REGION),
                        asList(RUSSIA, -MOSCOW_AND_MOSCOW_REGION)},
                {globalGeoTree, singletonList(RUSSIA),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -MOSCOW_AND_MOSCOW_REGION, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT), singletonList(MOSCOW_AND_MOSCOW_REGION),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, emptyList(), asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(GLOBAL_REGION_ID, -MOSCOW_AND_MOSCOW_REGION, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, singletonList(SIBERIA),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        singletonList(SIBERIA)},
                {globalGeoTree, singletonList(NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, NORTHWESTERN_FEDERAL_DISTRICT,
                                -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, singletonList(RUSSIA), asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -MOSCOW, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT),
                        asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, emptyList(), asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(GLOBAL_REGION_ID, -MOSCOW, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, singletonList(SIBERIA), asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        singletonList(SIBERIA)},
                {globalGeoTree, singletonList(CENTRAL_FEDERAL_DISTRICT),
                        asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(CENTRAL_FEDERAL_DISTRICT, -MOSCOW)},
                {globalGeoTree, singletonList(NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(NORTHWESTERN_FEDERAL_DISTRICT, -SAINT_PETERSBURG_AND_LENINGRAD_REGION)},
                {globalGeoTree, singletonList(MOSCOW_AND_MOSCOW_REGION),
                        asList(MOSCOW, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(MOSCOW_AND_MOSCOW_REGION, -MOSCOW)},
                {globalGeoTree, asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT)},
                {globalGeoTree, asList(AFRICA, RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(AFRICA, RUSSIA, -CENTRAL_FEDERAL_DISTRICT, -NORTHWESTERN_FEDERAL_DISTRICT)},
                {globalGeoTree, singletonList(AFRICA),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION), singletonList(AFRICA)},
                {globalGeoTree, asList(RUSSIA, MOSCOW, CENTRAL_FEDERAL_DISTRICT),
                        asList(MOSCOW_AND_MOSCOW_REGION, SAINT_PETERSBURG_AND_LENINGRAD_REGION),
                        asList(RUSSIA, -SAINT_PETERSBURG_AND_LENINGRAD_REGION, CENTRAL_FEDERAL_DISTRICT,
                                -MOSCOW_AND_MOSCOW_REGION)},
                // translocality
                {globalGeoTree, singletonList(RUSSIA), singletonList(CRIMEA), singletonList(RUSSIA)},
                {apiGeoTree, singletonList(RUSSIA), singletonList(CRIMEA), singletonList(RUSSIA)},
                {ruGeoTree, singletonList(RUSSIA), singletonList(CRIMEA), asList(RUSSIA, -CRIMEA)},
                {globalGeoTree, singletonList(UKRAINE), singletonList(CRIMEA), asList(UKRAINE, -CRIMEA)},
                {apiGeoTree, singletonList(UKRAINE), singletonList(CRIMEA), singletonList(UKRAINE)},
                {ruGeoTree, singletonList(UKRAINE), singletonList(CRIMEA), singletonList(UKRAINE)},
        });
    }

    @Test
    public void test() {
        assertThat(geoTree.excludeRegions(geoTargeting, regionsToExclude), equalTo(effectiveGeo));
    }
}
