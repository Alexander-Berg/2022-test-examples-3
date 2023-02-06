package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;
import java.util.function.Supplier;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.placements.model1.OutdoorPlacement;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestGroups.activeContentPromotionAdGroup;
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
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationBsTagsTest extends AdGroupsAddOperationTestBase {
    @Autowired
    private TestAdGroupBsTagsRepository testAdGroupBsTagsRepository;

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithCollectionType_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
            return fullContentPromotionAdGroup(ContentPromotionAdgroupType.COLLECTION).withCampaignId(campaignId);
        }, asList("content-promotion-collection"), asList("content-promotion-collection"));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithVideoType_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
            return fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO).withCampaignId(campaignId);
        }, asList("content-promotion-video"), asList("content-promotion-video"));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithServiceType_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
            return fullContentPromotionAdGroup(ContentPromotionAdgroupType.SERVICE).withCampaignId(campaignId);
        }, asList("yndx-services"), asList("yndx-services"));
    }

    @Test
    public void prepareAndApply_ContentPromotionAdGroup_WithEdaType_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
            return fullContentPromotionAdGroup(ContentPromotionAdgroupType.EDA).withCampaignId(campaignId);
        }, asList("yndx-eda"), asList("yndx-eda"));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_NullBsTags_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmGeoproductAdGroup(campaignId)
                    .withTargetTags(null)
                    .withPageGroupTags(null);
        }, asList("app-metro"), asList("app-metro"));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_EmptyBsTags_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmGeoproductAdGroup(campaignId)
                    .withTargetTags(emptyList())
                    .withPageGroupTags(emptyList());
        }, asList("app-metro"), asList("app-metro"));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_WithAppNaviBsTag_AppNaviBsTagSaved() {
        steps.featureSteps().addClientFeature(clientId, ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT, true);

        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmGeoproductAdGroup(campaignId)
                    .withTargetTags(singletonList("app-navi"))
                    .withPageGroupTags(singletonList("app-navi"));
        }, asList("app-navi"), asList("app-navi"));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_WithAppMetroBsTag_AppMetroBsTagSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmGeoproductAdGroup(campaignId)
                    .withTargetTags(singletonList("app-metro"))
                    .withPageGroupTags(singletonList("app-metro"));
        }, asList("app-metro"), asList("app-metro"));
    }

    @Test
    public void prepareAndApply_CpmGeoproductAdGroup_WithBothBsTags_BothBsTagsSaved() {
        steps.featureSteps().addClientFeature(clientId, ZERO_SPEED_PAGE_ENABLED_FOR_GEOPRODUCT, true);

        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmGeoproductAdGroup(campaignId)
                    .withTargetTags(asList("app-metro", "app-navi"))
                    .withPageGroupTags(asList("app-metro", "app-navi"));
        }, asList("app-metro", "app-navi"), asList("app-metro", "app-navi"));
    }

    @Test
    public void prepareAndApply_CpmYndxFrontpageAdGroup_TagsSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo).getCampaignId();
            return activeCpmYndxFrontpageAdGroup(campaignId);
        }, asList("portal-trusted"), asList("portal-trusted"));
    }

    @Test
    public void prepareAndApply_CpmOutdoorAdGroup_TagsNotSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            OutdoorPlacement placement = steps.placementSteps().addDefaultOutdoorPlacementWithOneBlock();
            return activeCpmOutdoorAdGroup(campaignId, placement);
        }, null, null);
    }

    @Test
    public void prepareAndApply_CpmBannerAdGroup_TagsNotSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveCpmBannerCampaign(clientInfo).getCampaignId();
            return activeCpmBannerAdGroup(campaignId);
        }, null, null);
    }

    @Test
    public void prepareAndApply_PerformanceAdGroup_TagsNotSaved() {
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActivePerformanceCampaign(clientInfo).getCampaignId();
            return activePerformanceAdGroup(campaignId, feedInfo.getFeedId());
        }, null, null);
    }

    @Test
    public void prepareAndApply_TextAdGroup_TagsNotSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo).getCampaignId();
            return activeTextAdGroup(campaignId);
        }, null, null);
    }

    @Test
    public void prepareAndApply_MobileContentAdGroup_TagsNotSaved() {
        testAdGroupAddCheckBsTagsInDb(() -> {
            Long campaignId = steps.campaignSteps().createActiveMobileAppCampaign(clientInfo).getCampaignId();
            return activeMobileAppAdGroup(campaignId);
        }, null, null);
    }

    @Test
    public void prepareAndApply_AdGroupTypeWithDefaultBsTags_DefaultBsTagsAdded() {
        enableTargetTagsAllowed();

        testAdGroupAddCheckBsTagsInDb(() -> {
                    Long campaignId =
                            steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
                    return activeContentPromotionAdGroup(campaignId, ContentPromotionAdgroupType.COLLECTION)
                            .withPageGroupTags(asList("page_group_tag-1", "page_group_tag-2"))
                            .withTargetTags(asList("target_tag-1", "target_tag-2"));
                },
                asList("page_group_tag-1", "page_group_tag-2", "content-promotion-collection"),
                asList("target_tag-1", "target_tag-2", "content-promotion-collection"));
    }

    @Test
    public void prepareAndApply_AdGroupTypeWithDefaultBsTags_DefaultBsTagsAddedWithoutDuplicates() {
        enableTargetTagsAllowed();

        testAdGroupAddCheckBsTagsInDb(() -> {
                    Long campaignId =
                            steps.contentPromotionCampaignSteps().createDefaultCampaign(clientInfo).getCampaignId();
                    return activeContentPromotionAdGroup(campaignId, ContentPromotionAdgroupType.COLLECTION)
                            .withPageGroupTags(asList("page_group_tag", "content-promotion-collection"))
                            .withTargetTags(asList("target_tag", "content-promotion-collection"));
                },
                asList("page_group_tag", "content-promotion-collection"),
                asList("target_tag", "content-promotion-collection"));
    }

    private void testAdGroupAddCheckBsTagsInDb(Supplier<AdGroup> adGroupInfoSupplier,
                                               List<String> expectedPageGroupTagsValue,
                                               List<String> expectedTargetTagsValue) {
        AdGroup adGroup = adGroupInfoSupplier.get();
        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result).matches(t -> isFullySuccessful().matches(t));
            testAdGroupBsTagsRepository
                    .softAssertionCheckAdGroupTagsInDbConsumer(soft, shard, result.get(0).getResult(),
                            expectedPageGroupTagsValue, expectedTargetTagsValue);
        });
    }

    private void enableTargetTagsAllowed() {
        steps.featureSteps().addClientFeature(clientId, TARGET_TAGS_ALLOWED, true);
    }
}
