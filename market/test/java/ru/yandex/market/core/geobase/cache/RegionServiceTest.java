package ru.yandex.market.core.geobase.cache;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.id.IdsUtils;
import ru.yandex.market.common.test.db.DataSetType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.Region;
import ru.yandex.market.core.geobase.model.RegionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class RegionServiceTest extends FunctionalTest {

    @Autowired
    private RegionService regionService;

    /*
     * <pre>
     *         1
     *     2       3
     *   4   5    6 7
     *  8 9   10
     * </pre>
     */

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void listAllRegions() {
        Collection<Region> expected = set(
                new Region(1, "1", null, RegionType.CITY, 0, 51),
                new Region(2, "2", 1L, RegionType.CITY, 0, 52),
                new Region(3, "3", 1L, RegionType.CITY, 0, 53),
                new Region(4, "4", 2L, RegionType.CITY, 0, null),
                new Region(5, "5", 2L, RegionType.CITY, 0, 51),
                new Region(6, "6", 3L, RegionType.CITY, 0, 51),
                new Region(7, "7", 3L, RegionType.CITY, 0, 52),
                new Region(8, "8", 4L, RegionType.CITY, 0, null),
                new Region(9, "9", 4L, RegionType.CITY, 0, null),
                new Region(10, "10", 5L, RegionType.CITY, 0, null)
        );

        Assertions.assertEquals(expected, set(regionService.listAllRegions()));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionsFrom() {
        Collection<Region> expected = set(
                new Region(2, "2", 1L, RegionType.CITY, 0, 52),
                new Region(4, "4", 2L, RegionType.CITY, 0, null),
                new Region(5, "5", 2L, RegionType.CITY, 0, 51),
                new Region(8, "8", 4L, RegionType.CITY, 0, null),
                new Region(9, "9", 4L, RegionType.CITY, 0, null),
                new Region(10, "10", 5L, RegionType.CITY, 0, null)
        );
        Assertions.assertEquals(expected, set(regionService.getSubregionsFrom(2)));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFrom() {
        Collection<Long> expected = set(2L, 4L, 5L, 8L, 9L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2)));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionsFromLimited() {
        Collection<Region> expected = set(
                new Region(2, "2", 1L, RegionType.CITY, 0, 52),
                new Region(4, "4", 2L, RegionType.CITY, 0, null),
                new Region(5, "5", 2L, RegionType.CITY, 0, 51),
                new Region(10, "10", 5L, RegionType.CITY, 0, null)
        );
        Assertions.assertEquals(expected, set(regionService.getSubregionsFrom(2, set(2L, 4L))));
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void getSubregionsByTypeFrom() {
        final var actual = regionService.getSubregionsByTypeFrom(225, Set.of(RegionType.CITY, RegionType.TOWN))
                .stream()
                .map(Region::getId)
                .collect(Collectors.toList());

        assertThat(actual, containsInAnyOrder(117122L, 64L));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFromLimited() {
        Collection<Long> expected = set(2L, 4L, 5L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, set(4L))));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFromLimited2() {
        Collection<Long> expected = set(2L, 4L, 5L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, set(4L, 5L))));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFromNull() {
        Collection<Long> expected = set(2L, 4L, 5L, 8L, 9L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, null)));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFromEmpty() {
        Collection<Long> expected = set(2L, 4L, 5L, 8L, 9L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, set())));
    }

    @Test
    @DbUnitDataSet(type = DataSetType.SINGLE_CSV, before = "regions.csv")
    void getSubregionIdsFromWrong() {
        Collection<Long> expected = set(2L, 4L, 5L, 8L, 9L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, set(1L))));
    }

    @Test
    @DbUnitDataSet(before = "regions.csv")
    void getSubregionIdsFromNonExistent() {
        Collection<Long> expected = set(2L, 4L, 5L, 8L, 9L, 10L);
        Assertions.assertEquals(expected, set(regionService.getSubregionIdsFrom(2, set(50L))));
    }


    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void getLeafToRootRegionsNonExistentTest() {
        Collection<Region> leafToRootRegions =
                regionService.getRootBranchRegions(new HashSet<>(Collections.singletonList(-1L)));
        Assertions.assertTrue(leafToRootRegions.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void getLeafToRootRegionsMixedTest() {
        Collection<Region> leafToRootRegions =
                regionService.getRootBranchRegions(new HashSet<>(Arrays.asList(-1L, 225L, 28857L, 117122L)));
        List<Long> expected =
                list(225L, 1L, 3L, 10001L, 10000L, 28857L, 24542L, 20548L, 20527L, 187L, 166L, 117122L, 98590L);
        assertThat(expected, containsInAnyOrder(leafToRootRegions.stream().map(Region::getId).toArray()));
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void findRegionsTest() {
        List<List<Region>> regions = regionService.findRegions("александровка", 10);

        List<List<Long>> expected = list(list(28857L, 24542L, 20548L, 20527L, 187L, 166L, 10001L, 10000L),
                list(117122L, 98590L, 1L, 3L, 225L, 10001L, 10000L));
        assertThat(expected, containsInAnyOrder(regions.stream().map(l -> l.stream().map(Region::getId).
                collect(Collectors.toList())).toArray()));
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void findRegionsEmptyTest() {
        List<List<Region>> regions = regionService.findRegions("ссср", 10);
        Assertions.assertTrue(regions.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void findRegionsLimitedTest() {
        int limit = 1;
        List<List<Region>> regions = regionService.findRegions("александровка", limit);
        Assertions.assertEquals(regions.size(), limit);
        List<Long> actual =
                regions.stream().map(l -> l.stream().map(Region::getId).
                        collect(Collectors.toList())).collect(Collectors.toList()).get(0);
        List<List<Long>> atLeastOneExpected = list(list(28857L, 24542L, 20548L, 20527L, 187L, 166L, 10001L, 10000L),
                list(117122L, 98590L, 1L, 3L, 225L, 10001L, 10000L));
        Assertions.assertTrue(atLeastOneExpected.contains(actual));
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void getRegions() {
        final List<Long> expected = Arrays.asList(1L, 64L, 59L);
        final List<Long> unknown = Arrays.asList(8L, 9L);
        final Map<Long, Region> actual = regionService.getRegions(ListUtils.union(unknown, expected));

        assertThat(actual.keySet(), containsInAnyOrder(expected.toArray()));
        assertThat(IdsUtils.toIdsList(actual.values()), containsInAnyOrder(expected.toArray()));
    }

    @Test
    @DbUnitDataSet(before = "region-trees.before.csv")
    void getRegion() {
        final Region region = regionService.getRegion(1);
        assertThat(region, equalTo(new Region(1, "1", null)));
    }

    @Test
    @DbUnitDataSet(before = "region-tree-for-main-city.csv")
    void getMainCity() {
        assertThat(regionService.findMainCity(37180L), equalTo(Optional.of(2L)));
    }
}
