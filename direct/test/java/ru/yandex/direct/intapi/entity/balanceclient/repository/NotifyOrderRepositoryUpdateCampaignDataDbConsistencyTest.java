package ru.yandex.direct.intapi.entity.balanceclient.repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.repository0.CampaignRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.container.NotifyOrderDbCampaignChanges;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Проверки того, что
 * {@link NotifyOrderRepository#updateCampaignData(DSLContext, Long, Long, Long, NotifyOrderDbCampaignChanges)}
 * не обновляет данные в базе, если во время обработки нотификации в базе изменились
 * {@code balance_tid} или ID общего счета.
 */
@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyOrderRepositoryUpdateCampaignDataDbConsistencyTest {
    @Autowired
    private NotifyOrderRepository notifyOrderRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    private static final long DB_BALANCE_TID = 123123L;
    private static final long NEW_BALANCE_TID = DB_BALANCE_TID + 1;

    private int shard;
    private DSLContext dslContext;
    private long campaignId;
    private long dbWalletId;
    private NotifyOrderDbCampaignChanges notifyOrderDbCampaignChanges;

    @Before
    public void setUp() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        ClientId clientId = defaultClient.getClientId();
        long uid = defaultClient.getUid();
        shard = defaultClient.getShard();

        Campaign wallet = TestCampaigns.activeWalletCampaign(clientId, uid);
        dbWalletId = steps.campaignSteps().createCampaign(wallet, defaultClient).getCampaignId();

        Campaign campaign = TestCampaigns.activeTextCampaign(clientId, uid);
        campaign.getBalanceInfo().withWalletCid(dbWalletId).withBalanceTid(DB_BALANCE_TID);
        campaignId = steps.campaignSteps().createCampaign(campaign, defaultClient).getCampaignId();

        dslContext = dslContextProvider.ppc(shard);
        notifyOrderDbCampaignChanges = new NotifyOrderDbCampaignChanges()
                .withBalanceTid(NEW_BALANCE_TID)
                .withSum(BigDecimal.TEN);
    }

    @Test
    public void updateCampaignsData_balanceTidChanged_returnsFalse() {
        boolean res = notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID - 1, dbWalletId,
                notifyOrderDbCampaignChanges
        );

        assertFalse(res);
    }

    @Test
    public void updateCampaignsData_balanceTidChanged_notUpdated() {
        notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID - 1, dbWalletId,
                notifyOrderDbCampaignChanges
        );

        assertNotUpdated();
    }

    @Test
    public void updateCampaignsData_walletIdChanged_returnsFalse() {
        boolean res = notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID, 0L,
                notifyOrderDbCampaignChanges
        );

        assertFalse(res);
    }

    @Test
    public void updateCampaignsData_walletIdChanged_notUpdated() {
        notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID, 0L,
                notifyOrderDbCampaignChanges
        );

        assertNotUpdated();
    }

    @Test
    public void updateCampaignsData_tidAndWalletNotChanged_returnsTrue() {
        boolean res = notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID, dbWalletId,
                notifyOrderDbCampaignChanges
        );

        assertTrue(res);
    }

    @Test
    public void updateCampaignsData_tidAndWalletNotChanged_isUpdated() {
        notifyOrderRepository.updateCampaignData(dslContext, campaignId,
                DB_BALANCE_TID, dbWalletId,
                notifyOrderDbCampaignChanges
        );

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, Collections.singletonList(campaignId));
        assertThat("balance_tid должен был обновиться",
                campaigns.get(0).getBalanceInfo().getBalanceTid(), is(NEW_BALANCE_TID));
    }

    private void assertNotUpdated() {
        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, Collections.singletonList(campaignId));
        assertThat("balance_tid не должен был обновиться",
                campaigns.get(0).getBalanceInfo().getBalanceTid(), is(DB_BALANCE_TID));
    }
}
