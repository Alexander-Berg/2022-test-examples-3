package ru.yandex.direct.core.entity.client.repository;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.AgencyClientRelation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppc.Tables.AGENCY_CLIENT_RELATIONS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AgencyClientRelationRepositoryTest {
    // TODO: Убрать после появления полноценных степов для создания агенств и клиентов
    private static final int SHARD = 1;

    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public AgencyClientRelationRepository agencyClientRelationRepository;
    @Autowired
    private Steps steps;

    private ClientId agencyId;
    private ClientId client1Id;
    private ClientId client2Id;

    @Before
    public void setUp() {
        agencyId = steps.clientSteps()
                .createClient(new ClientInfo().withShard(SHARD))
                .getClientId();
        client1Id = steps.clientSteps()
                .createClient(new ClientInfo().withShard(SHARD))
                .getClientId();
        client2Id = steps.clientSteps()
                .createClient(new ClientInfo().withShard(SHARD))
                .getClientId();
    }

    @Test
    public void testBindClients_NoClients() {
        agencyClientRelationRepository.bindClients(SHARD, agencyId, emptyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindClients_NewClients() {
        List<ClientId> clientIds = Arrays.asList(client1Id, client2Id);

        agencyClientRelationRepository.bindClients(SHARD, agencyId, clientIds);

        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(
                SHARD, clientIds);

        assertThat(
                relations,
                containsInAnyOrder(
                        beanDiffer(
                                new AgencyClientRelation().withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(true).withArchived(false)),
                        beanDiffer(
                                new AgencyClientRelation().withAgencyClientId(agencyId)
                                        .withClientClientId(client2Id)
                                        .withBinded(true).withArchived(false))));
    }

    @Test
    public void testBindClients_BindUnbindedClient() {
        agencyClientRelationRepository.bindClients(SHARD, agencyId, singletonList(client1Id));
        agencyClientRelationRepository.unbindClient(SHARD, client1Id, agencyId);

        agencyClientRelationRepository.bindClients(SHARD, agencyId, singletonList(client1Id));

        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(
                SHARD, singletonList(client1Id));

        assertThat(
                relations,
                contains(
                        beanDiffer(
                                new AgencyClientRelation().withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(true).withArchived(false))));
    }

    @Test
    public void testUnbindClient() {
        agencyClientRelationRepository.bindClients(
                SHARD, agencyId, singletonList(client1Id));

        agencyClientRelationRepository.unbindClient(SHARD, client1Id, agencyId);

        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(
                SHARD, singletonList(client1Id));

        assertThat(
                relations,
                contains(
                        beanDiffer(
                                new AgencyClientRelation().withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(false).withArchived(false))));
    }

    @Test
    public void testArchiveClient() {
        List<ClientId> clientIds = singletonList(client1Id);
        agencyClientRelationRepository.unarchiveClients(SHARD, agencyId, clientIds);

        agencyClientRelationRepository.archiveClients(SHARD, agencyId, clientIds);

        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(SHARD, clientIds);
        assertThat(
                relations,
                contains(
                        beanDiffer(
                                new AgencyClientRelation()
                                        .withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(true)
                                        .withArchived(true))));
    }

    @Test
    public void testUnarchiveClient() {
        List<ClientId> clientIds = singletonList(client1Id);
        agencyClientRelationRepository.archiveClients(SHARD, agencyId, clientIds);

        agencyClientRelationRepository.unarchiveClients(SHARD, agencyId, clientIds);

        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(SHARD, clientIds);
        assertThat(
                relations,
                contains(
                        beanDiffer(
                                new AgencyClientRelation()
                                        .withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(true)
                                        .withArchived(false))));
    }

    @Test
    public void testUnarchiveClientWhichNotExistInAgencyClientRelationTable() {
        List<ClientId> clientIds = singletonList(client1Id);
        dslContextProvider.ppc(SHARD)
                .deleteFrom(AGENCY_CLIENT_RELATIONS)
                .where(AGENCY_CLIENT_RELATIONS.CLIENT_CLIENT_ID.in(clientIds))
                .execute();
        List<AgencyClientRelation> relations = agencyClientRelationRepository.getByClients(SHARD, clientIds);
        assertThat("нет записи по клиенту в таблице agency_client_relations", relations, empty());

        agencyClientRelationRepository.unarchiveClients(SHARD, agencyId, clientIds);

        relations = agencyClientRelationRepository.getByClients(SHARD, clientIds);
        assertThat(
                relations,
                contains(
                        beanDiffer(
                                new AgencyClientRelation()
                                        .withAgencyClientId(agencyId)
                                        .withClientClientId(client1Id)
                                        .withBinded(true)
                                        .withArchived(false))));
    }
}
