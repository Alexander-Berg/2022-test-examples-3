package ru.yandex.direct.core.entity.placements.repository;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.PlacementSteps;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class PlacementRepositoryGetTest {

    private static final Long PAGE_ID_1 = 2L;
    private static final Long PAGE_ID_2 = 7L;
    private static final Long PAGE_ID_3 = 12L;
    private static final Long BLOCK_ID_1 = 1L;

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private PlacementRepository placementRepository;

    @Before
    public void before() {
        placementSteps.clearPlacements();
    }

    // get by type

    @Test(expected = IllegalArgumentException.class)
    public void getPlacementsByType_TypeIsNull_ThrowsException() {
        placementRepository.getPlacementsByType(null);
    }

    @Test
    public void getPlacementsByType_NoPlacementsAtAll() {
        Map<Long, Placement> placements = placementRepository.getPlacementsByType(PlacementType.OUTDOOR);
        assertThat(placements.keySet(), hasSize(0));
    }

    @Test
    public void getPlacementsByType_NoPlacementsOfType() {
        placementSteps.addPlacement(emptyCommonYandexPlacement(PAGE_ID_1));
        Map<Long, Placement> placements = placementRepository.getPlacementsByType(PlacementType.OUTDOOR);
        assertThat(placements.keySet(), hasSize(0));
    }

    @Test
    public void getPlacementsByType_OnePlacement() {
        Placement placement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(placement);
        Map<Long, Placement> placements = placementRepository.getPlacementsByType(PlacementType.OUTDOOR);
        assertThat(placements.keySet(), hasSize(1));
        assertThat(placements.get(PAGE_ID_1), beanDiffer(placement));
    }

    @Test
    public void getPlacementsByType_TwoPlacementsOfTypeAndOneAnother() {
        Placement placement1 = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        Placement placement2 = emptyOutdoorPlacement(PAGE_ID_2);
        placementSteps.addPlacement(placement1);
        placementSteps.addPlacement(placement2);
        placementSteps.addPlacement(emptyCommonYandexPlacement(PAGE_ID_3));
        Map<Long, Placement> placements = placementRepository.getPlacementsByType(PlacementType.OUTDOOR);
        assertThat(placements.keySet(), hasSize(2));
        assertThat(placements.get(PAGE_ID_1), beanDiffer(placement1));
        assertThat(placements.get(PAGE_ID_2), beanDiffer(placement2));
    }

    @Test
    public void getPlacementsByType_IndoorPlacement() {
        Placement placement = indoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(indoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(placement);
        Map<Long, Placement> placements = placementRepository.getPlacementsByType(PlacementType.INDOOR);
        assertThat(placements.keySet(), hasSize(1));
        assertThat(placements.get(PAGE_ID_1), beanDiffer(placement));
    }
}
