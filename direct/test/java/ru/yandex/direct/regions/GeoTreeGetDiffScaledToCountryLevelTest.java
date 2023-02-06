package ru.yandex.direct.regions;

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
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@RunWith(Parameterized.class)
public class GeoTreeGetDiffScaledToCountryLevelTest {

    private static final long RUSSIA = RUSSIA_REGION_ID;
    private static final long MOSCOW = MOSCOW_REGION_ID;
    private static final long UKRAINE = UKRAINE_REGION_ID;
    private static final long BELARUS = BY_REGION_ID;
    private static final long KIEV = 143L;
    private static final long CIS = 166L;
    private static final long CRIMEA = CRIMEA_REGION_ID;
    private static final long KERCH = 11464L;

    @Parameterized.Parameter
    public GeoTree geoTree;

    @Parameterized.Parameter(1)
    public List<Long> firstGeo;

    @Parameterized.Parameter(2)
    public List<Long> secondGeo;

    @Parameterized.Parameter(3)
    public List<Long> diff;

    @Parameterized.Parameters(name = "first geo = {1}, second geo = {2}, diff = {3}")
    public static Collection getParameters() {
        GeoTree globalGeoTree = TestGeoTrees.loadGlobalTree();
        GeoTree apiGeoTree = TestGeoTrees.loadApiTree();
        GeoTree ruGeoTree = TestGeoTrees.loadRussianTree();

        return asList(new Object[][]{
                {globalGeoTree, asList(MOSCOW, KIEV), asList(MOSCOW, -UKRAINE), singletonList(UKRAINE)},
                {globalGeoTree, singletonList(CIS), asList(CIS, -UKRAINE, -BELARUS), asList(UKRAINE, BELARUS)},
                {globalGeoTree, singletonList(RUSSIA), singletonList(RUSSIA), emptyList()},
                {globalGeoTree, asList(RUSSIA, UKRAINE), asList(RUSSIA, -UKRAINE), singletonList(UKRAINE)},
                {globalGeoTree, singletonList(GLOBAL_REGION_ID), singletonList(GLOBAL_REGION_ID), emptyList()},
                {globalGeoTree, asList(RUSSIA, -MOSCOW), emptyList(), singletonList(RUSSIA)},

                // translocality
                {globalGeoTree, asList(RUSSIA, UKRAINE), asList(UKRAINE, -CRIMEA), asList(RUSSIA, UKRAINE)},
                {apiGeoTree, asList(RUSSIA, UKRAINE), asList(UKRAINE, -CRIMEA), asList(RUSSIA, CRIMEA)},
                {ruGeoTree, asList(RUSSIA, UKRAINE), asList(UKRAINE, -CRIMEA), singletonList(RUSSIA)},

                {globalGeoTree, asList(RUSSIA, UKRAINE, CRIMEA), singletonList(UKRAINE), asList(RUSSIA, UKRAINE)},
                {apiGeoTree, asList(RUSSIA, UKRAINE, CRIMEA), singletonList(UKRAINE), asList(RUSSIA, CRIMEA)},
                {ruGeoTree, asList(RUSSIA, UKRAINE, CRIMEA), singletonList(UKRAINE), singletonList(RUSSIA)},

                {globalGeoTree, asList(RUSSIA, UKRAINE, KERCH), singletonList(UKRAINE), asList(RUSSIA, UKRAINE)},
                {apiGeoTree, asList(RUSSIA, UKRAINE, KERCH), singletonList(UKRAINE), asList(RUSSIA, CRIMEA)},
                {ruGeoTree, asList(RUSSIA, UKRAINE, KERCH), singletonList(UKRAINE), singletonList(RUSSIA)},
        });
    }

    @Test
    public void test() {
        assertThat(geoTree.getDiffScaledToCountryLevel(firstGeo, secondGeo), equalTo(diff));
    }
}
