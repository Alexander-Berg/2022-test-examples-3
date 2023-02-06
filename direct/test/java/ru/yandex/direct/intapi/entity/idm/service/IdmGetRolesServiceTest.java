package ru.yandex.direct.intapi.entity.idm.service;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.intapi.entity.idm.model.IdmUserRole;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGetRolesServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ClientService clientService;

    private IdmGetRolesService idmGetRolesService;

    private User support;
    private User teamLeader;
    private User developer;

    @Before
    public void setUp() {
        support = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.SUPPORT)
                .getChiefUserInfo().getUser();

        teamLeader = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo().getUser();
        clientService.updateClientRole(teamLeader.getClientId(), RbacRole.MANAGER, RbacSubrole.TEAMLEADER);

        developer = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.SUPERREADER)
                .getChiefUserInfo().getUser();
        userService.setDeveloperFlag(developer.getUid(), true);

        idmGetRolesService = new IdmGetRolesService(shardHelper, clientRepository, userRepository);
    }

    @Test
    public void getUserRoles() {
        IdmUserRole expectedRole = idmUserRole(support, IdmRole.SUPPORT);
        getUserRolesAndCheck(support.getDomainLogin(), singletonList(expectedRole));
    }

    @Test
    public void getUserRoles_whenUserIsBlocked() {
        userService.blockUser(support.getClientId(), support.getUid());
        getUserRolesAndCheck(support.getDomainLogin(), emptyList());
    }

    @Test
    public void getUserRoles_whenUserIsDeveloper() {
        IdmUserRole expectedRole = idmUserRole(developer, IdmRole.DEVELOPER);
        getUserRolesAndCheck(developer.getDomainLogin(), singletonList(expectedRole));
    }

    @Test
    public void getUserRoles_whenUserIsTeamLeader() {
        List<IdmUserRole> expectedRoles = asList(
                idmUserRole(teamLeader, IdmRole.MANAGER),
                idmUserRole(teamLeader, IdmRole.TEAMLEADER));

        getUserRolesAndCheck(teamLeader.getDomainLogin(), expectedRoles);
    }

    @Test
    public void getAllRoles() {
        List<IdmUserRole> expectedRoles = asList(
                idmUserRole(support, IdmRole.SUPPORT),
                idmUserRole(teamLeader, IdmRole.MANAGER),
                idmUserRole(teamLeader, IdmRole.TEAMLEADER),
                idmUserRole(developer, IdmRole.DEVELOPER));

        List<IdmUserRole> actualRoles = filterList(idmGetRolesService.getAllRoles(),
                r -> Set.of(support.getUid(), teamLeader.getUid(), developer.getUid()).contains(r.getUid()));

        assertThat(actualRoles, containsInAnyOrder(mapList(expectedRoles, BeanDifferMatcher::beanDiffer)));
    }

    private void getUserRolesAndCheck(String domainLogin, List<IdmUserRole> expectedRoles) {
        List<IdmUserRole> actualRoles = idmGetRolesService.getUserRoles(domainLogin);
        assertThat(actualRoles, containsInAnyOrder(mapList(expectedRoles, BeanDifferMatcher::beanDiffer)));
    }

    private static IdmUserRole idmUserRole(User user, IdmRole role) {
        return new IdmUserRole()
                .withDomainLogin(user.getDomainLogin().toLowerCase())
                .withPassportLogin(user.getLogin().toLowerCase())
                .withUid(user.getUid())
                .withRole(role);
    }
}
