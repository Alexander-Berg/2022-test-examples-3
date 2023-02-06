package ru.yandex.market.deliverycalculator.indexer.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionTreeBuilder;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

/**
 * Тесты для {@link GeoBaseSyncService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class GeoBaseSyncServiceTest extends FunctionalTest {

    @Autowired
    private GeoBaseSyncService geoBaseSyncService;

    @Autowired
    private RegionTreeBuilder<Region> regionRegionTreeBuilder;

    @Test
    @DisplayName("Успешное обновление дерева регионов")
    @DbUnitDataSet(before = "geoBaseSyncService.before.csv", after = "geoBaseSyncService.update.after.csv")
    void testSuccess() {
        final Region parent = new Region(1, "parent", RegionType.COUNTRY, null);
        final Region child10 = new Region(10, "child10", RegionType.REGION, parent);
        final Region child100 = new Region(100, "child100", RegionType.CITY, child10);
        final Region child200 = new Region(200, "child200", RegionType.CITY, child10);

        mockRegionTree(Arrays.asList(
                parent,
                child10,
                child100,
                child200
        ));

        geoBaseSyncService.syncGeoBase();
    }

    @Test
    @DisplayName("Не успешное обновление дерева регионов. От геобазы пришло слишком мало")
    @DbUnitDataSet(before = "geoBaseSyncService.before.csv", after = "geoBaseSyncService.before.csv")
    void testFail() {
        final Region parent = new Region(1, "parent", RegionType.COUNTRY, null);

        mockRegionTree(Collections.singletonList(
                parent
        ));

        final IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> geoBaseSyncService.syncGeoBase());
        Assertions.assertEquals("Geobase returned too few correct regions", exception.getMessage());
    }

    @SuppressWarnings("unchecked")
    private void mockRegionTree(final List<Region> regions) {
        final RegionTree<Region> regionTreeMock = Mockito.mock(RegionTree.class);

        Mockito.when(regionRegionTreeBuilder.buildRegionTree()).thenReturn(regionTreeMock);
        Mockito.when(regionTreeMock.getRegions()).thenReturn(regions);
    }

}
