package ru.yandex.direct.core.entity.retargeting.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingConditionServiceGetTruncatedShortcutsTest {
    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingConditionService retargetingConditionService;

    private ClientInfo clientInfo;
    private long uid;
    private ClientId clientId;
    private Long campaignId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        uid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        var campaign = TestCampaigns.activeTextCampaign(clientInfo.getClientId(), uid);
        campaignId = steps.campaignSteps().createCampaign(campaign, clientInfo).getCampaignId();
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_empty() {
        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, null);
        assertThat(shortcuts).isEmpty();
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_wrongCampaignType_empty() {
        var dynamicCampaign = TestCampaigns.activeDynamicCampaign(clientInfo.getClientId(), uid);
        var campaignId = steps.campaignSteps().createCampaign(dynamicCampaign, clientInfo).getCampaignId();

        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).isEmpty();
    }

    @Test
    public void getTruncatedRetargetingConditionShortcuts_noPrerequisitesMet_empty() {
        var shortcuts = retargetingConditionService.getTruncatedRetargetingConditionShortcuts(clientId, campaignId);
        assertThat(shortcuts).isEmpty();
    }
}
