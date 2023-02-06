package ru.yandex.direct.core.entity.turbolanding;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParams;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParamsItem;
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsResult;
import ru.yandex.direct.core.entity.turbolanding.service.UpdateCounterGrantsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbqueue.model.DbQueueJob;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RefreshMetrikaGrantsTest {
    private static final Long COUNTER_ID_1 = 1234L;
    private static final Long COUNTER_ID_2 = 5678L;

    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;

    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private DbQueueRepository dbQueueRepository;
    @Autowired
    private PpcRbac ppcRbac;
    @Autowired
    private ClientService clientService;

    private UpdateCounterGrantsService updateCounterGrantsService;

    private Long chiefAgencyRepUid;
    private Long limitedAgencyRepUid;
    private ClientInfo client;
    private ClientInfo agencySubClient;
    private ClientInfo agencySubClient2;
    private Long mainClientRepUid;
    private ClientInfo operator;


    @Before
    public void before() {
        // создаем агентство и представителей агентства
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        chiefAgencyRepUid = agency.getUid();
        var limitedAgencyRep = steps.userSteps().createRepresentative(agency, RbacRepType.LIMITED);
        limitedAgencyRepUid = limitedAgencyRep.getUid();
        // создаем клиентов
        client = steps.clientSteps().createDefaultClient();
        agencySubClient = steps.clientSteps().createDefaultClientUnderAgency(agency);
        mainClientRepUid = steps.userSteps().createRepresentative(agencySubClient, RbacRepType.MAIN).getUid();
        agencySubClient2 = steps.clientSteps().createClientUnderAgency(limitedAgencyRep, new ClientInfo());

        operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);

        MetrikaClient metrikaClient = mock(MetrikaClient.class);
        updateCounterGrantsService = new UpdateCounterGrantsService(
                shardHelper, metrikaClient, dbQueueRepository, ppcRbac, clientService);

        dbQueueSteps.registerJobType(UPDATE_COUNTER_GRANTS_JOB);
        dbQueueSteps.clearQueue(UPDATE_COUNTER_GRANTS_JOB);
    }

    @Test
    public void getRelatedUsersByClientIdTest() {
        List<ClientId> clientIds = mapList(asList(client, agencySubClient, agencySubClient2), ClientInfo::getClientId);
        Map<ClientId, Set<Long>> actualUserIdsByClientId =
                updateCounterGrantsService.getRelatedUsersByClientId(clientIds);

        Map<ClientId, Set<Long>> expectedUserIdsByClientId = ImmutableMap.of(
                client.getClientId(), singleton(client.getUid()),
                agencySubClient.getClientId(), asSet(agencySubClient.getUid(), mainClientRepUid, chiefAgencyRepUid),
                agencySubClient2.getClientId(), asSet(agencySubClient2.getUid(), chiefAgencyRepUid, limitedAgencyRepUid)
        );
        assertThat(actualUserIdsByClientId, beanDiffer(expectedUserIdsByClientId));
    }

    @Test
    public void getRelatedUsersWithManagersByClientIdTest() {
        // создаем менеджерские кампании клиенту
        Campaign campaign = TestCampaigns.newTextCampaign(client.getClientId(), client.getUid())
                .withManagerUid(RandomNumberUtils.nextPositiveLong());
        steps.campaignSteps().createCampaign(campaign, client);
        Campaign archivedCampaign = TestCampaigns.newTextCampaign(client.getClientId(), client.getUid())
                .withManagerUid(campaign.getManagerUid() + 1)
                .withArchived(true);
        steps.campaignSteps().createCampaign(archivedCampaign, client);

        Map<ClientId, Set<Long>> actualUserIdsByClientId =
                updateCounterGrantsService.getRelatedUsersByClientId(Set.of(client.getClientId()));

        Map<ClientId, Set<Long>> expectedUserIdsByClientId = ImmutableMap.of(
                client.getClientId(),
                Set.of(client.getUid(), campaign.getManagerUid(), archivedCampaign.getManagerUid())
        );
        assertThat(actualUserIdsByClientId, beanDiffer(expectedUserIdsByClientId));
    }

    @Test
    public void refreshMetrikaGrantsTest() {
        Map<ClientId, Set<Long>> countersByClientId = ImmutableMap.of(
                client.getClientId(), asSet(COUNTER_ID_1, COUNTER_ID_2),
                agencySubClient.getClientId(), singleton(COUNTER_ID_1),
                agencySubClient2.getClientId(), emptySet()
        );
        Long jobId = updateCounterGrantsService.refreshMetrikaGrants(operator.getUid(), countersByClientId).get(0);

        DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult> job =
                dbQueueRepository.findJobById(operator.getShard(), UPDATE_COUNTER_GRANTS_JOB, jobId);
        checkState(job != null);

        Map<Long, List<Long>> userIdsByCounterId = StreamEx.of(job.getArgs().getItems())
                .toMap(UpdateCounterGrantsParamsItem::getCounterId, UpdateCounterGrantsParamsItem::getUserIds);

        List<Long> userIds1 = asList(client.getUid(), agencySubClient.getUid(), mainClientRepUid, chiefAgencyRepUid);
        List<Long> userIds2 = singletonList(client.getUid());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(userIdsByCounterId.get(COUNTER_ID_1)).is(matchedBy(containsInAnyOrder(userIds1.toArray())));
        soft.assertThat(userIdsByCounterId.get(COUNTER_ID_2)).is(matchedBy(containsInAnyOrder(userIds2.toArray())));
        soft.assertAll();
    }
}
