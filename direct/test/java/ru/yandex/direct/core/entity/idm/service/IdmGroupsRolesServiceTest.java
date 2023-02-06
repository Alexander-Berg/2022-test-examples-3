package ru.yandex.direct.core.entity.idm.service;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRolesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupsRolesServiceTest {

    private static final long IDM_GROUP_ID = 11L;

    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRolesRepository idmGroupsRolesRepository;
    @Autowired
    private IdmGroupsRolesService idmGroupsRolesService;

    private IdmGroupRole startGroupRole;
    private Integer shard;
    private ClientId startClientId;

    @Before
    public void setUp() {
        ClientInfo startClientInfo = steps.clientSteps().createDefaultClient();
        IdmGroup startIdmGroup = steps.idmGroupSteps().addIfNotExistIdmGroup(IDM_GROUP_ID, IdmRequiredRole.MANAGER);
        startClientId = startClientInfo.getClientId();
        startGroupRole = new IdmGroupRole()
                .withClientId(startClientId)
                .withIdmGroupId(startIdmGroup.getIdmGroupId());
        shard = startClientInfo.getShard();
        idmGroupsRolesRepository.addRolesWhichNotExist(shard, singletonList(startGroupRole));
    }

    @Test
    public void addRolesWhichNotExist_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        IdmGroupRole newRole = new IdmGroupRole()
                .withClientId(clientInfo.getClientId())
                .withIdmGroupId(IDM_GROUP_ID);

        idmGroupsRolesService.addRolesWhichNotExist(asList(startGroupRole, newRole));

        List<IdmGroupRole> allRoles = idmGroupsRolesRepository.getAllRoles(shard);
        assertThat(allRoles).contains(startGroupRole, newRole);
    }

    @Test
    public void removeRoles_success() {
        idmGroupsRolesService.removeRoles(singletonList(startGroupRole));

        List<IdmGroupRole> allRoles = idmGroupsRolesService.getAllRoles();
        assertThat(allRoles).doesNotContain(startGroupRole);
    }

    @Test
    public void removeRole_success() {
        idmGroupsRolesService.removeRole(startClientId, IDM_GROUP_ID);

        List<IdmGroupRole> allRoles = idmGroupsRolesService.getAllRoles();
        assertThat(allRoles).doesNotContain(startGroupRole);
    }

    @Test
    public void getAllRoles_success() {
        List<IdmGroupRole> allRoles = idmGroupsRolesService.getAllRoles();
        assertThat(allRoles).contains(startGroupRole);
    }

    @Test
    public void getRole_success() {
        IdmGroupRole role = idmGroupsRolesService.getRole(startClientId, IDM_GROUP_ID).orElse(null);
        assertThat(role).isEqualTo(startGroupRole);
    }

    @Test
    public void getNextPageGroupRoles_success() {
        long secondIdmGroupId = 88L;
        IdmGroup secondIdmGroup = steps.idmGroupSteps().addIfNotExistIdmGroup(secondIdmGroupId,
                IdmRequiredRole.MANAGER);
        IdmGroupRoleInfo secondIdmGroupRoleInfo =
                steps.idmGroupSteps().addIdmGroupRole(
                        new IdmGroupRoleInfo()
                                .withClientInfo(
                                        steps.clientSteps()
                                                .createDefaultClientAnotherShard())
                                .withIdmGroup(secondIdmGroup));

        ArrayList<IdmGroupRole> groupRoles = new ArrayList<>();
        for (List<IdmGroupRole> roles = idmGroupsRolesService.getNextPageGroupRoles(startGroupRole,  1);
             isNotEmpty(roles);
             roles = idmGroupsRolesService.getNextPageGroupRoles(roles.get(0), 1)) {
            checkState(roles.size() == 1);
            groupRoles.addAll(roles);
        }

        assertThat(groupRoles)
                .as("all roles start from startGroupRole except for it")
                .contains(secondIdmGroupRoleInfo.getIdmGroupRole())
                .doesNotContain(startGroupRole);
    }

}
