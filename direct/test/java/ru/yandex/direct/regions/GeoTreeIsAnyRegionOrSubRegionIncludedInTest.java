package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class GeoTreeIsAnyRegionOrSubRegionIncludedInTest {
    private static GeoTree geoTree;

    @Parameterized.Parameter
    public Set<Long> regionIds;
    @Parameterized.Parameter(value = 1)
    public Set<Long> topRegionIds;
    @Parameterized.Parameter(value = 2)
    public boolean expectedResult;

    @Parameterized.Parameters(name = "{0} - {1} - {2}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{emptySet(), emptySet(), true},
                new Object[]{emptySet(), singleton(Region.RUSSIA_REGION_ID), true},
                new Object[]{
                        singleton(Region.MOSCOW_REGION_ID),
                        emptySet(),
                        true},
                new Object[]{
                        singleton(Region.MOSCOW_REGION_ID),
                        new LinkedHashSet<>(Arrays.asList(Region.RUSSIA_REGION_ID, -Region.CRIMEA_REGION_ID)),
                        true},
                new Object[]{
                        singleton(Region.MOSCOW_REGION_ID),
                        new LinkedHashSet<>(
                                Arrays.asList(Region.RUSSIA_REGION_ID, -Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)),
                        false},
                new Object[]{
                        new LinkedHashSet<>(Arrays.asList(Region.MOSCOW_REGION_ID, Region.UKRAINE_REGION_ID)),
                        new LinkedHashSet<>(
                                Arrays.asList(Region.GLOBAL_REGION_ID, -Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID)),
                        true});
    }

    @BeforeClass
    public static void setUpClass() {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    @Test
    public void test() {
        boolean actualResult = geoTree.isAnyRegionOrSubRegionIncludedIn(regionIds, topRegionIds);

        assertThat("Неправильное включение в регионы", actualResult, equalTo(expectedResult));
    }
}
