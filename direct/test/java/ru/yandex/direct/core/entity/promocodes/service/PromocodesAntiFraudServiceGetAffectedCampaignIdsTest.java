package ru.yandex.direct.core.entity.promocodes.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.currency.CurrencyCode;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activePerformanceCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newMcbannerCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PromocodesAntiFraudServiceGetAffectedCampaignIdsTest {

    @Autowired
    private PromocodesAntiFraudService service;

    @Autowired
    private CampaignSteps steps;

    @Test
    public void oneTextCampaign_returnsSame() {
        CampaignInfo campaign = steps.createCampaign(newTextCampaign(null, null));
        assertEquals(singletonList(campaign.getCampaignId()),
                service.getAffectedCampaignIds(campaign.getShard(), campaign.getCampaignId()));
    }

    @Test
    public void oneMcbannerCampaign_returnsSame() {
        CampaignInfo campaign = steps.createCampaign(newMcbannerCampaign(null, null));
        assertEquals(singletonList(campaign.getCampaignId()),
                service.getAffectedCampaignIds(campaign.getShard(), campaign.getCampaignId()));
    }

    @Test
    public void oneDynamicCampaign_returnsSame() {
        CampaignInfo campaign = steps.createCampaign(activeDynamicCampaign(null, null));
        assertEquals(singletonList(campaign.getCampaignId()),
                service.getAffectedCampaignIds(campaign.getShard(), campaign.getCampaignId()));
    }

    @Test
    public void onePerformanceCampaign_returnsEmpty() {
        CampaignInfo campaign = steps.createCampaign(activePerformanceCampaign(null, null));
        assertEquals(emptyList(), service.getAffectedCampaignIds(campaign.getShard(), campaign.getCampaignId()));
    }

    @Test
    public void emptyWalletCampaign_returnsEmpty() {
        CampaignInfo campaign = steps.createCampaign(activeWalletCampaign(null, null));
        assertEquals(emptyList(), service.getAffectedCampaignIds(campaign.getShard(), campaign.getCampaignId()));
    }

    @Test
    public void walletFourDifferentCampaignTypes_returnsTextAndMcbannerAndDynamic() {
        CampaignInfo wallet = steps.createCampaign(activeWalletCampaign(null, null));
        BalanceInfo balanceInfo = activeBalanceInfo(CurrencyCode.RUB).withWalletCid(wallet.getCampaignId());

        CampaignInfo textCampaign = steps.createCampaign(
                newTextCampaign(wallet.getClientId(), wallet.getUid()).withBalanceInfo(balanceInfo),
                wallet.getClientInfo());
        CampaignInfo mcbannerCampaign = steps.createCampaign(
                newMcbannerCampaign(wallet.getClientId(), wallet.getUid()).withBalanceInfo(balanceInfo),
                wallet.getClientInfo());
        CampaignInfo dynamicCampaign = steps.createCampaign(
                activeDynamicCampaign(wallet.getClientId(), wallet.getUid()).withBalanceInfo(balanceInfo),
                wallet.getClientInfo());
        CampaignInfo performanceCampaign = steps.createCampaign(
                activePerformanceCampaign(wallet.getClientId(), wallet.getUid()).withBalanceInfo(balanceInfo),
                wallet.getClientInfo());

        assertThat(service.getAffectedCampaignIds(wallet.getShard(), wallet.getCampaignId()),
                containsInAnyOrder(textCampaign.getCampaignId(), mcbannerCampaign.getCampaignId(),
                        dynamicCampaign.getCampaignId()));
    }
}
