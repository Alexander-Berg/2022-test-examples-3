package ru.yandex.direct.regions;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.regions.utils.TestGeoTrees;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.REGION_TYPE_COUNTRY;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

public class GeoTreeTest {
    private static final CompareStrategy REGION_COMPARE = allFieldsExcept(newPath("parent"));

    private static final long RYAZAN_REGION_ID = 11L;
    private static final long MINSK_REGION_ID = 29630L;
    private static final long MOSCOW_VDNH_METRO_ID = 20384L;
    private static final long SAINT_PETERBURG_NARVSKAY_METRO_ID = 20304L;

    private static GeoTree geoTree;

    @BeforeClass
    public static void beforeClass() {
        geoTree = TestGeoTrees.loadGlobalTree();
    }

    @Test
    public void get_RussiaRegionId_ReturnsRussiaRegionObject() {
        Region russiaRegion = new Region(225L, 3, "Россия", "Russia", "Росія", "Rusya", false);

        Region region = geoTree.getRegion(RUSSIA_REGION_ID);
        assertThat(region, beanDiffer(russiaRegion).useCompareStrategy(REGION_COMPARE));
    }

    @Test
    public void hasRegion_RegionIdPresents_ReturnsTrue() {
        assertThat(geoTree.hasRegion(RYAZAN_REGION_ID), is(true));
    }

    @Test
    public void hasRegion_ContainsMoscow() {
        assertThat(geoTree.hasRegion(MOSCOW_REGION_ID), equalTo(true));
    }

    @Test
    public void hasRegion_RegionIdDoesNotPresent_ReturnsFalse() {
        assertThat(geoTree.hasRegion(666L), is(false));
    }

    @Test
    public void hasMetro_IdPresents_ReturnsTrue() {
        assertThat(geoTree.hasMetro(MOSCOW_VDNH_METRO_ID), is(true));
    }

    @Test
    public void hasMetro_RegionIdDoesNotPresent_ReturnsFalse() {
        assertThat(geoTree.hasMetro(666L), is(false));
    }

    @Test
    public void isRegionIncludedIn_SubregionIncludedInRegion_ReturnsTrue() {
        assertThat(geoTree.isRegionIncludedIn(RYAZAN_REGION_ID, RUSSIA_REGION_ID), is(true));
    }

    @Test
    public void isRegionIncludedIn_SubregionNotIncludedInRegion_ReturnsFalse() {
        assertThat(geoTree.isRegionIncludedIn(MINSK_REGION_ID, RUSSIA_REGION_ID), is(false));
    }

    @Test
    public void upRegionTo_RyazanRegionToRussia_ReturnRussiaRegionId() {
        long uppedRegionId =
                geoTree.upRegionTo(RYAZAN_REGION_ID, singleton(RUSSIA_REGION_ID));
        assertThat(uppedRegionId, is(RUSSIA_REGION_ID));
    }

    @Test
    public void upRegionTo_RyazanRegionToTurkey_ReturnGlobalRegionId() {
        long uppedRegionId =
                geoTree.upRegionTo(RYAZAN_REGION_ID, singleton(TURKEY_REGION_ID));
        assertThat(uppedRegionId, is(GLOBAL_REGION_ID));
    }

    @Test
    public void upRegionToType_RyazanRegionToCountry_ReturnRussiaRegionId() {
        long uppedRegionId = geoTree.upRegionToType(RYAZAN_REGION_ID, REGION_TYPE_COUNTRY);
        assertThat(uppedRegionId, is(RUSSIA_REGION_ID));
    }

    @Test
    public void upRegionToType_GlobalRegionToCountry_ReturnGlobalRegionId() {
        long uppedRegionId = geoTree.upRegionToType(GLOBAL_REGION_ID, REGION_TYPE_COUNTRY);
        assertThat(uppedRegionId, is(GLOBAL_REGION_ID));
    }

    @Test
    public void isCityHasMetro_InvalidMetroId() {
        Region moscow = geoTree.getRegion(MOSCOW_REGION_ID);

        assertThat(geoTree.isCityHasMetro(moscow.getNameRu(), -1L), equalTo(false));
    }

    @Test
    public void isCityHasMetro_ValidCityAndMetroId() {
        Region moscow = geoTree.getRegion(MOSCOW_REGION_ID);

        assertThat(geoTree.isCityHasMetro(moscow.getNameRu(), MOSCOW_VDNH_METRO_ID), equalTo(true));
    }

    @Test
    public void isCityHasMetro_NotValidCityAndMetroId() {
        Region moscow = geoTree.getRegion(MOSCOW_REGION_ID);

        assertThat(geoTree.isCityHasMetro(moscow.getNameRu(), SAINT_PETERBURG_NARVSKAY_METRO_ID),
                equalTo(false));
    }

    @Test
    public void getModerationCountries_getCountry() {
        assertEquals(singleton(RUSSIA_REGION_ID),
                geoTree.getModerationCountries(singleton(Region.MOSCOW_REGION_ID)));
    }

    @Test
    public void getModerationCountries_getCrimeaApi() {
        GeoTree geoTree = TestGeoTrees.loadApiTree();
        assertEquals(geoTree.getRegions().values()
                        .stream()
                        .filter(r -> r.getType() == Region.REGION_TYPE_COUNTRY)
                        .map(Region::getId)
                        .collect(Collectors.toSet()),
                geoTree.getModerationCountries(singleton(Region.CRIMEA_REGION_ID)));
    }

    @Test
    public void getModerationCountries_getCis() {
        assertEquals(ImmutableSet.of(UKRAINE_REGION_ID, BY_REGION_ID, KAZAKHSTAN_REGION_ID),
                geoTree.getModerationCountries(singleton(Region.SNG_REGION_ID)));
    }

    @Test
    public void getModerationCountries_getWorld() {
        assertEquals(geoTree.getRegions().values()
                        .stream()
                        .filter(r -> r.getType() == Region.REGION_TYPE_COUNTRY)
                        .map(Region::getId)
                        .collect(Collectors.toSet()),
                geoTree.getModerationCountries(singleton(Region.GLOBAL_REGION_ID)));
    }

    @Test
    public void getModerationCountries_ignoreMinusGeo() {
        assertEquals(singleton(RUSSIA_REGION_ID),
                geoTree.getModerationCountries(Arrays.asList(Region.MOSCOW_REGION_ID, -Region.TURKEY_REGION_ID)));
    }

    @Test
    public void getModerationCountries_getEmpty() {
        assertEquals(emptySet(), geoTree.getModerationCountries(emptySet()));
    }

    @Test
    public void shouldReturnChildrenOfRegion() {
        var geoTree = TestGeoTrees.loadApiTree();

        var children = geoTree.getChildren(166);

        assertTrue(children.contains(187L));
    }
}
