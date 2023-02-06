package ru.yandex.direct.intapi.entity.inventori.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.core.entity.inventori.validation.CampaignDefectIds;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmBannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class InventoriValidationServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private InventoriValidationService inventoriValidationService;

    @Test
    public void validateCampaignIds_NonExistentCampaign_ValidationResultContainsCorrectError() {
        ValidationResult<List<Long>, Defect> vr = inventoriValidationService.validateCampaignIds(singletonList(5555L));

        assertThat(vr, hasDefectWithDefinition(
                validationError(path(index(0)), CampaignDefectIds.CampaignDefects.CAMPAIGN_NOT_EXISTS)));
    }

    @Test
    public void validateCampaignIds_DeletedCampaign_ValidationResultContainsCorrectError() {
        CampaignInfo campaignInfo =
                steps.campaignSteps().createCampaign(activeCpmBannerCampaign(null, null).withStatusEmpty(true));

        ValidationResult<List<Long>, Defect> vr = inventoriValidationService.validateCampaignIds(
                singletonList(campaignInfo.getCampaignId()));

        assertThat(vr, hasDefectWithDefinition(
                validationError(path(index(0)), CampaignDefectIds.CampaignDefects.CAMPAIGN_ALREADY_DELETED)));
    }

    @Test
    public void validateCampaignIds_CampaignWithWrongStrategyType_ValidationResultContainsCorrectError() {
        CampaignInfo campaignInfo =
                steps.campaignSteps().createCampaign(
                        activeCpmBannerCampaign(null, null).withStrategy(manualStrategy()));

        ValidationResult<List<Long>, Defect> vr = inventoriValidationService.validateCampaignIds(
                singletonList(campaignInfo.getCampaignId()));

        assertThat(vr, hasDefectWithDefinition(
                validationError(path(index(0)), CampaignDefectIds.StrategyDefects.INVALID_CAMPAIGN_STRATEGY)));
    }

}
