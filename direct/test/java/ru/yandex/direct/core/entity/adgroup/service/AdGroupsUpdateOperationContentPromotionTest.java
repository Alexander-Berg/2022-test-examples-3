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
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.contentPromotionAdGroupContentTypeChanged;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationContentPromotionTest extends AdGroupsUpdateOperationTestBase {

    @Test
    public void prepareAndApply_CollectionType_ChangeOnlyName_NoError() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
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
    public void prepareAndApply_VideoType_ChangeOnlyName_NoError() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
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
    public void prepareAndApply_CollectionType_ChangeMinusKeywords_NoError() {
        var adGroupInfoWithMinusKeywords = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION)
                        .withMinusKeywords(singletonList("раз два три")));
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(
                adGroupInfoWithMinusKeywords.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupInfoWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new ContentPromotionAdGroup()
                .withMinusKeywords(modelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS));

        assertThat("минус фразы изменены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))
                                .forFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name())).useMatcher(allOf(notNullValue(),
                                not(adGroupInfoWithMinusKeywords.getAdGroup().getMinusKeywordsId())))));
    }

    @Test
    public void prepareAndApply_VideoType_ChangeMinusKeywords_NoError() {
        var adGroupInfoWithMinusKeywords = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withMinusKeywords(singletonList("раз два три")));
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(
                adGroupInfoWithMinusKeywords.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());

        AdGroup actualAdGroup =
                adGroupRepository.getAdGroups(shard, singletonList(adGroupInfoWithMinusKeywords.getAdGroupId())).get(0);
        AdGroup expected = new ContentPromotionAdGroup()
                .withMinusKeywords(modelChanges.getChangedProp(AdGroup.MINUS_KEYWORDS));

        assertThat("минус фразы изменены успешно", actualAdGroup,
                beanDiffer(expected).useCompareStrategy(
                        onlyFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name()), newPath(AdGroup.MINUS_KEYWORDS.name()))
                                .forFields(newPath(AdGroup.MINUS_KEYWORDS_ID.name())).useMatcher(allOf(notNullValue(),
                                not(adGroupInfoWithMinusKeywords.getAdGroup().getMinusKeywordsId())))));
    }

    @Test
    public void prepareAndApply_VideoType_ChangeContentType_ValidationError() {
        var adGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
        ModelChanges<ContentPromotionAdGroup> modelChanges = new ModelChanges<ContentPromotionAdGroup>(
                adGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        modelChanges.process(ContentPromotionAdgroupType.COLLECTION, ContentPromotionAdGroup.CONTENT_PROMOTION_TYPE);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges.castModelUp(AdGroup.class)));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(), contains(validationError(path(index(0),
                field(ContentPromotionAdGroup.CONTENT_PROMOTION_TYPE)), contentPromotionAdGroupContentTypeChanged())));
    }

    @Test
    public void prepareAndApply_CollectionType_ChangeContentType_ValidationError() {
        var adGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<ContentPromotionAdGroup> modelChanges = new ModelChanges<ContentPromotionAdGroup>(
                adGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        modelChanges.process(ContentPromotionAdgroupType.VIDEO, ContentPromotionAdGroup.CONTENT_PROMOTION_TYPE);
        AdGroupsUpdateOperation updateOperation =
                createUpdateOperation(Applicability.FULL, singletonList(modelChanges.castModelUp(AdGroup.class)));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(), contains(validationError(path(index(0),
                field(ContentPromotionAdGroup.CONTENT_PROMOTION_TYPE)), contentPromotionAdGroupContentTypeChanged())));
    }

    @Test
    public void prepareAndApply_AdGroupsOfDistinctTypes_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(clientInfo,
                ContentPromotionAdgroupType.VIDEO);
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
    public void prepareAndApply_VideoType_ColectionTypeInDb_NoError() {
        var videoAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        var collectionAdGroup = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(
                videoAdGroup.getTypedCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> videoAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(videoAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_CollectionType_VideoTypeInDb_NoError() {
        AdGroupInfo videoAdGroup = adGroupSteps
                .createDefaultContentPromotionAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        AdGroupInfo collectionAdGroup = adGroupSteps.createDefaultContentPromotionAdGroup(
                videoAdGroup.getCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> collectionAdGroupModelChanges =
                modelChangesWithValidMinusKeywords(collectionAdGroup.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(collectionAdGroupModelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_VideoType_VideoTypeInDb_NoError() {
        AdGroupInfo videoAdGroupFirst = adGroupSteps
                .createDefaultContentPromotionAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        AdGroupInfo videoAdGroupSecond = adGroupSteps.createDefaultContentPromotionAdGroup(
                videoAdGroupFirst.getCampaignInfo(), ContentPromotionAdgroupType.VIDEO);
        ModelChanges<AdGroup> modelChanges =
                modelChangesWithValidMinusKeywords(videoAdGroupFirst.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(modelChanges));
        MassResult<Long> result = updateOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_CollectionType_CollectionTypeInDb_NoError() {
        AdGroupInfo collectionAdGroupFirst = adGroupSteps
                .createDefaultContentPromotionAdGroup(clientInfo, ContentPromotionAdgroupType.COLLECTION);
        AdGroupInfo collectionAdGroupSecond = adGroupSteps.createDefaultContentPromotionAdGroup(
                collectionAdGroupFirst.getCampaignInfo(), ContentPromotionAdgroupType.COLLECTION);
        ModelChanges<AdGroup> modelChanges = modelChangesWithValidMinusKeywords(
                collectionAdGroupFirst.getAdGroupId(), ContentPromotionAdGroup.class);
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(Applicability.FULL,
                singletonList(modelChanges));
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
