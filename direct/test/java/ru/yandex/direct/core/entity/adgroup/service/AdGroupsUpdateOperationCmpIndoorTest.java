package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notAllowedValue;
import static ru.yandex.direct.core.entity.adgroup.service.validation.types.CpmIndoorAdGroupValidation.INDOOR_GEO_DEFAULT;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorBlockWithOneSize;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithBlocks;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationCmpIndoorTest extends AdGroupsUpdateOperationTestBase {

    public static final String NEW_NAME = "имя";

    @Test
    public void prepareAndApply_ChangeName_SuccessfulChanging() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmIndoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithName(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));

        AdGroup expectedGroup = new CpmIndoorAdGroup()
                .withGeo(INDOOR_GEO_DEFAULT)
                .withMinusKeywords(emptyList())
                .withName(NEW_NAME)
                .withPageBlocks(((CpmIndoorAdGroup) adGroupInfo.getAdGroup()).getPageBlocks())
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);

        assertThat(actualAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ChangePageBlocks_SuccessfulChanging() {
        placementSteps.clearPlacements();
        final long pageId = 5879L;
        final long blockId = 5L;
        IndoorPlacement placement = indoorPlacementWithBlocks(pageId,
                singletonList(indoorBlockWithOneSize(pageId, blockId)));
        placementSteps.addPlacement(placement);

        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmIndoorAdGroup();

        List<PageBlock> newPageBlocks = singletonList(new PageBlock().withPageId(pageId).withImpId(blockId));
        ModelChanges<CpmIndoorAdGroup> modelChanges =
                new ModelChanges<>(adGroupInfo.getAdGroupId(), CpmIndoorAdGroup.class);
        modelChanges.process(newPageBlocks, CpmIndoorAdGroup.PAGE_BLOCKS);

        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(FULL, singletonList(modelChanges.castModelUp(AdGroup.class)), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        AdGroup expectedGroup = new CpmIndoorAdGroup()
                .withGeo(INDOOR_GEO_DEFAULT)
                .withMinusKeywords(emptyList())
                .withName(adGroupInfo.getAdGroup().getName())
                .withPageBlocks(newPageBlocks)
                .withStatusBsSynced(StatusBsSynced.NO)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(result.get(0).getResult())).get(0);
        assertThat(actualAdGroup, beanDiffer(expectedGroup).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void prepareAndApply_ChangeGeo_ValidationError() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultCpmIndoorAdGroup();
        ModelChanges<AdGroup> modelChanges = modelChangesWithGeo(adGroupInfo.getAdGroup());

        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, singletonList(modelChanges), adGroupInfo);
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(),
                hasDefectWithDefinition(validationError(path(index(0), field(AdGroup.GEO)), notAllowedValue())));
    }

    private ModelChanges<AdGroup> modelChangesWithName(AdGroup adGroup) {
        ModelChanges<AdGroup> modelChanges = new ModelChanges<>(adGroup.getId(), AdGroup.class);
        modelChanges.process(NEW_NAME, AdGroup.NAME);
        return modelChanges;
    }
}
