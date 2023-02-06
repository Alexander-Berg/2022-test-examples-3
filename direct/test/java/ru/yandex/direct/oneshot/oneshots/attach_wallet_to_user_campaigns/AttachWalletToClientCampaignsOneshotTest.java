package ru.yandex.direct.oneshot.oneshots.attach_wallet_to_user_campaigns;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueItem;
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueObjType;
import ru.yandex.direct.core.entity.balance.model.BalanceNotificationInfo;
import ru.yandex.direct.core.entity.balance.repository.BalanceInfoQueueRepository;
import ru.yandex.direct.core.entity.balance.service.BalanceInfoQueueService;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignBalanceService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;

@OneshotTest
@RunWith(SpringRunner.class)
public class AttachWalletToClientCampaignsOneshotTest {

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private BalanceInfoQueueService balanceInfoQueueService;
    @Autowired
    private BalanceInfoQueueRepository balanceInfoQueueRepository;
    @Mock
    private CampaignBalanceService campaignBalanceService;


    private AttachWalletToClientCampaignsOneshot oneshot;

    private int shard;
    private ClientId clientId;
    private Long walletId;
    private Long campaignId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        walletId = steps.campaignSteps().createWalletCampaign(clientInfo).getCampaignId();

        campaignId = steps.campaignSteps()
                .createCampaign(newTextCampaign(clientId, clientInfo.getUid()), clientInfo).getCampaignId();

        oneshot = new AttachWalletToClientCampaignsOneshot(dslContextProvider, shardHelper, clientService,
                campaignBalanceService, campaignTypedRepository, balanceInfoQueueService);

        when(campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(anyLong(), any(), any(), anyList()))
                .thenReturn(true)
                .thenReturn(false);
    }

    /**
     * Проверка привязывания кошелька к кампании клиента
     */
    @Test
    public void execute() {
        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        oneshot.execute(inputData, null);

        Campaign campaign = campaignRepository.getCampaigns(shard, singleton(campaignId)).get(0);

        assertThat(campaign.getWalletId()).isEqualTo(walletId);
    }

    /**
     * Проверка привязывания кошелька к нескольким кампаниям клиента
     */
    @Test
    public void execute_WhenUserHaveTwoCampaigns() {
        var campaignId2 = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();

        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        oneshot.execute(inputData, null);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, List.of(campaignId, campaignId2));
        Map<Long, Long> campaignIdToWalletId = StreamEx.of(campaigns)
                .mapToEntry(Campaign::getWalletId)
                .mapKeys(Campaign::getId)
                .toMap();

        Map<Long, Long> expectResult = Map.of(campaignId, walletId, campaignId2, walletId);
        MatcherAssert.assertThat(campaignIdToWalletId, beanDiffer(expectResult));
    }

    /**
     * Проверка привязывания кошелька для двух клиентов на разных шардах
     */
    @Test
    public void execute_FroTwoDifferentClientsInDifferentShards() {
        var clientInfo2 = steps.clientSteps().createDefaultClientAnotherShard();
        var walletId2 = steps.campaignSteps().createWalletCampaign(clientInfo2).getCampaignId();
        var campaignId2 = steps.campaignSteps().createActiveCampaign(clientInfo2).getCampaignId();

        InputData inputData = new InputData()
                .withClientIds(List.of(clientId.asLong(), clientInfo2.getClientId().asLong()));

        oneshot.execute(inputData, null);

        Campaign campaign1 = campaignRepository.getCampaigns(shard, singleton(campaignId)).get(0);
        Campaign campaign2 = campaignRepository.getCampaigns(clientInfo2.getShard(), singleton(campaignId2)).get(0);
        Map<Long, Long> campaignIdToWalletId =
                Map.of(campaign1.getId(), campaign1.getWalletId(), campaign2.getId(), campaign2.getWalletId());

        Map<Long, Long> expectResult = Map.of(campaignId, walletId, campaignId2, walletId2);
        MatcherAssert.assertThat(campaignIdToWalletId, beanDiffer(expectResult));
    }

    /**
     * Проверка привязывания кошелька к нескольким кампаниям клиента, когода одна из кампаний уже привязана
     */
    @Test
    public void execute_WhenUserHasUnderWalletCampaign_NotChanged() {
        var campaignId2 = steps.campaignSteps().createCampaignUnderWalletByCampaignType(CampaignType.TEXT,
                clientInfo, walletId, BigDecimal.ZERO).getCampaignId();

        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        oneshot.execute(inputData, null);

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, List.of(campaignId, campaignId2));
        Map<Long, Long> campaignIdToWalletId = StreamEx.of(campaigns)
                .mapToEntry(Campaign::getWalletId)
                .mapKeys(Campaign::getId)
                .toMap();

        Map<Long, Long> expectResult = Map.of(campaignId, walletId, campaignId2, walletId);
        MatcherAssert.assertThat(campaignIdToWalletId, beanDiffer(expectResult));
    }

    /**
     * Проверка отправки в баланс двух кампаний и кошелька
     */
    @Test
    public void execute_checkSendCampaignsAndWalletToBalance() {
        var campaignId2 = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();

        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        oneshot.execute(inputData, null);

        ArgumentCaptor<List<? extends CommonCampaign>> campaignsCaptor = ArgumentCaptor.forClass(List.class);
        verify(campaignBalanceService, times(2)).createOrUpdateOrdersOnFirstCampaignCreation(
                eq(clientInfo.getUid()), eq(null), eq(null), campaignsCaptor.capture());

        List<List<? extends CommonCampaign>> walletAndCampaignIdsSentToBalance = campaignsCaptor.getAllValues();

        SoftAssertions.assertSoftly(softly -> {
            Set<Long> walletIdsSentToBalance = StreamEx.of(walletAndCampaignIdsSentToBalance.get(0))
                    .map(BaseCampaign::getId)
                    .toSet();
            softly.assertThat(walletIdsSentToBalance)
                    .as("Id кошелька отправленного в баланс")
                    .isEqualTo(Set.of(walletId));

            Set<Long> campaignIdsSentToBalance = StreamEx.of(walletAndCampaignIdsSentToBalance.get(1))
                    .map(BaseCampaign::getId)
                    .toSet();
            softly.assertThat(campaignIdsSentToBalance)
                    .as("Id кампаний отправленных в баланс")
                    .isEqualTo(Set.of(campaignId, campaignId2));
        });
    }

    /**
     * Проверка добавления в очередь на переотправку в баланс
     */
    @Test
    public void execute_checkAddCampaignsToQueueOfSendingToBalance() {
        var campaignId2 = steps.campaignSteps().createActiveCampaign(clientInfo).getCampaignId();

        InputData inputData = new InputData()
                .withClientIds(singletonList(clientId.asLong()));

        oneshot.execute(inputData, null);

        var balanceNotificationInfo = List.of(
                new BalanceNotificationInfo()
                        .withCidOrUid(campaignId)
                        .withObjType(BalanceInfoQueueObjType.CID),
                new BalanceNotificationInfo()
                        .withCidOrUid(campaignId2)
                        .withObjType(BalanceInfoQueueObjType.CID)
        );

        List<BalanceInfoQueueItem> balanceInfoQueueItems = balanceInfoQueueRepository
                .getExistingRecordsInWaitStatus(dslContextProvider.ppc(shard), balanceNotificationInfo);

        Set<Long> campaignIds = StreamEx.of(balanceInfoQueueItems)
                .map(BalanceInfoQueueItem::getCidOrUid)
                .toSet();
        assertThat(campaignIds)
                .as("Кампании в очереди на переотправку в баланс")
                .containsOnly(campaignId, campaignId2);
    }
}
