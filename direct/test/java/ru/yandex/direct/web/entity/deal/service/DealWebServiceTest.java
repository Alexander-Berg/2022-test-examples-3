package ru.yandex.direct.web.entity.deal.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.deal.container.CampaignDeal;
import ru.yandex.direct.core.entity.deal.container.UpdateDealContainer;
import ru.yandex.direct.core.entity.deal.service.DealService;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.campaign.model.CampaignForWebDealDetails;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealWebServiceTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private DealWebService dealWebService;

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
                steps.clientSteps().createClient(new ClientInfo()
                        .withShard(agencyShard).withClient(agency()));
        client1ClientInfo = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(client1Shard));
        clientUser1 = client1ClientInfo.getChiefUserInfo();

        client2ClientInfo = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(client2Shard));
        clientUser2 = client2ClientInfo.getChiefUserInfo();

        //создаем кампании для клиентов
        campaignInfo1 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client1ClientInfo).withCampaign(
                        newTextCampaign(client1ClientInfo.getClientId(), client1ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_DEALS)));
        campaignInfo2 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client2ClientInfo).withCampaign(
                        newTextCampaign(client2ClientInfo.getClientId(), client2ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_DEALS)));
        campaignInfo3 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client1ClientInfo).withCampaign(
                        newTextCampaign(client1ClientInfo.getClientId(), client1ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_DEALS)));
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
        dealService.activateDeals(agencyClientInfo.getClientId(), dealIds, Applicability.FULL);

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
        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        updateDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "линк кампаний успешен");
    }

    @Test
    public void getCampaignsForWebDealDetails() {
        Map<Long, List<CampaignForWebDealDetails>> linkedCampaigns =
                dealWebService.getCampaignsForWebDealDetails(Arrays.asList(dealId, dealIdElse, dealWithoutCampaigns));
        linkedCampaigns.get(dealId).sort(Comparator.comparing(CampaignForWebDealDetails::getCampaignId));

        CampaignForWebDealDetails expectedCampaign1 = new CampaignForWebDealDetails()
                .withCampaignId(cid1)
                .withCampaignName(campaignInfo1.getCampaign().getName())
                .withClientId(campaignInfo1.getClientId().asLong())
                .withUserName(clientUser1.getUser().getLogin());
        CampaignForWebDealDetails expectedCampaign2 = new CampaignForWebDealDetails()
                .withCampaignId(cid2)
                .withCampaignName(campaignInfo2.getCampaign().getName())
                .withClientId(campaignInfo2.getClientId().asLong())
                .withUserName(clientUser2.getUser().getLogin());
        Map<Long, List<CampaignForWebDealDetails>> expectedLinkedCampaigns = new HashMap<>();
        expectedLinkedCampaigns.put(dealId, Arrays.asList(expectedCampaign1, expectedCampaign2));
        expectedLinkedCampaigns.put(dealIdElse, Collections.singletonList(expectedCampaign1));
        expectedLinkedCampaigns.get(dealId).sort(Comparator.comparing(CampaignForWebDealDetails::getCampaignId));

        assertThat(linkedCampaigns, beanDiffer(expectedLinkedCampaigns));
    }

    private Client agency() {
        return TestClients.defaultClient().withRole(RbacRole.AGENCY);
    }
}
