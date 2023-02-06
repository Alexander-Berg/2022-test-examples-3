package ru.yandex.direct.core.entity.campaign.repository;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultMcBannerCampaignWithSystemFields;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class CampaignArchivedTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    private ClientInfo defaultClient;

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    public static Object[][] archivedStatus() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
    }

    @Test
    @TestCaseName("archived: {0}")
    @Parameters(method = "archivedStatus")
    public void checkGetTextCampaignWithArchived(boolean archived) {
        Campaign campaign =
                activeTextCampaign(defaultClient.getClientId(), defaultClient.getUid()).withArchived(archived);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        TextCampaign textCampaign = (TextCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), singletonList(campaignInfo.getCampaignId())).get(0);
        assertThat(textCampaign.getStatusArchived())
                .isEqualTo(archived);
    }

    @Test
    @TestCaseName("archived: {0}")
    @Parameters(method = "archivedStatus")
    public void checkGetDynamicCampaignWithArchived(boolean archived) {
        Campaign campaign =
                activeDynamicCampaign(defaultClient.getClientId(), defaultClient.getUid()).withArchived(archived);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        DynamicCampaign dynamicCampaign = (DynamicCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), singletonList(campaignInfo.getCampaignId())).get(0);
        assertThat(dynamicCampaign.getStatusArchived())
                .isEqualTo(archived);
    }

    @Test
    @TestCaseName("archived: {0}")
    @Parameters(method = "archivedStatus")
    public void checkGetSmartCampaignWithArchived(boolean archived) {
        Campaign campaign =
                activePerformanceCampaign(defaultClient.getClientId(), defaultClient.getUid()).withArchived(archived);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign);
        SmartCampaign smartCampaign = (SmartCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), singletonList(campaignInfo.getCampaignId())).get(0);
        assertThat(smartCampaign.getStatusArchived())
                .isEqualTo(archived);
    }

    @Test
    @TestCaseName("archived: {0}")
    @Parameters(method = "archivedStatus")
    public void checkGetMcBannerCampaignWithArchived(boolean archived) {
        McBannerCampaign campaign = defaultMcBannerCampaignWithSystemFields()
                .withStatusArchived(archived);
        var campaignInfo = steps.mcBannerCampaignSteps().createCampaign(defaultClient, campaign);
        McBannerCampaign mcBannerCampaign = (McBannerCampaign) campaignTypedRepository
                .getTypedCampaigns(defaultClient.getShard(), singletonList(campaignInfo.getId())).get(0);
        assertThat(mcBannerCampaign.getStatusArchived())
                .isEqualTo(archived);
    }
}
