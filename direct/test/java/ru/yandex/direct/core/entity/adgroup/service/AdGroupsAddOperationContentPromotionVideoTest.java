package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.contentPromotionDistinctTypesWithExisting;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.contentPromotionSeveralTypesNotAllowed;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationContentPromotionVideoTest extends AdGroupsAddOperationTestBase {

    @Test
    public void prepareAndApply_PositiveTest() {
        var adGroup = clientContentPromotionVideoAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_MinusKeywords_NoErrors() {
        List<String> minusKeywords = asList("word1", "word2");
        AdGroup adGroup = clientContentPromotionVideoAdGroup(campaignId).withMinusKeywords(minusKeywords);

        MassResult<Long> result = createFullAddOperation(singletonList(adGroup)).prepareAndApply();
        assertThat(result, isFullySuccessful());
        Long contentVideoAdGroupId = result.get(0).getResult();
        AdGroup realAdGroup =
                adGroupRepository.getAdGroups(clientInfo.getShard(), singletonList(contentVideoAdGroupId)).get(0);

        assertThat(realAdGroup.getMinusKeywords(), is(minusKeywords));
        assertThat(realAdGroup.getMinusKeywordsId(), notNullValue());
    }

    @Test
    public void prepareAndApply_AdGroupsOfDistinctPromotionTypes_ValidationError() {
        var colllectionAdGroup = clientContentPromotionAdGroup(campaignId);
        var videoAdGroup = clientContentPromotionVideoAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL,
                Arrays.asList(colllectionAdGroup, videoAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(),
                containsInAnyOrder(validationError(path(index(0)), contentPromotionSeveralTypesNotAllowed()),
                        validationError(path(index(1)), contentPromotionSeveralTypesNotAllowed())));
    }

    @Test
    public void prepareAndApply_CollectionPromotionTypeInDb_ValidationError() {
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup((ContentPromotionCampaignInfo) campaignInfo,
                ContentPromotionAdgroupType.COLLECTION);
        var videoAdGroup = clientContentPromotionVideoAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(videoAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(path(index(0)), contentPromotionDistinctTypesWithExisting())));
    }

    @Test
    public void prepareAndApply_VideoPromotionTypeInDb_NoError() {
        steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup((ContentPromotionCampaignInfo) campaignInfo, ContentPromotionAdgroupType.VIDEO);
        var videoAdGroup = clientContentPromotionVideoAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(videoAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_PromotionVideoInDb_NoError() {
        steps.contentPromotionAdGroupSteps().createDefaultAdGroup((ContentPromotionCampaignInfo) campaignInfo);
        var videoAdGroup = clientContentPromotionVideoAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(videoAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Override
    protected CampaignInfo createModeratedCampaign() {
        var campaign = fullContentPromotionCampaign()
                .withStatusModerate(CampaignStatusModerate.YES);
        return steps.contentPromotionCampaignSteps().createCampaign(campaign);
    }

    private static ContentPromotionAdGroup clientContentPromotionVideoAdGroup(Long campaignId) {
        return new ContentPromotionAdGroup()
                .withType(AdGroupType.CONTENT_PROMOTION)
                .withContentPromotionType(ContentPromotionAdgroupType.VIDEO)
                .withCampaignId(campaignId)
                .withName("test content video group " + randomNumeric(5))
                .withGeo(singletonList(Region.RUSSIA_REGION_ID));
    }

    private static ContentPromotionAdGroup clientContentPromotionAdGroup(Long campaignId) {
        return new ContentPromotionAdGroup()
                .withType(AdGroupType.CONTENT_PROMOTION)
                .withContentPromotionType(ContentPromotionAdgroupType.COLLECTION)
                .withCampaignId(campaignId)
                .withName("test content promotion group " + randomNumeric(5))
                .withGeo(singletonList(Region.RUSSIA_REGION_ID));
    }
}
