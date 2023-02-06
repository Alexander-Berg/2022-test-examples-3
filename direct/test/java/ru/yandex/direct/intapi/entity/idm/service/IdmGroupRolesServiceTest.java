package ru.yandex.direct.intapi.entity.idm.service;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRepository;
import ru.yandex.direct.core.entity.idm.service.IdmGroupsRolesService;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.IdmGroupRoleName;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupRolesServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupRolesService idmGroupRolesService;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsRolesService idmGroupsRolesService;

    private IdmGroupRoleInfo idmGroupRoleInfo;
    private Long idmGroupId;

    @Before
    public void setUp() {
        idmGroupRoleInfo = steps.idmGroupSteps().createDefaultIdmGroupRole();
        idmGroupId = idmGroupRoleInfo.getIdmGroupId();
    }

    @Test
    public void addGroupRole_success() {
        ClientId clientId = steps.clientSteps().createDefaultClient().getClientId();
        addGroupRole(clientId, idmGroupId, true);
        checkGroupRole(clientId, idmGroupId);
    }

    @Test
    public void addGroupRole_success_whenClientRoleIsAgency() {
        ClientId clientId = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY)
                .getClientId();
        addGroupRole(clientId, idmGroupId, true);
        checkGroupRole(clientId, idmGroupId);
    }

    @Test
    public void addGroupRole_failure_whenClientRoleIsPlacer() {
        ClientId clientId = steps.clientSteps().createDefaultClientWithRole(RbacRole.PLACER)
                .getClientId();
        addGroupRole(clientId, idmGroupId, false);
        checkGroupRoleDoesNotExist(clientId, idmGroupId);
    }

    @Test
    public void addGroupRole_failure_whenClientDoesNotExist() {
        addGroupRole(ClientId.fromLong(-1L), idmGroupId, false);
    }

    @Test
    public void addGroupRole_failure_whenGroupIdIsNull() {
        ClientId clientId = steps.clientSteps().createDefaultClient().getClientId();
        addGroupRole(clientId, null, false);
    }

    @Test
    public void addGroupRole_success_whenIdmGroupDoesNotExist() {
        ClientId clientId = steps.clientSteps().createDefaultClient().getClientId();
        Long newIdmGroupId = 22L;

        addGroupRole(clientId, newIdmGroupId, true);
        checkGroupRole(clientId, newIdmGroupId);
        checkGroup(newIdmGroupId);
    }

    @Test
    public void removeGroupRole_success() {
        ClientId clientId = idmGroupRoleInfo.getClientInfo().getClientId();
        removeGroupRole(clientId, idmGroupId);
        checkGroupRoleDoesNotExist(clientId, idmGroupId);
    }

    @Test
    public void checkThereIsOnlyOneGroupRole() {
        // сейчас есть только одна групповая роль,
        // при добавлении новых групповых ролей или расширении ppcdict.idm_groups.role
        // нужно добавить соответствующую логику в IdmGroupRolesService

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(IdmGroupRoleName.values()).containsOnly(IdmGroupRoleName.MANAGER_FOR_CLIENT);
        softly.assertThat(IdmRequiredRole.values()).containsOnly(IdmRequiredRole.MANAGER);

        softly.assertAll();
    }

    private void addGroupRole(ClientId clientId, Long idmGroupId, boolean isSuccessful) {
        AddRoleRequest request = new AddRoleRequest()
                .withRole(IdmGroupRoleName.MANAGER_FOR_CLIENT.getTypedValue())
                .withClientId(clientId.asLong())
                .withGroupId(idmGroupId);

        IdmResponse response = idmGroupRolesService.addGroupRole(request);
        assumeThat(response.getCode(), is(isSuccessful ? 0 : 1));
    }

    private void removeGroupRole(ClientId clientId, Long idmGroupId) {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole(IdmGroupRoleName.MANAGER_FOR_CLIENT.getTypedValue())
                .withClientId(clientId.asLong())
                .withGroupId(idmGroupId);

        IdmResponse response = idmGroupRolesService.removeGroupRole(request);
        assumeThat(response.getCode(), is(0));
    }

    private void checkGroupRole(ClientId clientId, Long idmGroupId) {
        IdmGroupRole actualIdmGroupRole = idmGroupsRolesService.getRole(clientId, idmGroupId)
                .orElse(null);
        IdmGroupRole expectedIdmGroupRole = new IdmGroupRole()
                .withIdmGroupId(idmGroupId)
                .withClientId(clientId);
        assertThat(actualIdmGroupRole, beanDiffer(expectedIdmGroupRole));
    }

    private void checkGroupRoleDoesNotExist(ClientId clientId, Long idmGroupId) {
        Optional<IdmGroupRole> actualIdmGroupRole = idmGroupsRolesService.getRole(clientId, idmGroupId);
        Assertions.assertThat(actualIdmGroupRole).isNotPresent();
    }

    private void checkGroup(Long idmGroupId) {
        List<IdmGroup> actualIdmGroups = idmGroupsRepository.getGroups(singletonList(idmGroupId));
        IdmGroup expectedIdmGroup = new IdmGroup()
                .withIdmGroupId(idmGroupId)
                .withRequiredRole(IdmRequiredRole.MANAGER);
        assertThat(actualIdmGroups, contains(beanDiffer(expectedIdmGroup)));
    }
}
