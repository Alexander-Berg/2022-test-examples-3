package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaignMoney;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesCampaignRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private AggregatedStatusesCampaignRepository aggregatedStatusesCampaignRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    @Test
    public void getCampaignsMoneyByWalletIds() {
        var walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));

        var campaignInfo = steps.campaignSteps()
                .createCampaignUnderWallet(walletInfo.getClientInfo(), walletInfo.getCampaignId(), BigDecimal.ZERO);

        var camp = campaignRepository
                .getCampaigns(campaignInfo.getShard(), List.of(campaignInfo.getCampaignId()))
                .get(0);

        var campaignsMoneyByWalletIds = aggregatedStatusesCampaignRepository
                .getCampaignsMoneyByWalletIds(walletInfo.getShard(), List.of(walletInfo.getCampaignId()));

        assertThat(campaignsMoneyByWalletIds).isEqualTo(
                Map.of(
                        walletInfo.getCampaignId(),
                        List.of(
                                new AggregatedStatusCampaignMoney()
                                        .withId(camp.getId())
                                        .withCurrencyCode(camp.getCurrency())
                                        .withSum(camp.getSum())
                                        .withSumSpent(camp.getSumSpent())
                        )
                )
        );
    }
}
