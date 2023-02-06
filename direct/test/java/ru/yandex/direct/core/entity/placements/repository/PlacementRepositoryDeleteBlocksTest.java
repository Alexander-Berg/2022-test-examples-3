package ru.yandex.direct.core.entity.placements.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.PlacementSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.copyOutdoorPlacementForceSetGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorDeletedBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

// удаление отсутствующих в запросе блоков
@CoreTest
@RunWith(SpringRunner.class)
public class PlacementRepositoryDeleteBlocksTest {

    private static final Long PAGE_ID_1 = 2L;
    private static final Long PAGE_ID_2 = 7L;
    private static final Long BLOCK_ID_1 = 1L;
    private static final Long BLOCK_ID_2 = 3L;
    private static final Long BLOCK_ID_3 = 18L;

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private PlacementRepository placementRepository;

    @Before
    public void before() {
        placementSteps.clearPlacements();
    }

    @Test
    public void createOrUpdatePlacements_DeleteBlock() {
        placementSteps.addPlacement(outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1))));
        Placement newPlacement = emptyOutdoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DeleteTwoBlocks() {
        placementSteps.addPlacement(outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2))));
        Placement newPlacement = emptyOutdoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DeleteBlockAndAddBlock() {
        placementSteps.addPlacement(outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1))));
        Placement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DeleteBlockAndAddBlockAndUpdateBlock() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2)));
        placementSteps.addPlacement(sourcePlacement);

        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1),
                        outdoorDeletedBlock(PAGE_ID_1, BLOCK_ID_3)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId(),
                newPlacement.getBlocks().get(1).getGeoId());
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);

        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DeleteBlockAndUpdateBlock() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2)));
        placementSteps.addPlacement(sourcePlacement);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        OutdoorPlacement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId());

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DeleteBlockAndUpdateBlock_DontTouchOtherPlacement() {
        placementSteps.addPlacement(outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2))));

        Placement untouchedPlacement = outdoorPlacementWithBlocks(PAGE_ID_2,
                asList(outdoorBlockWithTwoSizes(PAGE_ID_2, BLOCK_ID_1),
                        outdoorDeletedBlock(PAGE_ID_2, BLOCK_ID_2)));
        placementSteps.addPlacement(untouchedPlacement);

        Placement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_2);
        assertThat(actualPlacement, beanDiffer(untouchedPlacement));
    }
}
