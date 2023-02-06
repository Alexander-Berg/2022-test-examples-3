package ru.yandex.direct.core.entity.placements.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.PlacementSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithEmptySizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize300x300;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonDeletedBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacementWithMirrors;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyDeletedIndoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyDeletedOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyIndoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class PlacementRepositoryAddTest {

    private static final Long PAGE_ID_1 = 2L;
    private static final Long PAGE_ID_2 = 7L;
    private static final Long BLOCK_ID_1 = 1L;
    private static final Long BLOCK_ID_2 = 3L;
    private static final String BLOCK_ID_1_BLOCK_CAPTION = "Test block caption of " + BLOCK_ID_1.toString();

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private PlacementRepository placementRepository;

    @Before
    public void before() {
        placementSteps.clearPlacements();
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyCommonYandexPlacement() {
        Placement placement = emptyCommonYandexPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyCommonYandexWithMirrorsPlacement() {
        Placement placement = emptyCommonYandexPlacementWithMirrors(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyOutdoorPlacement() {
        OutdoorPlacement placement = emptyOutdoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyDeletedOutdoorPlacement() {
        OutdoorPlacement placement = emptyDeletedOutdoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyDeletedOutdoorPlacement_BackwardCompatible() {
        OutdoorPlacement placement = emptyDeletedOutdoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));

        String blocks = placementSteps.getBackwardCompatibleBlocksValue(PAGE_ID_1);
        assertThat(blocks, is("{}"));
    }

    @Test
    public void createOrUpdatePlacements_CreateOutdoorPlacementWithInternalName() {
        String expectedInternalName = "internal_name";
        placementRepository.createOrUpdatePlacements(singletonList(emptyOutdoorPlacement(PAGE_ID_1)));
        placementSteps.addOperatorInternalName(PAGE_ID_1, "internal_name");

        OutdoorPlacement placement = emptyOutdoorPlacement(PAGE_ID_2);
        placementRepository.createOrUpdatePlacements(singletonList(placement));

        List<String> actualInternalNames = placementSteps.getOperatorInternalNames(PAGE_ID_2);
        assertThat(actualInternalNames, beanDiffer(singletonList(expectedInternalName)));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyIndoorPlacement() {
        IndoorPlacement placement = emptyIndoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyDeletedIndoorPlacement() {
        IndoorPlacement placement = emptyDeletedIndoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateEmptyDeletedIndoorPlacement_BackwardCompatible() {
        IndoorPlacement placement = emptyDeletedIndoorPlacement(PAGE_ID_1);
        placementRepository.createOrUpdatePlacements(singletonList(placement));

        String blocks = placementSteps.getBackwardCompatibleBlocksValue(PAGE_ID_1);
        assertThat(blocks, is("{}"));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithEmptySizesInNewCommonPlacement() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithEmptySizes(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithEmptySizesInNewCommonPlacement_BackwardCompatible() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithEmptySizes(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));

        String blocks = placementSteps.getBackwardCompatibleBlocksValue(PAGE_ID_1);
        assertThat(blocks, is(String.format("{\"%s\": []}", BLOCK_ID_1)));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithOneSizeInNewCommonPlacement() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithOneSizeInNewCommonPlacement_BackwardCompatible() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize300x300(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));

        String blocks = placementSteps.getBackwardCompatibleBlocksValue(PAGE_ID_1);
        assertThat(blocks, is(String.format("{\"%s\": [\"300x300\"]}", BLOCK_ID_1)));
    }

    @Test
    public void createOrUpdatePlacements_CreateDeletedBlockInNewCommonPlacement() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonDeletedBlock(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateTwoBlocksInNewCommonPlacement() {
        Placement placement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                asList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        commonDeletedBlock(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithOneSizeInNewOutdoorPlacement() {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithOneSizeAndCaptionInNewOutdoorPlacement() {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1, BLOCK_ID_1_BLOCK_CAPTION)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateTwoBlocksInNewOutdoorPlacement() {
        OutdoorPlacement placement = outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockWithOneSizeInNewIndoorPlacement() {
        IndoorPlacement placement = indoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(indoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateTwoBlocksInNewIndoorPlacement() {
        IndoorPlacement placement = indoorPlacementWithBlocks(PAGE_ID_1,
                asList(indoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        indoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(placement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(placement));
    }

    @Test
    public void createOrUpdatePlacements_CreateTwoPlacementsWithBlocks() {
        Placement placement1 = commonYandexPlacementWithBlocks(PAGE_ID_1,
                asList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1),
                        commonBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2)));
        Placement placement2 = outdoorPlacementWithBlocks(PAGE_ID_2,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_2, BLOCK_ID_1)));

        placementRepository.createOrUpdatePlacements(asList(placement1, placement2));

        Placement actualPlacement1 = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement1, beanDiffer(placement1));
        Placement actualPlacement2 = placementSteps.findPlacement(PAGE_ID_2);
        assertThat(actualPlacement2, beanDiffer(placement2));
    }
}
