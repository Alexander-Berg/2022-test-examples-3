package ru.yandex.direct.core.entity.placements.service;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementBlock;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize300x300;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.copyOutdoorPlacementForceSetGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdatePlacementsServiceUpdateExistingUntypedPlacementsTest extends UpdatePlacementsServiceBaseTest {

    @Test
    public void addBlocksToExistingUntypedPlacement() {
        Placement existingPlacement = emptyCommonYandexPlacement(PAGE_ID_1);
        placementSteps.addPlacement(existingPlacement);

        PlacementBlock newBlock1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock newBlock2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(newBlock1, newBlock2));

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void updateBlocksInExistingUntypedPlacement() {
        PlacementBlock oldBlock1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock oldBlock2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(oldBlock1, oldBlock2));
        placementSteps.addPlacement(existingPlacement);

        PlacementBlock newBlock1 = commonBlockWithTwoSizes(PAGE_ID_1, 123L);
        PlacementBlock newBlock2 = commonBlockWithOneSize(PAGE_ID_1, 124L);
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(newBlock1, newBlock2));

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void deleteBlocksInExistingUntypedPlacement() {
        PlacementBlock oldBlock1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock oldBlock2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(oldBlock1, oldBlock2));
        placementSteps.addPlacement(existingPlacement);

        Placement newPlacement = emptyCommonYandexPlacement(PAGE_ID_1);

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void addAndUpdateAndDeleteBlocksInExistingUntypedPlacement() {
        PlacementBlock block1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock block2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(existingPlacement);

        PlacementBlock updatedBlock = commonBlockWithOneSize(PAGE_ID_1, 124L);
        PlacementBlock newBlock = commonBlockWithOneSize300x300(PAGE_ID_1, 125L);
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void addAndUpdateAndDontDeleteBlockInExistingUntypedPlacement() {
        PlacementBlock block1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock block2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(existingPlacement);

        PlacementBlock oldBlock = block1;
        PlacementBlock updatedBlock = commonBlockWithOneSize(PAGE_ID_1, 124L);
        PlacementBlock newBlock = commonBlockWithOneSize300x300(PAGE_ID_1, 125L);
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));

        Placement expectedPlacement =
                commonYandexPlacementWithBlocks(PAGE_ID_1, asList(oldBlock, updatedBlock, newBlock));

        update(newPlacement, ImmutableMap.of(PAGE_ID_1, singleton(123L)));

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void changeTypeAndAddAndUpdateAndDeleteBlocksInExistingUntypedPlacement() {
        PlacementBlock block1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock block2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(existingPlacement);

        OutdoorBlock updatedBlock = outdoorBlockWithOneSize(PAGE_ID_1, 123L);
        OutdoorBlock newBlock = outdoorBlockWithTwoSizes(PAGE_ID_1, 125L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));

        update(newPlacement);

        OutdoorPlacement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement, null, newBlock.getGeoId());

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }
}
