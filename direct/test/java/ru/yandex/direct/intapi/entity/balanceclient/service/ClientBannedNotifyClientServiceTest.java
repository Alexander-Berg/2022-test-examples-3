package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyClientParameters;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(Parameterized.class)
public class ClientBannedNotifyClientServiceTest {

    private Integer shard;
    private ClientId clientId;

    @Autowired
    private Steps steps;
    @Autowired
    private ClientOptionsRepository clientOptionsRepository;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private NotifyClientService notifyClientService;

    private CampaignInfo walletInfo;
    private TestContextManager testContextManager;

    @Parameterized.Parameter
    public BigDecimal overdraftLimitDirect;
    @Parameterized.Parameter(1)
    public BigDecimal overdraftLimitBalance;
    @Parameterized.Parameter(2)
    public BigDecimal debtDirect;
    @Parameterized.Parameter(3)
    public BigDecimal debtBalance;
    @Parameterized.Parameter(4)
    public Boolean statusBalanceBannedDirect;
    @Parameterized.Parameter(5)
    public boolean statusBalanceBannedBalance;
    @Parameterized.Parameter(6)
    public StatusBsSynced expectedStatusBsSynced;

    @SuppressWarnings("unused")
    @Parameterized.Parameters(name = "overdraft limit in direct = {0}, overdraft limit in balance notification = {1},"
            + " debt in direct = {2}, debt in balance notification = {3},"
            + " client status banned in direct = {4}, client status banned in balance notifiction = {5},"
            + " expecting BS resync queue empty = {6}")
    public static Object[] params() {
        return new Object[][]{
                {BigDecimal.valueOf(10), BigDecimal.valueOf(0), BigDecimal.TEN, BigDecimal.TEN,
                        false, false, StatusBsSynced.NO},
                {BigDecimal.valueOf(0), BigDecimal.valueOf(10), BigDecimal.TEN, BigDecimal.TEN,
                        false, false, StatusBsSynced.NO},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        false, false, StatusBsSynced.YES},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        true, true, StatusBsSynced.YES},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        true, false, StatusBsSynced.NO},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        false, true, StatusBsSynced.NO},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        null, true, StatusBsSynced.NO},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(1), BigDecimal.TEN, BigDecimal.TEN,
                        null, false, StatusBsSynced.YES},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.TEN, BigDecimal.TEN,
                        false, false, StatusBsSynced.YES},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.TEN,
                        false, false, StatusBsSynced.NO},
                {BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.TEN, BigDecimal.ZERO,
                        false, false, StatusBsSynced.NO},
        };
    }

    @Before
    public void before() throws Exception {
        this.testContextManager = new TestContextManager(getClass());
        this.testContextManager.prepareTestInstance(this);

        walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        clientId = walletInfo.getClientInfo().getClientId();

        shard = walletInfo.getShard();
    }

    @Test
    public void testPersonalWalletStatusBsSyncSetToNoOnBalanceClientBan() {
        assumeThat(walletInfo.getCampaign().getStatusBsSynced(), is(StatusBsSynced.YES));
        steps.clientSteps().setOverdraftOptions(
                shard, clientId, overdraftLimitDirect, debtDirect, statusBalanceBannedDirect);
        clientOptionsRepository.updateAutoOverdraftLimit(shard, clientId, BigDecimal.TEN);
        notifyClientService.notifyClient(
                new NotifyClientParameters()
                        .withClientId(clientId.asLong())
                        .withTid(steps.clientSteps().getNextBalanceTid(clientId))
                        .withBalanceCurrency(CurrencyCode.RUB.name())
                        .withOverdraftLimit(overdraftLimitBalance)
                        .withOverdraftSpent(debtBalance)
                        .withStatusBalanceBanned(statusBalanceBannedBalance)
        );
        List<Campaign> campaigns =
                campaignService.getCampaigns(clientId, Collections.singletonList(walletInfo.getCampaignId()));
        assertThat(campaigns, hasItem(hasProperty("statusBsSynced", Matchers.is(expectedStatusBsSynced))));
    }

}
