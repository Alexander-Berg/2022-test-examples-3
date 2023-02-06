package ru.yandex.market.logistics.management.service.export.dynamic;

import java.util.List;

import org.assertj.core.error.AssertionErrorCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.management.service.export.dynamic.dto.RegionToRegion;
import ru.yandex.market.logistics.management.util.DynamicBuilderJUnitSoftAssertions;

class RegionHelperTest {
    public static final int ROOT_ID = 1;
    public static final int FROM_PARENT_ID = 2;
    public static final int FROM_ID = 3;
    public static final int TO_PARENT_ID = 4;
    public static final int TO_ID = 5;

    public static final Region ROOT = new Region(ROOT_ID, "from_parent", RegionType.CITY, null);
    public static final Region FROM_PARENT = new Region(FROM_PARENT_ID, "from_parent", RegionType.CITY, ROOT);
    public static final Region FROM = new Region(FROM_ID, "from", RegionType.CITY, FROM_PARENT);
    public static final Region TO_PARENT = new Region(TO_PARENT_ID, "to_parent", RegionType.CITY, ROOT);
    public static final Region TO = new Region(TO_ID, "to", RegionType.CITY, TO_PARENT);

    @RegisterExtension
    final DynamicBuilderJUnitSoftAssertions softly = new DynamicBuilderJUnitSoftAssertions(new AssertionErrorCreator());
    private RegionService regionService;
    private RegionHelper regionHelper;
    private RegionTree regionTree;

    @BeforeEach
    void setUp() {
        regionService = Mockito.mock(RegionService.class);
        regionTree = Mockito.mock(RegionTree.class);

        regionHelper = new RegionHelper(regionService);

        Mockito.when(regionService.getRegionTree()).thenReturn(regionTree);

        Mockito.when(regionTree.getRegion(ROOT_ID)).thenReturn(ROOT);
        Mockito.when(regionTree.getRegion(FROM_PARENT_ID)).thenReturn(FROM_PARENT);
        Mockito.when(regionTree.getRegion(FROM_ID)).thenReturn(FROM);
        Mockito.when(regionTree.getRegion(TO_PARENT_ID)).thenReturn(TO_PARENT);
        Mockito.when(regionTree.getRegion(TO_ID)).thenReturn(TO);
    }

    @Test
    public void testCreateParentPairs() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(FROM_ID, TO_ID, false);
        softly.assertThat(parentRegions).containsExactly(
            new RegionToRegion(FROM_ID, TO_PARENT_ID),
            new RegionToRegion(FROM_ID, ROOT_ID),
            new RegionToRegion(FROM_PARENT_ID, TO_ID),
            new RegionToRegion(FROM_PARENT_ID, TO_PARENT_ID),
            new RegionToRegion(FROM_PARENT_ID, ROOT_ID),
            new RegionToRegion(ROOT_ID, TO_ID),
            new RegionToRegion(ROOT_ID, TO_PARENT_ID),
            new RegionToRegion(ROOT_ID, ROOT_ID)
        );
    }

    @Test
    public void testCreateParentPairsWithCurrent() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(FROM_ID, TO_ID, true);
        softly.assertThat(parentRegions).containsExactly(
            new RegionToRegion(FROM_ID, TO_ID),
            new RegionToRegion(FROM_ID, TO_PARENT_ID),
            new RegionToRegion(FROM_ID, ROOT_ID),
            new RegionToRegion(FROM_PARENT_ID, TO_ID),
            new RegionToRegion(FROM_PARENT_ID, TO_PARENT_ID),
            new RegionToRegion(FROM_PARENT_ID, ROOT_ID),
            new RegionToRegion(ROOT_ID, TO_ID),
            new RegionToRegion(ROOT_ID, TO_PARENT_ID),
            new RegionToRegion(ROOT_ID, ROOT_ID)
        );
    }

    @Test
    public void testCreateParentPairsSame() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(FROM_ID, FROM_ID, false);
        softly.assertThat(parentRegions).containsExactly(
            new RegionToRegion(FROM_ID, FROM_PARENT_ID),
            new RegionToRegion(FROM_ID, ROOT_ID),
            new RegionToRegion(FROM_PARENT_ID, FROM_ID),
            new RegionToRegion(FROM_PARENT_ID, FROM_PARENT_ID),
            new RegionToRegion(FROM_PARENT_ID, ROOT_ID),
            new RegionToRegion(ROOT_ID, FROM_ID),
            new RegionToRegion(ROOT_ID, FROM_PARENT_ID),
            new RegionToRegion(ROOT_ID, ROOT_ID)
        );
    }

    @Test
    public void testCreateParentPairsParentAndChild() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(FROM_ID, FROM_PARENT_ID, false);
        softly.assertThat(parentRegions).containsExactly(
            new RegionToRegion(FROM_ID, ROOT_ID),
            new RegionToRegion(FROM_PARENT_ID, FROM_PARENT_ID),
            new RegionToRegion(FROM_PARENT_ID, ROOT_ID),
            new RegionToRegion(ROOT_ID, FROM_PARENT_ID),
            new RegionToRegion(ROOT_ID, ROOT_ID)
        );
    }

    @Test
    public void testCreateParentPairsEmpty() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(ROOT_ID, ROOT_ID, false);
        softly.assertThat(parentRegions).isEmpty();
    }

    @Test
    public void testCreateParentPairsOnlyCurrent() {
        List<RegionToRegion> parentRegions = regionHelper.getRegionParentChain(ROOT_ID, ROOT_ID, true);
        softly.assertThat(parentRegions).containsExactly(new RegionToRegion(ROOT_ID, ROOT_ID));
    }

    @Test
    public void testGetParents() {
        List<Integer> chain = regionHelper.getRegionParentChain(TO_ID, false);
        softly.assertThat(chain).containsExactly(TO_PARENT_ID, ROOT_ID);
    }

    @Test
    public void testGetParentsWithOwnId() {
        List<Integer> chain = regionHelper.getRegionParentChain(TO_ID, true);
        softly.assertThat(chain).containsExactly(TO_ID, TO_PARENT_ID, ROOT_ID);
    }
}
