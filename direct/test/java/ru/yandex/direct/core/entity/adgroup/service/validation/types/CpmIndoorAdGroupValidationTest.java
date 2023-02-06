package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
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
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmIndoorAdGroupValidation.INDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
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
public class CpmIndoorAdGroupValidationTest {

    private static final Long INDOOR_PAGE_ID_1 = 7L;
    private static final Long INDOOR_PAGE_ID_2 = 19L;
    private static final Long UNTYPED_PAGE_ID = 15L;

    private static final Long INDOOR_PAGE_1_BLOCK_ID_1 = 4L;
    private static final Long INDOOR_PAGE_1_BLOCK_ID_2 = 8L;

    private static final Long INDOOR_PAGE_2_BLOCK_ID_1 = 5L;
    private static final Long INDOOR_PAGE_2_BLOCK_ID_2 = INDOOR_PAGE_1_BLOCK_ID_2;
    private static final Long INDOOR_PAGE_2_BLOCK_ID_DELETED_3 = 19L;

    private static final Long UNTYPED_PAGE_BLOCK_ID_1 = 23L;
    private static final Long UNTYPED_PAGE_BLOCK_ID_2 = 59L;

    @Autowired
    private Steps steps;

    @Autowired
    private CpmIndoorAdGroupValidation validation;

    private IndoorPlacement indoorPlacement;

    @Before
    public void before() {
        IndoorPlacement indoorPlacement1 = indoorPlacementWithBlocks(INDOOR_PAGE_ID_1,
                asList(
                        indoorBlockWithOneSize(INDOOR_PAGE_ID_1, INDOOR_PAGE_1_BLOCK_ID_1),
                        indoorBlockWithTwoSizes(INDOOR_PAGE_ID_1, INDOOR_PAGE_1_BLOCK_ID_2)));
        IndoorPlacement indoorPlacement2 = indoorPlacementWithBlocks(INDOOR_PAGE_ID_2,
                asList(
                        indoorBlockWithOneSize(INDOOR_PAGE_ID_2, INDOOR_PAGE_2_BLOCK_ID_1),
                        indoorBlockWithTwoSizes(INDOOR_PAGE_ID_2, INDOOR_PAGE_2_BLOCK_ID_2),
                        indoorBlockWithTwoSizes(INDOOR_PAGE_ID_2, INDOOR_PAGE_2_BLOCK_ID_DELETED_3)));
        Placement untypedPlacement = commonYandexPlacementWithBlocks(UNTYPED_PAGE_ID,
                asList(
                        indoorBlockWithOneSize(UNTYPED_PAGE_ID, UNTYPED_PAGE_BLOCK_ID_1),
                        indoorBlockWithTwoSizes(UNTYPED_PAGE_ID, UNTYPED_PAGE_BLOCK_ID_2)));
        steps.placementSteps().clearPlacements();
        steps.placementSteps().addPlacement(indoorPlacement1);
        steps.placementSteps().addPlacement(indoorPlacement2);
        steps.placementSteps().addPlacement(untypedPlacement);
        indoorPlacement = indoorPlacement1;
    }

// geo

    @Test
    public void validGeo() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withGeo(INDOOR_GEO_DEFAULT);
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void invalidGeo() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withGeo(singletonList(BY_REGION_ID));
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmIndoorAdGroup.GEO)), notAllowedValue()))));
    }

// minus words

    @Test
    public void withNullMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withMinusKeywords(null);
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withEmptyMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withMinusKeywords(emptyList());
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withMinusKeywords(asList("minus1", "minus2"));
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmIndoorAdGroup.MINUS_KEYWORDS)), minusKeywordsNotAllowed()))));
    }

    @Test
    public void withNullLibraryMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withLibraryMinusKeywordsIds(null);
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withEmptyLibraryMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withLibraryMinusKeywordsIds(emptyList());
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void withLibraryMinusKeywords() {
        CpmIndoorAdGroup cpmIndoorAdGroup = clientCpmIndoorAdGroup(null, indoorPlacement)
                .withLibraryMinusKeywordsIds(singletonList(1L));
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result = validate(cpmIndoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0), field(CpmIndoorAdGroup.LIBRARY_MINUS_KEYWORDS_IDS)),
                        minusKeywordsNotAllowed()))));
    }

// placements

    @Test
    public void noErrorsForTwoAdGroupsWithPageBlocksFromDifferentPages() {
        CpmIndoorAdGroup cpmIndoorAdGroup1 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(singletonList(
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_1)
                                .withImpId(INDOOR_PAGE_1_BLOCK_ID_1)));
        CpmIndoorAdGroup cpmIndoorAdGroup2 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(singletonList(
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_2)
                                .withImpId(INDOOR_PAGE_2_BLOCK_ID_2)));
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result =
                validate(asList(cpmIndoorAdGroup1, cpmIndoorAdGroup2));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noErrorsForDuplicatesInDifferentAdGroups() {
        CpmIndoorAdGroup cpmIndoorAdGroup1 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(INDOOR_PAGE_ID_1)
                        .withImpId(INDOOR_PAGE_1_BLOCK_ID_1)));
        CpmIndoorAdGroup cpmIndoorAdGroup2 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(INDOOR_PAGE_ID_1)
                        .withImpId(INDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<List<CpmIndoorAdGroup>, Defect> result =
                validate(asList(cpmIndoorAdGroup1, cpmIndoorAdGroup2));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void unexistingAndBadTypePagesAndBlocksInDifferentAdGroupsCombo() {
        CpmIndoorAdGroup cpmIndoorAdGroup1 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_1)
                                .withImpId(INDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_1)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_1)
                                .withImpId(INDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(200L)
                                .withImpId(INDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_2)
                                .withImpId(INDOOR_PAGE_2_BLOCK_ID_2)));
        CpmIndoorAdGroup cpmIndoorAdGroup2 = clientCpmIndoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(200L)
                                .withImpId(INDOOR_PAGE_1_BLOCK_ID_2),
                        new PageBlock()
                                .withPageId(201L)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(INDOOR_PAGE_ID_2)
                                .withImpId(INDOOR_PAGE_2_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(UNTYPED_PAGE_ID)
                                .withImpId(UNTYPED_PAGE_BLOCK_ID_1)));

        ValidationResult<List<CpmIndoorAdGroup>, Defect> result =
                validate(asList(cpmIndoorAdGroup1, cpmIndoorAdGroup2));

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

    private ValidationResult<List<CpmIndoorAdGroup>, Defect> validate(CpmIndoorAdGroup adGroup) {
        return validate(singletonList(adGroup));
    }

    private ValidationResult<List<CpmIndoorAdGroup>, Defect> validate(List<CpmIndoorAdGroup> adGroups) {
        return validation.validateAdGroups(ClientId.fromLong(0L), adGroups);
    }

    private Path pageIdPath(int adGroupIndex, int blockIndex) {
        return path(index(adGroupIndex),
                field(CpmIndoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field(PageBlock.PAGE_ID));
    }

    @SuppressWarnings("SameParameterValue")
    private Path blockIdPath(int adGroupIndex, int blockIndex) {
        return path(index(adGroupIndex),
                field(CpmIndoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field(PageBlock.IMP_ID));
    }
}
