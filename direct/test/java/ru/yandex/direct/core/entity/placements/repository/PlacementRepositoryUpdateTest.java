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
import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.PlacementSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithOneSize300x300;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.copyIndoorPlacementForceSetGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.copyOutdoorPlacementForceSetGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyCommonYandexPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.emptyOutdoorPlacement;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSizeWithoutGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizesWithoutGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorDeletedBlock;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorDeletedBlockWithoutGeo;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;

@CoreTest
@RunWith(SpringRunner.class)
public class PlacementRepositoryUpdateTest {

    private static final Long PAGE_ID_1 = 2L;
    private static final Long PAGE_ID_2 = 7L;
    private static final Long BLOCK_ID_1 = 1L;
    private static final Long BLOCK_ID_2 = 3L;
    private static final String BLOCK_ID_1_BLOCK_CAPTION = "Test block caption of " + BLOCK_ID_1.toString();
    private static final String ANOTHER_BLOCK_CAPTION = "Another block caption";

    @Autowired
    private PlacementSteps placementSteps;

    @Autowired
    private PlacementRepository placementRepository;

    @Before
    public void before() {
        placementSteps.clearPlacements();
    }

    @Test
    public void createOrUpdatePlacements_UpdateEmptyCommonYandexPlacement() {
        placementSteps.addPlacement(emptyCommonYandexPlacement(PAGE_ID_1));
        Placement newPlacement = new Placement<>(PAGE_ID_1, null,
                "ya_new.ru", "new caption", "yandex", null, true, true, true, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateEmptyCommonYandexPlacementAddMirrors() {
        placementSteps.addPlacement(emptyCommonYandexPlacement(PAGE_ID_1));

        Placement newPlacement = new Placement<>(PAGE_ID_1, null,
                "ya_new.ru", "new caption", "yandex", null, true, true, true,
                emptyList(), List.of("mirror1.ru", "mirror2.ua"));

        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateEmptyOutdoorPlacement() {
        placementSteps.addPlacement(emptyOutdoorPlacement(PAGE_ID_1));
        Placement newPlacement = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "new-site.ru", "updated caption", "new-site", null, false, true, true, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_LoginChangedAfterUpdate() {
        Placement oldPlacement = new Placement<>(PAGE_ID_1, null,
                "old-site.ru", "old caption", "old-login", null, false, false, false, emptyList(), List.of());
        placementSteps.addPlacement(oldPlacement);
        Placement newPlacement = new Placement<>(PAGE_ID_1, null,
                "old-site.ru", "old caption", "new-login", null, false, false, false, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_ChangeTypeNullToNonnull() {
        Placement oldPlacement = new Placement<>(PAGE_ID_1, null,
                "old-site.ru", "old caption", "old-site", null, false, false, false, emptyList(), List.of());
        placementSteps.addPlacement(oldPlacement);
        Placement newPlacement = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "new-site.ru", "updated caption", "old-site", null, false, false, false, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createOrUpdatePlacements_ChangeTypeNonnullToNull_ThrowsException() {
        Placement oldPlacement = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "old-site.ru", "old caption", "old-site", null, false, false, false, emptyList(), List.of());
        placementSteps.addPlacement(oldPlacement);
        Placement newPlacement = new Placement<>(PAGE_ID_1, null,
                "new-site.ru", "updated caption", "new-site", null, false, true, true, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createOrUpdatePlacements_ChangeTypeNonnullToAnother_ThrowsException() {
        Placement oldPlacement = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "old-site.ru", "old caption", "old-site", null, false, false, false, emptyList(), List.of());
        placementSteps.addPlacement(oldPlacement);
        Placement newPlacement = new Placement<>(PAGE_ID_1, PlacementType.INDOOR,
                "new-site.ru", "updated caption", "new-site", null, false, true, true, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createOrUpdatePlacements_ChangeTypeNonnullToAnotherAndValidChangesPresent_ThrowsException() {
        Placement oldPlacement1 = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "old-site.ru", "old caption", "old-site", null, false, false, false, emptyList(), List.of());
        Placement oldPlacement2 = new Placement<>(PAGE_ID_2, PlacementType.OUTDOOR,
                "old-site.ru", "old caption", "old-site", null, false, false, false, emptyList(), List.of());
        placementSteps.addPlacements(oldPlacement1, oldPlacement2);
        Placement newPlacement1 = new Placement<>(PAGE_ID_1, PlacementType.OUTDOOR,
                "new-site.ru", "updated caption", "new-site", null, false, true, true, emptyList(), List.of());
        Placement newPlacement2 = new Placement<>(PAGE_ID_2, PlacementType.INDOOR,
                "new-site.ru", "updated caption", "new-site", null, false, true, true, emptyList(), List.of());
        placementRepository.createOrUpdatePlacements(asList(newPlacement1, newPlacement2));
    }

    @Test
    public void createOrUpdatePlacements_CreateBlockInExistingEmptyCommonPlacement() {
        placementSteps.addPlacement(emptyCommonYandexPlacement(PAGE_ID_1));
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateBlockInExistingCommonPlacement() {
        placementSteps.addPlacement(commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1))));
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(newPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateBlockInExistingCommonPlacement_BackwardCompatible() {
        placementSteps.addPlacement(commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1))));
        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize300x300(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        String blocks = placementSteps.getBackwardCompatibleBlocksValue(PAGE_ID_1);
        assertThat(blocks, is(String.format("{\"%s\": [\"300x300\"]}", BLOCK_ID_1)));
    }

    @Test
    public void createOrUpdatePlacements_UpdateBlockToDeletedStateInExistingOutdoorPlacement() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(sourcePlacement);

        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorDeletedBlock(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId());

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateBlockToDeletedWithCaptionStateInExistingOutdoorPlacement() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1, BLOCK_ID_1_BLOCK_CAPTION)));
        placementSteps.addPlacement(sourcePlacement);

        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorDeletedBlock(PAGE_ID_1, BLOCK_ID_1, ANOTHER_BLOCK_CAPTION)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId());

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_DoesntCleanGeoWhenUpdateBlockToDeletedState() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(sourcePlacement);

        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorDeletedBlockWithoutGeo(PAGE_ID_1, BLOCK_ID_1)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId());
        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_CreateAndUpdateBlocksInExistingOutdoorPlacement() {
        OutdoorPlacement sourcePlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(outdoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(sourcePlacement);

        OutdoorPlacement newPlacement = outdoorPlacementWithBlocks(PAGE_ID_1,
                asList(outdoorBlockWithOneSizeWithoutGeo(PAGE_ID_1, BLOCK_ID_1),
                        outdoorBlockWithTwoSizesWithoutGeo(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        OutdoorPlacement expectedPlacement = copyOutdoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId(), null);

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_CreateAndUpdateBlocksInExistingIndoorPlacement() {
        IndoorPlacement sourcePlacement = indoorPlacementWithBlocks(PAGE_ID_1,
                singletonList(indoorBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1)));
        placementSteps.addPlacement(sourcePlacement);

        IndoorPlacement newPlacement = indoorPlacementWithBlocks(PAGE_ID_1,
                asList(indoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1),
                        indoorBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_2)));
        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        IndoorPlacement expectedPlacement = copyIndoorPlacementForceSetGeo(newPlacement,
                sourcePlacement.getBlocks().get(0).getGeoId(),
                newPlacement.getBlocks().get(1).getGeoId());

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_1);
        assertThat(actualPlacement, beanDiffer(expectedPlacement));
    }

    @Test
    public void createOrUpdatePlacements_UpdateBlockInExistingCommonPlacement_DontTouchOtherPlacement() {
        placementSteps.addPlacement(commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithOneSize(PAGE_ID_1, BLOCK_ID_1))));

        Placement untouchedPlacement = commonYandexPlacementWithBlocks(PAGE_ID_2,
                singletonList(commonBlockWithTwoSizes(PAGE_ID_2, BLOCK_ID_1)));
        placementSteps.addPlacement(untouchedPlacement);

        Placement newPlacement = commonYandexPlacementWithBlocks(PAGE_ID_1,
                singletonList(commonBlockWithTwoSizes(PAGE_ID_1, BLOCK_ID_1)));

        placementRepository.createOrUpdatePlacements(singletonList(newPlacement));

        Placement actualPlacement = placementSteps.findPlacement(PAGE_ID_2);
        assertThat(actualPlacement, beanDiffer(untouchedPlacement));
    }
}
