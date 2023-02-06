package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class GeoTreeIsAnyRegionIncludedInTest {
    private static GeoTree geoTree;

    @Parameterized.Parameter
    public Collection<Long> regionIds;
    @Parameterized.Parameter(value = 1)
    public Set<Long> topRegionIds;
    @Parameterized.Parameter(value = 2)
    public boolean expectedResult;

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{emptyList(), emptySet(), true},
                new Object[]{emptyList(), singleton(Region.RUSSIA_REGION_ID), true},
                new Object[]{singletonList(Region.RUSSIA_REGION_ID), emptySet(), false},
                new Object[]{singletonList(-Region.RUSSIA_REGION_ID), emptySet(), true},
                new Object[]{
                        Lists.newArrayList(Region.MOSCOW_REGION_ID, Region.BY_REGION_ID),
                        singleton(Region.RUSSIA_REGION_ID),
                        true},
                new Object[]{
                        Lists.newArrayList(Region.MOSCOW_REGION_ID, Region.CRIMEA_REGION_ID),
                        singleton(Region.BY_REGION_ID),
                        false});
    }

    @BeforeClass
    public static void setUpClass() {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    @Test
    public void test() {
        boolean actualResult = geoTree.isAnyRegionIncludedIn(regionIds, topRegionIds);

        assertThat("Неправильное включение в регионы", actualResult, equalTo(expectedResult));
    }
}
