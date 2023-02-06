package ru.yandex.market.deliverycalculator.workflow.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.Region;
import ru.yandex.market.deliverycalculator.model.exception.RegionNotFoundException;
import ru.yandex.market.deliverycalculator.workflow.test.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для {@link CachedRegionService}.
 */
@DbUnitDataSet(before = "database/regions.csv")
class CachedRegionServiceTest extends FunctionalTest {

    @Autowired
    private RegionService tested;

    static Stream<Arguments> testFindParentRegionsWithGranularityInData() {
        return Stream.of(
                Arguments.of(213, 5, 5, List.of(1)),
                Arguments.of(213, 6, 6, List.of(213)),
                Arguments.of(213, 5, 6, List.of(213, 1)),
                Arguments.of(98585, 5, 6, List.of(1)),
                Arguments.of(98585, 0, 4, List.of(3, 225, 10001, 10000)),
                Arguments.of(10000, 0, 0, List.of(10000))
        );
    }

    @ParameterizedTest
    @MethodSource("testFindParentRegionsWithGranularityInData")
    void findParentRegionsWithGranularityIn(
            int childRegionId,
            int minGranularity,
            int maxGranularity,
            List<Integer> expectedRegionIds
    ) throws RegionNotFoundException {
        var parentRegions = tested.findParentRegionsWithGranularityIn(childRegionId, minGranularity, maxGranularity);
        assertThat(parentRegions.stream().map(Region::getId)).containsExactlyElementsOf(expectedRegionIds);
    }

    @Test
    void findParentRegionsWithGranularityIn_failsOnInvalidMinMax() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> tested.findParentRegionsWithGranularityIn(100500, 6, 5));
    }

    @Test
    void isSubregionOf() throws RegionNotFoundException {
        assertTrue(tested.isSubregionOf(213, 10000));
        assertTrue(tested.isSubregionOf(213, 10001));
        assertTrue(tested.isSubregionOf(111, 10000));
        assertTrue(tested.isSubregionOf(111, 111));
        assertFalse(tested.isSubregionOf(213, 241));
        assertFalse(tested.isSubregionOf(10000, 111));
    }

    @Test
    void isSubregionOf_analyzedRegionNotFound() {
        assertThatExceptionOfType(RegionNotFoundException.class)
                .isThrownBy(() -> tested.isSubregionOf(1122334455, 10000));
    }

    @Test
    void isSubregionOfAny() throws RegionNotFoundException {
        assertTrue(tested.isSubregionOfAny(213, Set.of(241, 10000)));
        assertTrue(tested.isSubregionOfAny(213, Set.of(10001, 225)));
        assertTrue(tested.isSubregionOfAny(111, Set.of(10000)));
        assertTrue(tested.isSubregionOfAny(111, Set.of(111)));
        assertTrue(tested.isSubregionOfAny(1124, Set.of(10000)));
        assertFalse(tested.isSubregionOfAny(213, Set.of(241, 120999)));
        assertFalse(tested.isSubregionOfAny(10000, Set.of(111, 120999)));
    }

    @Test
    void isSubregionOfAny_analyzedRegionNotFound() {
        assertThatExceptionOfType(RegionNotFoundException.class)
                .isThrownBy(() -> tested.isSubregionOfAny(1122334455, Set.of(10000)));
    }
}
