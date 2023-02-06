package ru.yandex.direct.core.entity.placements.service;

import java.util.List;

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
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize300x300;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.copyOutdoorPlacementForceSetGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize2;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdatePlacementsServiceUpdateExistingTypedPlacementsTest extends UpdatePlacementsServiceBaseTest {

    @Test
    public void addBlockToExistingTypedPlacement() {
        OutdoorPlacement placement = emptyOutdoorPlacement(PAGE_ID_1);
        placementSteps.addPlacement(placement);

        OutdoorBlock block = outdoorBlockWithOneSize(PAGE_ID_1, 12L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlock(PAGE_ID_1, block);

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void updateBlockInExistingTypedPlacement() {
        OutdoorBlock block = outdoorBlockWithOneSize(PAGE_ID_1, 12L);
        OutdoorPlacement placement = outdoorPlacementWithBlock(PAGE_ID_1, block);
        placementSteps.addPlacement(placement);

        OutdoorBlock updatedBlock = outdoorBlockWithOneSize2(PAGE_ID_1, 12L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlock(PAGE_ID_1, updatedBlock);

        OutdoorPlacement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement, block.getGeoId());

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void markDeletedBlockInExistingTypedPlacement() {
        OutdoorBlock block = outdoorBlockWithOneSize(PAGE_ID_1, 123L);
        OutdoorPlacement placement = outdoorPlacementWithBlock(PAGE_ID_1, block);
        placementSteps.addPlacement(placement);

        OutdoorPlacement newPlacement = emptyOutdoorPlacement(PAGE_ID_1);

        OutdoorBlock markedDeletedBlock = block.markDeleted();
        OutdoorPlacement expectedPlacement = replaceBlocks(newPlacement, singletonList(markedDeletedBlock));

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void addAndUpdateAndMarkDeletedBlockInExistingOutdoorPlacement() {
        OutdoorBlock block1 = outdoorBlockWithOneSize(PAGE_ID_1, 123L);
        OutdoorBlock block2 = outdoorBlockWithOneSize2(PAGE_ID_1, 124L);
        OutdoorPlacement placement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(placement);

        OutdoorBlock updatedBlock = outdoorBlockWithTwoSizes(PAGE_ID_1, 124L);
        OutdoorBlock newBlock = outdoorBlockWithOneSize(PAGE_ID_1, 125L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));

        OutdoorBlock markedDeletedBlock = block1.markDeleted();
        OutdoorPlacement expectedPlacement = replaceBlocks(newPlacement,
                asList(markedDeletedBlock, updatedBlock, newBlock));
        expectedPlacement = copyOutdoorPlacementForceSetGeo(expectedPlacement,
                block1.getGeoId(), block2.getGeoId(), newBlock.getGeoId());

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void addAndUpdateAndDontDeleteBlockInExistingOutdoorPlacement() {
        OutdoorBlock block1 = outdoorBlockWithOneSize(PAGE_ID_1, 123L);
        OutdoorBlock block2 = outdoorBlockWithOneSize2(PAGE_ID_1, 124L);
        OutdoorPlacement placement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(placement);

        OutdoorBlock oldBlock = block1;
        OutdoorBlock updatedBlock = outdoorBlockWithTwoSizes(PAGE_ID_1, 124L);
        OutdoorBlock newBlock = outdoorBlockWithOneSize(PAGE_ID_1, 125L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));


        OutdoorPlacement expectedPlacement = replaceBlocks(newPlacement,
                asList(oldBlock, updatedBlock, newBlock));
        expectedPlacement = copyOutdoorPlacementForceSetGeo(expectedPlacement,
                block1.getGeoId(), block2.getGeoId(), newBlock.getGeoId());

        update(newPlacement, ImmutableMap.of(PAGE_ID_1, singleton(123L)));

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

// typed and untyped in one request

    @Test
    public void updateTypedAndUntypedPlacementWithMarkingAsDeleted() {
        // untyped
        PlacementBlock block1 = commonBlockWithOneSize(PAGE_ID_1, 123L);
        PlacementBlock block2 = commonBlockWithTwoSizes(PAGE_ID_1, 124L);
        Placement existingUntypedPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, asList(block1, block2));
        placementSteps.addPlacement(existingUntypedPlacement);
        PlacementBlock newBlock = commonBlockWithOneSize300x300(PAGE_ID_1, 125L);
        Placement newUntypedPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1, singletonList(newBlock));

        // typed
        OutdoorBlock block = outdoorBlockWithOneSize(PAGE_ID_2, 123L);
        OutdoorPlacement placement = outdoorPlacementWithBlock(PAGE_ID_2, block);
        placementSteps.addPlacement(placement);

        OutdoorPlacement newTypedPlacement = emptyOutdoorPlacement(PAGE_ID_2);

        // update
        update(asList(newUntypedPlacement, newTypedPlacement));

        // check untyped result
        Placement actualUntypedPlacement = get(PAGE_ID_1);
        assertThat(actualUntypedPlacement, beanDiffer(newUntypedPlacement));

        // check typed result
        OutdoorBlock markedDeletedBlock = block.markDeleted();
        OutdoorPlacement expectedTypedPlacement =
                replaceBlocks(newTypedPlacement, singletonList(markedDeletedBlock));
        Placement actualTypedPlacement = get(PAGE_ID_2);
        assertThat(actualTypedPlacement, beanDiffer(expectedTypedPlacement));
    }

    private OutdoorPlacement replaceBlocks(Placement placement, List<OutdoorBlock> blocks) {
        return new OutdoorPlacement(placement.getId(),
                placement.getDomain(), placement.getCaption(), placement.getLogin(), placement.getOperatorName(),
                placement.isYandexPage(), placement.isDeleted(), placement.isTesting(), blocks, List.of());
    }
}
