package ru.yandex.direct.intapi.entity.idm.service;

import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.IdmSupportForClientRole;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.model.SupportClientRelation;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGetSupportForClientRolesTest {

    @Autowired
    private Steps steps;
    @Autowired
    private RbacClientsRelations rbacClientsRelations;
    @Autowired
    private IdmSupportForClientService idmSupportForClientService;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private User operator1;
    private User operator2;
    private ClientInfo operator3;
    private ClientId clientId1;
    private ClientId clientId2;

    @Before
    public void setUp() {
        operator1 = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.LIMITED_SUPPORT)
                .getChiefUserInfo().getUser();
        operator2 = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.LIMITED_SUPPORT)
                .getChiefUserInfo().getUser();
        operator3 = steps.clientSteps().createDefaultClient();

        clientId1 = steps.clientSteps().createDefaultClient().getClientId();
        clientId2 = steps.clientSteps().createDefaultClient().getClientId();

        rbacClientsRelations.addSupportRelation(clientId1, operator1.getClientId());
        rbacClientsRelations.addSupportRelation(clientId1, operator2.getClientId());
        rbacClientsRelations.addSupportRelation(clientId1, operator3.getClientId());
        rbacClientsRelations.addSupportRelation(clientId1, ClientId.fromLong(0L));
        rbacClientsRelations.addSupportRelation(clientId2, operator1.getClientId());
    }

    @Test
    public void getNextPageRoles_allRoles() {
        List<IdmSupportForClientRole> roles = idmSupportForClientService.getNextPageRoles(null, 100);
        List<IdmSupportForClientRole> filteredRoles = StreamEx.of(roles)
                .filter(role -> Set.of(clientId1, clientId2).contains(role.getSubjectClientId()))
                .toList();

        List<IdmSupportForClientRole> expectedRoles = List.of(
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(ClientId.fromLong(0L))
                        .withDomainLogin("-")
                        .withPassportLogin("-"),
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(operator1.getClientId())
                        .withDomainLogin(operator1.getDomainLogin().toLowerCase())
                        .withPassportLogin(operator1.getLogin().toLowerCase()),
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(operator2.getClientId())
                        .withDomainLogin(operator2.getDomainLogin().toLowerCase())
                        .withPassportLogin(operator2.getLogin().toLowerCase()),
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(operator3.getClientId())
                        .withDomainLogin("-")
                        .withPassportLogin(operator3.getLogin().toLowerCase()),
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId2)
                        .withSupportClientId(operator1.getClientId())
                        .withDomainLogin(operator1.getDomainLogin().toLowerCase())
                        .withPassportLogin(operator1.getLogin().toLowerCase())
        );
        MatcherAssert.assertThat(filteredRoles, beanDiffer(expectedRoles));
    }

    @Test
    public void getNextPageRoles_nextNRoles() {
        SupportClientRelation lastRelation = new SupportClientRelation()
                .withSubjectClientId(clientId1)
                .withSupportClientId(operator1.getClientId());
        List<IdmSupportForClientRole> roles = idmSupportForClientService.getNextPageRoles(lastRelation, 2);

        List<IdmSupportForClientRole> expectedRoles = List.of(
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(operator2.getClientId())
                        .withDomainLogin(operator2.getDomainLogin().toLowerCase())
                        .withPassportLogin(operator2.getLogin().toLowerCase()),
                new IdmSupportForClientRole()
                        .withSubjectClientId(clientId1)
                        .withSupportClientId(operator3.getClientId())
                        .withDomainLogin("-")
                        .withPassportLogin(operator3.getLogin().toLowerCase())
        );
        MatcherAssert.assertThat(roles, beanDiffer(expectedRoles));
    }
}
