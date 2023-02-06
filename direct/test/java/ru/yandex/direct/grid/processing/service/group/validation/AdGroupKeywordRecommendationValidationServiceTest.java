package ru.yandex.direct.grid.processing.service.group.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupKeywordRecommendationValidationServiceTest {

    @Autowired
    private AdGroupKeywordRecommendationValidationService service;

    @Autowired
    private Steps steps;

    @Test
    public void checkTextCampaign_Success() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        var adgroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);

        var vr =
                service.validateCampaignType(campaignInfo.getClientId(), adgroupInfo.getAdGroupId(), null);

        assertThat(vr.hasAnyErrors(), equalTo(false));
    }

    @Test
    public void checkDynamicCampaign_Failure() {
        var campaignInfo = steps.campaignSteps().createActiveDynamicCampaign();
        var adgroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(campaignInfo);

        var vr =
                service.validateCampaignType(campaignInfo.getClientId(), adgroupInfo.getAdGroupId(), null);

        assertThat(vr.hasAnyErrors(), equalTo(true));
        assertThat(vr.getErrors(), hasItem(AdGroupKeywordRecommendationValidationService.invalidCampaignType()));
    }

}
