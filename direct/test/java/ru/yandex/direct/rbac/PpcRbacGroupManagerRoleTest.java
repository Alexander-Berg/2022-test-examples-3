package ru.yandex.direct.rbac;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.model.RbacAccessType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.Clients.CLIENTS;
import static ru.yandex.direct.rbac.model.RbacAccessType.NONE;
import static ru.yandex.direct.rbac.model.RbacAccessType.READ_WRITE;

@RunWith(SpringJUnit4ClassRunner.class)
@CoreTest
public class PpcRbacGroupManagerRoleTest {
    @Autowired
    PpcRbac ppcRbac;
    @Autowired
    RbacService rbacService;
    @Autowired
    Steps steps;
    @Autowired
    DslContextProvider dslContextProvider;
    @Autowired
    ShardHelper shardHelper;

    private ClientInfo manager;
    private ClientInfo servicedClient;

    @Before
    public void setUp() throws Exception {
        manager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        servicedClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
    }

    @Test
    public void grantAccess_groupRolePresent() {
        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());
        addGroupAccess(groupId, servicedClient.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(READ_WRITE);
    }

    @Test
    public void noAccess_whenNoGroupAccess() {
        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(NONE);
    }

    @Test
    public void noAccess_noGroupMembership() {
        long groupId = createGroup();
        addGroupAccess(groupId, servicedClient.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(NONE);
    }

    @Test
    public void noAccess_differentGroups() {
        long groupOneId = createGroup();
        long groupTwoId = createGroup();
        addGroupMembership(groupOneId, manager.getClientId());
        addGroupAccess(groupTwoId, servicedClient.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(NONE);
    }

    @Test
    public void success_sameGroupManager() {
        ClientInfo managerTwo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());
        addGroupMembership(groupId, managerTwo.getClientId());

        assertThat(getAccessType(manager.getUid(), managerTwo.getUid())).isEqualTo(READ_WRITE);
    }

    @Test
    public void noAccess_differentGroupManager() {
        ClientInfo managerTwo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        long groupOneId = createGroup();
        addGroupMembership(groupOneId, manager.getClientId());
        long groupTwoId = createGroup();
        addGroupMembership(groupTwoId, managerTwo.getClientId());

        assertThat(getAccessType(manager.getUid(), managerTwo.getUid())).isEqualTo(NONE);
    }

    @Test
    public void success_forAgency() {
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);

        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());
        addGroupAccess(groupId, agency.getClientId());

        assertThat(getAccessType(manager.getUid(), agency.getUid())).isEqualTo(READ_WRITE);
    }

    @Test
    public void success_forAgencySubclient() {
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        UserInfo limitedAgency = steps.userSteps().createUser(agency, RbacRepType.LIMITED);
        setAgency(servicedClient, limitedAgency);

        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());
        addGroupAccess(groupId, agency.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(READ_WRITE);
    }

    @Test
    public void noAccess_differentAgencySubclient() {
        // К клиенту servicedClient имеет доступ agencyOne
        ClientInfo agencyOne = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        UserInfo limitedAgencyOne = steps.userSteps().createUser(agencyOne, RbacRepType.LIMITED);

        setAgency(servicedClient, limitedAgencyOne);

        // Менеджер manager имеет групповой доступ только к agencyTwo
        ClientInfo agencyTwo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        //noinspection unused
        UserInfo limitedAgencyTwo = steps.userSteps().createUser(agencyTwo, RbacRepType.LIMITED);

        long groupId = createGroup();
        addGroupMembership(groupId, manager.getClientId());
        addGroupAccess(groupId, agencyTwo.getClientId());

        assertThat(getAccessType(manager.getUid(), servicedClient.getUid())).isEqualTo(NONE);
    }

    private void addGroupAccess(Long groupId, ClientId subjectClientId) {
        steps.idmGroupSteps().addGroupAccess(groupId, subjectClientId);
    }

    private void addGroupMembership(Long groupId, ClientId managerClientId) {
        steps.idmGroupSteps().addGroupMembership(groupId, managerClientId);
    }

    private long createGroup() {
        return steps.idmGroupSteps().createManagerIdmGroup();
    }

    private void setAgency(ClientInfo client, UserInfo agency) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.AGENCY_CLIENT_ID, agency.getClientInfo().getClientId().asLong())
                .set(CLIENTS.AGENCY_UID, agency.getUid())
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
    }

    private RbacAccessType getAccessType(Long operatorUid, Long clientUid) {
        return ppcRbac.getAccessTypes(operatorUid, singletonList(clientUid)).get(clientUid);
    }

}
