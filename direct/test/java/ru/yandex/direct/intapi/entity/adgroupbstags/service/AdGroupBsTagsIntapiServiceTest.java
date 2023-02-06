package ru.yandex.direct.intapi.entity.adgroupbstags.service;

import java.util.List;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupBsTags;
import ru.yandex.direct.core.entity.adgroup.model.PageGroupTagEnum;
import ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestAdGroupBsTagsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.adgroupbstags.model.AdGroupBsTagsResponse;
import ru.yandex.direct.intapi.validation.model.IntapiValidationResponse;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.COLLECTION;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.SERVICE;
import static ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType.VIDEO;
import static ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum.CONTENT_PROMOTION_COLLECTION_TAG;
import static ru.yandex.direct.core.entity.adgroup.model.TargetTagEnum.YDO_ADGROUP_BS_TAG;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.notFound;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupBsTagsIntapiServiceTest {
    @Autowired
    private AdGroupBsTagsIntapiService adGroupBsTagsIntapiService;
    @Autowired
    private TestAdGroupBsTagsRepository testAdGroupBsTagsRepository;
    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;

    @Test
    public void setYdoBsTags_TextCampaignsOfValidOperator_Success() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup(
                adGroupInfoFirst.getCampaignInfo());
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupInfoFirst.getUid(),
                singletonList(adGroupInfoFirst.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(YDO_ADGROUP_BS_TAG))
                .withPageGroupTags(singletonList(PageGroupTagEnum.YDO_ADGROUP_BS_TAG));
        assertSuccessSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    @Test
    public void setYdoBsTags_ContentPromotionCampaignsOfValidOperator_Success() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(VIDEO);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(adGroupInfoFirst.getTypedCampaignInfo(), VIDEO);
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(YDO_ADGROUP_BS_TAG))
                .withPageGroupTags(singletonList(PageGroupTagEnum.YDO_ADGROUP_BS_TAG));
        assertSuccessSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    @Test
    public void setYdoBsTags_TextCampaignsOfInvalidOperator_Error() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup();
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        assertErrorSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), null);
    }

    @Test
    public void setYdoBsTags_ContentPromotionCampaignsOfInvalidOperator_Error() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(VIDEO);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(VIDEO);
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG))
                .withPageGroupTags(singletonList(PageGroupTagEnum.CONTENT_PROMOTION_VIDEO_TAG));
        assertErrorSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    @Test
    public void setAndGetYdoBsTags_ContentPromotionAdGroup_Success() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of(YDO_ADGROUP_BS_TAG.getTypedValue(), "yndx-services-exp-4");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupIds, tags);
        assertSuccessSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), tags);

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getYdoAdGroupBsTags(adGroupIds);
        assertSuccessGetBsTags(getResponse, adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId(), tags);
    }

    @Test
    public void setAndGetEdaBsTags_Success() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = activeCpmBannerCampaign(clientInfo.getClientId(),
                clientInfo.getUid()).withSource(CampaignSource.EDA);
        var campaignInfo = steps.campaignSteps().createCampaign(campaign);
        var adGroupInfoFirst = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        var adGroupInfoSecond = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of("eda");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setEdaAdGroupBsTags(adGroupIds, tags);
        assertSuccessSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), tags);

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getEdaAdGroupBsTags(adGroupIds);
        assertSuccessGetBsTags(getResponse, adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId(), tags);
    }

    @Test
    public void setEdaBsTags_WrongBsTag() {
        var clientInfo = steps.clientSteps().createDefaultClient();
        var campaign = activeCpmBannerCampaign(clientInfo.getClientId(),
                clientInfo.getUid()).withSource(CampaignSource.EDA);
        var campaignInfo = steps.campaignSteps().createCampaign(campaign);
        var adGroupInfoFirst = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        var adGroupInfoSecond = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of("eddaaaaa", "eda_cpm");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setEdaAdGroupBsTags(adGroupIds, tags);
        assertErrorSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), null,
                null, inCollection());
    }

    @Test
    public void setYdoBsTags_ContentPromotionAdGroup_WrongContentPromotionType() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(COLLECTION);

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of(YDO_ADGROUP_BS_TAG.getTypedValue(), "yndx-services-exp-4");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupIds, tags);
        assertErrorSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), List.of(YDO_ADGROUP_BS_TAG.getTypedValue()),
                List.of(CONTENT_PROMOTION_COLLECTION_TAG.getTypedValue()), adGroupTypeNotSupported());

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getYdoAdGroupBsTags(adGroupIds);
        assertErrorGetBsTags(getResponse, adGroupTypeNotSupported());
    }

    @Test
    public void setEdaBsTags_WrongCampaignSource() {
        var adGroupInfoFirst = steps.adGroupSteps().createDefaultAdGroup();
        var adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup();

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of("eda");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setEdaAdGroupBsTags(adGroupIds, tags);
        assertErrorSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), null, null, notFound());

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getEdaAdGroupBsTags(adGroupIds);
        assertErrorGetBsTags(getResponse, notFound());
    }

    @Test
    public void setEdaBsTags_WrongPid() {
        List<Long> adGroupIds = List.of(-5L, -10L);
        List<String> tags = List.of("eda");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setEdaAdGroupBsTags(adGroupIds, tags);
        assertTrue(setResponse.validationResult().getErrors().get(0).getCode()
                .equals(notFound().defectId().getCode()));

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getEdaAdGroupBsTags(adGroupIds);
        assertTrue(getResponse.validationResult().getErrors().get(0).getCode()
                .equals(notFound().defectId().getCode()));
    }

    @Test
    public void setYdoBsTags_ContentPromotionAdGroup_WrongAdGroupType() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);
        var adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup();

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of(YDO_ADGROUP_BS_TAG.getTypedValue(), "yndx-services-exp-4");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupIds, tags);
        assertErrorSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), List.of(YDO_ADGROUP_BS_TAG.getTypedValue()), null, notFound());

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getYdoAdGroupBsTags(adGroupIds);
        assertErrorGetBsTags(getResponse, notFound());
    }

    @Test
    public void setYdoBsTags_ContentPromotionAdGroup_WrongBsTag() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(SERVICE);

        List<Long> adGroupIds = List.of(adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId());
        List<String> tags = List.of(YDO_ADGROUP_BS_TAG.getTypedValue(), "yndx-services-wrong-tag");

        IntapiValidationResponse setResponse = adGroupBsTagsIntapiService.setYdoAdGroupBsTags(adGroupIds, tags);
        assertErrorSetBsTags(setResponse, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), List.of(YDO_ADGROUP_BS_TAG.getTypedValue()),
                List.of(YDO_ADGROUP_BS_TAG.getTypedValue()), inCollection());

        AdGroupBsTagsResponse getResponse = adGroupBsTagsIntapiService.getYdoAdGroupBsTags(adGroupIds);
        assertSuccessGetBsTags(getResponse, adGroupInfoFirst.getAdGroupId(), adGroupInfoSecond.getAdGroupId(),
                List.of(YDO_ADGROUP_BS_TAG.getTypedValue()));
    }

    @Test
    public void setMapsTestBsTags_TextCampaignsOfValidOperator_Success() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup(
                adGroupInfoFirst.getCampaignInfo());
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setMapsTestAdGroupBsTags(
                adGroupInfoFirst.getUid(),
                singletonList(adGroupInfoFirst.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(TargetTagEnum.MAPS_TEST))
                .withPageGroupTags(singletonList(PageGroupTagEnum.MAPS_TEST));
        assertSuccessSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    @Test
    public void setMapsTestBsTags_ContentPromotionCampaignsOfValidOperator_Success() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(VIDEO);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(adGroupInfoFirst.getTypedCampaignInfo(), VIDEO);
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setMapsTestAdGroupBsTags(
                adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(TargetTagEnum.MAPS_TEST))
                .withPageGroupTags(singletonList(PageGroupTagEnum.MAPS_TEST));
        assertSuccessSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    @Test
    public void setMapsTestBsTags_TextCampaignsOfInvalidOperator_Error() {
        AdGroupInfo adGroupInfoFirst = steps.adGroupSteps().createActiveTextAdGroup();
        AdGroupInfo adGroupInfoSecond = steps.adGroupSteps().createActiveTextAdGroup();
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setMapsTestAdGroupBsTags(
                adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        assertErrorSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), null);
    }

    @Test
    public void setMapsTestBsTags_ContentPromotionCampaignsOfInvalidOperator_Error() {
        var adGroupInfoFirst = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(VIDEO);
        var adGroupInfoSecond = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(VIDEO);
        IntapiValidationResponse response = adGroupBsTagsIntapiService.setMapsTestAdGroupBsTags(
                adGroupInfoFirst.getUid(),
                ImmutableList.of(adGroupInfoFirst.getCampaignId(), adGroupInfoSecond.getCampaignId()));
        AdGroupBsTags expected = new AdGroupBsTags()
                .withTargetTags(singletonList(TargetTagEnum.CONTENT_PROMOTION_VIDEO_TAG))
                .withPageGroupTags(singletonList(PageGroupTagEnum.CONTENT_PROMOTION_VIDEO_TAG));
        assertErrorSetBsTags(response, AdGroupIdShard.from(adGroupInfoFirst),
                AdGroupIdShard.from(adGroupInfoSecond), expected);
    }

    private void assertSuccessSetBsTags(IntapiValidationResponse response,
                                        AdGroupIdShard adGroupInfoFirst,
                                        AdGroupIdShard adGroupInfoSecond,
                                        AdGroupBsTags expectedTags) {
        assertSetBsTags(response, adGroupInfoFirst, adGroupInfoSecond, expectedTags, true);
    }

    private void assertErrorSetBsTags(IntapiValidationResponse response,
                                      AdGroupIdShard adGroupInfoFirst,
                                      AdGroupIdShard adGroupInfoSecond,
                                      AdGroupBsTags expectedTags) {
        assertSetBsTags(response, adGroupInfoFirst, adGroupInfoSecond, expectedTags, false);
    }

    private void assertSetBsTags(IntapiValidationResponse response,
                                 AdGroupIdShard adGroupInfoFirst,
                                 AdGroupIdShard adGroupInfoSecond,
                                 AdGroupBsTags expectedTags, boolean expectedSuccessful) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroupInfoFirst.getShard(),
                List.of(adGroupInfoFirst.getAdGroupId()));
        adGroups.addAll(adGroupRepository.getAdGroups(adGroupInfoSecond.getShard(),
                List.of(adGroupInfoSecond.getAdGroupId())));
        StatusBsSynced expectedStatusBsSynced = expectedSuccessful ? StatusBsSynced.NO : StatusBsSynced.YES;
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.isSuccessful()).isEqualTo(expectedSuccessful);
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbConsumer(soft, adGroupInfoFirst.getShard(),
                    adGroupInfoFirst.getAdGroupId(), expectedTags);
            soft.assertThat(adGroups.get(0).getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbConsumer(soft, adGroupInfoSecond.getShard(),
                    adGroupInfoSecond.getAdGroupId(), expectedTags);
            soft.assertThat(adGroups.get(1).getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
        });
    }

    private void assertSuccessSetBsTags(IntapiValidationResponse response,
                                        AdGroupIdShard adGroupInfoFirst,
                                        AdGroupIdShard adGroupInfoSecond,
                                        List<String> expectedTags) {
        assertSetBsTags(response, adGroupInfoFirst, adGroupInfoSecond,
                expectedTags, expectedTags, null, true);
    }

    private void assertErrorSetBsTags(IntapiValidationResponse response,
                                      AdGroupIdShard adGroupInfoFirst,
                                      AdGroupIdShard adGroupInfoSecond,
                                      List<String> expectedTagsFirst,
                                      List<String> expectedTagsSecond,
                                      Defect expectedDefect) {
        assertSetBsTags(response, adGroupInfoFirst, adGroupInfoSecond,
                expectedTagsFirst, expectedTagsSecond, expectedDefect, false);
    }

    private void assertSetBsTags(IntapiValidationResponse response,
                                 AdGroupIdShard adGroupInfoFirst,
                                 AdGroupIdShard adGroupInfoSecond,
                                 List<String> expectedTagsFirst, List<String> expectedTagsSecond,
                                 Defect expectedDefect, boolean expectedSuccessful) {
        List<AdGroup> adGroups = adGroupRepository.getAdGroups(adGroupInfoFirst.getShard(),
                List.of(adGroupInfoFirst.getAdGroupId()));
        adGroups.addAll(adGroupRepository.getAdGroups(adGroupInfoSecond.getShard(),
                List.of(adGroupInfoSecond.getAdGroupId())));
        StatusBsSynced expectedStatusBsSynced = expectedSuccessful ? StatusBsSynced.NO : StatusBsSynced.YES;
        String expectedTagsJsonFirst = tagsToJson(expectedTagsFirst);
        String expectedTagsJsonSecond = tagsToJson(expectedTagsSecond);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.isSuccessful()).isEqualTo(expectedSuccessful);
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(soft, adGroupInfoFirst.getShard(),
                    adGroupInfoFirst.getAdGroupId(), expectedTagsJsonFirst, expectedTagsJsonFirst);
            soft.assertThat(adGroups.get(0).getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
            testAdGroupBsTagsRepository.softAssertionCheckAdGroupTagsInDbRawConsumer(soft,
                    adGroupInfoSecond.getShard(), adGroupInfoSecond.getAdGroupId(),
                    expectedTagsJsonSecond, expectedTagsJsonSecond);
            soft.assertThat(adGroups.get(1).getStatusBsSynced()).isEqualTo(expectedStatusBsSynced);
            if (!response.isSuccessful()) {
                soft.assertThat(response.validationResult().getErrors().get(0).getCode())
                        .isEqualTo(expectedDefect.defectId().getCode());
            }
        });
    }

    private void assertSuccessGetBsTags(AdGroupBsTagsResponse response,
                                        Long adGroupIdFirst,
                                        Long adGroupIdSecond,
                                        List<String> expectedTags) {
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.isSuccessful()).isTrue();
            soft.assertThat(response.getBsTagsByAdGroupId()).containsOnlyKeys(adGroupIdFirst, adGroupIdSecond);
            soft.assertThat(response.getBsTagsByAdGroupId().get(adGroupIdFirst)).isEqualTo(expectedTags);
            soft.assertThat(response.getBsTagsByAdGroupId().get(adGroupIdSecond)).isEqualTo(expectedTags);
        });
    }

    private void assertErrorGetBsTags(AdGroupBsTagsResponse response, Defect expectedDefect) {
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.isSuccessful()).isFalse();
            soft.assertThat(response.getBsTagsByAdGroupId()).isNull();
            soft.assertThat(response.validationResult().getErrors().get(0).getCode())
                    .isEqualTo(expectedDefect.defectId().getCode());
        });
    }

    private String tagsToJson(List<String> tags) {
        if (tags == null) {
            return null;
        }
        return StreamEx.of(tags)
                .map(tag -> String.format("\"%s\"", tag))
                .joining(", ", "[", "]");
    }

    // todo убрать после перехода на единый AdGroupInfo
    private static class AdGroupIdShard {
        private final Long adGroupId;
        private final int shard;

        private AdGroupIdShard(Long adGroupId, int shard) {
            this.adGroupId = adGroupId;
            this.shard = shard;
        }

        private static AdGroupIdShard from(AdGroupInfo adGroupInfo) {
            return new AdGroupIdShard(adGroupInfo.getAdGroupId(), adGroupInfo.getShard());
        }

        private static AdGroupIdShard from(ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo<?> adGroupInfo) {
            return new AdGroupIdShard(adGroupInfo.getAdGroupId(), adGroupInfo.getShard());
        }

        private Long getAdGroupId() {
            return adGroupId;
        }

        private int getShard() {
            return shard;
        }
    }
}
