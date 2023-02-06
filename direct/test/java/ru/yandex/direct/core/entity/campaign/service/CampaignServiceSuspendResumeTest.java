package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка приостановки и возобновления показов кампании")
public class CampaignServiceSuspendResumeTest {
    private static final boolean RESUMED = true;
    private static final boolean SUSPENDED = false;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaignInfo;
    private Long campaignId;
    LocalDateTime lastChange;

    private void createCampaignWithStatusShow(boolean statusShow) {
        lastChange = LocalDateTime.now().minus(1, HOURS).truncatedTo(SECONDS);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusShow(statusShow)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withLastChange(lastChange));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testSuspendSuccess() {
        createCampaignWithStatusShow(RESUMED);
        MassResult<Long> result = campaignService.suspendResumeCampaigns(
                singletonList(campaignId), SUSPENDED,
                campaignInfo.getUid(), campaignInfo.getClientId()
        );
        assumeThat(result, isFullySuccessful());

        Campaign campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
        assertNotEquals(lastChange, campaign.getLastChange());
        assertEquals(StatusBsSynced.NO, campaign.getStatusBsSynced());
        assertEquals(SUSPENDED, campaign.getStatusShow());
    }

    @Test
    public void testResumeSuccess() {
        createCampaignWithStatusShow(SUSPENDED);
        MassResult<Long> result = campaignService.suspendResumeCampaigns(
                singletonList(campaignId), RESUMED,
                campaignInfo.getUid(), campaignInfo.getClientId()
        );
        assumeThat(result, isFullySuccessful());

        Campaign campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
        assertNotEquals(lastChange, campaign.getLastChange());
        assertEquals(StatusBsSynced.NO, campaign.getStatusBsSynced());
        assertEquals(RESUMED, campaign.getStatusShow());
    }

}
