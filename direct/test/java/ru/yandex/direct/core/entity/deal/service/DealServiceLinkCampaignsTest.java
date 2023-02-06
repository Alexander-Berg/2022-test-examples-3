package ru.yandex.direct.core.entity.deal.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.deal.container.CampaignDeal;
import ru.yandex.direct.core.entity.deal.container.UpdateDealContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmDealsCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealServiceLinkCampaignsTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    private int agencyShard = 1;
    private int client1Shard = 1;
    private int client2Shard = 2;


    private ClientInfo agencyClientInfo;
    private ClientInfo client1ClientInfo;
    private ClientInfo client2ClientInfo;
    private UserInfo clientUser1;
    private UserInfo clientUser2;
    private Long dealId, dealIdElse, dealWithoutCampaigns;
    private CampaignInfo campaignInfo1;
    private CampaignInfo campaignInfo2;
    private CampaignInfo campaignInfo3;
    private Long cid1, cid2, cid3;

    @Before
    public void setUp() {
        //создаем клиентов
        agencyClientInfo =
                steps.clientSteps().createClient(new ClientInfo().withShard(agencyShard)
                        .withClient(defaultClient().withRole(RbacRole.AGENCY)));
        clientUser1 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(client1Shard)).getChiefUserInfo();
        client1ClientInfo = clientUser1.getClientInfo();
        clientUser2 = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(client2Shard)).getChiefUserInfo();
        client2ClientInfo = clientUser2.getClientInfo();

        //создаем кампании для клиентов
        campaignInfo1 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client1ClientInfo).withCampaign(
                        newTextCampaign(client1ClientInfo.getClientId(), client1ClientInfo.getUid())
                                .withType(CampaignType.CPM_DEALS)
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())));
        campaignInfo2 =
                steps.campaignSteps().createCampaign(activeCpmDealsCampaign(client2ClientInfo.getClientId(),
                        client2ClientInfo.getUid())
                        .withAgencyId(agencyClientInfo.getClientId().asLong())
                        .withAgencyUid(agencyClientInfo.getUid()),
                        client2ClientInfo);
        campaignInfo3 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client1ClientInfo).withCampaign(
                        newTextCampaign(client1ClientInfo.getClientId(), client1ClientInfo.getUid())
                                .withType(CampaignType.CPM_DEALS)
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())));
        cid1 = campaignInfo1.getCampaignId();
        cid2 = campaignInfo2.getCampaignId();
        cid3 = campaignInfo3.getCampaignId();

        //добавляем сделки
        List<DealInfo> createdDeals = steps.dealSteps().addRandomDeals(agencyClientInfo, 3);
        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        dealId = dealIds.get(0);
        dealIdElse = dealIds.get(1);
        dealWithoutCampaigns = dealIds.get(2);

        //привязываеть кампании можно только к активным сделкам
        dealService.activateDeals(agencyClientInfo.getClientId(), dealIds, Applicability.PARTIAL);

        //привязываем 2 кампании из разных шардов
        UpdateDealContainer updateDealContainer = new UpdateDealContainer();
        updateDealContainer.withAdded(
                asList(
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealId),
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealIdElse),
                        new CampaignDeal().withCampaignId(cid2)
                                .withDealId(dealId)
                ));
        CampaignSimple campaign2 = campaignRepository.getCampaignsSimple(client2Shard, singletonList(cid2)).get(cid2);
        assumeThat(campaign2.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        updateDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "линк кампаний успешен");
    }

    @Test
    public void getLinkedCampaignsByClientId() {
        Map<Long, List<Long>> linkedCampaigns =
                dealService.getLinkedCampaignsByClientId(agencyClientInfo.getClientId());
        Collections.sort(linkedCampaigns.getOrDefault(dealId, emptyList()));

        Map<Long, List<Long>> expectedKLinkedCampaigns = new HashMap<>();
        expectedKLinkedCampaigns.put(dealId, Arrays.asList(cid1, cid2));
        expectedKLinkedCampaigns.put(dealIdElse, Collections.singletonList(cid1));
        Collections.sort(expectedKLinkedCampaigns.get(dealId));

        assertThat(linkedCampaigns, beanDiffer(expectedKLinkedCampaigns));
    }

    @Test
    public void getLinkedCampaignsByDealIds() {
        Map<Long, List<Long>> linkedCampaigns =
                dealService.getLinkedCampaignsByDealIds(Arrays.asList(dealId, dealIdElse, dealWithoutCampaigns));
        Collections.sort(linkedCampaigns.getOrDefault(dealId, emptyList()));

        Map<Long, List<Long>> expectedKLinkedCampaigns = new HashMap<>();
        expectedKLinkedCampaigns.put(dealId, Arrays.asList(cid1, cid2));
        expectedKLinkedCampaigns.put(dealIdElse, Collections.singletonList(cid1));
        Collections.sort(expectedKLinkedCampaigns.get(dealId));

        assertThat(linkedCampaigns, beanDiffer(expectedKLinkedCampaigns));
    }

    @Test
    public void unlinkCampaigns() {
        //отвязываем одну из кампаний
        UpdateDealContainer updateDealContainer = new UpdateDealContainer();
        updateDealContainer.withRemoved(
                singletonList(
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealId)
                ));

        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        updateDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "линк кампаний успешен");

        Map<Long, List<Long>> linkedCampaigns =
                dealService.getLinkedCampaignsByClientId(agencyClientInfo.getClientId());

        Map<Long, List<Long>> expectedLinkedCampaigns = new HashMap<>();
        expectedLinkedCampaigns.put(dealId, Collections.singletonList(cid2));
        expectedLinkedCampaigns.put(dealIdElse, Collections.singletonList(cid1));

        assertThat(linkedCampaigns, beanDiffer(expectedLinkedCampaigns));
    }

    @Test
    public void getLinkedDealsByCampaignsIds() {
        Map<Long, List<Long>> linkedDeals = dealService.getLinkedDealsByCampaignsIds(Arrays.asList(cid1, cid2, cid3));
        Collections.sort(linkedDeals.get(cid1));

        Map<Long, List<Long>> expectedLinkedDeals = new HashMap<>();
        expectedLinkedDeals.put(cid1, Arrays.asList(dealId, dealIdElse));
        expectedLinkedDeals.put(cid2, Collections.singletonList(dealId));
        Collections.sort(expectedLinkedDeals.get(cid1));

        assertThat(linkedDeals, beanDiffer(expectedLinkedDeals));
    }

    @Test
    public void getLinkedDealsByCampaignsIdsInShard() {
        Map<Long, List<Long>> linkedDeals =
                dealService.getLinkedDealsByCampaignsIdsInShard(client1Shard, Arrays.asList(cid1, cid2, cid3));
        Collections.sort(linkedDeals.get(cid1));

        Map<Long, List<Long>> expectedLinkedDeals = new HashMap<>();
        expectedLinkedDeals.put(cid1, Arrays.asList(dealId, dealIdElse));
        Collections.sort(expectedLinkedDeals.get(cid1));

        assertThat(linkedDeals, beanDiffer(expectedLinkedDeals));
    }

    @Test
    public void getStatusBsSyncedOfActiveCampaign() {
        CampaignSimple campaign2 = campaignRepository.getCampaignsSimple(client2Shard, singletonList(cid2)).get(cid2);
        assertThat(campaign2.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    public void unlinkCampaignAndCheckStatusBsSynced() {
        campaignRepository.updateStatusBsSynced(client1Shard, singletonList(cid1), StatusBsSynced.YES);
        CampaignSimple campaign = campaignRepository.getCampaignsSimple(client1Shard, singletonList(cid1)).get(cid1);
        assumeThat(campaign.getStatusBsSynced(), equalTo(StatusBsSynced.YES));
        //отвязываем одну из кампаний
        UpdateDealContainer updateDealContainer = new UpdateDealContainer();
        updateDealContainer.withRemoved(
                singletonList(
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealId)
                ));

        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        updateDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "анлинк кампаний успешен");

        campaign = campaignRepository.getCampaignsSimple(client1Shard, singletonList(cid1)).get(cid1);
        assertThat(campaign.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    public void unlinkCampaignAndLinkAgain() {
        //отвязываем одну из кампаний
        UpdateDealContainer unlinkDealContainer = new UpdateDealContainer();
        unlinkDealContainer.withRemoved(
                singletonList(
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealId)
                ));

        Result<UpdateDealContainer> unlinkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        unlinkDealContainer);
        checkState(unlinkResult.getErrors().isEmpty(), "анлинк кампаний успешен");

        //привязываем кампанию обратно
        UpdateDealContainer linkDealContainer = new UpdateDealContainer();
        linkDealContainer.withAdded(
                singletonList(
                        new CampaignDeal().withCampaignId(cid1)
                                .withDealId(dealId)
                ));

        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        linkDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "линк кампаний успешен");

        Map<Long, List<Long>> linkedCampaigns =
                dealService.getLinkedCampaignsByClientId(agencyClientInfo.getClientId());
        Collections.sort(linkedCampaigns.getOrDefault(dealId, emptyList()));

        Map<Long, List<Long>> expectedLinkedCampaigns = new HashMap<>();
        expectedLinkedCampaigns.put(dealId, Arrays.asList(cid1, cid2));
        expectedLinkedCampaigns.put(dealIdElse, Collections.singletonList(cid1));
        Collections.sort(expectedLinkedCampaigns.get(dealId));

        assertThat(linkedCampaigns, beanDiffer(expectedLinkedCampaigns));
    }

    private Client agency() {
        return TestClients.defaultClient().withRole(RbacRole.AGENCY);
    }
}
