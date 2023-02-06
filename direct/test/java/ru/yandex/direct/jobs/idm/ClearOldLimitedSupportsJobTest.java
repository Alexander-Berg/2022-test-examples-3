package ru.yandex.direct.jobs.idm;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacRole;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.direct.common.db.PpcPropertyNames.ENABLE_CLEAR_OLD_LIMITED_SUPPORTS_JOB;


@JobsTest
@ExtendWith(SpringExtension.class)
class ClearOldLimitedSupportsJobTest {
    @Autowired
    private Steps steps;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private RbacClientsRelations rbacClientsRelations;
    @Autowired
    private ClientService clientService;
    @Autowired
    private UserService userService;

    private ClearOldLimitedSupportsJob job;
    private PpcProperty<Boolean> enableClearOldLimitedSupportsJobProperty;
    private ClientInfo limitedSupport;

    @BeforeEach
    void before() {
        limitedSupport = createOperatorWithRole(RbacRole.LIMITED_SUPPORT);
        int shard = limitedSupport.getShard();

        job = new ClearOldLimitedSupportsJob(shard, ppcPropertiesSupport,
                clientRepository, rbacClientsRelations, clientService, userService);

        enableClearOldLimitedSupportsJobProperty = ppcPropertiesSupport.get(ENABLE_CLEAR_OLD_LIMITED_SUPPORTS_JOB);
        enableClearOldLimitedSupportsJobProperty.set(true);
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    @Test
    void clearOldLimitedSupports_success() {
        blockUser(limitedSupport.getClientId(), limitedSupport.getUid());
        executeJob();
        checkRole(limitedSupport.getClientId(), RbacRole.EMPTY);
        checkStatusBlocked(limitedSupport.getUid(), false);
    }

    @Test
    void clearOldLimitedSupports_whenStatusBlockedIsNo() {
        executeJob();
        checkRole(limitedSupport.getClientId(), RbacRole.LIMITED_SUPPORT);
        checkStatusBlocked(limitedSupport.getUid(), false);
    }

    @Test
    void clearOldLimitedSupports_whenHasSupportRelations() {
        blockUser(limitedSupport.getClientId(), limitedSupport.getUid());

        ClientInfo client = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addSupportRelation(client.getClientId(), limitedSupport.getClientId());

        executeJob();
        checkRole(limitedSupport.getClientId(), RbacRole.LIMITED_SUPPORT);
        checkStatusBlocked(limitedSupport.getUid(), true);
    }

    @Test
    void clearOldLimitedSupports_whenRoleIsNotLimitedSupport() {
        ClientInfo placer = createOperatorWithRole(RbacRole.PLACER);
        blockUser(placer.getClientId(), placer.getUid());

        executeJob();
        checkRole(placer.getClientId(), RbacRole.PLACER);
        checkStatusBlocked(placer.getUid(), true);
    }

    @Test
    void clearOldLimitedSupports_whenJobIsNotEnabled() {
        blockUser(limitedSupport.getClientId(), limitedSupport.getUid());
        enableClearOldLimitedSupportsJobProperty.set(false);

        executeJob();
        checkRole(limitedSupport.getClientId(), RbacRole.LIMITED_SUPPORT);
        checkStatusBlocked(limitedSupport.getUid(), true);
    }

    @Test
    void clearOldLimitedSupports_whenTwoLimitedSupports() {
        ClientInfo anotherLimitedSupport = createOperatorWithRole(RbacRole.LIMITED_SUPPORT);

        blockUser(limitedSupport.getClientId(), limitedSupport.getUid());
        blockUser(anotherLimitedSupport.getClientId(), anotherLimitedSupport.getUid());

        executeJob();

        checkRole(limitedSupport.getClientId(), RbacRole.EMPTY);
        checkStatusBlocked(limitedSupport.getUid(), false);

        checkRole(anotherLimitedSupport.getClientId(), RbacRole.EMPTY);
        checkStatusBlocked(anotherLimitedSupport.getUid(), false);
    }

    private void checkRole(ClientId clientId, RbacRole expectedRole) {
        Client client = checkNotNull(clientService.getClient(clientId));
        Assertions.assertThat(client.getRole())
                .as("role")
                .isEqualTo(expectedRole);
    }

    private void checkStatusBlocked(Long uid, Boolean expectedStatusBlocked) {
        User user = checkNotNull(userService.getUser(uid));
        Assertions.assertThat(user.getStatusBlocked())
                .as("statusBlocked")
                .isEqualTo(expectedStatusBlocked);
    }

    private ClientInfo createOperatorWithRole(RbacRole role) {
        return steps.clientSteps().createDefaultClientWithRole(role);
    }

    private void blockUser(ClientId clientId, Long userId) {
        userService.blockUser(clientId, userId);
    }
}
