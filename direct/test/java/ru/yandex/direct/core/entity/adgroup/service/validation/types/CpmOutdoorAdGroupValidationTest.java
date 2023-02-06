package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.minusKeywordsNotAllowed;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notAllowedValue;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmOutdoorAdGroupValidation.OUTDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.regions.Region.BY_REGION_ID;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CpmOutdoorAdGroupValidationTest {

    private static final Long OUTDOOR_PAGE_ID_1 = 7L;
    private static final Long OUTDOOR_PAGE_ID_2 = 19L;
    private static final Long UNTYPED_PAGE_ID = 15L;

    private static final Long OUTDOOR_PAGE_1_BLOCK_ID_1 = 4L;
    private static final Long OUTDOOR_PAGE_1_BLOCK_ID_2 = 8L;

    private static final Long OUTDOOR_PAGE_2_BLOCK_ID_1 = 5L;
    private static final Long OUTDOOR_PAGE_2_BLOCK_ID_2 = OUTDOOR_PAGE_1_BLOCK_ID_2;
    private static final Long OUTDOOR_PAGE_2_BLOCK_ID_DELETED_3 = 19L;

    private static final Long UNTYPED_PAGE_BLOCK_ID_1 = 23L;
    private static final Long UNTYPED_PAGE_BLOCK_ID_2 = 59L;

    @Autowired
    private Steps steps;

    @Autowired
    private CpmOutdoorAdGroupValidation validation;

    private OutdoorPlacement outdoorPlacement;

    @Before
    public void before() {
        OutdoorPlacement outdoorPlacement1 = outdoorPlacementWithBlocks(OUTDOOR_PAGE_ID_1,
                asList(
                        outdoorBlockWithOneSize(OUTDOOR_PAGE_ID_1, OUTDOOR_PAGE_1_BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(OUTDOOR_PAGE_ID_1, OUTDOOR_PAGE_1_BLOCK_ID_2)));
        OutdoorPlacement outdoorPlacement2 = outdoorPlacementWithBlocks(OUTDOOR_PAGE_ID_2,
                asList(
                        outdoorBlockWithOneSize(OUTDOOR_PAGE_ID_2, OUTDOOR_PAGE_2_BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(OUTDOOR_PAGE_ID_2, OUTDOOR_PAGE_2_BLOCK_ID_2),
                        outdoorBlockWithTwoSizes(OUTDOOR_PAGE_ID_2, OUTDOOR_PAGE_2_BLOCK_ID_DELETED_3)));
        Placement untypedPlacement = commonYandexPlacementWithBlocks(UNTYPED_PAGE_ID,
                asList(
                        outdoorBlockWithOneSize(UNTYPED_PAGE_ID, UNTYPED_PAGE_BLOCK_ID_1),
                        outdoorBlockWithTwoSizes(UNTYPED_PAGE_ID, UNTYPED_PAGE_BLOCK_ID_2)));
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addPlacement(outdoorPlacement1);
        steps.placementSteps().addPlacement(outdoorPlacement2);
        steps.placementSteps().addPlacement(untypedPlacement);
        outdoorPlacement = outdoorPlacement1;
    }

// geo

    @Test
    public void validGeo() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withGeo(OUTDOOR_GEO_DEFAULT);
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void invalidGeo() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withGeo(singletonList(BY_REGION_ID));
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmOutdoorAdGroup.GEO)), notAllowedValue()))));
    }

// minus words

    @Test
    public void withNullMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withMinusKeywords(null);
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withEmptyMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withMinusKeywords(emptyList());
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withMinusKeywords(asList("minus1", "minus2"));
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmOutdoorAdGroup.MINUS_KEYWORDS)), minusKeywordsNotAllowed()))));
    }

    @Test
    public void withNullLibraryMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withLibraryMinusKeywordsIds(null);
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withEmptyLibraryMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withLibraryMinusKeywordsIds(emptyList());
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withLibraryMinusKeywords() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, outdoorPlacement)
                .withLibraryMinusKeywordsIds(singletonList(1L));
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmOutdoorAdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        minusKeywordsNotAllowed()))));
    }

// placements

    @Test
    public void noErrorsForTwoAdGroupsWithPageBlocksFromDifferentPages() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup1 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        CpmOutdoorAdGroup cpmOutdoorAdGroup2 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_2)
                                .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_2)));
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result =
                validate(asList(cpmOutdoorAdGroup1, cpmOutdoorAdGroup2));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noErrorsForDuplicatesInDifferentAdGroups() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup1 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        CpmOutdoorAdGroup cpmOutdoorAdGroup2 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result =
                validate(asList(cpmOutdoorAdGroup1, cpmOutdoorAdGroup2));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void unexistingAndBadTypePagesAndBlocksInDifferentAdGroupsCombo() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup1 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(200L)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_2)
                                .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_2)));
        CpmOutdoorAdGroup cpmOutdoorAdGroup2 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(200L)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(201L)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_2)
                                .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(UNTYPED_PAGE_ID)
                                .withImpId(UNTYPED_PAGE_BLOCK_ID_1)));

        ValidationResult<List<CpmOutdoorAdGroup>, Defect> result =
                validate(asList(cpmOutdoorAdGroup1, cpmOutdoorAdGroup2));

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(blockIdPath(0, 1), objectNotFound()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0, 3), objectNotFound()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(1, 0), objectNotFound()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(1, 1), objectNotFound()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(1, 3), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(5);
    }

    private ValidationResult<List<CpmOutdoorAdGroup>, Defect> validate(CpmOutdoorAdGroup adGroup) {
        return validate(singletonList(adGroup));
    }

    private ValidationResult<List<CpmOutdoorAdGroup>, Defect> validate(List<CpmOutdoorAdGroup> adGroups) {
        return validation.validateAdGroups(ClientId.fromLong(0L), adGroups);
    }

    private Path pageIdPath(int adGroupIndex, int blockIndex) {
        return path(index(adGroupIndex),
                field(CpmOutdoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field("pageId"));
    }

    @SuppressWarnings("SameParameterValue")
    private Path blockIdPath(int adGroupIndex, int blockIndex) {
        return path(index(adGroupIndex),
                field(CpmOutdoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field("impId"));
    }
}
