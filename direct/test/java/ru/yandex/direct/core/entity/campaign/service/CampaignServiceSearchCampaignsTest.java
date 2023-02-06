package ru.yandex.direct.core.entity.campaign.service;

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

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignServiceSearchCampaignsTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private Steps steps;

    private int agencyShard = 1;
    private int client1Shard = 1;
    private int client2Shard = 2;
    private int client3Shard = 2;

    private ClientInfo agencyClientInfo;
    private ClientInfo client1ClientInfo;
    private ClientInfo client2ClientInfo;
    private ClientInfo client3ClientInfo;
    private ClientId clientId1, clientId2, clientId3;
    private Long cid1, cid2, cid3, cid4;

    @Before
    public void setUp() {
        //создаем клиентов
        agencyClientInfo =
                steps.clientSteps().createClient(new ClientInfo().withShard(agencyShard).withClient(agency()));
        client1ClientInfo = steps.clientSteps()
                .createClientUnderAgency(agencyClientInfo, new ClientInfo().withShard(client1Shard));
        clientId1 = client1ClientInfo.getClientId();
        client2ClientInfo = steps.clientSteps()
                .createClientUnderAgency(agencyClientInfo, new ClientInfo().withShard(client2Shard));
        clientId2 = client2ClientInfo.getClientId();
        client3ClientInfo =steps.clientSteps()
                .createClientUnderAgency(agencyClientInfo, new ClientInfo().withShard(client3Shard));
        clientId3 = client3ClientInfo.getClientId();

        //создаем кампании для клиентов
        CampaignInfo campaignInfo1 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client1ClientInfo).withCampaign(
                        newTextCampaign(clientId1, client1ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_BANNER)));
        CampaignInfo campaignInfo2 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client2ClientInfo).withCampaign(
                        newTextCampaign(clientId2, client2ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_BANNER)));
        CampaignInfo campaignInfo3 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client3ClientInfo).withCampaign(
                        newTextCampaign(clientId3, client3ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.CPM_BANNER)));
        CampaignInfo campaignInfo4 =
                steps.campaignSteps().createCampaign(new CampaignInfo().withClientInfo(client3ClientInfo).withCampaign(
                        newTextCampaign(clientId3, client3ClientInfo.getUid())
                                .withAgencyId(agencyClientInfo.getClientId().asLong())
                                .withAgencyUid(agencyClientInfo.getUid())
                                .withType(CampaignType.TEXT)));
        cid1 = campaignInfo1.getCampaignId();
        cid2 = campaignInfo2.getCampaignId();
        cid3 = campaignInfo3.getCampaignId();
        cid4 = campaignInfo4.getCampaignId();
    }

    @Test
    public void searchCampaignsByClientIdTest() {
        List<CampaignSimple> campaigns = campaignService.searchCampaignsByClientIdAndTypes(clientId3,
                Collections.singletonList(ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_BANNER));
        assertThat(campaigns, beanDiffer(Collections.singletonList((CampaignSimple) new Campaign().withId(cid3)))
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void searchCampaignsByClientIdsTest() {
        Map<Long, List<CampaignSimple>> campaigns =
                campaignService.searchCampaignsByClientIdsAndTypes(
                        Arrays.asList(clientId1.asLong(), clientId2.asLong(), clientId3.asLong()),
                        Arrays.asList(ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_BANNER,
                                ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT));
        campaigns.getOrDefault(clientId3.asLong(), emptyList()).sort(Comparator.comparing(CampaignSimple::getId));

        CampaignSimple expectedCampaign1 = new Campaign().withId(cid1);
        CampaignSimple expectedCampaign2 = new Campaign().withId(cid2);
        CampaignSimple expectedCampaign3 = new Campaign().withId(cid3);
        CampaignSimple expectedCampaign4 = new Campaign().withId(cid4);
        Map<Long, List<CampaignSimple>> expectedCampaigns = new HashMap<>();
        expectedCampaigns.put(clientId1.asLong(), Collections.singletonList(expectedCampaign1));
        expectedCampaigns.put(clientId2.asLong(), Collections.singletonList(expectedCampaign2));
        expectedCampaigns.put(clientId3.asLong(), Arrays.asList(expectedCampaign3, expectedCampaign4));
        expectedCampaigns.get(clientId3.asLong()).sort(Comparator.comparing(CampaignSimple::getId));

        assertThat(campaigns,
                beanDiffer(expectedCampaigns).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    private Client agency() {
        return TestClients.defaultClient().withRole(RbacRole.AGENCY);
    }
}
