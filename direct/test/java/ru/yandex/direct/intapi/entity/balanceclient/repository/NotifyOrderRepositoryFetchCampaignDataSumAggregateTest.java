package ru.yandex.direct.intapi.entity.balanceclient.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.AggregatingSumStatus;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestWalletCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyOrderRepositoryFetchCampaignDataSumAggregateTest {

    @Autowired
    private NotifyOrderRepository notifyOrderRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private TestWalletCampaignRepository testWalletCampaignRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo clientInfo;
    private int shard;
    private ClientId clientId;
    private Long uid;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        uid = clientInfo.getUid();
    }

    @Test
    public void fetchCampaignData_GetWalletWithHavingAggregatedStatus_IsSumAggregatedYes() {
        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.activeWalletCampaign(clientId, uid), clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        testWalletCampaignRepository
                .addDefaultWalletWithMigrationStatus(shard, campaignId, AggregatingSumStatus.YES);

        CampaignDataForNotifyOrder dbData = notifyOrderRepository.fetchCampaignData(shard, campaignId);
        assertThat(dbData.getWalletAggregateMigrated(), is(AggregatingSumStatus.YES));
    }

    @Test
    public void fetchCampaignData_GetTextCampaignOnWalletWithHavingAggregatedStatus_IsSumAggregatedYes() {
        Long walletId = testWalletCampaignRepository
                .addDefaultWalletWithMigrationStatus(shard, AggregatingSumStatus.YES);

        CampaignInfo campaignInfo = steps.campaignSteps()
                .createCampaign(TestCampaigns.activeTextCampaign(clientId, uid), clientInfo);
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.WALLET_CID, walletId)
                .where(CAMPAIGNS.CID.eq(campaignInfo.getCampaignId()))
                .execute();

        CampaignDataForNotifyOrder dbData =
                notifyOrderRepository.fetchCampaignData(shard, campaignInfo.getCampaignId());

        assertThat(dbData.getWalletAggregateMigrated(), is(AggregatingSumStatus.YES));
    }
}
