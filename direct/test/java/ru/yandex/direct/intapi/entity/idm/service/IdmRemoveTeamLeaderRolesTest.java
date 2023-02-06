package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.ManagerHierarchyInfo;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.client.service.ManagerHierarchyService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.rbac.PpcRbac;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_MANAGERS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmRemoveTeamLeaderRolesTest {

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private PpcRbac ppcRbac;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private ManagerHierarchyService managerHierarchyService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private RbacClientsRelations rbacClientsRelations;

    private IdmNotificationMailSenderService mailSenderService;
    private IdmRemoveRoleService idmRemoveRoleService;

    private UserInfo teamLeader;
    private UserInfo teamLeaderWithSubordinates;
    private UserInfo superTeamLeader;
    private UserInfo superTeamLeaderWithSubordinates;
    private UserInfo manager;
    private UserInfo managerWithSupervisor;

    @Before
    public void setUp() {
        mailSenderService = mock(IdmNotificationMailSenderService.class);

        idmRemoveRoleService = new IdmRemoveRoleService(userService, clientService,
                ppcRbac, managerHierarchyService, mailSenderService,
                rbacClientsRelations);

        teamLeader = createManagerWithSubRole(RbacSubrole.TEAMLEADER);
        setManagerHierarchy(teamLeader.getClientInfo(), emptyList());

        teamLeaderWithSubordinates = createManagerWithSubRole(RbacSubrole.TEAMLEADER);
        managerWithSupervisor = createManagerWithSubRole(null);
        setManagerHierarchy(teamLeaderWithSubordinates.getClientInfo(),
                singletonList(managerWithSupervisor.getClientInfo()));

        superTeamLeader = createManagerWithSubRole(RbacSubrole.SUPERTEAMLEADER);
        setManagerHierarchy(superTeamLeader.getClientInfo(), emptyList());

        superTeamLeaderWithSubordinates = createManagerWithSubRole(RbacSubrole.SUPERTEAMLEADER);
        UserInfo anotherTeamLeader = createManagerWithSubRole(RbacSubrole.TEAMLEADER);
        setManagerHierarchy(superTeamLeaderWithSubordinates.getClientInfo(),
                singletonList(anotherTeamLeader.getClientInfo()));

        manager = createManagerWithSubRole(null);
        setManagerHierarchy(manager.getClientInfo(), emptyList());
    }

    @Test
    public void removeRole_whenTeamLeaderHasNotSubordinates() {
        removeRole(IdmRole.TEAMLEADER, teamLeader);
        checkUser(teamLeader.getUid(), RbacRole.MANAGER, null, false);
        verify(mailSenderService, never()).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenTeamLeaderHasSubordinates() {
        removeRole(IdmRole.TEAMLEADER, teamLeaderWithSubordinates);
        checkUser(teamLeaderWithSubordinates.getUid(), RbacRole.MANAGER, RbacSubrole.TEAMLEADER, true);
        verify(mailSenderService).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenSuperTeamLeaderHasNotSubordinates() {
        removeRole(IdmRole.SUPERTEAMLEADER, superTeamLeader);
        checkUser(superTeamLeader.getUid(), RbacRole.MANAGER, null, false);
        verify(mailSenderService, never()).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenSuperTeamLeaderHasSubordinates() {
        removeRole(IdmRole.SUPERTEAMLEADER, superTeamLeaderWithSubordinates);
        checkUser(superTeamLeaderWithSubordinates.getUid(), RbacRole.MANAGER, RbacSubrole.SUPERTEAMLEADER, true);
        verify(mailSenderService).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenRoleIsManager() {
        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.EMPTY, null, false);
        verify(mailSenderService, never()).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenManagerHasCampaigns() {
        steps.campaignSteps().createCampaign(activeTextCampaign(null, null)
                .withManagerUid(manager.getUid()));

        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.MANAGER, null, true);
        verify(mailSenderService).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenManagerHasAgency() {
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        testClientRepository.bindManagerToAgency(agency.getShard(), agency.getClientId(), manager.getUser());

        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.MANAGER, null, true);
        verify(mailSenderService).sendRemoveRoleEmail(any());
    }

    @Test
    public void removeRole_whenManagerHasClients() {
        ClientInfo client = steps.clientSteps().createDefaultClient();
        testClientRepository.bindManagerToClient(client.getShard(), client.getClientId(), manager.getUid());

        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.EMPTY, null, false);

        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(client.getShard(), singletonList(client.getClientId().asLong()));
        assertTrue(clientsManagers.isEmpty());
    }

    @Test
    public void removeRole_whenManagerHasClientsWithEmptyCampaigns() {
        Campaign emptyCampaign = activeTextCampaign(null, null)
                .withManagerUid(manager.getUid())
                .withStatusEmpty(true);

        ClientInfo client = steps.campaignSteps().createCampaign(emptyCampaign).getClientInfo();
        testClientRepository.bindManagerToClient(client.getShard(), client.getClientId(), manager.getUid());

        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.EMPTY, null, false);

        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(client.getShard(), singletonList(client.getClientId().asLong()));
        assertThat(clientsManagers, equalTo(
                ImmutableMap.of(client.getClientId().asLong(), singletonList(manager.getUid()))));
    }

    @Test
    public void removeRole_whenManagerHasSupervisor() {
        removeRole(IdmRole.MANAGER, managerWithSupervisor);
        checkUser(managerWithSupervisor.getUid(), RbacRole.EMPTY, null, false);

        List<ManagerHierarchyInfo> managersInfo = managerHierarchyService.massGetManagerData(
                mapList(asList(managerWithSupervisor, teamLeaderWithSubordinates),
                        m -> m.getClientInfo().getClientId()));

        ManagerHierarchyInfo expectedTeamLeaderInfo = new ManagerHierarchyInfo()
                .withManagerClientId(teamLeaderWithSubordinates.getClientInfo().getClientId())
                .withSubordinatesClientId(emptyList())
                .withSubordinatesUid(emptyList());

        assertThat(managersInfo, contains(beanDiffer(expectedTeamLeaderInfo)
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void removeTwoRoles_whenRoleIsTeamLeader() {
        removeRole(IdmRole.TEAMLEADER, teamLeader);
        removeRole(IdmRole.MANAGER, teamLeader);
        checkUser(teamLeader.getUid(), RbacRole.EMPTY, null, false);
    }

    @Test
    public void removeTwoRoles_whenRoleIsSuperTeamLeader() {
        removeRole(IdmRole.SUPERTEAMLEADER, superTeamLeader);
        removeRole(IdmRole.MANAGER, superTeamLeader);
        checkUser(superTeamLeader.getUid(), RbacRole.EMPTY, null, false);
    }

    @Test
    public void removeManagerRole_whenNoRecordInManagerHierarchy() {
        managerHierarchyService.deleteManagerData(manager.getClientId());
        removeRole(IdmRole.MANAGER, manager);
        checkUser(manager.getUid(), RbacRole.EMPTY, null, false);
    }

    private UserInfo createManagerWithSubRole(RbacSubrole subRole) {
        RbacRole role = RbacRole.MANAGER;
        UserInfo userInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(role).getChiefUserInfo();
        clientService.updateClientRole(userInfo.getClientInfo().getClientId(), role, subRole);
        userInfo.getUser().withSubRole(subRole);
        return userInfo;
    }

    public void setManagerHierarchy(ClientInfo leader, Collection<ClientInfo> subordinates) {
        try {
            testClientRepository.setManagerHierarchy(leader, subordinates);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Long, List<Long>> getClientsManagers(int shard, Collection<Long> clientIds) {
        return StreamEx.of(dslContextProvider.ppc(shard)
                        .select(CLIENT_MANAGERS.CLIENT_ID, CLIENT_MANAGERS.MANAGER_UID)
                        .from(CLIENT_MANAGERS)
                        .where(CLIENT_MANAGERS.CLIENT_ID.in(clientIds))
                        .stream())
                .mapToEntry(CLIENT_MANAGERS.CLIENT_ID::getValue, CLIENT_MANAGERS.MANAGER_UID::getValue)
                .collapseKeys()
                .toMap();
    }

    private void removeRole(IdmRole role, UserInfo userInfo) {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole(role.getTypedValue())
                .withPassportLogin(userInfo.getUser().getLogin())
                .withDomainLogin(userInfo.getUser().getDomainLogin());

        IdmResponse response = idmRemoveRoleService.removeRole(request);
        assumeThat(response.getCode(), is(0));
    }

    private void checkUser(Long uid, RbacRole expectedRole, RbacSubrole expectedSubRole,
                           Boolean expectedStatusBlocked) {
        User actualUser = userService.getUser(uid);

        User expectedUser = new User()
                .withRole(expectedRole)
                .withSubRole(expectedSubRole)
                .withStatusBlocked(expectedStatusBlocked);

        assertThat(actualUser, beanDiffer(expectedUser).useCompareStrategy(onlyFields(
                newPath(User.ROLE.name()), newPath(User.SUB_ROLE.name()), newPath(User.STATUS_BLOCKED.name()))));
    }
}
