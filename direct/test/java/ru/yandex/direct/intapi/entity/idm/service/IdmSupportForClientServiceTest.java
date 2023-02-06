package ru.yandex.direct.intapi.entity.idm.service;

import java.util.Map;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmRole;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.rbac.RbacClientsRelationsStorage;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.model.ClientsRelationType;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.idm.converter.IdmCommonNames.SUPPORT_FOR_CLIENT_ROLE_NAME;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmSupportForClientServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private IdmUserManagementIntapiService idmUserManagementIntapiService;
    @Autowired
    private RbacClientsRelationsStorage rbacClientsRelationsStorage;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;

    private User operator;
    private User placer;
    private ClientId agencyClientId;
    private ClientId clientClientId;

    @Before
    public void setUp() {
        operator = createOperatorWithRole(RbacRole.LIMITED_SUPPORT);
        placer = createOperatorWithRole(RbacRole.PLACER);
        clientClientId = steps.clientSteps().createDefaultClient().getClientId();
        agencyClientId = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY).getClientId();
    }

    @Test
    public void addRole_success() {
        addSupportForClientRole(operator, clientClientId, true);
        checkRelationExists(operator.getClientId(), clientClientId);
    }

    @Test
    public void addRole_success_whenClientRoleIsAgency() {
        addSupportForClientRole(operator, agencyClientId, true);
        checkRelationExists(operator.getClientId(), agencyClientId);
    }

    @Test
    public void addRole_success_whenRoleExists() {
        addSupportForClientRole(operator, clientClientId, true);
        addSupportForClientRole(operator, clientClientId, true);
        checkRelationExists(operator.getClientId(), clientClientId);
    }

    @Test
    public void addRole_failure_whenInvalidOperatorRole() {
        addSupportForClientRole(placer, clientClientId, false);
        checkRelationDoesNotExist(placer.getClientId(), clientClientId);
    }

    @Test
    public void removeRole_success() {
        addSupportForClientRole(operator, clientClientId, true);
        removeSupportForClientRole(operator, clientClientId);
        checkRelationDoesNotExist(operator.getClientId(), clientClientId);
    }

    @Test
    public void removeRole_success_whenRoleDoesNotExist() {
        removeSupportForClientRole(operator, clientClientId);
        checkRelationDoesNotExist(operator.getClientId(), clientClientId);
    }

    @Test
    public void removeRole_success_whenOperatorHasNoSupportForClientRole() {
        // Проверяем случай, когда роль "саппорт с ограниченным доступом"
        // отзывается после роли "саппорт с доступом к клиенту"
        addSupportForClientRole(operator, clientClientId, true);

        removeSupportForClientRole(operator, clientClientId);
        removeLimitedSupportRole(operator);

        // Ожидаем, что обе роли успешно отозваны
        checkRole(operator.getUid(), RbacRole.EMPTY);
        checkRelationDoesNotExist(operator.getClientId(), clientClientId);
    }

    @Test
    public void removeRole_blockUser_whenOperatorHasSupportForClientRole() {
        // Проверяем случай, когда роль "саппорт с ограниченным доступом"
        // отзывается перед ролью "саппорт с доступом к клиенту"
        addSupportForClientRole(operator, clientClientId, true);

        removeLimitedSupportRole(operator);

        // Ожидаем, что роль осталась
        checkRole(operator.getUid(), RbacRole.LIMITED_SUPPORT);
        // При этом пользователь должен быть заблокирован в интерфейсе
        User user = checkNotNull(userService.getUser(operator.getUid()));
        Assertions.assertThat(user.getStatusBlocked())
                .as("user.statusBlocked")
                .isEqualTo((Boolean) true);

        // Роль "саппорт с доступом к клиенту" сохраняется, её Idm должен отзывать отдельно
        checkRelationExists(operator.getClientId(), clientClientId);
    }

    private void addSupportForClientRole(User support, ClientId subjectClientId, boolean isSuccessful) {
        AddRoleRequest request = new AddRoleRequest()
                .withRole(SUPPORT_FOR_CLIENT_ROLE_NAME)
                .withPassportLogin(support.getLogin())
                .withDomainLogin(support.getDomainLogin())
                .withClientId(subjectClientId.asLong());

        IdmResponse response = idmUserManagementIntapiService.addRole(request);
        assumeThat(response.getCode(), is(isSuccessful ? 0 : 1));
    }

    private void removeSupportForClientRole(User support, ClientId subjectClientId) {
        removeRole(SUPPORT_FOR_CLIENT_ROLE_NAME, support, subjectClientId.asLong());
    }

    private void removeLimitedSupportRole(User support) {
        removeRole(IdmRole.LIMITED_SUPPORT.getTypedValue(), support, null);
    }

    private void removeRole(String role, User support, @Nullable Long clientId) {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole(role)
                .withPassportLogin(support.getLogin())
                .withDomainLogin(support.getDomainLogin())
                .withClientId(clientId);

        IdmResponse response = idmUserManagementIntapiService.removeRole(request);
        assumeThat(response.getCode(), is(0));
    }

    private void checkRelationExists(ClientId operatorClientId, ClientId subjectClientId) {
        Long relationId = getRelationId(operatorClientId, subjectClientId);
        assertThat(relationId, notNullValue());
    }

    private void checkRelationDoesNotExist(ClientId operatorClientId, ClientId subjectClientId) {
        Long relationId = getRelationId(operatorClientId, subjectClientId);
        assertThat(relationId, nullValue());
    }

    private Long getRelationId(ClientId operatorClientId, ClientId subjectClientId) {
        int shard = shardHelper.getShardByClientIdStrictly(subjectClientId);
        return rbacClientsRelationsStorage.getRelationId(shard, operatorClientId, subjectClientId,
                ClientsRelationType.SUPPORT_FOR_CLIENT);
    }

    private User createOperatorWithRole(RbacRole role) {
        return steps.clientSteps().createDefaultClientWithRoleInAnotherShard(role).getChiefUserInfo().getUser();
    }

    private void checkRole(Long uid, RbacRole expectedRole) {
        Map<Long, Client> clientMap = clientService.massGetClientsByUids(singleton(uid));
        Client client = clientMap.get(uid);
        Assertions.assertThat(client.getRole()).as("role").isEqualTo(expectedRole);
    }
}
