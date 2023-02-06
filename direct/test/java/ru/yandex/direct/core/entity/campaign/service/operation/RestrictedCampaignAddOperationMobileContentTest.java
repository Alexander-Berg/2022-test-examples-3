package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMobileContentCampaign;


@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class RestrictedCampaignAddOperationMobileContentTest {
    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private MobileContentCampaign validMobileContentCampaign;

    @Before
    public void before() {
        clientInfo = steps.userSteps().createDefaultUser().getClientInfo();
        var mobileAppInfo = steps.mobileAppSteps().createDefaultMobileApp(clientInfo);
        validMobileContentCampaign = defaultMobileContentCampaign()
                .withMobileAppId(mobileAppInfo.getMobileAppId());
    }

    @Test
    public void add_Valid_Success() {
        CampaignOptions options = new CampaignOptions();
        RestrictedCampaignsAddOperation operation = campaignOperationService.createRestrictedCampaignAddOperation(
                List.of(validMobileContentCampaign),
                clientInfo.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                options);

        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
    }

    @Test
    public void add_WithoutMobileApp_ValidationFail() {
        var campaignWithoutApp = validMobileContentCampaign.withMobileAppId(null);
        CampaignOptions options = new CampaignOptions();
        RestrictedCampaignsAddOperation operation = campaignOperationService.createRestrictedCampaignAddOperation(
                List.of(campaignWithoutApp),
                clientInfo.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                options);

        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
    }

    @Test
    public void add_WithoutMobileApp_WithSkipValidation_Valid_Success() {
        var campaignWithoutApp = validMobileContentCampaign.withMobileAppId(null);
        CampaignOptions options = new CampaignOptions.Builder().withSkipValidateMobileApp(true).build();
        RestrictedCampaignsAddOperation operation = campaignOperationService.createRestrictedCampaignAddOperation(
                List.of(campaignWithoutApp),
                clientInfo.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                options);

        Optional<MassResult<Long>> result = operation.prepare();

        assertThat(result).isEmpty();
    }
}
