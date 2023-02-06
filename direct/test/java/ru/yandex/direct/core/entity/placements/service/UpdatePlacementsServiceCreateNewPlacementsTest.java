package ru.yandex.direct.core.entity.placements.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.placements.model1.OutdoorBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacementWithMirrors;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize2;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdatePlacementsServiceCreateNewPlacementsTest extends UpdatePlacementsServiceBaseTest {

    @Test
    public void newEmptyPlacementWhenNothingExistsInDatabase() {
        Placement placement = emptyCommonYandexPlacement(PAGE_ID_1);
        update(placement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void newEmptyPlacementsWhenAnotherPlacementExistsInDatabase() {
        Placement existingPlacement = emptyCommonYandexPlacement(PAGE_ID_1);
        placementSteps.addPlacement(existingPlacement);

        Placement placement1 = emptyCommonYandexPlacement(PAGE_ID_2);
        Placement placement2 = emptyOutdoorPlacement(PAGE_ID_3);
        update(asList(placement1, placement2));

        Placement actualExistingPlacement = get(PAGE_ID_1);
        Placement actualPlacement1 = get(PAGE_ID_2);
        Placement actualPlacement2 = get(PAGE_ID_3);
        assertThat(actualExistingPlacement, beanDiffer(existingPlacement));
        assertThat(actualPlacement1, beanDiffer(placement1));
        assertThat(actualPlacement2, beanDiffer(placement2));
    }

    @Test
    public void newEmptyPlacementsWithMirrorsWhenAnotherPlacementExistsInDatabase() {
        Placement existingPlacement = emptyCommonYandexPlacement(PAGE_ID_1);
        placementSteps.addPlacement(existingPlacement);

        Placement placement1 = emptyCommonYandexPlacementWithMirrors(PAGE_ID_2);
        Placement placement2 = emptyOutdoorPlacement(PAGE_ID_3);
        update(asList(placement1, placement2));

        Placement actualExistingPlacement = get(PAGE_ID_1);
        Placement actualPlacement1 = get(PAGE_ID_2);
        Placement actualPlacement2 = get(PAGE_ID_3);
        assertThat(actualExistingPlacement, beanDiffer(existingPlacement));
        assertThat(actualPlacement1, beanDiffer(placement1));
        assertThat(actualPlacement2, beanDiffer(placement2));
    }

    @Test
    public void newPlacement() {
        Placement placement = commonYandexPlacementWithBlock(PAGE_ID_1,
                commonBlockWithOneSize(PAGE_ID_1, 123L));
        update(placement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void newPlacementWhenAnotherPlacementWithBlockWithSameIdExistsInDatabase() {
        Placement existingPlacement = commonYandexPlacementWithBlock(PAGE_ID_1,
                commonBlockWithOneSize(PAGE_ID_1, 123L));
        placementSteps.addPlacement(existingPlacement);

        Placement placement = commonYandexPlacementWithBlock(PAGE_ID_2,
                commonBlockWithOneSize(PAGE_ID_2, 123L));
        update(placement);

        Placement actualExistingPlacement = get(PAGE_ID_1);
        assertThat(actualExistingPlacement, beanDiffer(existingPlacement));

        Placement actualPlacement = get(PAGE_ID_2);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void newEmptyTypedPlacement() {
        OutdoorPlacement newPlacement = new OutdoorPlacement(PAGE_ID_1, "yandex.ru", "yandex", LOGIN, null, true, false,
                false, emptyList(), List.of());

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void newTypedPlacement() {
        OutdoorBlock updatedBlock = outdoorBlockWithOneSize(PAGE_ID_1, 124L);
        OutdoorBlock newBlock = outdoorBlockWithOneSize2(PAGE_ID_1, 125L);
        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1, asList(updatedBlock, newBlock));

        update(newPlacement);

        Placement actualPlacement = get(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }
}
