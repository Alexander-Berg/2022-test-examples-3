package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmOutdoorAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.adgroup.service.bstags.AdGroupBsTagsUtils.tagNames;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmBannerAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmGeoproductAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmOutdoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeMobileAppAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activePerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;
import static ru.yandex.direct.feature.FeatureName.TARGET_TAGS_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT;
import static ru.yandex.direct.operation.Applicability.FULL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsUpdateOperationBsTagsTest extends AdGroupsUpdateOperationTestBase {
    @Autowired
    private TestAdGroupBsTagsRepository testAdGroupBsTagsRepository;

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithCollectionType_TagsSaved() {
        Long adGroupId = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")))
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.CONTENT_PROMOTION_COLLECTION_TAG))
                .withTargetTags(singletonList(TargetTagEnum.CONTENT_PROMOTION_COLLECTION_TAG)));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithVideoType_TagsSaved() {
        Long adGroupId = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")))
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.CONTENT_PROMOTION_VIDEO_TAG))
                .withTargetTags(singletonList(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG)));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithServiceType_TagsSaved() {
        Long adGroupId = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.SERVICE)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")))
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.YDO_ADGROUP_BS_TAG))
                .withTargetTags(singletonList(TargetTagEnum.YDO_ADGROUP_BS_TAG)));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithEdaType_TagsSaved() {
        Long adGroupId = steps.contentPromotionAdGroupSteps().createAdGroup(
                clientInfo, fullContentPromotionAdGroup(ContentPromotionAdgroupType.EDA)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")))
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.EDA_ADGROUP_BS_TAG))
                .withTargetTags(singletonList(TargetTagEnum.EDA_ADGROUP_BS_TAG)));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_TagsSaved() {
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeCpmGeoproductAdGroup(null)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, CpmGeoproductAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.APP_METRO_TAG))
                .withTargetTags(singletonList(TargetTagEnum.APP_METRO_TAG)));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_UpdateFromMetroToNavi_TagsSaved() {
        steps.featureSteps().addClientFeature(clientId, ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT, true);

        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeCpmGeoproductAdGroup(null)
                        .withPageGroupTags(List.of("app-metro"))
                        .withTargetTags(List.of("app-metro")),
                clientInfo)
                .getAdGroupId();

        AdGroupBsTags tagsToSet = new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.APP_NAVI_TAG))
                .withTargetTags(singletonList(TargetTagEnum.APP_NAVI_TAG));
        AdGroupBsTags expectedTags = new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.APP_NAVI_TAG))
                .withTargetTags(singletonList(TargetTagEnum.APP_NAVI_TAG));

        updateBsTagsAndCheckInDb(adGroupId, CpmGeoproductAdGroup.class, tagsToSet, expectedTags);
    }

    @Test
    public void prepareAndApply_CpmYndxFrontpageAdGroup_TagsSaved() {
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeCpmYndxFrontpageAdGroup(null)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, CpmYndxFrontpageAdGroup.class, new AdGroupBsTags()
                .withPageGroupTags(singletonList(PageGroupTagEnum.FRONTPAGE_TAG))
                .withTargetTags(singletonList(TargetTagEnum.FRONTPAGE_TAG)));
    }

    @Test
    public void prepareAndApply_CpmOutdoorAdGroup_TagsNotSaved() {
        OutdoorPlacement placement = placementSteps.addDefaultOutdoorPlacementWithOneBlock();
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeCpmOutdoorAdGroup(null, placement)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, CpmOutdoorAdGroup.class, null);
    }

    @Test
    public void prepareAndApply_CpmBannerAdGroup_TagsNotSaved() {
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeCpmBannerAdGroup(null)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, CpmBannerAdGroup.class, null);
    }

    @Test
    public void prepareAndApply_PerformanceAdGroup_TagsNotSaved() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activePerformanceAdGroup(null, feedInfo.getFeedId())
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, PerformanceAdGroup.class, null);
    }

    @Test
    public void prepareAndApply_TextAdGroup_TagsNotSaved() {
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeTextAdGroup(null)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, TextAdGroup.class, null);
    }

    @Test
    public void prepareAndApply_MobileContentAdGroup_TagsNotSaved() {
        Long adGroupId = steps.adGroupSteps().createAdGroup(
                activeMobileAppAdGroup(null)
                        .withPageGroupTags(List.of("non-existing-tag"))
                        .withTargetTags(List.of("non-existing-tag")),
                clientInfo)
                .getAdGroupId();
        testAdGroupAddCheckBsTagsInDb(adGroupId, MobileContentAdGroup.class, null);
    }

    @Test
    public void prepareAndApply_AdGroupTypeWithDefaultBsTags_DefaultBsTagsAdded() {
        enableTargetTagsAllowed();

        Long adGroupId = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO).getAdGroupId();

        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class,
                asList("page_group_tag-1", "page_group_tag-2"),
                asList("page_group_tag-1", "page_group_tag-2"),
                asList("page_group_tag-1", "page_group_tag-2", "content-promotion-video"),
                asList("page_group_tag-1", "page_group_tag-2", "content-promotion-video"));
    }

    @Test
    public void prepareAndApply_AdGroupTypeWithDefaultBsTags_DefaultBsTagsAddedWithoutDuplicates() {
        enableTargetTagsAllowed();

        Long adGroupId = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO).getAdGroupId();

        testAdGroupAddCheckBsTagsInDb(adGroupId, ContentPromotionAdGroup.class,
                asList("page_group_tag", "content-promotion-video"),
                asList("page_group_tag", "content-promotion-video"),
                asList("page_group_tag", "content-promotion-video"),
                asList("page_group_tag", "content-promotion-video"));
    }

    private void testAdGroupAddCheckBsTagsInDb(Long adGroupId,
                                               Class<? extends AdGroup> clazz,
                                               AdGroupBsTags expectedTags) {
        List<ModelChanges<AdGroup>> modelChangesList = singletonList(new ModelChanges<>(adGroupId, clazz)
                .process(emptyList(), AdGroup.PAGE_GROUP_TAGS)
                .process(emptyList(), AdGroup.TARGET_TAGS)
                .castModelUp(AdGroup.class));
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, modelChangesList,
                operatorUid, clientId, shard);
        MassResult<Long> result = updateOperation.prepareAndApply();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).matches(t -> isFullySuccessful().matches(t));
            testAdGroupBsTagsRepository
                    .softAssertionCheckAdGroupTagsInDbConsumer(soft, shard, result.get(0).getResult(), expectedTags);
        });
    }

    private void updateBsTagsAndCheckInDb(Long adGroupId,
                                          Class<? extends AdGroup> clazz,
                                          AdGroupBsTags tagsToSet,
                                          AdGroupBsTags expectedTags) {
        List<ModelChanges<AdGroup>> modelChangesList = singletonList(new ModelChanges<>(adGroupId, clazz)
                .process(tagNames(tagsToSet.getPageGroupTags()), AdGroup.PAGE_GROUP_TAGS)
                .process(tagNames(tagsToSet.getTargetTags()), AdGroup.TARGET_TAGS)
                .castModelUp(AdGroup.class));
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, modelChangesList,
                operatorUid, clientId, shard);
        MassResult<Long> result = updateOperation.prepareAndApply();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).matches(t -> isFullySuccessful().matches(t));
            testAdGroupBsTagsRepository
                    .softAssertionCheckAdGroupTagsInDbConsumer(soft, shard, result.get(0).getResult(), expectedTags);
        });
    }

    private void testAdGroupAddCheckBsTagsInDb(Long adGroupId,
                                               Class<? extends AdGroup> clazz,
                                               List<String> newPageGroupTags,
                                               List<String> newTargetTags,
                                               List<String> expectedPageGroupTags,
                                               List<String> expectedTargetTags) {
        List<ModelChanges<AdGroup>> modelChangesList = singletonList(new ModelChanges<>(adGroupId, clazz)
                .process(newPageGroupTags, AdGroup.PAGE_GROUP_TAGS)
                .process(newTargetTags, AdGroup.TARGET_TAGS)
                .castModelUp(AdGroup.class));
        AdGroupsUpdateOperation updateOperation = createUpdateOperation(FULL, modelChangesList,
                operatorUid, clientId, shard);
        MassResult<Long> result = updateOperation.prepareAndApply();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).matches(t -> isFullySuccessful().matches(t));
            testAdGroupBsTagsRepository
                    .softAssertionCheckAdGroupTagsInDbConsumer(soft, shard, result.get(0).getResult(),
                            expectedPageGroupTags, expectedTargetTags);
        });
    }

    private void enableTargetTagsAllowed() {
        steps.featureSteps().addClientFeature(clientId, TARGET_TAGS_ALLOWED, true);
    }
}
