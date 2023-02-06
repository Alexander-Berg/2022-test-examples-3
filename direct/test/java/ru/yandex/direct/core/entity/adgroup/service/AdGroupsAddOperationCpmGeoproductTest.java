package ru.yandex.direct.core.entity.adgroup.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.CpmGeoproductAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupTypeNotSupported;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupsAddOperationCpmGeoproductTest extends AdGroupsAddOperationTestBase {

    @Test
    public void prepareAndApply_PositiveTest() {
        CpmGeoproductAdGroup adGroup = clientCpmGeoproductAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_AddMinusKeywords_ValidationError() {
        CpmGeoproductAdGroup adGroup = clientCpmGeoproductAdGroup(campaignId)
                .withMinusKeywords(singletonList("minusword"));

        AdGroupsAddOperation addOperation = createFullAddOperation(singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result, isSuccessful(false));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(validationError(path(index(0),
                field(CpmGeoproductAdGroup.MINUS_KEYWORDS)), AdGroupDefectIds.Gen.MINUS_KEYWORDS_NOT_ALLOWED)));
    }

    @Test
    public void prepareAndApply_addIntoAdGroupsWithGeoproduct_NoError() {
        steps.adGroupSteps().createActiveCpmGeoproductAdGroup(campaignInfo);
        CpmGeoproductAdGroup adGroup = clientCpmGeoproductAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result, isFullySuccessful());
    }

    @Test
    public void prepareAndApply_addIntoAdGroupsWithoutGeoproduct_ValidationError() {
        steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        CpmGeoproductAdGroup adGroup = clientCpmGeoproductAdGroup(campaignId);

        AdGroupsAddOperation addOperation = createAddOperation(Applicability.FULL, singletonList(adGroup));
        MassResult<Long> result = addOperation.prepareAndApply();
        assertThat(result.getValidationResult().flattenErrors(),
                contains(validationError(adGroupTypeNotSupported().defectId())));
    }

    @Override
    protected CampaignInfo createModeratedCampaign() {
        Campaign campaign = activeCpmBannerCampaign(null, null).withStatusModerate(StatusModerate.YES);
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(campaign));
    }

    private static CpmGeoproductAdGroup clientCpmGeoproductAdGroup(Long campaignId) {
        return new CpmGeoproductAdGroup()
                .withType(AdGroupType.CPM_GEOPRODUCT)
                .withCampaignId(campaignId)
                .withName("test cpm geoproduct group " + randomNumeric(5))
                .withGeo(singletonList(Region.RUSSIA_REGION_ID));
    }
}
