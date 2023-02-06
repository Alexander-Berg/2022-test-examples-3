package ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageGeoUtils.getAdGroupRegionsIncludedIntoRestrictedRegions;
import static ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageGeoUtils.getAdGroupRegionsInclusiveRestrictedRegions;
import static ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID;
import static ru.yandex.direct.regions.Region.KYIV_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.TURKEY_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FrontpageGeoUtilsTest {
    @Autowired
    GeoTreeFactory geoTreeFactory;
    GeoTree geoTree;

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
    }

    @Test
    public void getWarningRegions_EmptyList_Test() {
        List<Long> geo = ImmutableList.of(RUSSIA_REGION_ID, -MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
        Set<Long> regionsWithRestrictions = ImmutableSet.of(MOSCOW_REGION_ID);
        Set<Long> result = getAdGroupRegionsInclusiveRestrictedRegions(geoTree, geo, regionsWithRestrictions);
        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    public void getWarningRegions_SingleWarning_Test() {
        List<Long> geo = ImmutableList.of(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
        Set<Long> regionsWithRestrictions =
                ImmutableSet.of(MOSCOW_REGION_ID, SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID);
        Set<Long> result = getAdGroupRegionsInclusiveRestrictedRegions(geoTree, geo, regionsWithRestrictions);
        assertThat(result.size(), equalTo(1));
        assertThat(result.iterator().next(), equalTo(MOSCOW_REGION_ID));
    }

    @Test
    public void getWarningRegions_IgnoreItself_EmptyList_Test() {
        List<Long> geo = ImmutableList.of(RUSSIA_REGION_ID);
        Set<Long> regionsWithRestrictions = ImmutableSet.of(RUSSIA_REGION_ID);
        Set<Long> result = getAdGroupRegionsInclusiveRestrictedRegions(geoTree, geo, regionsWithRestrictions);
        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    public void getErrorRegionsIgnoreMinusRegionTest() {
        List<Long> geo = ImmutableList.of(MOSCOW_REGION_ID, -MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID);
        Set<Long> regionsWithRestrictions = ImmutableSet.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID);
        Map<Long, Long> result = getAdGroupRegionsIncludedIntoRestrictedRegions(geoTree, geo, regionsWithRestrictions);
        assertThat(result.size(), equalTo(1));
        assertThat(result.keySet().iterator().next(), equalTo(MOSCOW_REGION_ID));
        assertThat(result.get(MOSCOW_REGION_ID), equalTo(RUSSIA_REGION_ID));
    }

    @Test
    public void getErrorRegionsMultipleRegionsTest() {
        List<Long> geo = ImmutableList.of(MOSCOW_REGION_ID, KYIV_REGION_ID, KAZAKHSTAN_REGION_ID, TURKEY_REGION_ID);
        Set<Long> regionsWithRestrictions =
                ImmutableSet.of(RUSSIA_REGION_ID, UKRAINE_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                        KAZAKHSTAN_REGION_ID);
        Map<Long, Long> result = getAdGroupRegionsIncludedIntoRestrictedRegions(geoTree, geo, regionsWithRestrictions);
        assertThat(result.size(), equalTo(3));
        assertThat(new ArrayList<>(result.keySet()), containsInAnyOrder(equalTo(MOSCOW_REGION_ID),
                equalTo(KYIV_REGION_ID), equalTo(KAZAKHSTAN_REGION_ID)));
        assertThat(result.get(MOSCOW_REGION_ID), equalTo(MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID));
        assertThat(result.get(KYIV_REGION_ID), equalTo(UKRAINE_REGION_ID));
        assertThat(result.get(KAZAKHSTAN_REGION_ID), equalTo(KAZAKHSTAN_REGION_ID));
    }
}
