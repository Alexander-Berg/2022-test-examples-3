package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyTextCampaign;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка приостановки и возобновления показов кампании")
public class CampaignServiceArchiveTest {
    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaignInfo;
    private Long campaignId;
    LocalDateTime lastChange;

    private void createCampaignWithStatusShow() {
        lastChange = LocalDateTime.now().minus(61, MINUTES);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusShow(false)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withLastChange(lastChange)
                        .withLastShowTime(lastChange)
                        .withStatusActive(false)
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveSuccess() {
        createCampaignWithStatusShow();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );
        Campaign campaign = campaignService.getCampaigns(campaignInfo.getClientId(), singletonList(campaignId)).get(0);
        assertThat(result, isFullySuccessful());


        assertThat(campaign.getLastChange(), not(equalTo(lastChange)));
        assertThat(campaign.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
        assertThat(campaign.getStatusShow(), equalTo(false));
        assertThat(campaign.getStatusArchived(), equalTo(true));
    }

    private void createCampaignWithMoney() {
        lastChange = LocalDateTime.now().minus(61, MINUTES);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusShow(false)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withLastChange(lastChange)
                        .withLastShowTime(lastChange)
                        .withStatusActive(false));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveCampaignWithMoneyFail() {
        createCampaignWithMoney();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );

        assertThat(result, not(isFullySuccessful()));

    }

    private void createEmptyCampaign() {
        lastChange = LocalDateTime.now().minus(61, MINUTES);
        campaignInfo = campaignSteps.createCampaign(
                emptyTextCampaign(null, null));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveEmptyCampaignFail() {
        createEmptyCampaign();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );

        assertThat(result, not(isFullySuccessful()));
    }

    private void createCampaignWithWalletType() {
        lastChange = LocalDateTime.now().minus(61, MINUTES);
        campaignInfo = campaignSteps.createCampaign(
                activeWalletCampaign(null, null)
                        .withStatusShow(false)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withLastChange(lastChange)
                        .withLastShowTime(lastChange)
                        .withStatusActive(false)
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveCampaignWithWalletTypeFail() {
        createCampaignWithWalletType();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );

        assertThat(result, not(isFullySuccessful()));
    }

    private void createNonStoppedCampaign() {
        lastChange = LocalDateTime.now().minus(1, MINUTES).truncatedTo(SECONDS);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusShow(true)
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withLastChange(lastChange)
                        .withLastShowTime(lastChange)
                        .withStatusActive(true)
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveNonStoppedCampaignFail() {
        createNonStoppedCampaign();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );

        assertThat(result, not(isFullySuccessful()));
    }

    private void createArchivedCampaign() {
        lastChange = LocalDateTime.now().minus(61, MINUTES);
        campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null)
                        .withStatusShow(false)
                        .withStatusBsSynced(StatusBsSynced.NO)
                        .withArchived(true)
                        .withLastChange(lastChange)
                        .withLastShowTime(lastChange)
                        .withStatusActive(false)
                        .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB)));
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void testArchiveArchivedCampaignSuccess() {
        createArchivedCampaign();
        MassResult<Long> result = campaignService.archiveCampaigns(singletonList(campaignId), campaignInfo.getUid(),
                campaignInfo.getClientId()
        );

        assertThat(result, isFullySuccessful());
        assertThat(result.get(0).getWarnings().get(0).getDefect().defectId(),
                equalTo(CampaignDefectIds.Gen.CAMPAIGN_ALREADY_ARCHIVED));
    }
}
