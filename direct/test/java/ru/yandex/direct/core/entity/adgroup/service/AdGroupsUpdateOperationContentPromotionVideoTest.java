package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.AdGroupUpdateOperationParams;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationContentPromotionVideoTest extends AdGroupsUpdateOperationTestBase {

    @Test
    public void prepareAndApply_ChangeOnlyName_NoError() {
        var adGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidName(adGroupInfo.getAdGroup(),
                ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();

        assertThat(result, isFullySuccessful());

        AdGroup realAdGroup =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(result.get(0).getResult())).get(0);
        assertThat(realAdGroup.getName(), equalTo(NEW_NAME));
    }

    @Test
    public void prepareAndApply_ChangeMinusKeywords_NoError() {
        var adGroupWithMinusKeywords = steps.contentPromotionAdGroupSteps()
                .createAdGroup(clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withMinusKeywords(singletonList("раз два три")));
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(
                adGroupWithMinusKeywords.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new ContentPromotionAdGroup()
                .withMinusKeywords(modelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS));

        assertThat("минус фразы изменены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))
                                .forFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name())).useMatcher(allOf(notNullValue(),
                                not(adGroupWithMinusKeywords.getAdGroup().getMinusKeywordsId())))));
    }

    @Test
    public void prepareAndApply_AdGroupsOfDistinctTypes_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo);
        var collectionAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                videoAdGroup.getTypedCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> videoAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        ModelChanges<AdGroup> collectionAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(collectionAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                Arrays.asList(videoAdGroupModelChanges, collectionAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_AdGroupsOfVideoTypes_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var collectionAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                videoAdGroup.getTypedCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> videoAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        ModelChanges<AdGroup> collectionAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(collectionAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                Arrays.asList(videoAdGroupModelChanges, collectionAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_TwoAdGroups_NoError() {
        var videoAdGroupFirst = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var videoAdGroupSecond = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(videoAdGroupFirst.getTypedCampaignInfo(), ContentPromotionAdgroupType.VIDEO);
        ModelChanges<AdGroup> videoAdGroupFirstModelChanges = modelChangesWithValidMinusKeywords(
                videoAdGroupFirst.getAdGroupId(), ContentPromotionAdGroup.class);
        ModelChanges<AdGroup> videoAdGroupSecondModelChanges = modelChangesWithValidMinusKeywords(
                videoAdGroupSecond.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                Arrays.asList(videoAdGroupFirstModelChanges, videoAdGroupSecondModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_ColectionPromotionTypeInDb_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var collectionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(videoAdGroup.getTypedCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> videoAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(videoAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_VideoPromotionTypeInDb_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var videoPromotionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(videoAdGroup.getTypedCampaignInfo(), ContentPromotionAdgroupType.VIDEO);
        ModelChanges<AdGroup> videoAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(videoAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_SecondGroup_NoError() {
        var videoAdGroupFirst = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var videoAdGroupSecond = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(videoAdGroupFirst.getTypedCampaignInfo(), ContentPromotionAdgroupType.VIDEO);
        ModelChanges<AdGroup> videoAdGroupModelChanges = modelChangesWithValidMinusKeywords(
                videoAdGroupFirst.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(videoAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    private AdGroupsUpdateOperation createUpdateOperation(List<ModelChanges<AdGroup>> modelChangesList) {
        return adGroupsUpdateOperationFactory.newInstance(
                Applicability.FULL,
                modelChangesList,
                AdGroupUpdateOperationParams.builder()
                        .withModerationMode(ModerationMode.DEFAULT)
                        .withValidateInterconnections(true)
                        .build(),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                clientInfo.getUid(),
                clientInfo.getClientId(),
                clientInfo.getShard());
    }
}
