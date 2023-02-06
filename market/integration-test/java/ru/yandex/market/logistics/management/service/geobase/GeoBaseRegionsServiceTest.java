package ru.yandex.market.logistics.management.service.geobase;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.RegionEntity;
import ru.yandex.market.logistics.management.repository.geoBase.GeoBaseRepository;

import static java.util.stream.Collectors.toList;
import static ru.yandex.market.logistics.management.util.TestRegions.EARTH_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_REGION_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.NON_EXISTS_REGION_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.RUSSIA_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;

class GeoBaseRegionsServiceTest extends AbstractContextualTest {
    @Autowired
    private RegionService regionService;

    @Autowired
    private GeoBaseRegionsService geoBaseService;

    @Autowired
    private GeoBaseRepository geoBaseRepository;

    private RegionTree<Region> regionTree;

    @BeforeEach
    private void setup() {
        regionTree = buildRegionTree();
        mockRegionTree(regionTree);
        geoBaseService.syncRegions();
    }

    @Test
    void testSyncRegionsIsSuccess() {
        List<Integer> ids = regionTree.getRegions().stream().map(Region::getId).collect(toList());
        softly.assertThat(geoBaseRepository.findAll())
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    void testGetAllDescendantsIsSuccess() {
        softly.assertThat(geoBaseService.getAllDescendants(MOSCOW_REGION_ID))
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrder(MOSCOW_REGION_ID, MOSCOW_ID);
    }

    @Test
    void testGetAllDescendantsWithoutDescendantsIsSuccess() {
        softly.assertThat(geoBaseService.getAllDescendants(MOSCOW_ID))
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrder(MOSCOW_ID);
    }

    @Test
    void testGetAllDescendantsIdsIsSuccess() {
        softly.assertThat(geoBaseService.getAllDescendantsIds(MOSCOW_REGION_ID))
            .containsExactlyInAnyOrder(MOSCOW_REGION_ID, MOSCOW_ID);
    }

    @Test
    void testGetAllDescendantsIdsWithoutDescendantsIsSuccess() {
        softly.assertThat(geoBaseService.getAllDescendantsIds(MOSCOW_ID))
            .containsExactlyInAnyOrder(MOSCOW_ID);
    }

    @Test
    void testGetAllAncestorsIsSuccess() {
        softly.assertThat(geoBaseService.getAllAncestors(Set.of(MOSCOW_ID)))
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrder(EARTH_ID, RUSSIA_ID, MOSCOW_REGION_ID, MOSCOW_ID);
    }

    @Test
    void testGetAllAncestorsIds() {
        softly.assertThat(geoBaseService.getAllAncestorsIds(MOSCOW_ID))
            .containsExactlyInAnyOrder(EARTH_ID, RUSSIA_ID, MOSCOW_REGION_ID, MOSCOW_ID);
    }

    @Test
    void testGetAllAncestorsWithoutAncestorsIsSuccess() {
        softly.assertThat(geoBaseService.getAllAncestors(Set.of(EARTH_ID)))
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrder(EARTH_ID);
    }

    @Test
    void testGetDataForNonExistsRegionReturnsEmptyCollection() {
        softly.assertThat(geoBaseService.getAllDescendants(NON_EXISTS_REGION_ID)).isEmpty();
        softly.assertThat(geoBaseService.getAllDescendantsIds(NON_EXISTS_REGION_ID)).isEmpty();
        softly.assertThat(geoBaseService.getAllAncestors(Set.of(NON_EXISTS_REGION_ID))).isEmpty();
    }

    @Test
    @DatabaseSetup({
        "/data/service/geobaseregions/db/before/regions.xml",
        "/data/service/geobaseregions/db/before/radial_zones_and_country.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/geobaseregions/db/before/radial_zones_and_country.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void testSyncRegionsWithReferencedValues() {
        List<Integer> ids = regionTree.getRegions().stream().map(Region::getId).collect(toList());
        softly.assertThat(geoBaseRepository.findAll())
            .extracting(RegionEntity::getId)
            .containsExactlyInAnyOrderElementsOf(ids);
    }

    private void mockRegionTree(RegionTree<Region> regionTree) {
        Mockito.when(regionService.getRegionTree()).thenReturn(regionTree);
    }
}
