package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.entity.placements.model1.Placement;
import ru.yandex.direct.core.entity.placements.model1.PlacementType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.clientCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.commonYandexPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorBlockWithTwoSizes;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorPlacementWithBlocks;
import static ru.yandex.direct.core.testing.data.TestPlacements.outdoorTestingPlacement;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupPageBlocksValidatorTest {

    private static final int PAGE_BLOCKS_MAX = 100;

    private static final Long OUTDOOR_PAGE_ID_1 = 7L;
    private static final Long OUTDOOR_PAGE_ID_2 = 19L;
    private static final Long OUTDOOR_PAGE_ID_3 = 20L;
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
    private AdGroupPageBlocksValidatorFactory adGroupPageBlocksValidatorFactory;

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
    }

// valid placements

    @Test
    public void noErrorsForOnePageBlock() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noErrorsForTwoPageBlocksFromOnePage() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_2)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noErrorsForTwoPageBlocksFromDifferentPages() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_2)
                                .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_2)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void noErrorsForTestingPageForClientWithEnabledFeature() {
        OutdoorPlacement testingPlacement = outdoorTestingPlacement(OUTDOOR_PAGE_ID_3,
                outdoorBlockWithOneSize(OUTDOOR_PAGE_ID_3, OUTDOOR_PAGE_1_BLOCK_ID_1));
        steps.placementSteps().addPlacement(testingPlacement);
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_3)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = adGroupPageBlocksValidatorFactory
                .createValidator(singletonList(cpmOutdoorAdGroup), CpmOutdoorAdGroup.PAGE_BLOCKS, PlacementType.OUTDOOR,
                        PAGE_BLOCKS_MAX, true).apply(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

// invalid placements

    @Test
    public void nullPageBlocks() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(null);
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS)), notNull()))));
    }

    @Test
    public void emptyPageBlocks() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(emptyList());
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS)),
                        notEmptyCollection()))));
    }

    @Test
    public void tooManyPageBlocks() {
        final int max = PAGE_BLOCKS_MAX;
        List<PageBlock> pageBlocks = IntStream.range(0, max + 1)
                .mapToObj(i -> new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1))
                .collect(toList());
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(pageBlocks);
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS)),
                        maxCollectionSize(max)))));
    }

    @Test
    public void nullItemInPageBlocks() {
        List<PageBlock> pageBlocks = new ArrayList<>();
        pageBlocks.add(null);
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(pageBlocks);
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS), index(0)),
                        notNull()))));
    }

    @Test
    public void nullPageId() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(null)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0), notNull()))));
    }

    @Test
    public void nullBlockId() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(null)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(impIdPath(0), notNull()))));
    }

    @Test
    public void duplicatedPageBlocks() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup1 = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup1);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS), index(0)),
                        duplicatedElement()))));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(CpmOutdoorAdGroup.PAGE_BLOCKS), index(1)),
                        duplicatedElement()))));
        assertThat(result.flattenErrors()).hasSize(2);
    }

    @Test
    public void unexistingPageAndNoPlacementsAtDatabase() {
        steps.placementSteps().clearPlacements();
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0), objectNotFound()))));
    }

    @Test
    public void unexistingPage() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(200L)
                        .withImpId(100L)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void existingPagesAndUnexistingPage() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(200L)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_2)
                                .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_2)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(1), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void unexistingBlock() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(100L)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(impIdPath(0), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void blockFromAnotherPage() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_1)
                        .withImpId(OUTDOOR_PAGE_2_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(impIdPath(0), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void existingBlocksAndUnexistingBlock() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(asList(
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(100L),
                        new PageBlock()
                                .withPageId(OUTDOOR_PAGE_ID_1)
                                .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_2)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(impIdPath(1), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void badTypePage() {
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(UNTYPED_PAGE_ID)
                        .withImpId(UNTYPED_PAGE_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = validate(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    @Test
    public void testingPageWithoutEnabledFeature() {
        OutdoorPlacement testingPlacement = outdoorTestingPlacement(OUTDOOR_PAGE_ID_3,
                outdoorBlockWithOneSize(OUTDOOR_PAGE_ID_3, OUTDOOR_PAGE_1_BLOCK_ID_1));
        steps.placementSteps().addPlacement(testingPlacement);
        CpmOutdoorAdGroup cpmOutdoorAdGroup = clientCpmOutdoorAdGroup(null, null)
                .withPageBlocks(singletonList(new PageBlock()
                        .withPageId(OUTDOOR_PAGE_ID_3)
                        .withImpId(OUTDOOR_PAGE_1_BLOCK_ID_1)));
        ValidationResult<CpmOutdoorAdGroup, Defect> result = adGroupPageBlocksValidatorFactory
                .createValidator(singletonList(cpmOutdoorAdGroup), CpmOutdoorAdGroup.PAGE_BLOCKS, PlacementType.OUTDOOR,
                        PAGE_BLOCKS_MAX, false).apply(cpmOutdoorAdGroup);
        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(pageIdPath(0), objectNotFound()))));
        assertThat(result.flattenErrors()).hasSize(1);
    }

    private ValidationResult<CpmOutdoorAdGroup, Defect> validate(CpmOutdoorAdGroup adGroup) {
        return validate(adGroup, CpmOutdoorAdGroup.PAGE_BLOCKS, PlacementType.OUTDOOR);
    }

    private <T extends AdGroup, TPropHolder extends Model> ValidationResult<T, Defect> validate(
            T adGroup, ModelProperty<TPropHolder, List<PageBlock>> pageBlocksProperty,
            PlacementType allowedPlacementType) {
        AdGroupPageBlocksValidator<T, TPropHolder> validator =
                createValidator(singletonList(adGroup), pageBlocksProperty, allowedPlacementType);
        return validator.apply(adGroup);
    }

    private <T extends AdGroup, TPropHolder extends Model> AdGroupPageBlocksValidator<T, TPropHolder> createValidator(
            List<T> adGroups, ModelProperty<TPropHolder, List<PageBlock>> pageBlocksProperty,
            PlacementType allowedPlacementType) {
        return adGroupPageBlocksValidatorFactory
                .createValidator(adGroups, pageBlocksProperty, allowedPlacementType, PAGE_BLOCKS_MAX, false);
    }

    private Path pageIdPath(int blockIndex) {
        return path(field(CpmOutdoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field(PageBlock.PAGE_ID));
    }

    @SuppressWarnings("SameParameterValue")
    private Path impIdPath(int blockIndex) {
        return path(field(CpmOutdoorAdGroup.PAGE_BLOCKS),
                index(blockIndex),
                field(PageBlock.IMP_ID));
    }

}
