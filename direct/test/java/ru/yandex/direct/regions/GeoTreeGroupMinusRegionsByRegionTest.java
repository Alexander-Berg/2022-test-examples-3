package ru.yandex.direct.regions;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.direct.regions.GeoTree.groupMinusRegionsByRegion;

public class GeoTreeGroupMinusRegionsByRegionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void groupMinusRegionsByRegion_geoTargetingIsEmpty_ReturnEmptyMap() {
        assertThat(groupMinusRegionsByRegion(emptyList()), equalTo(emptyMap()));
    }

    @Test
    public void groupMinusRegionsByRegion_geoTargetingStartsWithMinusRegion_ThrowException() {
        List<Long> geoTargeting = asList(-1L, 2L, -3L, -4L);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(
                "incorrectly formed geoTargeting: " + geoTargeting.toString() + " - minus regions must follow region");

        groupMinusRegionsByRegion(geoTargeting);
    }

    @Test
    public void groupMinusRegionsByRegion_validGeoTargeting_ReturnCorrectMap() {
        List<Long> geoTargeting = asList(1L, -2L, 3L, -4L, -5L, 6L, 7L);

        Map<Long, List<Long>> result = groupMinusRegionsByRegion(geoTargeting);

        assertThat(result.keySet(), contains(1L, 3L, 6L, 7L));

        assertThat(result.get(1L), contains(-2L));
        assertThat(result.get(3L), contains(-4L, -5L));
        assertThat(result.get(6L), empty());
        assertThat(result.get(7L), empty());
    }
}
