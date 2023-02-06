package ru.yandex.direct.core.entity.walletparams.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.walletparams.container.WalletParams;
import ru.yandex.direct.core.entity.walletparams.repository.WalletParamsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class WalletParamsServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private WalletParamsRepository walletParamsRepository;

    @Autowired
    private WalletParamsService walletParamsService;

    @Autowired
    private TestWalletCampaignRepository testWalletCampaignRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    public static final BigDecimal CAMPAIGN_CHIPS_COST = BigDecimal.TEN;

    private CampaignInfo walletInfo;
    private ClientInfo clientInfo;
    private long walletId;
    private int shard;

    @Before
    public void before() {
        walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        clientInfo = walletInfo.getClientInfo();
        walletId = walletInfo.getCampaignId();
        shard = walletInfo.getShard();

        walletParamsRepository.addWalletParams(shard, new WalletParams()
                .withWalletId(walletId)
                .withTotalBalanceTid(0L)
                .withTotalSum(BigDecimal.ZERO));

        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, CAMPAIGN_CHIPS_COST);
        walletParamsService.updateTotalCost(walletId);

        campaignRepository.updateStatusBsSynced(shard, singleton(walletId), StatusBsSynced.YES);
    }

    @Test
    public void updateTotalCost_AddCampaignUnderWallet_TotalChipsCostEqualsToChipsCostInCampaigns() {
        BigDecimal newChipsCosts = BigDecimal.TEN;
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, newChipsCosts);

        walletParamsService.updateTotalCost(walletId);

        BigDecimal expected = CAMPAIGN_CHIPS_COST.add(newChipsCosts);
        BigDecimal totalChipsCost = testWalletCampaignRepository.getTotalChipsCosts(shard, walletId);
        assertThat(totalChipsCost.compareTo(expected), is(0));
    }

    @Test
    public void updateTotalChipsCost_CostUpdated_ResetBsSyncStatus() {
        steps.campaignSteps().createCampaignUnderWallet(clientInfo, walletId, BigDecimal.ONE);
        walletParamsService.updateTotalCost(walletId);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(walletId));
        assumeThat(campaigns, hasSize(1));

        assertThat(campaigns.get(0).getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void updateTotalChipsCost_CostNotUpdated_BsSyncStatusNotReset() {
        walletParamsService.updateTotalCost(walletId);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singleton(walletId));
        assumeThat(campaigns, hasSize(1));

        assertThat(campaigns.get(0).getStatusBsSynced(), is(StatusBsSynced.YES));
    }
}
