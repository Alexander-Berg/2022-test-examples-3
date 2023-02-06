package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.StatusBsSynced.NO;
import static ru.yandex.direct.core.entity.StatusBsSynced.YES;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка обновления кампании")
public class CampaignServiceUpdateTest {

    private static final String DOMAIN_YANDEX = "yandex.ru";
    private static final String DOMAIN_FOR_REMOVAL = "wrong.com";

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaignInfo;
    private Long campaignId;

    @Before
    public void before() {
        LocalDateTime lastChange = LocalDateTime.now().minus(1, HOURS).truncatedTo(SECONDS);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusBsSynced(YES)
                        .withLastChange(lastChange));
        campaignId = campaignInfo.getCampaignId();

    }

    @Test
    public void addOrRemoveDisabledVideoDomains_Success() {
        campaignService.addOrRemoveDisabledVideoDomains(campaignInfo.getUid(), campaignInfo.getClientId(), campaignId,
                singletonList(DOMAIN_FOR_REMOVAL), true);
        Campaign campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
        assertEquals(singletonList(DOMAIN_FOR_REMOVAL), campaign.getDisabledVideoPlacements());
        assertEquals(NO, campaign.getStatusBsSynced());
    }

    @Test
    public void addOrRemoveDisabledVideoDomains_NotDisableYandexDomain() {
        ValidationResult<Campaign, Defect> result =
                campaignService.addOrRemoveDisabledVideoDomains(campaignInfo.getUid(), campaignInfo.getClientId(),
                        campaignId, singletonList(DOMAIN_YANDEX), true);
        assertTrue(result.hasAnyErrors());
        Campaign campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
        assertNull(campaign.getDisabledVideoPlacements());
        assertEquals(YES, campaign.getStatusBsSynced());
    }

}
